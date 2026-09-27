package com.carepulse.service;

import com.carepulse.dto.AppointmentWaitlistResponseDTO;
import com.carepulse.dto.JoinWaitlistRequestDTO;
import com.carepulse.entity.AppointmentWaitlist;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.Patient;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.AppointmentWaitlistRepository;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AppointmentWaitlistService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentWaitlistService.class);

    private final AppointmentWaitlistRepository waitlistRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public AppointmentWaitlistService(AppointmentWaitlistRepository waitlistRepository,
                                      PatientRepository patientRepository,
                                      DoctorRepository doctorRepository,
                                      NotificationService notificationService,
                                      AuditLogService auditLogService) {
        this.waitlistRepository = waitlistRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AppointmentWaitlistResponseDTO joinWaitlist(String patientEmail, JoinWaitlistRequestDTO request) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for email: " + patientEmail));

        Doctor doctor = null;
        String spec = request.getSpecialization();
        if (request.getDoctorId() != null) {
            doctor = doctorRepository.findById(request.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));
            if (spec == null || spec.isBlank()) {
                spec = doctor.getSpecialization();
            }
        }

        if (request.getPreferredDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Preferred waitlist date cannot be in the past");
        }

        // Check for duplicate active waitlist entry
        if (doctor != null) {
            waitlistRepository.findByPatientIdAndDoctorIdAndPreferredDateAndStatus(
                    patient.getId(), doctor.getId(), request.getPreferredDate(), "WAITING"
            ).ifPresent(w -> {
                throw new BadRequestException("You are already on the waitlist for Dr. " + w.getDoctor().getFullName() + " on " + request.getPreferredDate());
            });
        }

        AppointmentWaitlist entry = new AppointmentWaitlist(
                patient,
                doctor,
                spec,
                request.getPreferredDate(),
                request.getPreferredTime(),
                request.getNotes()
        );

        AppointmentWaitlist saved = waitlistRepository.save(entry);

        String docName = doctor != null ? "Dr. " + doctor.getFullName() : "Specialist in " + spec;
        String dateStr = request.getPreferredDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));

        notificationService.createNotification(
                patient.getUser(),
                "Added to Appointment Waitlist",
                "You have been added to the waitlist for " + docName + " on " + dateStr + ". We will notify you immediately if a slot opens up!",
                "WAITLIST"
        );

        auditLogService.log(patientEmail, "WAITLIST_JOINED", "Waitlist:" + saved.getId(), "Joined waitlist for " + docName + " on " + dateStr);

        return mapToDTO(saved);
    }

    public List<AppointmentWaitlistResponseDTO> getMyWaitlistEntries(String patientEmail) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        return waitlistRepository.findByPatientOrderByCreatedAtDesc(patient).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<AppointmentWaitlistResponseDTO> getAllWaitlistEntries() {
        return waitlistRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelWaitlistEntry(Long waitlistId, String callerEmail, String role) {
        AppointmentWaitlist entry = waitlistRepository.findById(waitlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Waitlist entry not found with ID: " + waitlistId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isOwner = entry.getPatient().getUser().getEmail().equalsIgnoreCase(callerEmail);

        if (!isAdmin && !isOwner) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to cancel this waitlist entry");
        }

        entry.setStatus("CANCELLED");
        waitlistRepository.save(entry);

        auditLogService.log(callerEmail, "WAITLIST_CANCELLED", "Waitlist:" + waitlistId, "Cancelled waitlist entry");
    }

    @Transactional
    public void processWaitlistOnSlotAvailable(Doctor doctor, LocalDate date, LocalTime slotTime) {
        if (doctor == null || date == null) return;

        List<AppointmentWaitlist> candidates = waitlistRepository.findEligibleWaitingCandidates(
                doctor.getId(), doctor.getSpecialization(), date
        );

        if (candidates.isEmpty()) {
            return;
        }

        // Filter by preferred time if specified, else take first
        AppointmentWaitlist matchedCandidate = null;
        if (slotTime != null) {
            for (AppointmentWaitlist c : candidates) {
                if (c.getPreferredTime() == null || c.getPreferredTime().equals(slotTime)) {
                    matchedCandidate = c;
                    break;
                }
            }
        }

        if (matchedCandidate == null) {
            matchedCandidate = candidates.get(0);
        }

        matchedCandidate.setStatus("NOTIFIED");
        waitlistRepository.save(matchedCandidate);

        String dateStr = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        String timeStr = slotTime != null ? " at " + slotTime : "";

        notificationService.createNotification(
                matchedCandidate.getPatient().getUser(),
                "Appointment Slot Available!",
                "Great news! A consultation slot opened up with Dr. " + doctor.getFullName() + " on " + dateStr + timeStr + ". Please log in and book your appointment now.",
                "WAITLIST"
        );

        auditLogService.log(
                matchedCandidate.getPatient().getUser().getEmail(),
                "WAITLIST_PATIENT_NOTIFIED",
                "Waitlist:" + matchedCandidate.getId(),
                "Notified patient for open slot with Dr. " + doctor.getFullName() + " on " + dateStr
        );

        log.info("Notified waitlisted patient ID {} for opened slot with Dr. {} on {}",
                matchedCandidate.getPatient().getId(), doctor.getFullName(), date);
    }

    @Transactional
    public void markWaitlistAsBooked(Long patientId, Long doctorId, LocalDate date) {
        if (patientId == null || doctorId == null || date == null) return;
        waitlistRepository.findByPatientIdAndDoctorIdAndPreferredDateAndStatus(patientId, doctorId, date, "WAITING")
                .ifPresent(w -> {
                    w.setStatus("BOOKED");
                    waitlistRepository.save(w);
                });
        waitlistRepository.findByPatientIdAndDoctorIdAndPreferredDateAndStatus(patientId, doctorId, date, "NOTIFIED")
                .ifPresent(w -> {
                    w.setStatus("BOOKED");
                    waitlistRepository.save(w);
                });
    }

    @Transactional
    public int expirePastWaitlists() {
        LocalDate today = LocalDate.now();
        List<AppointmentWaitlist> expired = waitlistRepository.findByStatusAndPreferredDateLessThan("WAITING", today);
        for (AppointmentWaitlist w : expired) {
            w.setStatus("EXPIRED");
            waitlistRepository.save(w);
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} past waitlist entries prior to {}", expired.size(), today);
        }
        return expired.size();
    }

    private AppointmentWaitlistResponseDTO mapToDTO(AppointmentWaitlist w) {
        return new AppointmentWaitlistResponseDTO(
                w.getId(),
                w.getPatient().getId(),
                w.getPatient().getFullName(),
                w.getDoctor() != null ? w.getDoctor().getId() : null,
                w.getDoctor() != null ? w.getDoctor().getFullName() : null,
                w.getSpecialization(),
                w.getPreferredDate(),
                w.getPreferredTime(),
                w.getStatus(),
                w.getNotes(),
                w.getCreatedAt()
        );
    }
}
