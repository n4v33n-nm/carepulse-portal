package com.carepulse.service;

import com.carepulse.dto.*;
import com.carepulse.entity.*;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorLeaveService {

    private final DoctorLeaveRepository doctorLeaveRepository;
    private final AppointmentReassignmentRepository reassignmentRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public DoctorLeaveService(DoctorLeaveRepository doctorLeaveRepository,
                              AppointmentReassignmentRepository reassignmentRepository,
                              DoctorRepository doctorRepository,
                              AppointmentRepository appointmentRepository,
                              UserRepository userRepository,
                              DoctorAvailabilityRepository doctorAvailabilityRepository,
                              NotificationService notificationService,
                              AuditLogService auditLogService) {
        this.doctorLeaveRepository = doctorLeaveRepository;
        this.reassignmentRepository = reassignmentRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    /**
     * 1. Doctor submits a leave request
     */
    @Transactional
    public DoctorLeaveResponseDTO submitLeaveRequest(String doctorEmail, DoctorLeaveRequest request) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for email: " + doctorEmail));

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("Leave end date cannot be earlier than start date");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Leave start date cannot be in the past");
        }

        // Prevent duplicate overlapping active/pending leaves
        boolean hasOverlap = doctorLeaveRepository.hasOverlappingLeave(
                doctor.getId(), request.getStartDate(), request.getEndDate());
        if (hasOverlap) {
            throw new BadRequestException("You already have an active or pending leave request covering the specified date range");
        }

        DoctorLeave leave = new DoctorLeave(doctor, request.getStartDate(), request.getEndDate(), request.getReason().trim());
        DoctorLeave saved = doctorLeaveRepository.save(leave);

        auditLogService.log(doctorEmail, "LEAVE_REQUESTED", "DoctorLeave",
                "Submitted leave request from " + request.getStartDate() + " to " + request.getEndDate() + " (Reason: " + request.getReason() + ")");

        return mapToDTO(saved);
    }

    /**
     * 2. Doctor views their leave history
     */
    @Transactional(readOnly = true)
    public List<DoctorLeaveResponseDTO> getMyLeaves(String doctorEmail) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for email: " + doctorEmail));

        return doctorLeaveRepository.findByDoctorIdOrderByStartDateDesc(doctor.getId())
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 3. Admin views all leave requests with optional status filter
     */
    @Transactional(readOnly = true)
    public List<DoctorLeaveResponseDTO> getAllLeaves(String statusFilter) {
        List<DoctorLeave> leaves;
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(statusFilter)) {
            leaves = doctorLeaveRepository.findByStatusOrderByCreatedAtDesc(statusFilter.trim().toUpperCase());
        } else {
            leaves = doctorLeaveRepository.findAllByOrderByCreatedAtDesc();
        }

        return leaves.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 4. Retrieve single leave request with authorization check
     */
    @Transactional(readOnly = true)
    public DoctorLeaveResponseDTO getLeaveById(Long leaveId, String userEmail, String userRole) {
        DoctorLeave leave = doctorLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor leave request not found with ID: " + leaveId));

        if ("DOCTOR".equalsIgnoreCase(userRole)) {
            if (!leave.getDoctor().getUser().getEmail().equalsIgnoreCase(userEmail)) {
                throw new AccessDeniedException("You do not have permission to view this leave request");
            }
        }

        return mapToDTO(leave);
    }

    /**
     * 5. Admin reviews (approves/rejects) a leave request
     */
    @Transactional
    public DoctorLeaveResponseDTO reviewLeaveRequest(Long leaveId, String adminEmail, DoctorLeaveReviewRequest request) {
        DoctorLeave leave = doctorLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor leave request not found with ID: " + leaveId));

        if (!"PENDING".equalsIgnoreCase(leave.getStatus())) {
            throw new BadRequestException("Only PENDING leave requests can be reviewed. Current status: " + leave.getStatus());
        }

        User adminUser = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with email: " + adminEmail));

        String newStatus = request.getStatus().trim().toUpperCase();
        leave.setStatus(newStatus);
        leave.setAdminNotes(request.getAdminNotes());
        leave.setReviewedBy(adminUser);
        leave.setReviewedAt(LocalDateTime.now());

        DoctorLeave saved = doctorLeaveRepository.save(leave);

        Doctor doctor = leave.getDoctor();

        if ("APPROVED".equals(newStatus)) {
            LocalDate today = LocalDate.now();
            if (!today.isBefore(leave.getStartDate()) && !today.isAfter(leave.getEndDate())) {
                doctor.setAvailabilityStatus("ON_LEAVE");
                doctorRepository.save(doctor);
            }

            // Notify Doctor
            notificationService.createNotification(
                    doctor.getUser(),
                    "Leave Request Approved",
                    "Your leave request from " + leave.getStartDate() + " to " + leave.getEndDate() + " has been approved by admin.",
                    "LEAVE_APPROVED"
            );

            // Count affected appointments
            List<Appointment> affected = appointmentRepository.findAppointmentsForDoctorInDateRange(
                    doctor.getId(), leave.getStartDate(), leave.getEndDate(), Arrays.asList("PENDING", "CONFIRMED"));

            auditLogService.log(adminEmail, "LEAVE_APPROVED", "DoctorLeave",
                    "Approved leave #" + leaveId + " for Dr. " + doctor.getFullName() + ". Affected appointments: " + affected.size());
        } else {
            // Notify Doctor of rejection
            notificationService.createNotification(
                    doctor.getUser(),
                    "Leave Request Rejected",
                    "Your leave request from " + leave.getStartDate() + " to " + leave.getEndDate() + " was rejected. Reason/Notes: " +
                            (request.getAdminNotes() != null ? request.getAdminNotes() : "None provided"),
                    "LEAVE_REJECTED"
            );

            auditLogService.log(adminEmail, "LEAVE_REJECTED", "DoctorLeave",
                    "Rejected leave #" + leaveId + " for Dr. " + doctor.getFullName());
        }

        return mapToDTO(saved);
    }

    /**
     * 6. Admin inspects appointments affected by an approved or pending leave
     */
    @Transactional(readOnly = true)
    public List<AppointmentReassignmentDTO> getAffectedAppointments(Long leaveId) {
        DoctorLeave leave = doctorLeaveRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor leave request not found with ID: " + leaveId));

        List<Appointment> affectedAppointments = appointmentRepository.findAppointmentsForDoctorInDateRange(
                leave.getDoctor().getId(), leave.getStartDate(), leave.getEndDate(), Arrays.asList("PENDING", "CONFIRMED"));

        List<AppointmentReassignmentDTO> dtos = new ArrayList<>();
        for (Appointment appt : affectedAppointments) {
            Optional<AppointmentReassignment> activeReassignment =
                    reassignmentRepository.findFirstByAppointmentIdAndPatientConsentStatusOrderByReassignedAtDesc(
                            appt.getId(), "PROPOSED");

            if (activeReassignment.isPresent()) {
                dtos.add(mapReassignmentToDTO(activeReassignment.get()));
            } else {
                AppointmentReassignmentDTO dto = new AppointmentReassignmentDTO();
                dto.setAppointmentId(appt.getId());
                dto.setAppointmentDate(appt.getAppointmentDate());
                dto.setAppointmentTime(appt.getAppointmentTime());
                dto.setAppointmentReason(appt.getReason());
                dto.setAppointmentStatus(appt.getStatus());
                dto.setPatientId(appt.getPatient().getId());
                dto.setPatientName(appt.getPatient().getFullName());
                dto.setPatientEmail(appt.getPatient().getUser().getEmail());
                dto.setDoctorLeaveId(leave.getId());
                dto.setOriginalDoctorId(leave.getDoctor().getId());
                dto.setOriginalDoctorName(leave.getDoctor().getFullName());
                dto.setOriginalDoctorSpecialization(leave.getDoctor().getSpecialization());
                dto.setPatientConsentStatus("UNASSIGNED");
                dtos.add(dto);
            }
        }

        return dtos;
    }

    /**
     * 7. Find and score replacement doctor candidates for an affected appointment
     */
    @Transactional(readOnly = true)
    public List<ReplacementDoctorCandidateDTO> findReplacementDoctorCandidates(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        Doctor originalDoctor = appointment.getDoctor();
        String targetSpecialization = originalDoctor.getSpecialization();
        LocalDate apptDate = appointment.getAppointmentDate();
        LocalTime apptTime = appointment.getAppointmentTime();
        String dayOfWeek = apptDate.getDayOfWeek().name();

        List<Doctor> candidateDoctors = doctorRepository.findBySpecializationContainingIgnoreCase(targetSpecialization);

        List<ReplacementDoctorCandidateDTO> result = new ArrayList<>();

        for (Doctor candidate : candidateDoctors) {
            if (candidate.getId().equals(originalDoctor.getId())) {
                continue; // Cannot replace with the same doctor
            }

            ReplacementDoctorCandidateDTO dto = new ReplacementDoctorCandidateDTO();
            dto.setDoctorId(candidate.getId());
            dto.setDoctorName(candidate.getFullName());
            dto.setSpecialization(candidate.getSpecialization());
            dto.setExperienceYears(candidate.getExperienceYears());
            dto.setConsultationFee(candidate.getConsultationFee());

            // 1. Check if candidate is on approved leave on this date
            boolean onLeave = doctorLeaveRepository.isDoctorOnApprovedLeave(candidate.getId(), apptDate);
            dto.setOnApprovedLeave(onLeave);

            // 2. Check candidate working hours on this day of week
            List<DoctorAvailability> availList = doctorAvailabilityRepository.findByDoctorIdAndDayOfWeek(candidate.getId(), dayOfWeek);
            boolean availableOnDay = false;
            LocalTime shiftStart = null;
            LocalTime shiftEnd = null;

            if (!availList.isEmpty()) {
                DoctorAvailability da = availList.get(0);
                if (da.isAvailable()) {
                    shiftStart = da.getStartTime();
                    shiftEnd = da.getEndTime();
                    // Slot must fall within working hours and not in break
                    boolean withinHours = (apptTime.equals(shiftStart) || apptTime.isAfter(shiftStart)) &&
                            apptTime.plusMinutes(da.getSlotDurationMinutes()).isBefore(shiftEnd.plusSeconds(1));
                    boolean inBreak = false;
                    if (da.getBreakStartTime() != null && da.getBreakEndTime() != null) {
                        inBreak = (apptTime.equals(da.getBreakStartTime()) || apptTime.isAfter(da.getBreakStartTime())) &&
                                apptTime.isBefore(da.getBreakEndTime());
                    }
                    availableOnDay = withinHours && !inBreak;
                }
            }

            dto.setAvailableOnDay(availableOnDay);
            dto.setShiftStart(shiftStart);
            dto.setShiftEnd(shiftEnd);

            // 3. Check for conflicting active appointments at exact same date & time
            List<Appointment> conflicts = appointmentRepository.findActiveAppointmentsForDoctorAtTime(
                    candidate.getId(), apptDate, apptTime);
            boolean slotCollision = !conflicts.isEmpty();
            dto.setSlotCollision(slotCollision);

            // 4. Calculate existing daily appointment workload for load balancing
            long dailyLoad = appointmentRepository.countByDoctorIdAndAppointmentDate(candidate.getId(), apptDate);
            dto.setDayAppointmentsCount(dailyLoad);

            // Eligibility determination
            boolean eligible = !onLeave && availableOnDay && !slotCollision;
            dto.setRecommended(eligible);

            if (onLeave) {
                dto.setMatchExplanation("Unavailable: Physician is on approved leave on " + apptDate);
            } else if (!availableOnDay) {
                dto.setMatchExplanation("Unavailable: Not scheduled or slot is outside duty hours on " + dayOfWeek);
            } else if (slotCollision) {
                dto.setMatchExplanation("Unavailable: Conflict with existing booking at " + apptTime);
            } else {
                dto.setMatchExplanation("Eligible: Same specialization (" + candidate.getSpecialization() + "), on duty, zero schedule conflict. Current day load: " + dailyLoad + " appointments.");
            }

            result.add(dto);
        }

        // Sort: Recommended first, then by lowest existing daily appointment workload
        result.sort((a, b) -> {
            if (a.isRecommended() != b.isRecommended()) {
                return Boolean.compare(!a.isRecommended(), !b.isRecommended());
            }
            return Long.compare(a.getDayAppointmentsCount(), b.getDayAppointmentsCount());
        });

        return result;
    }

    /**
     * 8. Admin proposes a replacement doctor for an appointment
     */
    @Transactional
    public AppointmentReassignmentDTO proposeReplacementDoctor(Long appointmentId,
                                                               Long replacementDoctorId,
                                                               String adminEmail,
                                                               String adminNotes) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        Doctor originalDoctor = appointment.getDoctor();

        if (originalDoctor.getId().equals(replacementDoctorId)) {
            throw new BadRequestException("Replacement doctor cannot be the same as the original doctor");
        }

        Doctor replacementDoctor = doctorRepository.findById(replacementDoctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Replacement doctor not found with ID: " + replacementDoctorId));

        // Validate medical specialization match
        if (!originalDoctor.getSpecialization().trim().equalsIgnoreCase(replacementDoctor.getSpecialization().trim())) {
            throw new BadRequestException("Replacement doctor must have the same medical specialization (" +
                    originalDoctor.getSpecialization() + "). Selected doctor has: " + replacementDoctor.getSpecialization());
        }

        // Validate replacement doctor is not on leave
        if (doctorLeaveRepository.isDoctorOnApprovedLeave(replacementDoctor.getId(), appointment.getAppointmentDate())) {
            throw new BadRequestException("Selected replacement doctor is on approved leave on " + appointment.getAppointmentDate());
        }

        // Validate no slot collision
        List<Appointment> collisions = appointmentRepository.findActiveAppointmentsForDoctorAtTime(
                replacementDoctor.getId(), appointment.getAppointmentDate(), appointment.getAppointmentTime());
        if (!collisions.isEmpty()) {
            throw new BadRequestException("Selected replacement doctor already has an active appointment at " +
                    appointment.getAppointmentTime() + " on " + appointment.getAppointmentDate());
        }

        User adminUser = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found with email: " + adminEmail));

        // Associate with active leave if exists
        List<DoctorLeave> leaves = doctorLeaveRepository.findApprovedLeavesOnDate(originalDoctor.getId(), appointment.getAppointmentDate());
        DoctorLeave associatedLeave = leaves.isEmpty() ? null : leaves.get(0);

        // Cancel previous proposed reassignment if any
        reassignmentRepository.findFirstByAppointmentIdAndPatientConsentStatusOrderByReassignedAtDesc(
                appointment.getId(), "PROPOSED").ifPresent(prev -> {
            prev.setPatientConsentStatus("REJECTED");
            prev.setAdminNotes("Superseded by new proposed replacement");
            reassignmentRepository.save(prev);
        });

        AppointmentReassignment reassignment = new AppointmentReassignment(
                appointment, associatedLeave, originalDoctor, replacementDoctor, adminUser, adminNotes);

        AppointmentReassignment saved = reassignmentRepository.save(reassignment);

        // Notify patient with required consent prompt
        notificationService.createNotification(
                appointment.getPatient().getUser(),
                "Physician Reassignment Proposed",
                "Due to approved physician leave, Dr. " + replacementDoctor.getFullName() +
                        " (" + replacementDoctor.getSpecialization() + ") has been proposed for your appointment on " +
                        appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() +
                        ". Please review and confirm your consent in your appointments portal.",
                "REASSIGNMENT_PROPOSED"
        );

        auditLogService.log(adminEmail, "REASSIGNMENT_PROPOSED", "AppointmentReassignment",
                "Proposed Dr. " + replacementDoctor.getFullName() + " as replacement for Appointment #" + appointmentId +
                        " (Patient: " + appointment.getPatient().getFullName() + ")");

        return mapReassignmentToDTO(saved);
    }

    /**
     * 9. Patient accepts or rejects the proposed replacement doctor
     */
    @Transactional
    public AppointmentReassignmentDTO patientRespondToReassignment(Long reassignmentId,
                                                                   String patientEmail,
                                                                   PatientConsentRequest request) {
        AppointmentReassignment reassignment = reassignmentRepository.findById(reassignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment reassignment not found with ID: " + reassignmentId));

        Appointment appointment = reassignment.getAppointment();

        // Security check: only the assigned patient can respond
        if (!appointment.getPatient().getUser().getEmail().equalsIgnoreCase(patientEmail)) {
            throw new AccessDeniedException("You do not have permission to respond to this reassignment proposal");
        }

        if (!"PROPOSED".equalsIgnoreCase(reassignment.getPatientConsentStatus())) {
            throw new BadRequestException("This reassignment proposal has already been processed with status: " +
                    reassignment.getPatientConsentStatus());
        }

        String action = request.getAction().trim().toUpperCase();
        reassignment.setPatientConsentStatus(action.equals("ACCEPT") ? "ACCEPTED" : "REJECTED");
        reassignment.setPatientNotes(request.getPatientNotes());
        reassignment.setPatientRespondedAt(LocalDateTime.now());

        if ("ACCEPT".equals(action)) {
            // Apply doctor reassignment to the appointment
            Doctor replacementDoctor = reassignment.getReplacementDoctor();
            appointment.setDoctor(replacementDoctor);
            appointment.setStatus("CONFIRMED");
            appointmentRepository.save(appointment);

            // Notify Replacement Doctor (granting access to patient context)
            notificationService.createNotification(
                    replacementDoctor.getUser(),
                    "New Reassigned Consultation Confirmed",
                    "Patient " + appointment.getPatient().getFullName() + " accepted reassignment for consultation on " +
                            appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() +
                            ". You now have full authorized access to patient clinical records.",
                    "REASSIGNMENT_CONFIRMED"
            );

            // Notify Patient
            notificationService.createNotification(
                    appointment.getPatient().getUser(),
                    "Appointment Reassignment Confirmed",
                    "Your consultation with Dr. " + replacementDoctor.getFullName() + " on " +
                            appointment.getAppointmentDate() + " at " + appointment.getAppointmentTime() + " is confirmed.",
                    "APPOINTMENT_CONFIRMED"
            );

            auditLogService.log(patientEmail, "REASSIGNMENT_ACCEPTED", "AppointmentReassignment",
                    "Patient accepted replacement Dr. " + replacementDoctor.getFullName() + " for Appointment #" + appointment.getId());
        } else {
            // Rejection handled safely
            auditLogService.log(patientEmail, "REASSIGNMENT_REJECTED", "AppointmentReassignment",
                    "Patient declined replacement Dr. " + reassignment.getReplacementDoctor().getFullName() +
                            " for Appointment #" + appointment.getId() + ". Patient Notes: " + request.getPatientNotes());

            // Notify original doctor and admin
            notificationService.createNotification(
                    appointment.getPatient().getUser(),
                    "Reassignment Declined",
                    "You declined the proposed replacement doctor. You may reschedule your consultation with another available physician or cancel.",
                    "REASSIGNMENT_DECLINED"
            );
        }

        AppointmentReassignment saved = reassignmentRepository.save(reassignment);
        return mapReassignmentToDTO(saved);
    }

    /**
     * 10. Patient views their pending reassignments
     */
    @Transactional(readOnly = true)
    public List<AppointmentReassignmentDTO> getPatientPendingReassignments(String patientEmail) {
        User user = userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + patientEmail));

        return reassignmentRepository.findByAppointmentPatientIdAndPatientConsentStatusOrderByReassignedAtDesc(
                user.getId(), "PROPOSED")
                .stream()
                .map(this::mapReassignmentToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 11. Doctor views reassignments where they are original or replacement doctor
     */
    @Transactional(readOnly = true)
    public List<AppointmentReassignmentDTO> getDoctorReassignedHistory(String doctorEmail) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with email: " + doctorEmail));

        List<AppointmentReassignment> asReplacement = reassignmentRepository.findByReplacementDoctorIdOrderByReassignedAtDesc(doctor.getId());
        List<AppointmentReassignment> asOriginal = reassignmentRepository.findByOriginalDoctorIdOrderByReassignedAtDesc(doctor.getId());

        Set<Long> seenIds = new HashSet<>();
        List<AppointmentReassignmentDTO> result = new ArrayList<>();

        for (AppointmentReassignment r : asReplacement) {
            if (seenIds.add(r.getId())) {
                result.add(mapReassignmentToDTO(r));
            }
        }
        for (AppointmentReassignment r : asOriginal) {
            if (seenIds.add(r.getId())) {
                result.add(mapReassignmentToDTO(r));
            }
        }

        return result;
    }

    // Helper Mappers
    private DoctorLeaveResponseDTO mapToDTO(DoctorLeave leave) {
        DoctorLeaveResponseDTO dto = new DoctorLeaveResponseDTO();
        dto.setId(leave.getId());
        dto.setDoctorId(leave.getDoctor().getId());
        dto.setDoctorName(leave.getDoctor().getFullName());
        dto.setDoctorSpecialization(leave.getDoctor().getSpecialization());
        dto.setDoctorEmail(leave.getDoctor().getUser().getEmail());
        dto.setStartDate(leave.getStartDate());
        dto.setEndDate(leave.getEndDate());
        dto.setReason(leave.getReason());
        dto.setStatus(leave.getStatus());
        dto.setAdminNotes(leave.getAdminNotes());
        if (leave.getReviewedBy() != null) {
            dto.setReviewedById(leave.getReviewedBy().getId());
            dto.setReviewedByName(leave.getReviewedBy().getEmail());
        }
        dto.setReviewedAt(leave.getReviewedAt());
        dto.setCreatedAt(leave.getCreatedAt());

        // Count affected appointments
        List<Appointment> affected = appointmentRepository.findAppointmentsForDoctorInDateRange(
                leave.getDoctor().getId(), leave.getStartDate(), leave.getEndDate(), Arrays.asList("PENDING", "CONFIRMED"));
        dto.setAffectedAppointmentsCount(affected.size());

        return dto;
    }

    private AppointmentReassignmentDTO mapReassignmentToDTO(AppointmentReassignment r) {
        AppointmentReassignmentDTO dto = new AppointmentReassignmentDTO();
        dto.setId(r.getId());
        dto.setAppointmentId(r.getAppointment().getId());
        dto.setAppointmentDate(r.getAppointment().getAppointmentDate());
        dto.setAppointmentTime(r.getAppointment().getAppointmentTime());
        dto.setAppointmentReason(r.getAppointment().getReason());
        dto.setAppointmentStatus(r.getAppointment().getStatus());
        dto.setPatientId(r.getAppointment().getPatient().getId());
        dto.setPatientName(r.getAppointment().getPatient().getFullName());
        dto.setPatientEmail(r.getAppointment().getPatient().getUser().getEmail());

        if (r.getDoctorLeave() != null) {
            dto.setDoctorLeaveId(r.getDoctorLeave().getId());
        }

        dto.setOriginalDoctorId(r.getOriginalDoctor().getId());
        dto.setOriginalDoctorName(r.getOriginalDoctor().getFullName());
        dto.setOriginalDoctorSpecialization(r.getOriginalDoctor().getSpecialization());

        dto.setReplacementDoctorId(r.getReplacementDoctor().getId());
        dto.setReplacementDoctorName(r.getReplacementDoctor().getFullName());
        dto.setReplacementDoctorSpecialization(r.getReplacementDoctor().getSpecialization());

        if (r.getReassignedByAdmin() != null) {
            dto.setReassignedByAdminId(r.getReassignedByAdmin().getId());
            dto.setReassignedByAdminName(r.getReassignedByAdmin().getEmail());
        }

        dto.setPatientConsentStatus(r.getPatientConsentStatus());
        dto.setPatientNotes(r.getPatientNotes());
        dto.setAdminNotes(r.getAdminNotes());
        dto.setReassignedAt(r.getReassignedAt());
        dto.setPatientRespondedAt(r.getPatientRespondedAt());

        return dto;
    }
}
