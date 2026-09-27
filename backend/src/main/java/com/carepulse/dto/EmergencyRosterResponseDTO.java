package com.carepulse.dto;

import com.carepulse.entity.EmergencyDoctorRoster;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class EmergencyRosterResponseDTO {

    private Long id;

    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private String doctorPhone;
    private String doctorQualification;
    private Double doctorRating;

    private LocalDate rosterDate;
    private String shiftName;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private String dutyStatus;
    private String doctorAvailabilityStatus;

    private long activeEmergencyCasesCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public EmergencyRosterResponseDTO() {
    }

    public static EmergencyRosterResponseDTO fromEntity(EmergencyDoctorRoster roster, long activeEmergencyCasesCount) {
        if (roster == null) return null;

        EmergencyRosterResponseDTO dto = new EmergencyRosterResponseDTO();
        dto.setId(roster.getId());

        if (roster.getDoctor() != null) {
            dto.setDoctorId(roster.getDoctor().getId());
            dto.setDoctorName(roster.getDoctor().getFullName());
            dto.setDoctorSpecialization(roster.getDoctor().getSpecialization());
            dto.setDoctorPhone(roster.getDoctor().getPhone());
            dto.setDoctorQualification(roster.getDoctor().getQualification());
            dto.setDoctorRating(roster.getDoctor().getRating());
        }

        dto.setRosterDate(roster.getRosterDate());
        dto.setShiftName(roster.getShiftName());
        dto.setShiftStart(roster.getShiftStart());
        dto.setShiftEnd(roster.getShiftEnd());
        dto.setDutyStatus(roster.getDutyStatus());
        dto.setDoctorAvailabilityStatus(roster.getDoctorAvailabilityStatus());
        dto.setActiveEmergencyCasesCount(activeEmergencyCasesCount);
        dto.setCreatedAt(roster.getCreatedAt());
        dto.setUpdatedAt(roster.getUpdatedAt());

        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Double getDoctorRating() {
        return doctorRating;
    }

    public void setDoctorRating(Double doctorRating) {
        this.doctorRating = doctorRating;
    }

    public LocalDate getRosterDate() {
        return rosterDate;
    }

    public void setRosterDate(LocalDate rosterDate) {
        this.rosterDate = rosterDate;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public LocalTime getShiftStart() {
        return shiftStart;
    }

    public void setShiftStart(LocalTime shiftStart) {
        this.shiftStart = shiftStart;
    }

    public LocalTime getShiftEnd() {
        return shiftEnd;
    }

    public void setShiftEnd(LocalTime shiftEnd) {
        this.shiftEnd = shiftEnd;
    }

    public String getDutyStatus() {
        return dutyStatus;
    }

    public void setDutyStatus(String dutyStatus) {
        this.dutyStatus = dutyStatus;
    }

    public String getDoctorAvailabilityStatus() {
        return doctorAvailabilityStatus;
    }

    public void setDoctorAvailabilityStatus(String doctorAvailabilityStatus) {
        this.doctorAvailabilityStatus = doctorAvailabilityStatus;
    }

    public long getActiveEmergencyCasesCount() {
        return activeEmergencyCasesCount;
    }

    public void setActiveEmergencyCasesCount(long activeEmergencyCasesCount) {
        this.activeEmergencyCasesCount = activeEmergencyCasesCount;
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
