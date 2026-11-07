package com.carepulse.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_reports", indexes = {
        @Index(name = "idx_reports_patient", columnList = "patient_id"),
        @Index(name = "idx_reports_date", columnList = "report_date"),
        @Index(name = "idx_reports_type", columnList = "report_type")
})
public class MedicalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "report_type", nullable = false, length = 50)
    private String reportType; // BLOOD_TEST, SCAN_REPORT, LAB_REPORT, DISCHARGE_SUMMARY, OTHER

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 255)
    private String storedFilename;

    @Column(name = "file_type", nullable = false, length = 100)
    private String fileType; // application/pdf, image/jpeg, image/png

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    public MedicalReport() {
    }

    public MedicalReport(Patient patient, Doctor doctor, String title, String reportType,
                         String originalFilename, String storedFilename, String fileType,
                         Long fileSize, String description, LocalDate reportDate) {
        this.patient = patient;
        this.doctor = doctor;
        this.title = title;
        this.reportType = reportType;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.description = description;
        this.reportDate = reportDate != null ? reportDate : LocalDate.now();
    }

    @PrePersist
    protected void onCreate() {
        this.uploadedAt = LocalDateTime.now();
        if (this.reportDate == null) {
            this.reportDate = LocalDate.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getStoredFilename() {
        return storedFilename;
    }

    public void setStoredFilename(String storedFilename) {
        this.storedFilename = storedFilename;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}

