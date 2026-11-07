package com.carepulse.dto;

import com.carepulse.entity.MedicalReport;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MedicalReportResponseDTO {

    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String title;
    private String reportType;
    private String originalFilename;
    private String fileType;
    private Long fileSize;
    private String formattedSize;
    private String description;
    private LocalDate reportDate;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
    private String viewUrl;

    public MedicalReportResponseDTO() {
    }

    public static MedicalReportResponseDTO fromEntity(MedicalReport report) {
        MedicalReportResponseDTO dto = new MedicalReportResponseDTO();
        dto.setId(report.getId());
        if (report.getPatient() != null) {
            dto.setPatientId(report.getPatient().getId());
            dto.setPatientName(report.getPatient().getFullName());
        }
        if (report.getDoctor() != null) {
            dto.setDoctorId(report.getDoctor().getId());
            dto.setDoctorName(report.getDoctor().getFullName());
        }
        dto.setTitle(report.getTitle());
        dto.setReportType(report.getReportType());
        dto.setOriginalFilename(report.getOriginalFilename());
        dto.setFileType(report.getFileType());
        dto.setFileSize(report.getFileSize());
        dto.setFormattedSize(formatFileSize(report.getFileSize()));
        dto.setDescription(report.getDescription());
        dto.setReportDate(report.getReportDate());
        dto.setUploadedAt(report.getUploadedAt());
        dto.setDownloadUrl("/api/reports/" + report.getId() + "/download");
        dto.setViewUrl("/api/reports/" + report.getId() + "/view");
        return dto;
    }

    private static String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
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

    public String getFormattedSize() {
        return formattedSize;
    }

    public void setFormattedSize(String formattedSize) {
        this.formattedSize = formattedSize;
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

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getViewUrl() {
        return viewUrl;
    }

    public void setViewUrl(String viewUrl) {
        this.viewUrl = viewUrl;
    }
}

