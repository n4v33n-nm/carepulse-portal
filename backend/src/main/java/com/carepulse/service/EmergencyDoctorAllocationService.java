package com.carepulse.service;

import com.carepulse.entity.Doctor;
import com.carepulse.entity.EmergencyDoctorRoster;
import com.carepulse.entity.EmergencyRequest;
import com.carepulse.repository.AppointmentRepository;
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
 * emergency-duty doctors to incoming emergency requests with concurrency protection,
 * multi-factor workload balancing, and waiting queue routing.
 */
@Service
public class EmergencyDoctorAllocationService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyDoctorAllocationService.class);

    private final EmergencyDoctorRosterRepository rosterRepository;
    private final EmergencyRequestRepository requestRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public EmergencyDoctorAllocationService(EmergencyDoctorRosterRepository rosterRepository,
                                            EmergencyRequestRepository requestRepository,
                                            DoctorRepository doctorRepository,
                                            AppointmentRepository appointmentRepository) {
        this.rosterRepository = rosterRepository;
        this.requestRepository = requestRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
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
     * 1. Query emergency rosters active on given date/time (including cross-midnight shifts).
     * 2. If NO doctors on emergency duty at all, transition request to NO_DOCTOR_AVAILABLE.
     * 3. Filter for AVAILABLE duty status and AVAILABLE doctor status, excluding doctors
     *    currently in consultation or handling an active appointment.
     * 4. If all on-duty emergency doctors are occupied/busy:
     *    Queue the request into WAITING status (Requirement 8) with priority (Requirement 9).
     * 5. Rank available candidates deterministically using multi-factor criteria:
     *    a) Specialization match (Cardiology, Dermatology, General, etc.)
     *    b) Lowest active emergency workload (ASSIGNED / IN_PROGRESS cases)
     *    c) Lowest total emergency assignments today
     *    d) Lowest regular appointment workload today
     *    e) Deterministic tiebreaker (Doctor ID)
     * 6. Acquire pessimistic write lock on candidate roster, verify still AVAILABLE.
     * 7. Atomically transition doctor and roster to BUSY (Requirement 6).
     * 8. If candidate was claimed concurrently, safely fallback to next ranked candidate.
     */
    @Transactional
    public EmergencyRequest allocateDoctor(EmergencyRequest request, LocalDate currentDate, LocalTime currentTime) {
        LocalDate yesterday = currentDate.minusDays(1);

        // Step 1: Find all active emergency-duty rosters for current time window (including cross-midnight)
        List<EmergencyDoctorRoster> activeDutyRosters = rosterRepository.findAvailableEmergencyRosters(currentDate, yesterday)
                .stream()
                .filter(r -> r.isActiveAt(currentDate, currentTime))
                .filter(r -> "EMERGENCY_DUTY".equalsIgnoreCase(r.getDutyStatus()))
                .collect(Collectors.toList());

        // Step 2: If no doctors are scheduled on emergency duty at all, set NO_DOCTOR_AVAILABLE
        if (activeDutyRosters.isEmpty()) {
            log.warn("No active emergency-duty physicians rostered at date {} time {}", currentDate, currentTime);
            request.setStatus("NO_DOCTOR_AVAILABLE");
            return requestRepository.save(request);
        }

        // Step 3: Filter for doctors whose current roster and base status are AVAILABLE
        // Also check if doctor is currently handling an active appointment (Requirement 7)
        List<EmergencyDoctorRoster> availableCandidates = activeDutyRosters.stream()
                .filter(r -> "AVAILABLE".equalsIgnoreCase(r.getDoctorAvailabilityStatus()))
                .filter(r -> {
                    Doctor doc = r.getDoctor();
                    if (doc == null || !"AVAILABLE".equalsIgnoreCase(doc.getAvailabilityStatus())) {
                        return false;
                    }
                    // Requirement 7: Normal appointment conflict check
                    // Check if doctor has an active appointment in consultation at current time
                    boolean hasActiveAppt = !appointmentRepository.findActiveAppointmentsForDoctorAtTime(
                            doc.getId(), currentDate, currentTime).isEmpty();
                    return !hasActiveAppt;
                })
                .collect(Collectors.toList());

        // Step 4: If all on-duty emergency doctors are busy, put request into WAITING queue (Requirement 8)
        if (availableCandidates.isEmpty()) {
            log.info("All emergency-duty physicians are currently occupied at {} {}. Placing request into WAITING queue.",
                    currentDate, currentTime);
            request.setStatus("WAITING");
            if (request.getPriority() == null || request.getPriority().isBlank()) {
                request.setPriority("NORMAL");
            }
            return requestRepository.save(request);
        }

        // Step 5: Rank candidates deterministically
        String requestedCategory = request.getCategory() != null ? request.getCategory().trim() : "General";

        // Precompute multi-factor workload metrics for explainable ranking
        Map<Long, Long> activeEmergencyLoad = new HashMap<>();
        Map<Long, Long> todayTotalEmergencyLoad = new HashMap<>();
        Map<Long, Long> todayAppointmentLoad = new HashMap<>();

        LocalDateTime dayStart = currentDate.atStartOfDay();
        LocalDateTime dayEnd = currentDate.atTime(LocalTime.MAX);
        List<String> activeStatuses = List.of("ASSIGNED", "IN_PROGRESS");

        for (EmergencyDoctorRoster r : availableCandidates) {
            Doctor doc = r.getDoctor();
            long activeCases = requestRepository.countByAssignedDoctorAndStatusIn(doc, activeStatuses);
            activeEmergencyLoad.put(doc.getId(), activeCases);

            long todayCases = requestRepository.countByAssignedDoctorAndRequestTimeBetween(doc, dayStart, dayEnd);
            todayTotalEmergencyLoad.put(doc.getId(), todayCases);

            long appts = appointmentRepository.countByDoctorIdAndAppointmentDate(doc.getId(), currentDate);
            todayAppointmentLoad.put(doc.getId(), appts);
        }

        List<EmergencyDoctorRoster> rankedCandidates = availableCandidates.stream()
                .sorted((r1, r2) -> {
                    Doctor d1 = r1.getDoctor();
                    Doctor d2 = r2.getDoctor();

                    // Factor 1: Specialization match preference
                    boolean d1Matches = matchesSpecialization(d1.getSpecialization(), requestedCategory);
                    boolean d2Matches = matchesSpecialization(d2.getSpecialization(), requestedCategory);
                    if (d1Matches && !d2Matches) return -1;
                    if (!d1Matches && d2Matches) return 1;

                    // Factor 2: Lowest active emergency caseload (ASSIGNED / IN_PROGRESS)
                    long load1 = activeEmergencyLoad.getOrDefault(d1.getId(), 0L);
                    long load2 = activeEmergencyLoad.getOrDefault(d2.getId(), 0L);
                    if (load1 != load2) {
                        return Long.compare(load1, load2);
                    }

                    // Factor 3: Lowest total emergency assignments today (fair daily workload)
                    long today1 = todayTotalEmergencyLoad.getOrDefault(d1.getId(), 0L);
                    long today2 = todayTotalEmergencyLoad.getOrDefault(d2.getId(), 0L);
                    if (today1 != today2) {
                        return Long.compare(today1, today2);
                    }

                    // Factor 4: Lowest regular appointments scheduled today
                    long appt1 = todayAppointmentLoad.getOrDefault(d1.getId(), 0L);
                    long appt2 = todayAppointmentLoad.getOrDefault(d2.getId(), 0L);
                    if (appt1 != appt2) {
                        return Long.compare(appt1, appt2);
                    }

                    // Factor 5: Deterministic tiebreaker (Doctor ID)
                    return Long.compare(d1.getId(), d2.getId());
                })
                .collect(Collectors.toList());

        // Step 6: Atomic assignment with pessimistic write lock (Double-assignment protection)
        for (EmergencyDoctorRoster candidate : rankedCandidates) {
            Optional<EmergencyDoctorRoster> lockedRosterOpt = rosterRepository.findByIdWithLock(candidate.getId());
            if (lockedRosterOpt.isPresent()) {
                EmergencyDoctorRoster lockedRoster = lockedRosterOpt.get();
                Doctor doctor = lockedRoster.getDoctor();

                // Re-verify under database write lock
                if ("AVAILABLE".equalsIgnoreCase(lockedRoster.getDoctorAvailabilityStatus())
                        && "EMERGENCY_DUTY".equalsIgnoreCase(lockedRoster.getDutyStatus())
                        && lockedRoster.isActiveAt(currentDate, currentTime)
                        && "AVAILABLE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {

                    // Assign doctor
                    request.setAssignedDoctor(doctor);
                    request.setStatus("ASSIGNED");
                    request.setAssignedTime(LocalDateTime.now());

                    // Transition statuses to BUSY as per Requirement 6 (AVAILABLE -> BUSY upon assignment)
                    lockedRoster.setDoctorAvailabilityStatus("BUSY");
                    rosterRepository.save(lockedRoster);

                    doctor.setAvailabilityStatus("BUSY");
                    doctorRepository.save(doctor);

                    List<EmergencyDoctorRoster> docTodayRosters = rosterRepository.findTodayDutyForDoctor(doctor.getId(), currentDate, yesterday);
                    for (EmergencyDoctorRoster tr : docTodayRosters) {
                        tr.setDoctorAvailabilityStatus("BUSY");
                        rosterRepository.save(tr);
                    }

                    log.info("Successfully allocated Doctor {} (Specialization: {}) to Emergency Request #{}",
                            doctor.getFullName(), doctor.getSpecialization(), request.getId());

                    return requestRepository.save(request);
                } else {
                    log.info("Candidate Doctor {} was claimed concurrently or is no longer available. Checking next candidate...",
                            doctor.getFullName());
                }
            }
        }

        // If all candidates failed lock check concurrently, queue the request
        log.warn("All candidate doctors were claimed concurrently. Queuing Emergency Request #{}", request.getId());
        request.setStatus("WAITING");
        if (request.getPriority() == null || request.getPriority().isBlank()) {
            request.setPriority("NORMAL");
        }
        return requestRepository.save(request);
    }

    /**
     * Attempts to assign the highest-priority waiting emergency request to the given doctor.
     * Called when a doctor completes a consultation or transitions back to AVAILABLE.
     */
    @Transactional
    public Optional<EmergencyRequest> assignWaitingRequestToDoctor(Doctor doctor, LocalDate currentDate, LocalTime currentTime) {
        if (doctor == null || !"AVAILABLE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
            return Optional.empty();
        }

        LocalDate yesterday = currentDate.minusDays(1);
        List<EmergencyDoctorRoster> activeDutyRosters = rosterRepository.findTodayDutyForDoctor(doctor.getId(), currentDate, yesterday)
                .stream()
                .filter(r -> r.isActiveAt(currentDate, currentTime))
                .filter(r -> "EMERGENCY_DUTY".equalsIgnoreCase(r.getDutyStatus()))
                .filter(r -> "AVAILABLE".equalsIgnoreCase(r.getDoctorAvailabilityStatus()))
                .collect(Collectors.toList());

        if (activeDutyRosters.isEmpty()) {
            return Optional.empty();
        }

        // Query waiting requests: URGENT priority first, then earliest requestTime (FIFO)
        List<EmergencyRequest> waitingRequests = requestRepository.findByStatusOrderByPriorityDescRequestTimeAsc("WAITING");
        if (waitingRequests.isEmpty()) {
            return Optional.empty();
        }

        // Prefer waiting request matching doctor's specialization if any, else pick top waiting request
        EmergencyRequest chosenRequest = waitingRequests.stream()
                .filter(req -> matchesSpecialization(doctor.getSpecialization(), req.getCategory()))
                .findFirst()
                .orElse(waitingRequests.get(0));

        // Lock roster and assign
        EmergencyDoctorRoster activeRoster = activeDutyRosters.get(0);
        Optional<EmergencyDoctorRoster> lockedOpt = rosterRepository.findByIdWithLock(activeRoster.getId());
        if (lockedOpt.isPresent()) {
            EmergencyDoctorRoster lockedRoster = lockedOpt.get();
            if ("AVAILABLE".equalsIgnoreCase(lockedRoster.getDoctorAvailabilityStatus())) {
                chosenRequest.setAssignedDoctor(doctor);
                chosenRequest.setStatus("ASSIGNED");
                chosenRequest.setAssignedTime(LocalDateTime.now());

                // Transition to BUSY
                lockedRoster.setDoctorAvailabilityStatus("BUSY");
                rosterRepository.save(lockedRoster);

                doctor.setAvailabilityStatus("BUSY");
                doctorRepository.save(doctor);

                for (EmergencyDoctorRoster r : activeDutyRosters) {
                    r.setDoctorAvailabilityStatus("BUSY");
                    rosterRepository.save(r);
                }

                EmergencyRequest saved = requestRepository.save(chosenRequest);
                log.info("Assigned waiting Emergency Request #{} to newly available Doctor {}",
                        saved.getId(), doctor.getFullName());
                return Optional.of(saved);
            }
        }

        return Optional.empty();
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
