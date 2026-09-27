package com.carepulse.service;

import com.carepulse.dto.DoctorStatusUpdateRequestDTO;
import com.carepulse.dto.EmergencyRosterRequestDTO;
import com.carepulse.dto.EmergencyRosterResponseDTO;
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
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmergencyRosterService {

    private final EmergencyDoctorRosterRepository rosterRepository;
    private final DoctorRepository doctorRepository;
    private final EmergencyRequestRepository requestRepository;
    private final AuditLogService auditLogService;

    public EmergencyRosterService(EmergencyDoctorRosterRepository rosterRepository,
                                  DoctorRepository doctorRepository,
                                  EmergencyRequestRepository requestRepository,
                                  AuditLogService auditLogService) {
        this.rosterRepository = rosterRepository;
        this.doctorRepository = doctorRepository;
        this.requestRepository = requestRepository;
        this.auditLogService = auditLogService;
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

        String shiftName = req.getShiftName() != null ? req.getShiftName().toUpperCase() : "MORNING";

        // Prevent duplicate roster assignments for the same date and shift
        if (rosterRepository.existsByDoctorIdAndRosterDateAndShiftName(doctor.getId(), req.getRosterDate(), shiftName)) {
            throw new InvalidEmergencyRosterException("Doctor " + doctor.getFullName() +
                    " is already assigned to the " + shiftName + " shift on " + req.getRosterDate());
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

        EmergencyDoctorRoster roster = new EmergencyDoctorRoster(
                doctor,
                req.getRosterDate(),
                shiftName,
                shiftStart,
                shiftEnd,
                req.getDutyStatus() != null ? req.getDutyStatus().toUpperCase() : "EMERGENCY_DUTY",
                req.getDoctorAvailabilityStatus() != null ? req.getDoctorAvailabilityStatus().toUpperCase() : "AVAILABLE"
        );

        EmergencyDoctorRoster saved = rosterRepository.save(roster);

        auditLogService.log(
                adminEmail,
                "EMERGENCY_ROSTER_CREATED",
                "EmergencyRoster:" + saved.getId(),
                "Assigned Dr. " + doctor.getFullName() + " to " + shiftName + " shift on " + req.getRosterDate()
        );

        return mapToDTO(saved);
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
                "EMERGENCY_ROSTER_DELETED",
                "EmergencyRoster:" + id,
                "Removed Dr. " + docName + " from " + shift + " shift on " + date
        );
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

        String newStatus = req.getAvailabilityStatus().toUpperCase();
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
