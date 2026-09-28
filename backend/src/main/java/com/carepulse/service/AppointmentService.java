package com.carepulse.service;

import com.carepulse.dto.AppointmentRequest;
import com.carepulse.dto.AppointmentRescheduleRequestDTO;
import com.carepulse.dto.AppointmentStatusUpdateRequest;
import com.carepulse.entity.Appointment;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.DoctorAvailability;
import com.carepulse.entity.EmergencyDoctorRoster;
import com.carepulse.entity.Patient;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.AppointmentRepository;
import com.carepulse.repository.DoctorAvailabilityRepository;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.EmergencyDoctorRosterRepository;
import com.carepulse.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final CaregiverService caregiverService;
    private final AppointmentWaitlistService waitlistService;
    private final DoctorAvailabilityRepository doctorAvailabilityRepository;
    private final EmergencyDoctorRosterRepository emergencyRosterRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              NotificationService notificationService,
                              AuditLogService auditLogService,
                              CaregiverService caregiverService,
                              AppointmentWaitlistService waitlistService,
                              DoctorAvailabilityRepository doctorAvailabilityRepository,
                              EmergencyDoctorRosterRepository emergencyRosterRepository) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.caregiverService = caregiverService;
        this.waitlistService = waitlistService;
        this.doctorAvailabilityRepository = doctorAvailabilityRepository;
        this.emergencyRosterRepository = emergencyRosterRepository;
    }

    @Transactional
    public Appointment bookAppointment(String patientEmail, AppointmentRequest request) {
        if (request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot book an appointment for a past date");
        }
        if (request.getAppointmentDate().isEqual(LocalDate.now()) && request.getAppointmentTime().isBefore(java.time.LocalTime.now())) {
            throw new BadRequestException("Cannot book an appointment for a past time slot today");
        }

        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for email: " + patientEmail));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        if ("ON_LEAVE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
            throw new BadRequestException("Dr. " + doctor.getFullName() + " is currently on leave and unavailable for booking");
        }

        validateDoctorWorkingHours(doctor.getId(), doctor.getFullName(), request.getAppointmentDate(), request.getAppointmentTime());
        validateEmergencyDutyConflict(doctor.getId(), doctor.getFullName(), request.getAppointmentDate(), request.getAppointmentTime());

        // Double-booking check: Doctor
        List<Appointment> doctorConflicts = appointmentRepository.findActiveAppointmentsForDoctorAtTime(
                doctor.getId(), request.getAppointmentDate(), request.getAppointmentTime()
        );
        if (!doctorConflicts.isEmpty()) {
            throw new BadRequestException("The selected time slot (" + request.getAppointmentTime() + ") is no longer available with Dr. " + doctor.getFullName());
        }

        // Double-booking check: Patient
        List<Appointment> patientConflicts = appointmentRepository.findActiveAppointmentsForPatientAtTime(
                patient.getId(), request.getAppointmentDate(), request.getAppointmentTime()
        );
        if (!patientConflicts.isEmpty()) {
            throw new BadRequestException("You already have an active appointment scheduled at " + request.getAppointmentTime() + " on " + request.getAppointmentDate());
        }

        Appointment appointment = new Appointment(
                patient,
                doctor,
                request.getAppointmentDate(),
                request.getAppointmentTime(),
                request.getReason()
        );
        appointment.setStatus("PENDING");
        Appointment saved = appointmentRepository.save(appointment);

        // Empathy-aware notification message for patient
        String pref = patient.getUser().getCommunicationPreference();
        String dateStr = request.getAppointmentDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        String timeStr = request.getAppointmentTime().toString();
        String patientMsg;
        if ("SIMPLE".equalsIgnoreCase(pref)) {
            patientMsg = "Your appointment request with Dr. " + doctor.getFullName() + " for " + dateStr + " at " + timeStr + " is submitted.";
        } else if ("PROFESSIONAL".equalsIgnoreCase(pref)) {
            patientMsg = "Confirmation Pending: Consultation request #" + saved.getId() + " with Dr. " + doctor.getFullName() + " submitted for " + dateStr + " at " + timeStr + ".";
        } else {
            patientMsg = "Your appointment with Dr. " + doctor.getFullName() + " is booked for " + dateStr + " at " + timeStr + ". Please keep any previous medical reports ready!";
        }

        notificationService.createNotification(patient.getUser(), "Appointment Booked", patientMsg, "APPOINTMENT");
        notificationService.createNotification(doctor.getUser(), "New Appointment Request", "New consultation request from patient " + patient.getFullName() + " on " + dateStr + " at " + timeStr + ".", "APPOINTMENT");

        auditLogService.log(patientEmail, "APPOINTMENT_CREATED", "Appointment:" + saved.getId(), "Booked appointment with Dr. " + doctor.getFullName());

        waitlistService.markWaitlistAsBooked(patient.getId(), doctor.getId(), request.getAppointmentDate());

        return saved;
    }

    public List<Appointment> getAppointmentsForPatient(String patientEmail) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        return appointmentRepository.findByPatientOrderByAppointmentDateDescAppointmentTimeDesc(patient);
    }

    public List<Appointment> getAppointmentsForDoctor(String doctorEmail) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        return appointmentRepository.findByDoctorOrderByAppointmentDateDescAppointmentTimeDesc(doctor);
    }

    public List<Appointment> getDoctorAppointmentsForDate(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        return appointmentRepository.findByDoctorAndAppointmentDateOrderByAppointmentTimeAsc(doctor, date);
    }

    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));
    }

    public Appointment getAppointmentById(Long id, String callerEmail, String role) {
        Appointment appointment = getAppointmentById(id);
        if ("ADMIN".equalsIgnoreCase(role)) {
            return appointment;
        }
        if (appointment.getPatient() != null && appointment.getPatient().getUser().getEmail().equalsIgnoreCase(callerEmail)) {
            return appointment;
        }
        if (appointment.getDoctor() != null && appointment.getDoctor().getUser().getEmail().equalsIgnoreCase(callerEmail)) {
            return appointment;
        }
        if (appointment.getPatient() != null && caregiverService.isAuthorizedCaregiver(appointment.getPatient().getId(), callerEmail)) {
            return appointment;
        }
        throw new org.springframework.security.access.AccessDeniedException("You do not have permission to view this appointment");
    }

    @Transactional
    public Appointment updateAppointmentStatus(Long id, String userEmail, AppointmentStatusUpdateRequest request) {
        return updateAppointmentStatus(id, userEmail, null, request);
    }

    @Transactional
    public Appointment updateAppointmentStatus(Long id, String userEmail, String role, AppointmentStatusUpdateRequest request) {
        Appointment appointment = getAppointmentById(id);
        String oldStatus = appointment.getStatus();
        String newStatus = request.getStatus().toUpperCase();

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isDoctor = appointment.getDoctor() != null && appointment.getDoctor().getUser().getEmail().equalsIgnoreCase(userEmail);
        boolean isPatient = appointment.getPatient() != null && appointment.getPatient().getUser().getEmail().equalsIgnoreCase(userEmail);
        boolean isCaregiver = appointment.getPatient() != null && caregiverService.isAuthorizedCaregiver(appointment.getPatient().getId(), userEmail);

        if (!isAdmin) {
            if ("CONFIRMED".equalsIgnoreCase(newStatus) || "COMPLETED".equalsIgnoreCase(newStatus)) {
                if (!isDoctor) {
                    throw new org.springframework.security.access.AccessDeniedException("Only the treating physician or administrator can confirm or complete an appointment");
                }
            } else if ("CANCELLED".equalsIgnoreCase(newStatus)) {
                if (!isPatient && !isDoctor && !isCaregiver) {
                    throw new org.springframework.security.access.AccessDeniedException("You do not have permission to cancel this appointment");
                }
            } else {
                if (!isDoctor) {
                    throw new org.springframework.security.access.AccessDeniedException("Unauthorized to modify appointment status to " + newStatus);
                }
            }
        }

        appointment.setStatus(newStatus);
        if (request.getConsultationNotes() != null) {
            appointment.setConsultationNotes(request.getConsultationNotes());
        }
        if (request.getCancellationReason() != null) {
            appointment.setCancellationReason(request.getCancellationReason());
        }

        Appointment updated = appointmentRepository.save(appointment);

        String dateStr = appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        String timeStr = appointment.getAppointmentTime().toString();
        String pref = appointment.getPatient().getUser().getCommunicationPreference();

        String statusMsg;
        if ("CONFIRMED".equalsIgnoreCase(newStatus)) {
            if ("SIMPLE".equalsIgnoreCase(pref)) {
                statusMsg = "Your appointment on " + dateStr + " at " + timeStr + " is confirmed.";
            } else if ("PROFESSIONAL".equalsIgnoreCase(pref)) {
                statusMsg = "Notification: Consultation #" + updated.getId() + " confirmed for " + dateStr + " at " + timeStr + ".";
            } else {
                statusMsg = "Great news! Dr. " + appointment.getDoctor().getFullName() + " has confirmed your appointment on " + dateStr + " at " + timeStr + ". We look forward to seeing you!";
            }
            notificationService.createNotification(appointment.getPatient().getUser(), "Appointment Confirmed", statusMsg, "APPOINTMENT");
        } else if ("CANCELLED".equalsIgnoreCase(newStatus)) {
            String reason = request.getCancellationReason() != null ? " Reason: " + request.getCancellationReason() : "";
            statusMsg = "Your appointment scheduled for " + dateStr + " has been cancelled." + reason;
            notificationService.createNotification(appointment.getPatient().getUser(), "Appointment Cancelled", statusMsg, "APPOINTMENT");
            notificationService.createNotification(appointment.getDoctor().getUser(), "Appointment Cancelled", "Appointment with " + appointment.getPatient().getFullName() + " on " + dateStr + " was cancelled.", "APPOINTMENT");
        } else if ("COMPLETED".equalsIgnoreCase(newStatus)) {
            notificationService.createNotification(appointment.getPatient().getUser(), "Consultation Completed", "Your consultation with Dr. " + appointment.getDoctor().getFullName() + " is completed. Check your medical records and prescriptions.", "APPOINTMENT");
        }

        if ("CANCELLED".equalsIgnoreCase(newStatus)) {
            auditLogService.log(userEmail, "APPOINTMENT_CANCELLED", "Appointment:" + updated.getId(), "Appointment cancelled. Reason: " + request.getCancellationReason());
            // Section 3 & 4: Automatically check waitlist and notify waiting patients of the freed slot
            waitlistService.processWaitlistOnSlotAvailable(appointment.getDoctor(), appointment.getAppointmentDate(), appointment.getAppointmentTime());
        } else {
            auditLogService.log(userEmail, "APPOINTMENT_STATUS_UPDATED", "Appointment:" + updated.getId(), "Status changed from " + oldStatus + " to " + newStatus);
        }

        return updated;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @Transactional
    public Appointment rescheduleAppointment(Long id, String userEmail, String role, AppointmentRescheduleRequestDTO request) {
        Appointment appointment = getAppointmentById(id);

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isDoctor = appointment.getDoctor() != null && appointment.getDoctor().getUser().getEmail().equalsIgnoreCase(userEmail);
        boolean isPatient = appointment.getPatient() != null && appointment.getPatient().getUser().getEmail().equalsIgnoreCase(userEmail);
        boolean isCaregiver = appointment.getPatient() != null && caregiverService.isAuthorizedCaregiver(appointment.getPatient().getId(), userEmail);

        if (!isAdmin && !isDoctor && !isPatient && !isCaregiver) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to reschedule this appointment");
        }

        if ("COMPLETED".equalsIgnoreCase(appointment.getStatus())) {
            throw new BadRequestException("Completed appointments cannot be rescheduled");
        }
        if ("CANCELLED".equalsIgnoreCase(appointment.getStatus())) {
            throw new BadRequestException("Cancelled appointments cannot be rescheduled");
        }

        if (request.getNewDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot reschedule to a past date");
        }
        if (request.getNewDate().isEqual(LocalDate.now()) && request.getNewTime().isBefore(LocalTime.now())) {
            throw new BadRequestException("Cannot reschedule to a past time slot today");
        }

        Doctor doctor = appointment.getDoctor();
        if ("ON_LEAVE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
            throw new BadRequestException("Dr. " + doctor.getFullName() + " is currently on leave");
        }

        validateDoctorWorkingHours(doctor.getId(), doctor.getFullName(), request.getNewDate(), request.getNewTime());
        validateEmergencyDutyConflict(doctor.getId(), doctor.getFullName(), request.getNewDate(), request.getNewTime());

        // Check doctor conflicts at new time (excluding this appointment)
        List<Appointment> doctorConflicts = appointmentRepository.findActiveAppointmentsForDoctorAtTime(
                doctor.getId(), request.getNewDate(), request.getNewTime()
        ).stream().filter(a -> !a.getId().equals(appointment.getId())).toList();
        if (!doctorConflicts.isEmpty()) {
            throw new BadRequestException("The selected slot (" + request.getNewTime() + ") is already booked with Dr. " + doctor.getFullName());
        }

        // Check patient conflicts at new time (excluding this appointment)
        List<Appointment> patientConflicts = appointmentRepository.findActiveAppointmentsForPatientAtTime(
                appointment.getPatient().getId(), request.getNewDate(), request.getNewTime()
        ).stream().filter(a -> !a.getId().equals(appointment.getId())).toList();
        if (!patientConflicts.isEmpty()) {
            throw new BadRequestException("Patient already has an active appointment scheduled at " + request.getNewTime() + " on " + request.getNewDate());
        }

        LocalDate oldDate = appointment.getAppointmentDate();
        LocalTime oldTime = appointment.getAppointmentTime();

        appointment.setAppointmentDate(request.getNewDate());
        appointment.setAppointmentTime(request.getNewTime());
        if (request.getReason() != null && !request.getReason().isBlank()) {
            appointment.setReason(request.getReason());
        }
        appointment.setStatus("PENDING");
        Appointment saved = appointmentRepository.save(appointment);

        // Releasing old slot: Notify any waitlisted patients for the freed slot
        waitlistService.processWaitlistOnSlotAvailable(doctor, oldDate, oldTime);

        // Notifications
        String dateStr = request.getNewDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        notificationService.createNotification(
                appointment.getPatient().getUser(),
                "Appointment Rescheduled",
                "Your appointment with Dr. " + doctor.getFullName() + " was rescheduled to " + dateStr + " at " + request.getNewTime() + ".",
                "APPOINTMENT"
        );
        notificationService.createNotification(
                doctor.getUser(),
                "Appointment Rescheduled",
                "Consultation with patient " + appointment.getPatient().getFullName() + " rescheduled to " + dateStr + " at " + request.getNewTime() + ".",
                "APPOINTMENT"
        );

        auditLogService.log(userEmail, "APPOINTMENT_RESCHEDULED", "Appointment:" + saved.getId(),
                "Rescheduled from " + oldDate + " " + oldTime + " to " + request.getNewDate() + " " + request.getNewTime());

        return saved;
    }

    private void validateDoctorWorkingHours(Long doctorId, String doctorName, LocalDate date, LocalTime time) {
        String dayOfWeek = date.getDayOfWeek().name();
        List<DoctorAvailability> availList = doctorAvailabilityRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
        if (!availList.isEmpty()) {
            boolean working = availList.stream().anyMatch(a -> a.isAvailable() &&
                    (time.equals(a.getStartTime()) || time.isAfter(a.getStartTime())) &&
                    time.isBefore(a.getEndTime()) &&
                    (a.getBreakStartTime() == null || a.getBreakEndTime() == null ||
                            time.isBefore(a.getBreakStartTime()) || !time.isBefore(a.getBreakEndTime())));
            if (!working) {
                throw new BadRequestException("Dr. " + doctorName + " does not have consultation hours at " + time + " on " + dayOfWeek);
            }
        }
    }

    private void validateEmergencyDutyConflict(Long doctorId, String doctorName, LocalDate date, LocalTime time) {
        List<EmergencyDoctorRoster> rosters = emergencyRosterRepository.findByDoctorIdAndRosterDate(doctorId, date);
        for (EmergencyDoctorRoster r : rosters) {
            if ("EMERGENCY_DUTY".equalsIgnoreCase(r.getDutyStatus())) {
                boolean inShift;
                if (r.getShiftStart().isBefore(r.getShiftEnd())) {
                    inShift = (time.equals(r.getShiftStart()) || time.isAfter(r.getShiftStart())) && time.isBefore(r.getShiftEnd());
                } else {
                    inShift = time.equals(r.getShiftStart()) || time.isAfter(r.getShiftStart()) || time.isBefore(r.getShiftEnd());
                }
                if (inShift) {
                    throw new BadRequestException("Dr. " + doctorName + " is scheduled for emergency duty during this time (" + r.getShiftName() + " shift)");
                }
            }
        }
    }
}
