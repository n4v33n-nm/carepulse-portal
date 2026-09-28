package com.carepulse.service;

import com.carepulse.dto.MedicalRecordRequest;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.MedicalRecord;
import com.carepulse.entity.Patient;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.AppointmentRepository;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.EmergencyRequestRepository;
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
    private final AppointmentRepository appointmentRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository,
                                PatientRepository patientRepository,
                                DoctorRepository doctorRepository,
                                NotificationService notificationService,
                                AuditLogService auditLogService,
                                CaregiverService caregiverService,
                                AppointmentRepository appointmentRepository,
                                EmergencyRequestRepository emergencyRequestRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.caregiverService = caregiverService;
        this.appointmentRepository = appointmentRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
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

    public boolean isDoctorAuthorizedForPatient(String doctorEmail, Long patientId) {
        if (doctorEmail == null || patientId == null) return false;
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail).orElse(null);
        if (doctor == null) return false;

        return appointmentRepository.existsByDoctorIdAndPatientId(doctor.getId(), patientId)
                || emergencyRequestRepository.existsByAssignedDoctorIdAndPatientId(doctor.getId(), patientId)
                || medicalRecordRepository.existsByPatientIdAndDoctorId(patientId, doctor.getId());
    }

    public List<MedicalRecord> getRecordsForPatientId(Long patientId, String viewerEmail, String role) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isAuthorizedDoctor = "DOCTOR".equalsIgnoreCase(role) && isDoctorAuthorizedForPatient(viewerEmail, patientId);
        boolean isSelf = patientRepository.findById(patientId)
                .map(p -> p.getUser().getEmail().equalsIgnoreCase(viewerEmail))
                .orElse(false);
        boolean isCaregiver = caregiverService.isAuthorizedCaregiverWithPermission(patientId, viewerEmail, "VIEW_RECORDS");

        if (!isAdmin && !isAuthorizedDoctor && !isSelf && !isCaregiver) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to access records for this patient");
        }

        if (isAdmin && !isSelf && !isCaregiver) {
            auditLogService.log(viewerEmail, "ADMIN_CLINICAL_AUDIT_ACCESS", "PatientRecords:" + patientId, "Administrative audit access to patient records");
        } else {
            auditLogService.log(viewerEmail, "MEDICAL_RECORD_VIEWED", "PatientRecords:" + patientId, "Viewed records for patient: " + patientId);
        }

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
        boolean isPatient = record.getPatient() != null && record.getPatient().getUser().getEmail().equalsIgnoreCase(callerEmail);
        boolean isAuthorDoctor = record.getDoctor() != null && record.getDoctor().getUser().getEmail().equalsIgnoreCase(callerEmail);
        boolean isTreatingDoctor = "DOCTOR".equalsIgnoreCase(role) && record.getPatient() != null && isDoctorAuthorizedForPatient(callerEmail, record.getPatient().getId());
        boolean isCaregiver = record.getPatient() != null && caregiverService.isAuthorizedCaregiverWithPermission(record.getPatient().getId(), callerEmail, "VIEW_RECORDS");

        if (!isAdmin && !isPatient && !isAuthorDoctor && !isTreatingDoctor && !isCaregiver) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to view this medical record");
        }

        if (isAdmin && !isPatient && !isCaregiver) {
            auditLogService.log(callerEmail, "ADMIN_CLINICAL_AUDIT_ACCESS", "MedicalRecord:" + id, "Administrative audit access to medical record details");
        } else {
            auditLogService.log(callerEmail, "MEDICAL_RECORD_VIEWED", "MedicalRecord:" + id, "Viewed medical record details");
        }
        return record;
    }

    @Transactional
    public MedicalRecord generateAiDraftSummary(Long recordId, String doctorEmail) {
        MedicalRecord record = getRecordById(recordId);
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (!record.getDoctor().getId().equals(doctor.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Only the attending doctor can generate clinical AI summary drafts");
        }

        // Section 15: AI creates a structured draft summary for physician verification
        StringBuilder draft = new StringBuilder();
        draft.append("Clinical Summary Draft (AI-Assisted):\n");
        draft.append("• Diagnosis: ").append(record.getDiagnosis()).append("\n");
        if (record.getSymptoms() != null && !record.getSymptoms().isBlank()) {
            draft.append("• Reported Symptoms: ").append(record.getSymptoms()).append("\n");
        }
        if (record.getTreatment() != null && !record.getTreatment().isBlank()) {
            draft.append("• Prescribed Treatment: ").append(record.getTreatment()).append("\n");
        }
        if (record.getConsultationNotes() != null && !record.getConsultationNotes().isBlank()) {
            draft.append("• Clinical Notes: ").append(record.getConsultationNotes()).append("\n");
        }
        draft.append("[Physician Verification Required: Please review, modify if necessary, and approve or reject before finalizing this clinical summary.]");

        record.setAiDraftSummary(draft.toString());
        record.setSummaryStatus("PENDING_REVIEW");

        MedicalRecord saved = medicalRecordRepository.save(record);
        auditLogService.log(doctorEmail, "AI_SUMMARY_DRAFTED", "MedicalRecord:" + recordId, "Generated AI draft summary for physician review");
        return saved;
    }

    @Transactional
    public MedicalRecord reviewAiSummary(Long recordId, String doctorEmail, com.carepulse.dto.ReviewSummaryRequestDTO request) {
        MedicalRecord record = getRecordById(recordId);
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        if (!record.getDoctor().getId().equals(doctor.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Only the attending doctor can review and finalize the clinical summary");
        }

        String action = request.getAction().toUpperCase();
        if ("APPROVE".equals(action)) {
            String summaryContent = (request.getEditedSummary() != null && !request.getEditedSummary().isBlank()) ?
                    request.getEditedSummary() : record.getAiDraftSummary();
            record.setClinicalSummary(summaryContent);
            record.setSummaryStatus("APPROVED");
        } else if ("EDIT".equals(action)) {
            record.setClinicalSummary(request.getEditedSummary());
            record.setSummaryStatus("APPROVED");
        } else if ("REJECT".equals(action)) {
            record.setSummaryStatus("REJECTED");
            record.setAiDraftSummary(null);
        } else {
            throw new com.carepulse.exception.BadRequestException("Invalid summary review action: " + action);
        }

        MedicalRecord saved = medicalRecordRepository.save(record);
        auditLogService.log(doctorEmail, "AI_SUMMARY_REVIEWED", "MedicalRecord:" + recordId, "Review action: " + action);
        return saved;
    }
}
