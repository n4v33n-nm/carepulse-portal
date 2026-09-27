package com.carepulse.service;

import com.carepulse.dto.*;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.EmergencyDoctorRoster;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.InvalidEmergencyRosterException;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.EmergencyDoctorRosterRepository;
import com.carepulse.repository.EmergencyRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmergencyRosterService {

    private final EmergencyDoctorRosterRepository rosterRepository;
    private final DoctorRepository doctorRepository;
    private final EmergencyRequestRepository requestRepository;
    private final AuditLogService auditLogService;
    private final EmergencyRosterGenerationService rosterGenerationService;

    public EmergencyRosterService(EmergencyDoctorRosterRepository rosterRepository,
                                  DoctorRepository doctorRepository,
                                  EmergencyRequestRepository requestRepository,
                                  AuditLogService auditLogService,
                                  EmergencyRosterGenerationService rosterGenerationService) {
        this.rosterRepository = rosterRepository;
        this.doctorRepository = doctorRepository;
        this.requestRepository = requestRepository;
        this.auditLogService = auditLogService;
        this.rosterGenerationService = rosterGenerationService;
    }

    public List<EmergencyRosterResponseDTO> autoGenerateRoster(LocalDate targetDate, Integer customDoctorsPerShift, String adminEmail) {
        return rosterGenerationService.generateRosterForDate(targetDate, customDoctorsPerShift, adminEmail);
    }

    public List<EmergencyRosterResponseDTO> getRosterByDate(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<EmergencyDoctorRoster> rosters = rosterRepository.findByRosterDateOrderByShiftStartAsc(targetDate);
        return rosters.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public List<EmergencyRosterResponseDTO> getRosterByDateRange(LocalDate start, LocalDate end) {
        LocalDate startDate = start != null ? start : LocalDate.now();
        LocalDate endDate = end != null ? end : startDate.plusDays(7);
        List<EmergencyDoctorRoster> rosters = rosterRepository.findByRosterDateBetweenOrderByRosterDateAscShiftStartAsc(startDate, endDate);
        return rosters.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    /**
     * Retrieves actual doctors from PostgreSQL for the Admin emergency roster dropdown.
     * Excludes patients and inactive accounts.
     */
    public List<DoctorRosterOptionDTO> getDoctorsForRoster() {
        return doctorRepository.findAll().stream()
                .filter(d -> d.getUser() != null && d.getUser().isActive() && "DOCTOR".equalsIgnoreCase(d.getUser().getRole()))
                .sorted(Comparator.comparing(Doctor::getFullName))
                .map(DoctorRosterOptionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmergencyRosterResponseDTO createRosterEntry(EmergencyRosterRequestDTO req, String adminEmail) {
        if (req.getDoctorId() == null) {
            throw new BadRequestException("Doctor ID must be provided");
        }
        if (req.getRosterDate() == null) {
            throw new BadRequestException("Roster date must be provided");
        }

        Doctor doctor = doctorRepository.findById(req.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + req.getDoctorId()));

        if (doctor.getUser() == null || !"DOCTOR".equalsIgnoreCase(doctor.getUser().getRole())) {
            throw new BadRequestException("Selected user is not a certified physician");
        }

        if ("ON_LEAVE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
            throw new InvalidEmergencyRosterException("Doctor " + doctor.getFullName() +
                    " is currently on leave and cannot be assigned to emergency duty");
        }

        String shiftName = req.getShiftName() != null ? req.getShiftName().trim().toUpperCase() : "MORNING";

        // Prevent duplicate assignment for the exact shift name on that date
        if (rosterRepository.existsByDoctorIdAndRosterDateAndShiftName(doctor.getId(), req.getRosterDate(), shiftName)) {
            throw new InvalidEmergencyRosterException("Doctor " + doctor.getFullName() +
                    " is already assigned to the " + shiftName + " emergency shift on " + req.getRosterDate());
        }

        LocalTime shiftStart = req.getShiftStart();
        LocalTime shiftEnd = req.getShiftEnd();

        // Preset shifts defaults if not explicitly passed
        if (shiftStart == null || shiftEnd == null) {
            switch (shiftName) {
                case "EVENING" -> {
                    shiftStart = LocalTime.of(14, 0);
                    shiftEnd = LocalTime.of(20, 0);
                }
                case "NIGHT" -> {
                    shiftStart = LocalTime.of(20, 0);
                    shiftEnd = LocalTime.of(8, 0);
                }
                case "MORNING" -> {
                    shiftStart = LocalTime.of(8, 0);
                    shiftEnd = LocalTime.of(14, 0);
                }
                default -> {
                    if (shiftStart == null) shiftStart = LocalTime.of(8, 0);
                    if (shiftEnd == null) shiftEnd = LocalTime.of(14, 0);
                }
            }
        }

        // Check for overlapping shifts on the same date
        List<EmergencyDoctorRoster> existingRosters = rosterRepository.findByDoctorIdAndRosterDate(doctor.getId(), req.getRosterDate());
        for (EmergencyDoctorRoster existing : existingRosters) {
            if (shiftsOverlap(shiftStart, shiftEnd, existing.getShiftStart(), existing.getShiftEnd())) {
                throw new InvalidEmergencyRosterException("Doctor " + doctor.getFullName() +
                        " already has an overlapping emergency shift (" + existing.getShiftName() + " " +
                        existing.getShiftStart() + " - " + existing.getShiftEnd() + ") on " + req.getRosterDate());
            }
        }

        String dutyStatus = req.getDutyStatus() != null && !req.getDutyStatus().isBlank() ?
                req.getDutyStatus().trim().toUpperCase() : "EMERGENCY_DUTY";

        String availabilityStatus = req.getDoctorAvailabilityStatus() != null && !req.getDoctorAvailabilityStatus().isBlank() ?
                req.getDoctorAvailabilityStatus().trim().toUpperCase() : "AVAILABLE";

        EmergencyDoctorRoster roster = new EmergencyDoctorRoster(
                doctor,
                req.getRosterDate(),
                shiftName,
                shiftStart,
                shiftEnd,
                dutyStatus,
                availabilityStatus
        );

        EmergencyDoctorRoster saved = rosterRepository.save(roster);

        // If assigning for today, ensure doctor's base status is available if previously off-duty
        if (req.getRosterDate().equals(LocalDate.now())) {
            if ("OFF_DUTY".equalsIgnoreCase(doctor.getAvailabilityStatus()) || doctor.getAvailabilityStatus() == null) {
                doctor.setAvailabilityStatus("AVAILABLE");
                doctorRepository.save(doctor);
            }
        }

        auditLogService.log(
                adminEmail,
                "ADMIN_ASSIGNED_EMERGENCY_DUTY",
                "EmergencyRoster:" + saved.getId(),
                "Assigned Dr. " + doctor.getFullName() + " to " + shiftName + " (" + shiftStart + "-" + shiftEnd + ") on " + req.getRosterDate()
        );

        return mapToDTO(saved);
    }

    private boolean shiftsOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) return false;
        // Simple interval overlap for non-cross-midnight
        if (start1.isBefore(end1) && start2.isBefore(end2)) {
            return start1.isBefore(end2) && start2.isBefore(end1);
        }
        // Cross-midnight shifts overlap if either start or end falls within the other
        return true;
    }

    @Transactional
    public EmergencyRosterResponseDTO updateRosterEntry(Long id, EmergencyRosterRequestDTO req, String adminEmail) {
        EmergencyDoctorRoster roster = rosterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency roster entry not found with ID: " + id));

        if (req.getShiftName() != null) roster.setShiftName(req.getShiftName().toUpperCase());
        if (req.getShiftStart() != null) roster.setShiftStart(req.getShiftStart());
        if (req.getShiftEnd() != null) roster.setShiftEnd(req.getShiftEnd());
        if (req.getDutyStatus() != null) roster.setDutyStatus(req.getDutyStatus().toUpperCase());
        if (req.getDoctorAvailabilityStatus() != null) roster.setDoctorAvailabilityStatus(req.getDoctorAvailabilityStatus().toUpperCase());

        EmergencyDoctorRoster updated = rosterRepository.save(roster);

        auditLogService.log(
                adminEmail,
                "EMERGENCY_ROSTER_UPDATED",
                "EmergencyRoster:" + updated.getId(),
                "Updated roster entry for Dr. " + updated.getDoctor().getFullName()
        );

        return mapToDTO(updated);
    }

    @Transactional
    public void deleteRosterEntry(Long id, String adminEmail) {
        EmergencyDoctorRoster roster = rosterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Emergency roster entry not found with ID: " + id));

        String docName = roster.getDoctor().getFullName();
        LocalDate date = roster.getRosterDate();
        String shift = roster.getShiftName();

        rosterRepository.delete(roster);

        auditLogService.log(
                adminEmail,
                "ADMIN_REMOVED_EMERGENCY_DUTY",
                "EmergencyRoster:" + id,
                "Removed Dr. " + docName + " from " + shift + " shift on " + date
        );
    }

    /**
     * Structured summary for Doctor Dashboard: GET /api/doctor/emergency-duty/today
     * Requirement 13: Accurate backend-provided emergency caseload, duty shift, and status.
     */
    public DoctorTodayDutyDTO getDoctorTodayDutySummary(String doctorEmail) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for email: " + doctorEmail));

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalTime now = LocalTime.now();

        List<EmergencyDoctorRoster> rosters = rosterRepository.findTodayDutyForDoctor(doctor.getId(), today, yesterday);
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.atTime(LocalTime.MAX);

        long activeCases = requestRepository.countByAssignedDoctorAndStatusIn(
                doctor,
                List.of("ASSIGNED", "IN_PROGRESS")
        );
        long totalToday = requestRepository.countByAssignedDoctorAndRequestTimeBetween(doctor, dayStart, dayEnd);
        long completedToday = requestRepository.countByAssignedDoctorAndStatusAndRequestTimeBetween(doctor, "COMPLETED", dayStart, dayEnd);

        if (rosters.isEmpty()) {
            return new DoctorTodayDutyDTO(
                    false,
                    "NONE",
                    null,
                    null,
                    doctor.getAvailabilityStatus(),
                    activeCases,
                    totalToday,
                    completedToday,
                    activeCases,
                    null
            );
        }

        // Find active roster right now, or earliest scheduled for today
        EmergencyDoctorRoster activeRoster = rosters.stream()
                .filter(r -> r.isActiveAt(now))
                .findFirst()
                .orElse(rosters.get(0));

        DoctorTodayDutyDTO dto = new DoctorTodayDutyDTO(
                true,
                activeRoster.getShiftName(),
                activeRoster.getShiftStart() != null ? activeRoster.getShiftStart().toString().substring(0, 5) : null,
                activeRoster.getShiftEnd() != null ? activeRoster.getShiftEnd().toString().substring(0, 5) : null,
                activeRoster.getDoctorAvailabilityStatus(),
                activeCases,
                totalToday,
                completedToday,
                activeCases,
                activeRoster.getId()
        );

        dto.setAllTodayShifts(rosters.stream().map(this::mapToDTO).collect(Collectors.toList()));
        return dto;
    }

    public List<EmergencyRosterResponseDTO> getDoctorTodayDuty(String doctorEmail) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for email: " + doctorEmail));

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<EmergencyDoctorRoster> rosters = rosterRepository.findTodayDutyForDoctor(doctor.getId(), today, yesterday);
        return rosters.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public Doctor updateDoctorStatus(String doctorEmail, DoctorStatusUpdateRequestDTO req) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for email: " + doctorEmail));

        String newStatus = req.getAvailabilityStatus().trim().toUpperCase();

        // Requirement 6: Backend-controlled validation. Doctor cannot force AVAILABLE if handling active emergency cases
        if ("AVAILABLE".equalsIgnoreCase(newStatus)) {
            long activeCases = requestRepository.countByAssignedDoctorAndStatusIn(
                    doctor,
                    List.of("ASSIGNED", "IN_PROGRESS")
            );
            if (activeCases > 0) {
                throw new BadRequestException("Cannot set status to AVAILABLE while handling " + activeCases +
                        " active emergency case(s). Please complete your active emergency consultations first.");
            }
        }

        doctor.setAvailabilityStatus(newStatus);
        doctorRepository.save(doctor);

        // Also sync today's active emergency duty rosters for this doctor
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        List<EmergencyDoctorRoster> todayRosters = rosterRepository.findTodayDutyForDoctor(doctor.getId(), today, yesterday);
        for (EmergencyDoctorRoster r : todayRosters) {
            r.setDoctorAvailabilityStatus(newStatus);
            rosterRepository.save(r);
        }

        auditLogService.log(
                doctorEmail,
                "DOCTOR_AVAILABILITY_STATUS_UPDATED",
                "Doctor:" + doctor.getId(),
                "Doctor " + doctor.getFullName() + " updated availability status to " + newStatus
        );

        return doctor;
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
