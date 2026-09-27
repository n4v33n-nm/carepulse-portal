package com.carepulse.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentWaitTimeResponseDTO {

    private Long appointmentId;
    private Long doctorId;
    private String doctorName;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private int patientsAheadCount;
    private int averageConsultationMinutes;
    private int estimatedWaitMinutes;
    private LocalTime estimatedConsultationStart;
    private String doctorStatus;
    private boolean doctorOnEmergencyDuty;
    private String explanation;

    public AppointmentWaitTimeResponseDTO() {
    }

    public AppointmentWaitTimeResponseDTO(Long appointmentId, Long doctorId, String doctorName,
                                         LocalDate appointmentDate, LocalTime appointmentTime,
                                         int patientsAheadCount, int averageConsultationMinutes,
                                         int estimatedWaitMinutes, LocalTime estimatedConsultationStart,
                                         String doctorStatus, boolean doctorOnEmergencyDuty,
                                         String explanation) {
        this.appointmentId = appointmentId;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.appointmentDate = appointmentDate;
        this.appointmentTime = appointmentTime;
        this.patientsAheadCount = patientsAheadCount;
        this.averageConsultationMinutes = averageConsultationMinutes;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
        this.estimatedConsultationStart = estimatedConsultationStart;
        this.doctorStatus = doctorStatus;
        this.doctorOnEmergencyDuty = doctorOnEmergencyDuty;
        this.explanation = explanation;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
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

    public int getPatientsAheadCount() {
        return patientsAheadCount;
    }

    public void setPatientsAheadCount(int patientsAheadCount) {
        this.patientsAheadCount = patientsAheadCount;
    }

    public int getAverageConsultationMinutes() {
        return averageConsultationMinutes;
    }

    public void setAverageConsultationMinutes(int averageConsultationMinutes) {
        this.averageConsultationMinutes = averageConsultationMinutes;
    }

    public int getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(int estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public LocalTime getEstimatedConsultationStart() {
        return estimatedConsultationStart;
    }

    public void setEstimatedConsultationStart(LocalTime estimatedConsultationStart) {
        this.estimatedConsultationStart = estimatedConsultationStart;
    }

    public String getDoctorStatus() {
        return doctorStatus;
    }

    public void setDoctorStatus(String doctorStatus) {
        this.doctorStatus = doctorStatus;
    }

    public boolean isDoctorOnEmergencyDuty() {
        return doctorOnEmergencyDuty;
    }

    public void setDoctorOnEmergencyDuty(boolean doctorOnEmergencyDuty) {
        this.doctorOnEmergencyDuty = doctorOnEmergencyDuty;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
