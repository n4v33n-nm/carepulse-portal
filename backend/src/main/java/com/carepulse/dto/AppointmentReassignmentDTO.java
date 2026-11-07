package com.carepulse.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AppointmentReassignmentDTO {

    private Long id;
    private Long appointmentId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String appointmentReason;
    private String appointmentStatus;

    private Long patientId;
    private String patientName;
    private String patientEmail;

    private Long doctorLeaveId;

    private Long originalDoctorId;
    private String originalDoctorName;
    private String originalDoctorSpecialization;

    private Long replacementDoctorId;
    private String replacementDoctorName;
    private String replacementDoctorSpecialization;

    private Long reassignedByAdminId;
    private String reassignedByAdminName;

    private String patientConsentStatus; // PROPOSED, ACCEPTED, REJECTED
    private String patientNotes;
    private String adminNotes;
    private LocalDateTime reassignedAt;
    private LocalDateTime patientRespondedAt;

    public AppointmentReassignmentDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public LocalTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public String getAppointmentReason() {
        return appointmentReason;
    }

    public void setAppointmentReason(String appointmentReason) {
        this.appointmentReason = appointmentReason;
    }

    public String getAppointmentStatus() {
        return appointmentStatus;
    }

    public void setAppointmentStatus(String appointmentStatus) {
        this.appointmentStatus = appointmentStatus;
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

    public String getPatientEmail() {
        return patientEmail;
    }

    public void setPatientEmail(String patientEmail) {
        this.patientEmail = patientEmail;
    }

    public Long getDoctorLeaveId() {
        return doctorLeaveId;
    }

    public void setDoctorLeaveId(Long doctorLeaveId) {
        this.doctorLeaveId = doctorLeaveId;
    }

    public Long getOriginalDoctorId() {
        return originalDoctorId;
    }

    public void setOriginalDoctorId(Long originalDoctorId) {
        this.originalDoctorId = originalDoctorId;
    }

    public String getOriginalDoctorName() {
        return originalDoctorName;
    }

    public void setOriginalDoctorName(String originalDoctorName) {
        this.originalDoctorName = originalDoctorName;
    }

    public String getOriginalDoctorSpecialization() {
        return originalDoctorSpecialization;
    }

    public void setOriginalDoctorSpecialization(String originalDoctorSpecialization) {
        this.originalDoctorSpecialization = originalDoctorSpecialization;
    }

    public Long getReplacementDoctorId() {
        return replacementDoctorId;
    }

    public void setReplacementDoctorId(Long replacementDoctorId) {
        this.replacementDoctorId = replacementDoctorId;
    }

    public String getReplacementDoctorName() {
        return replacementDoctorName;
    }

    public void setReplacementDoctorName(String replacementDoctorName) {
        this.replacementDoctorName = replacementDoctorName;
    }

    public String getReplacementDoctorSpecialization() {
        return replacementDoctorSpecialization;
    }

    public void setReplacementDoctorSpecialization(String replacementDoctorSpecialization) {
        this.replacementDoctorSpecialization = replacementDoctorSpecialization;
    }

    public Long getReassignedByAdminId() {
        return reassignedByAdminId;
    }

    public void setReassignedByAdminId(Long reassignedByAdminId) {
        this.reassignedByAdminId = reassignedByAdminId;
    }

    public String getReassignedByAdminName() {
        return reassignedByAdminName;
    }

    public void setReassignedByAdminName(String reassignedByAdminName) {
        this.reassignedByAdminName = reassignedByAdminName;
    }

    public String getPatientConsentStatus() {
        return patientConsentStatus;
    }

    public void setPatientConsentStatus(String patientConsentStatus) {
        this.patientConsentStatus = patientConsentStatus;
    }

    public String getPatientNotes() {
        return patientNotes;
    }

    public void setPatientNotes(String patientNotes) {
        this.patientNotes = patientNotes;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public LocalDateTime getReassignedAt() {
        return reassignedAt;
    }

    public void setReassignedAt(LocalDateTime reassignedAt) {
        this.reassignedAt = reassignedAt;
    }

    public LocalDateTime getPatientRespondedAt() {
        return patientRespondedAt;
    }

    public void setPatientRespondedAt(LocalDateTime patientRespondedAt) {
        this.patientRespondedAt = patientRespondedAt;
    }
}

