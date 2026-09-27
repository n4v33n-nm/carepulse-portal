package com.carepulse.service;

import com.carepulse.dto.PrescriptionRequest;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.MedicalRecord;
import com.carepulse.entity.Patient;
import com.carepulse.entity.Prescription;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.MedicalRecordRepository;
import com.carepulse.repository.PatientRepository;
import com.carepulse.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final CaregiverService caregiverService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PatientRepository patientRepository,
                               DoctorRepository doctorRepository,
                               MedicalRecordRepository medicalRecordRepository,
                               NotificationService notificationService,
                               AuditLogService auditLogService,
                               CaregiverService caregiverService) {
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.caregiverService = caregiverService;
    }

    @Transactional
    public Prescription createPrescription(String doctorEmail, PrescriptionRequest request) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));

        MedicalRecord record = null;
        if (request.getMedicalRecordId() != null) {
            record = medicalRecordRepository.findById(request.getMedicalRecordId()).orElse(null);
        }

        Prescription prescription = new Prescription(
                patient,
                doctor,
                record,
                request.getMedicineName(),
                request.getDosage(),
                request.getFrequency(),
                request.getDuration(),
                request.getInstructions(),
                request.getIssuedDate() != null ? request.getIssuedDate() : LocalDate.now()
        );

        Prescription saved = prescriptionRepository.save(prescription);

        notificationService.createNotification(
                patient.getUser(),
                "New Prescription Issued",
                "Dr. " + doctor.getFullName() + " prescribed " + prescription.getMedicineName() + " (" + prescription.getDosage() + "). Follow dosage instructions carefully.",
                "PRESCRIPTION"
        );

        auditLogService.log(doctorEmail, "PRESCRIPTION_CREATED", "Prescription:" + saved.getId(), "Issued " + prescription.getMedicineName() + " to patient " + patient.getFullName());

        return saved;
    }

    public List<Prescription> getPrescriptionsForPatient(String patientEmail) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        return prescriptionRepository.findByPatientOrderByIssuedDateDesc(patient);
    }

    public List<Prescription> getPrescriptionsForPatientId(Long patientId) {
        return getPrescriptionsForPatientId(patientId, null, null);
    }

    public List<Prescription> getPrescriptionsForPatientId(Long patientId, String callerEmail, String role) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isDoctor = "DOCTOR".equalsIgnoreCase(role);
        boolean isSelf = callerEmail != null && patientRepository.findById(patientId)
                .map(p -> p.getUser().getEmail().equalsIgnoreCase(callerEmail))
                .orElse(false);
        boolean isCaregiver = callerEmail != null && caregiverService.isAuthorizedCaregiver(patientId, callerEmail);

        if (callerEmail != null && !isAdmin && !isDoctor && !isSelf && !isCaregiver) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to view prescriptions for this patient");
        }

        return prescriptionRepository.findByPatientIdOrderByIssuedDateDesc(patientId);
    }

    public List<Prescription> getPrescriptionsByDoctor(String doctorEmail) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        return prescriptionRepository.findByDoctorOrderByIssuedDateDesc(doctor);
    }

    public Prescription getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with ID: " + id));
    }

    public Prescription getPrescriptionById(Long id, String callerEmail, String role) {
        Prescription prescription = getPrescriptionById(id);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isDoctor = "DOCTOR".equalsIgnoreCase(role);
        boolean isPatient = prescription.getPatient() != null && prescription.getPatient().getUser().getEmail().equalsIgnoreCase(callerEmail);
        boolean isAuthorDoctor = prescription.getDoctor() != null && prescription.getDoctor().getUser().getEmail().equalsIgnoreCase(callerEmail);
        boolean isCaregiver = prescription.getPatient() != null && caregiverService.isAuthorizedCaregiver(prescription.getPatient().getId(), callerEmail);

        if (!isAdmin && !isDoctor && !isPatient && !isAuthorDoctor && !isCaregiver) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to view this prescription");
        }

        return prescription;
    }
}
