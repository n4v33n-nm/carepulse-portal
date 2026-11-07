package com.carepulse.service;

import com.carepulse.dto.MedicalReportResponseDTO;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.MedicalReport;
import com.carepulse.entity.Patient;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.MedicalReportRepository;
import com.carepulse.repository.PatientRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.*;

@Service
public class MedicalReportService {

    private final MedicalReportRepository medicalReportRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final MedicalRecordService medicalRecordService;
    private final CaregiverService caregiverService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Value("${carepulse.storage.report-dir:uploads/medical-reports}")
    private String storageDir;

    private Path rootLocation;

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    public MedicalReportService(MedicalReportRepository medicalReportRepository,
                                PatientRepository patientRepository,
                                DoctorRepository doctorRepository,
                                MedicalRecordService medicalRecordService,
                                CaregiverService caregiverService,
                                NotificationService notificationService,
                                AuditLogService auditLogService) {
        this.medicalReportRepository = medicalReportRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.medicalRecordService = medicalRecordService;
        this.caregiverService = caregiverService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @PostConstruct
    public void init() {
        try {
            this.rootLocation = Paths.get(storageDir).toAbsolutePath().normalize();
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory for medical reports", e);
        }
    }

    @Transactional
    public MedicalReportResponseDTO uploadReport(String userEmail,
                                                String role,
                                                MultipartFile file,
                                                String title,
                                                String reportType,
                                                LocalDate reportDate,
                                                String description,
                                                Long doctorId,
                                                Long explicitPatientId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a file to upload");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds the 10MB limit. Current size: " + (file.getSize() / (1024 * 1024)) + "MB");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "report");
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Invalid filename sequence: " + originalFilename);
        }

        String extension = "";
        int dotIdx = originalFilename.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = originalFilename.substring(dotIdx + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Unsupported file format ." + extension + ". Permitted formats: PDF, JPG, JPEG, PNG");
        }

        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported file content type: " + contentType + ". Permitted: application/pdf, image/jpeg, image/png");
        }

        // Determine patient
        Patient patient;
        if ("PATIENT".equalsIgnoreCase(role)) {
            patient = patientRepository.findByUserEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for current user"));
        } else if (("DOCTOR".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) && explicitPatientId != null) {
            patient = patientRepository.findById(explicitPatientId)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + explicitPatientId));
            if ("DOCTOR".equalsIgnoreCase(role) && !medicalRecordService.isDoctorAuthorizedForPatient(userEmail, explicitPatientId)) {
                throw new AccessDeniedException("You are not authorized to upload reports for this patient");
            }
        } else {
            throw new BadRequestException("Patient ID is required when uploaded by a healthcare provider");
        }

        Doctor doctor = null;
        if (doctorId != null) {
            doctor = doctorRepository.findById(doctorId).orElse(null);
        } else if ("DOCTOR".equalsIgnoreCase(role)) {
            doctor = doctorRepository.findByUserEmail(userEmail).orElse(null);
        }

        // Store file safely using UUID
        String storedFilename = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        try {
            Path targetLocation = this.rootLocation.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store medical report file on disk", ex);
        }

        MedicalReport report = new MedicalReport(
                patient,
                doctor,
                title != null && !title.trim().isEmpty() ? title.trim() : originalFilename,
                reportType != null ? reportType.toUpperCase() : "OTHER",
                originalFilename,
                storedFilename,
                contentType != null ? contentType : "application/octet-stream",
                file.getSize(),
                description,
                reportDate != null ? reportDate : LocalDate.now()
        );

        MedicalReport saved = medicalReportRepository.save(report);

        // Audit log
        auditLogService.log(userEmail, "MEDICAL_REPORT_UPLOADED", "MedicalReport:" + saved.getId(),
                "Uploaded report '" + saved.getTitle() + "' for patient " + patient.getFullName());

        // Notification if doctor is associated
        if (doctor != null && doctor.getUser() != null) {
            notificationService.createNotification(
                    doctor.getUser(),
                    "New Patient Medical Report",
                    "Patient " + patient.getFullName() + " uploaded a new report: " + saved.getTitle(),
                    "RECORD"
            );
        }

        return MedicalReportResponseDTO.fromEntity(saved);
    }

    public List<MedicalReportResponseDTO> getReportsForPatient(String patientEmail) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));
        auditLogService.log(patientEmail, "MEDICAL_REPORTS_LISTED", "PatientReports", "Viewed personal uploaded reports");
        return medicalReportRepository.findByPatientOrderByReportDateDesc(patient)
                .stream().map(MedicalReportResponseDTO::fromEntity).toList();
    }

    public List<MedicalReportResponseDTO> getReportsByPatientId(Long patientId, String callerEmail, String role) {
        verifyAccessPermission(patientId, callerEmail, role);

        if ("ADMIN".equalsIgnoreCase(role)) {
            auditLogService.log(callerEmail, "ADMIN_CLINICAL_AUDIT_ACCESS", "PatientReports:" + patientId, "Admin accessed patient uploaded reports");
        } else {
            auditLogService.log(callerEmail, "MEDICAL_REPORTS_VIEWED", "PatientReports:" + patientId, "Viewed uploaded reports for patient ID " + patientId);
        }

        return medicalReportRepository.findByPatientIdOrderByReportDateDesc(patientId)
                .stream().map(MedicalReportResponseDTO::fromEntity).toList();
    }

    public MedicalReportResponseDTO getReportById(Long reportId, String callerEmail, String role) {
        MedicalReport report = medicalReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical report not found with ID: " + reportId));

        verifyAccessPermission(report.getPatient().getId(), callerEmail, role);
        return MedicalReportResponseDTO.fromEntity(report);
    }

    public Resource loadReportFileResource(Long reportId, String callerEmail, String role) {
        MedicalReport report = medicalReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical report not found with ID: " + reportId));

        verifyAccessPermission(report.getPatient().getId(), callerEmail, role);

        try {
            Path filePath = this.rootLocation.resolve(report.getStoredFilename()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                auditLogService.log(callerEmail, "MEDICAL_REPORT_ACCESSED", "MedicalReport:" + reportId,
                        "Accessed binary file: " + report.getOriginalFilename());
                return resource;
            } else {
                throw new ResourceNotFoundException("Report file could not be read on disk");
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Report file path is invalid: " + report.getStoredFilename());
        }
    }

    public MedicalReport getReportEntity(Long reportId) {
        return medicalReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical report not found with ID: " + reportId));
    }

    @Transactional
    public void deleteReport(Long reportId, String callerEmail, String role) {
        MedicalReport report = medicalReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical report not found with ID: " + reportId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isSelf = report.getPatient() != null && report.getPatient().getUser().getEmail().equalsIgnoreCase(callerEmail);

        if (!isAdmin && !isSelf) {
            throw new AccessDeniedException("Only the report owner or administrator can delete this medical report");
        }

        // Delete physical file
        try {
            Path filePath = this.rootLocation.resolve(report.getStoredFilename()).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log warning but proceed to delete record
        }

        medicalReportRepository.delete(report);
        auditLogService.log(callerEmail, "MEDICAL_REPORT_DELETED", "MedicalReport:" + reportId,
                "Deleted report: " + report.getTitle());
    }

    private void verifyAccessPermission(Long patientId, String callerEmail, String role) {
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isAuthorizedDoctor = "DOCTOR".equalsIgnoreCase(role) && medicalRecordService.isDoctorAuthorizedForPatient(callerEmail, patientId);
        boolean isSelf = patientRepository.findById(patientId)
                .map(p -> p.getUser().getEmail().equalsIgnoreCase(callerEmail))
                .orElse(false);
        boolean isCaregiver = caregiverService.isAuthorizedCaregiverWithPermission(patientId, callerEmail, "VIEW_RECORDS");

        if (!isAdmin && !isAuthorizedDoctor && !isSelf && !isCaregiver) {
            throw new AccessDeniedException("You do not have permission to view or download this patient's medical reports");
        }
    }
}
