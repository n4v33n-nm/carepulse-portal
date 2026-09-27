package com.carepulse.dto;

import com.carepulse.entity.EmergencyRequest;
import java.time.LocalDateTime;

public class EmergencyRequestResponseDTO {

    private Long id;

    // Patient details
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private String patientBloodGroup;
    private String emergencyContact;

    // Doctor details (null if not yet assigned or no doctor available)
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private String doctorPhone;
    private String doctorQualification;

    private LocalDateTime requestTime;
    private LocalDateTime assignedTime;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
    private String status;
    private String priority;
    private String category;
    private String description;
    private String doctorNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmergencyRequestResponseDTO() {
    }

    public static EmergencyRequestResponseDTO fromEntity(EmergencyRequest req) {
        if (req == null) return null;

        EmergencyRequestResponseDTO dto = new EmergencyRequestResponseDTO();
        dto.setId(req.getId());

        if (req.getPatient() != null) {
            dto.setPatientId(req.getPatient().getId());
            dto.setPatientName(req.getPatient().getFullName());
            dto.setPatientPhone(req.getPatient().getPhone());
            dto.setPatientBloodGroup(req.getPatient().getBloodGroup());
            dto.setEmergencyContact(req.getPatient().getEmergencyContact());
        }

        if (req.getAssignedDoctor() != null) {
            dto.setDoctorId(req.getAssignedDoctor().getId());
            dto.setDoctorName(req.getAssignedDoctor().getFullName());
            dto.setDoctorSpecialization(req.getAssignedDoctor().getSpecialization());
            dto.setDoctorPhone(req.getAssignedDoctor().getPhone());
            dto.setDoctorQualification(req.getAssignedDoctor().getQualification());
        }

        dto.setRequestTime(req.getRequestTime());
        dto.setAssignedTime(req.getAssignedTime());
        dto.setStartedTime(req.getStartedTime());
        dto.setCompletedTime(req.getCompletedTime());
        dto.setStatus(req.getStatus());
        dto.setPriority(req.getPriority());
        dto.setCategory(req.getCategory());
        dto.setDescription(req.getDescription());
        dto.setDoctorNotes(req.getDoctorNotes());
        dto.setCreatedAt(req.getCreatedAt());
        dto.setUpdatedAt(req.getUpdatedAt());

        return dto;
    }

    // Getters and Setters
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

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getPatientBloodGroup() {
        return patientBloodGroup;
    }

    public void setPatientBloodGroup(String patientBloodGroup) {
        this.patientBloodGroup = patientBloodGroup;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
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

    public String getDoctorSpecialization() {
        return doctorSpecialization;
    }

    public void setDoctorSpecialization(String doctorSpecialization) {
        this.doctorSpecialization = doctorSpecialization;
    }

    public String getDoctorPhone() {
        return doctorPhone;
    }

    public void setDoctorPhone(String doctorPhone) {
        this.doctorPhone = doctorPhone;
    }

    public String getDoctorQualification() {
        return doctorQualification;
    }

    public void setDoctorQualification(String doctorQualification) {
        this.doctorQualification = doctorQualification;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public LocalDateTime getAssignedTime() {
        return assignedTime;
    }

    public void setAssignedTime(LocalDateTime assignedTime) {
        this.assignedTime = assignedTime;
    }

    public LocalDateTime getStartedTime() {
        return startedTime;
    }

    public void setStartedTime(LocalDateTime startedTime) {
        this.startedTime = startedTime;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDoctorNotes() {
        return doctorNotes;
    }

    public void setDoctorNotes(String doctorNotes) {
        this.doctorNotes = doctorNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
