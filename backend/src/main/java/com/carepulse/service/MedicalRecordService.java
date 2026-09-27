package com.carepulse.service;

import com.carepulse.dto.MedicalRecordRequest;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.MedicalRecord;
import com.carepulse.entity.Patient;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.MedicalRecordRepository;
import com.carepulse.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final CaregiverService caregiverService;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository,
                                PatientRepository patientRepository,
                                DoctorRepository doctorRepository,
                                NotificationService notificationService,
                                AuditLogService auditLogService,
                                CaregiverService caregiverService) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.caregiverService = caregiverService;
    }

    @Transactional
    public MedicalRecord createRecord(String doctorEmail, MedicalRecordRequest request) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));

        MedicalRecord record = new MedicalRecord(
                patient,
                doctor,
                request.getRecordDate() != null ? request.getRecordDate() : LocalDate.now(),
                request.getDiagnosis(),
                request.getSymptoms(),
                request.getTreatment(),
                request.getConsultationNotes()
        );
        record.setAttachments(request.getAttachments());

        MedicalRecord saved = medicalRecordRepository.save(record);

        notificationService.createNotification(
                patient.getUser(),
                "New Medical Record Added",
                "Dr. " + doctor.getFullName() + " added a new consultation note/record: " + record.getDiagnosis(),
                "RECORD"
        );

        auditLogService.log(doctorEmail, "MEDICAL_RECORD_CREATED", "MedicalRecord:" + saved.getId(), "Added record for patient ID: " + patient.getId());

        return saved;
    }

    public List<MedicalRecord> getRecordsForPatient(String patientEmail) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        auditLogService.log(patientEmail, "MEDICAL_RECORD_VIEWED", "PatientRecords", "Viewed personal health records");
        return medicalRecordRepository.findByPatientOrderByRecordDateDesc(patient);
    }

    public List<MedicalRecord> getRecordsForPatientId(Long patientId, String viewerEmail) {
        return getRecordsForPatientId(patientId, viewerEmail, null);
    }

    public List<MedicalRecord> getRecordsForPatientId(Long patientId, String viewerEmail, String role) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isDoctor = "DOCTOR".equalsIgnoreCase(role);
        boolean isSelf = patientRepository.findById(patientId)
                .map(p -> p.getUser().getEmail().equalsIgnoreCase(viewerEmail))
                .orElse(false);
        boolean isCaregiver = caregiverService.isAuthorizedCaregiver(patientId, viewerEmail);

        if (!isAdmin && !isDoctor && !isSelf && !isCaregiver) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access records for this patient");
        }

        auditLogService.log(viewerEmail, "MEDICAL_RECORD_VIEWED", "PatientRecords:" + patientId, "Viewed records for patient: " + patientId);
        return medicalRecordRepository.findByPatientIdOrderByRecordDateDesc(patientId);
    }

    public List<MedicalRecord> getRecordsByDoctor(String doctorEmail) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        return medicalRecordRepository.findByDoctorOrderByRecordDateDesc(doctor);
    }

    public MedicalRecord getRecordById(Long id) {
        return medicalRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found with ID: " + id));
    }

    public MedicalRecord getRecordById(Long id, String callerEmail, String role) {
        MedicalRecord record = getRecordById(id);
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isDoctor = "DOCTOR".equalsIgnoreCase(role);
        boolean isPatient = record.getPatient() != null && record.getPatient().getUser().getEmail().equalsIgnoreCase(callerEmail);
        boolean isAuthorDoctor = record.getDoctor() != null && record.getDoctor().getUser().getEmail().equalsIgnoreCase(callerEmail);
        boolean isCaregiver = record.getPatient() != null && caregiverService.isAuthorizedCaregiver(record.getPatient().getId(), callerEmail);

        if (!isAdmin && !isDoctor && !isPatient && !isAuthorDoctor && !isCaregiver) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to view this medical record");
        }

        auditLogService.log(callerEmail, "MEDICAL_RECORD_VIEWED", "MedicalRecord:" + id, "Viewed medical record details");
        return record;
    }
}
