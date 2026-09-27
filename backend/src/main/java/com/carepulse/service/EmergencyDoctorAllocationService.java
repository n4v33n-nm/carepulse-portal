package com.carepulse.service;

import com.carepulse.entity.Doctor;
import com.carepulse.entity.EmergencyDoctorRoster;
import com.carepulse.entity.EmergencyRequest;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.EmergencyDoctorRosterRepository;
import com.carepulse.repository.EmergencyRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for deterministic, explainable allocation of available
 * emergency-duty doctors to incoming emergency requests with concurrency protection.
 */
@Service
public class EmergencyDoctorAllocationService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyDoctorAllocationService.class);

    private final EmergencyDoctorRosterRepository rosterRepository;
    private final EmergencyRequestRepository requestRepository;
    private final DoctorRepository doctorRepository;

    public EmergencyDoctorAllocationService(EmergencyDoctorRosterRepository rosterRepository,
                                            EmergencyRequestRepository requestRepository,
                                            DoctorRepository doctorRepository) {
        this.rosterRepository = rosterRepository;
        this.requestRepository = requestRepository;
        this.doctorRepository = doctorRepository;
    }

    /**
     * Allocates an available emergency-duty doctor using current system date and time.
     */
    @Transactional
    public EmergencyRequest allocateDoctor(EmergencyRequest request) {
        return allocateDoctor(request, LocalDate.now(), LocalTime.now());
    }

    /**
     * Deterministic doctor allocation algorithm with pessimistic concurrency protection:
     * 1. Query emergency rosters active on given date/time.
     * 2. Filter for EMERGENCY_DUTY and AVAILABLE status.
     * 3. Rank candidates by:
     *    a) Specialization match (if category matches doctor specialization)
     *    b) Lowest active emergency workload
     *    c) Deterministic tiebreaker (earliest assigned / doctor ID)
     * 4. Pessimistically lock candidate roster in database, verify still AVAILABLE.
     * 5. Atomically transition doctor and roster to IN_CONSULTATION.
     * 6. If no doctor available, set status to NO_DOCTOR_AVAILABLE.
     */
    @Transactional
    public EmergencyRequest allocateDoctor(EmergencyRequest request, LocalDate currentDate, LocalTime currentTime) {
        LocalDate yesterday = currentDate.minusDays(1);

        // Step 1 & 2: Find all rosters for today or cross-midnight shifts from yesterday with EMERGENCY_DUTY
        List<EmergencyDoctorRoster> activeDutyRosters = rosterRepository.findAvailableEmergencyRosters(currentDate, yesterday)
                .stream()
                .filter(r -> r.isActiveAt(currentTime))
                .filter(r -> "EMERGENCY_DUTY".equalsIgnoreCase(r.getDutyStatus()))
                .filter(r -> "AVAILABLE".equalsIgnoreCase(r.getDoctorAvailabilityStatus()))
                .collect(Collectors.toList());

        if (activeDutyRosters.isEmpty()) {
            log.warn("No active emergency-duty doctors found at date {} time {}", currentDate, currentTime);
            request.setStatus("NO_DOCTOR_AVAILABLE");
            return requestRepository.save(request);
        }

        // Step 3 & 4: Filter out doctors whose base doctor availability status is not AVAILABLE
        List<EmergencyDoctorRoster> availableCandidates = activeDutyRosters.stream()
                .filter(r -> {
                    Doctor doc = r.getDoctor();
                    return doc != null && "AVAILABLE".equalsIgnoreCase(doc.getAvailabilityStatus());
                })
                .collect(Collectors.toList());

        if (availableCandidates.isEmpty()) {
            log.warn("Emergency-duty doctors are on duty but currently busy/unavailable at {} {}", currentDate, currentTime);
            request.setStatus("NO_DOCTOR_AVAILABLE");
            return requestRepository.save(request);
        }

        // Step 5: Rank candidates deterministically
        String requestedCategory = request.getCategory() != null ? request.getCategory().trim() : "General";

        // Precompute active workload per doctor for explainability and deterministic sorting
        Map<Long, Long> doctorWorkloadMap = new HashMap<>();
        List<String> activeStatuses = List.of("ASSIGNED", "IN_PROGRESS");
        for (EmergencyDoctorRoster r : availableCandidates) {
            long activeCases = requestRepository.countByAssignedDoctorAndStatusIn(r.getDoctor(), activeStatuses);
            doctorWorkloadMap.put(r.getDoctor().getId(), activeCases);
        }

        List<EmergencyDoctorRoster> rankedCandidates = availableCandidates.stream()
                .sorted((r1, r2) -> {
                    Doctor d1 = r1.getDoctor();
                    Doctor d2 = r2.getDoctor();

                    // Criterion 1: Specialization match
                    boolean d1Matches = matchesSpecialization(d1.getSpecialization(), requestedCategory);
                    boolean d2Matches = matchesSpecialization(d2.getSpecialization(), requestedCategory);
                    if (d1Matches && !d2Matches) return -1;
                    if (!d1Matches && d2Matches) return 1;

                    // Criterion 2: Lowest current emergency workload
                    long load1 = doctorWorkloadMap.getOrDefault(d1.getId(), 0L);
                    long load2 = doctorWorkloadMap.getOrDefault(d2.getId(), 0L);
                    if (load1 != load2) {
                        return Long.compare(load1, load2);
                    }

                    // Criterion 3: Deterministic tiebreaker (Doctor ID)
                    return Long.compare(d1.getId(), d2.getId());
                })
                .collect(Collectors.toList());

        // Step 6: Atomic assignment with pessimistic write lock (Double-assignment protection)
        for (EmergencyDoctorRoster candidate : rankedCandidates) {
            Optional<EmergencyDoctorRoster> lockedRosterOpt = rosterRepository.findByIdWithLock(candidate.getId());
            if (lockedRosterOpt.isPresent()) {
                EmergencyDoctorRoster lockedRoster = lockedRosterOpt.get();
                Doctor doctor = lockedRoster.getDoctor();

                // Re-verify under lock
                if ("AVAILABLE".equalsIgnoreCase(lockedRoster.getDoctorAvailabilityStatus())
                        && "EMERGENCY_DUTY".equalsIgnoreCase(lockedRoster.getDutyStatus())
                        && lockedRoster.isActiveAt(currentTime)
                        && "AVAILABLE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {

                    // Assign doctor
                    request.setAssignedDoctor(doctor);
                    request.setStatus("ASSIGNED");
                    request.setAssignedTime(LocalDateTime.now());

                    // Transition statuses to IN_CONSULTATION to block duplicate allocations
                    lockedRoster.setDoctorAvailabilityStatus("IN_CONSULTATION");
                    rosterRepository.save(lockedRoster);

                    doctor.setAvailabilityStatus("IN_CONSULTATION");
                    doctorRepository.save(doctor);

                    log.info("Successfully allocated Doctor {} (Specialization: {}) to Emergency Request #{}",
                            doctor.getFullName(), doctor.getSpecialization(), request.getId());

                    return requestRepository.save(request);
                } else {
                    log.info("Candidate Doctor {} was claimed concurrently or is no longer available. Checking next candidate...",
                            doctor.getFullName());
                }
            }
        }

        // If all candidates failed lock/availability check
        log.warn("All candidate doctors were unavailable upon lock acquisition for request #{}", request.getId());
        request.setStatus("NO_DOCTOR_AVAILABLE");
        return requestRepository.save(request);
    }

    /**
     * Determines whether doctor's clinical specialization matches requested category.
     * Performs safe, case-insensitive substring matching.
     */
    public boolean matchesSpecialization(String doctorSpec, String category) {
        if (doctorSpec == null || category == null) return false;
        if ("General".equalsIgnoreCase(category) || "Other".equalsIgnoreCase(category)) {
            // General category is broadly acceptable
            return doctorSpec.toLowerCase().contains("general") || doctorSpec.toLowerCase().contains("internal");
        }
        return doctorSpec.toLowerCase().contains(category.toLowerCase()) ||
               category.toLowerCase().contains(doctorSpec.toLowerCase());
    }
}
