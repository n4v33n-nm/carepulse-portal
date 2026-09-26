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

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository,
                                PatientRepository patientRepository,
                                DoctorRepository doctorRepository,
                                NotificationService notificationService,
                                AuditLogService auditLogService) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
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
}
