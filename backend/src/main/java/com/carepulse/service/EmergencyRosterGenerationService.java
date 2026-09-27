package com.carepulse.service;

import com.carepulse.dto.EmergencyRosterResponseDTO;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.EmergencyDoctorRoster;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.InvalidEmergencyRosterException;
import com.carepulse.repository.AppointmentRepository;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.EmergencyDoctorRosterRepository;
import com.carepulse.repository.EmergencyRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for deterministic, explainable automatic generation of daily
 * emergency physician duty rosters with fair rotational workload distribution.
 */
@Service
public class EmergencyRosterGenerationService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyRosterGenerationService.class);

    private final EmergencyDoctorRosterRepository rosterRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmergencyRequestRepository requestRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Value("${emergency.morning.start:08:00}")
    private String morningStartStr;

    @Value("${emergency.morning.end:14:00}")
    private String morningEndStr;

    @Value("${emergency.evening.start:14:00}")
    private String eveningStartStr;

    @Value("${emergency.evening.end:20:00}")
    private String eveningEndStr;

    @Value("${emergency.night.start:20:00}")
    private String nightStartStr;

    @Value("${emergency.night.end:08:00}")
    private String nightEndStr;

    @Value("${emergency.roster.doctors-per-shift:1}")
    private int defaultDoctorsPerShift;

    public EmergencyRosterGenerationService(EmergencyDoctorRosterRepository rosterRepository,
                                           DoctorRepository doctorRepository,
                                           AppointmentRepository appointmentRepository,
                                           EmergencyRequestRepository requestRepository,
                                           AuditLogService auditLogService,
                                           NotificationService notificationService) {
        this.rosterRepository = rosterRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.requestRepository = requestRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    /**
     * Represents standard shift window definition.
     */
    public record ShiftDefinition(String name, LocalTime start, LocalTime end) {}

    /**
     * Generates a balanced, rotated daily emergency roster for the given date.
     *
     * Rotation Algorithm:
     * 1. Validates that target date is not in the past.
     * 2. Identifies all licensed, active physicians; filters out doctors currently ON_LEAVE.
     * 3. Calculates historical metrics per physician:
     *    - Total emergency shifts in previous 7 days (primary fairness factor).
     *    - Scheduled regular appointments on target date (workload conflict factor).
     *    - Previous night shift assignment (mandatory rest period: no morning shift directly after night shift).
     * 4. Calculates a deterministic score for each doctor:
     *    score = (past7DaysShifts * 100) + (appointmentCount * 10)
     *    Sorted ascending (fewest shifts + lowest conflict first, tie-broken deterministically by Doctor ID).
     * 5. Allocates physicians across standard shifts (MORNING, EVENING, NIGHT) avoiding intraday overlaps.
     * 6. Persists new roster entries and logs HIPAA-compliant audit trail.
     */
    @Transactional
    public List<EmergencyRosterResponseDTO> generateRosterForDate(LocalDate targetDate, Integer customDoctorsPerShift, String adminEmail) {
        if (targetDate == null) {
            throw new BadRequestException("Target roster date cannot be null");
        }
        if (targetDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot generate emergency roster for past date: " + targetDate);
        }

        // Retrieve active certified doctors
        List<Doctor> allDoctors = doctorRepository.findAll().stream()
                .filter(d -> d.getUser() != null && d.getUser().isActive() && "DOCTOR".equalsIgnoreCase(d.getUser().getRole()))
                .collect(Collectors.toList());

        if (allDoctors.isEmpty()) {
            throw new InvalidEmergencyRosterException("No active physicians available in the system.");
        }

        // Filter out physicians who are ON_LEAVE
        List<Doctor> eligibleDoctors = allDoctors.stream()
                .filter(d -> !"ON_LEAVE".equalsIgnoreCase(d.getAvailabilityStatus()))
                .collect(Collectors.toList());

        if (eligibleDoctors.isEmpty()) {
            throw new InvalidEmergencyRosterException("All physicians are currently marked as ON_LEAVE. Cannot generate roster.");
        }

        int doctorsPerShift = (customDoctorsPerShift != null && customDoctorsPerShift > 0)
                ? customDoctorsPerShift
                : (eligibleDoctors.size() >= 5 ? 2 : Math.max(1, defaultDoctorsPerShift));

        List<ShiftDefinition> shiftDefs = List.of(
                new ShiftDefinition("MORNING", parseTime(morningStartStr, LocalTime.of(8, 0)), parseTime(morningEndStr, LocalTime.of(14, 0))),
                new ShiftDefinition("EVENING", parseTime(eveningStartStr, LocalTime.of(14, 0)), parseTime(eveningEndStr, LocalTime.of(20, 0))),
                new ShiftDefinition("NIGHT", parseTime(nightStartStr, LocalTime.of(20, 0)), parseTime(nightEndStr, LocalTime.of(8, 0)))
        );

        // Fetch existing assignments for target date to prevent duplicate allocations
        List<EmergencyDoctorRoster> existingOnDate = rosterRepository.findByRosterDate(targetDate);
        Set<String> existingDocShiftPairs = existingOnDate.stream()
                .map(r -> r.getDoctor().getId() + "_" + r.getShiftName().toUpperCase())
                .collect(Collectors.toSet());

        // Check if any doctor worked the NIGHT shift on targetDate - 1 (mandatory rest for MORNING shift)
        LocalDate previousDay = targetDate.minusDays(1);
        List<EmergencyDoctorRoster> prevNightRosters = rosterRepository.findByRosterDateAndDutyStatus(previousDay, "EMERGENCY_DUTY")
                .stream()
                .filter(r -> "NIGHT".equalsIgnoreCase(r.getShiftName()))
                .collect(Collectors.toList());
        Set<Long> prevNightDoctorIds = prevNightRosters.stream()
                .map(r -> r.getDoctor().getId())
                .collect(Collectors.toSet());

        // Calculate past 7 days emergency shifts and regular appointments on target date for each doctor
        LocalDate historyStart = targetDate.minusDays(7);
        LocalDate historyEnd = targetDate.minusDays(1);

        Map<Long, Long> pastDutyCounts = new HashMap<>();
        Map<Long, Long> appointmentCounts = new HashMap<>();

        for (Doctor doc : eligibleDoctors) {
            long pastCount = rosterRepository.countByDoctorIdAndRosterDateBetween(doc.getId(), historyStart, historyEnd);
            pastDutyCounts.put(doc.getId(), pastCount);

            long apptCount = appointmentRepository.countByDoctorIdAndAppointmentDate(doc.getId(), targetDate);
            appointmentCounts.put(doc.getId(), apptCount);
        }

        // Track doctors assigned today across all shifts during this generation run
        Map<Long, Integer> intradayAssignmentCount = new HashMap<>();
        for (EmergencyDoctorRoster ex : existingOnDate) {
            intradayAssignmentCount.put(ex.getDoctor().getId(),
                    intradayAssignmentCount.getOrDefault(ex.getDoctor().getId(), 0) + 1);
        }

        List<EmergencyDoctorRoster> newlyCreatedRosters = new ArrayList<>();

        // Allocate each shift sequentially
        for (ShiftDefinition shift : shiftDefs) {
            long existingForThisShift = existingOnDate.stream()
                    .filter(r -> r.getShiftName().equalsIgnoreCase(shift.name()))
                    .count();
            int neededForThisShift = (int) Math.max(0, doctorsPerShift - existingForThisShift);

            if (neededForThisShift <= 0) {
                log.info("Shift {} on {} already has {} doctors assigned (needed: {}). Skipping.",
                        shift.name(), targetDate, existingForThisShift, doctorsPerShift);
                continue;
            }

            // Rank eligible candidates specifically for this shift
            List<Doctor> candidatesForShift = eligibleDoctors.stream()
                    .filter(doc -> {
                        // Check if doctor is already assigned to this exact shift
                        if (existingDocShiftPairs.contains(doc.getId() + "_" + shift.name())) {
                            return false;
                        }
                        // Rest rule: No MORNING shift right after previous day's NIGHT shift
                        if ("MORNING".equalsIgnoreCase(shift.name()) && prevNightDoctorIds.contains(doc.getId())) {
                            return false;
                        }
                        return true;
                    })
                    .sorted((d1, d2) -> {
                        // Priority 1: Intraday fair distribution (prefer doctors not yet assigned on this day)
                        int intra1 = intradayAssignmentCount.getOrDefault(d1.getId(), 0);
                        int intra2 = intradayAssignmentCount.getOrDefault(d2.getId(), 0);
                        if (intra1 != intra2) {
                            return Integer.compare(intra1, intra2);
                        }

                        // Priority 2: Historical past 7 days shifts (prefer doctors with fewer recent shifts)
                        long past1 = pastDutyCounts.getOrDefault(d1.getId(), 0L);
                        long past2 = pastDutyCounts.getOrDefault(d2.getId(), 0L);
                        if (past1 != past2) {
                            return Long.compare(past1, past2);
                        }

                        // Priority 3: Lowest regular appointment workload on target date
                        long appt1 = appointmentCounts.getOrDefault(d1.getId(), 0L);
                        long appt2 = appointmentCounts.getOrDefault(d2.getId(), 0L);
                        if (appt1 != appt2) {
                            return Long.compare(appt1, appt2);
                        }

                        // Priority 4: Deterministic ID tiebreaker
                        return Long.compare(d1.getId(), d2.getId());
                    })
                    .collect(Collectors.toList());

            // Select up to neededForThisShift doctors
            int assignedCount = 0;
            for (Doctor chosenDoc : candidatesForShift) {
                if (assignedCount >= neededForThisShift) break;

                EmergencyDoctorRoster roster = new EmergencyDoctorRoster(
                        chosenDoc,
                        targetDate,
                        shift.name(),
                        shift.start(),
                        shift.end(),
                        "EMERGENCY_DUTY",
                        "AVAILABLE"
                );

                EmergencyDoctorRoster saved = rosterRepository.save(roster);
                newlyCreatedRosters.add(saved);

                // Update trackers
                existingDocShiftPairs.add(chosenDoc.getId() + "_" + shift.name());
                intradayAssignmentCount.put(chosenDoc.getId(),
                        intradayAssignmentCount.getOrDefault(chosenDoc.getId(), 0) + 1);
                pastDutyCounts.put(chosenDoc.getId(),
                        pastDutyCounts.getOrDefault(chosenDoc.getId(), 0L) + 1);

                // If generating for today, ensure doctor's base status is AVAILABLE if was previously OFF_DUTY
                if (targetDate.equals(LocalDate.now())) {
                    if ("OFF_DUTY".equalsIgnoreCase(chosenDoc.getAvailabilityStatus()) || chosenDoc.getAvailabilityStatus() == null) {
                        chosenDoc.setAvailabilityStatus("AVAILABLE");
                        doctorRepository.save(chosenDoc);
                    }
                }

                assignedCount++;
                log.info("Assigned Dr. {} to {} emergency duty on {} (Historical 7-day load: {})",
                        chosenDoc.getFullName(), shift.name(), targetDate, pastDutyCounts.get(chosenDoc.getId()));
            }
        }

        // Audit log
        String logDetails = String.format("Auto-generated %d emergency duty shifts for %s by %s",
                newlyCreatedRosters.size(), targetDate, adminEmail);
        auditLogService.log(
                adminEmail,
                "ROSTER_GENERATED",
                "EmergencyRoster:" + targetDate,
                logDetails
        );

        return newlyCreatedRosters.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private LocalTime parseTime(String str, LocalTime defaultTime) {
        if (str == null || str.isBlank()) return defaultTime;
        try {
            String clean = str.trim();
            if (clean.length() == 5) clean = clean + ":00";
            return LocalTime.parse(clean);
        } catch (Exception e) {
            return defaultTime;
        }
    }

    private EmergencyRosterResponseDTO mapToDTO(EmergencyDoctorRoster roster) {
        long activeCases = 0;
        if (roster.getDoctor() != null) {
            activeCases = requestRepository.countByAssignedDoctorAndStatusIn(
                    roster.getDoctor(),
                    List.of("ASSIGNED", "IN_PROGRESS")
            );
        }
        return EmergencyRosterResponseDTO.fromEntity(roster, activeCases);
    }
}
