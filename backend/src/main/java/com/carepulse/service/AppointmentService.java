package com.carepulse.service;

import com.carepulse.dto.AppointmentRequest;
import com.carepulse.dto.AppointmentStatusUpdateRequest;
import com.carepulse.entity.Appointment;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.Patient;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.AppointmentRepository;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository,
                              NotificationService notificationService,
                              AuditLogService auditLogService) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Appointment bookAppointment(String patientEmail, AppointmentRequest request) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for email: " + patientEmail));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

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

    @Transactional
    public Appointment updateAppointmentStatus(Long id, String userEmail, AppointmentStatusUpdateRequest request) {
        Appointment appointment = getAppointmentById(id);
        String oldStatus = appointment.getStatus();
        String newStatus = request.getStatus().toUpperCase();

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

        auditLogService.log(userEmail, "APPOINTMENT_STATUS_UPDATED", "Appointment:" + updated.getId(), "Status changed from " + oldStatus + " to " + newStatus);

        return updated;
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }
}
