package com.carepulse.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class JoinWaitlistRequestDTO {

    private Long doctorId;
    private String specialization;

    @NotNull(message = "Preferred date is required")
    @FutureOrPresent(message = "Preferred date cannot be in the past")
    private LocalDate preferredDate;

    private LocalTime preferredTime;
    private String notes;

    public JoinWaitlistRequestDTO() {
    }

    public JoinWaitlistRequestDTO(Long doctorId, String specialization, LocalDate preferredDate, LocalTime preferredTime, String notes) {
        this.doctorId = doctorId;
        this.specialization = specialization;
        this.preferredDate = preferredDate;
        this.preferredTime = preferredTime;
        this.notes = notes;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public LocalDate getPreferredDate() {
        return preferredDate;
    }

    public void setPreferredDate(LocalDate preferredDate) {
        this.preferredDate = preferredDate;
    }

    public LocalTime getPreferredTime() {
        return preferredTime;
    }

    public void setPreferredTime(LocalTime preferredTime) {
        this.preferredTime = preferredTime;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
