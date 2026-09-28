package com.carepulse.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class AppointmentRescheduleRequestDTO {

    @NotNull(message = "New appointment date is required")
    @FutureOrPresent(message = "New appointment date cannot be in the past")
    private LocalDate newDate;

    @NotNull(message = "New appointment time is required")
    private LocalTime newTime;

    private String reason;

    public AppointmentRescheduleRequestDTO() {
    }

    public AppointmentRescheduleRequestDTO(LocalDate newDate, LocalTime newTime, String reason) {
        this.newDate = newDate;
        this.newTime = newTime;
        this.reason = reason;
    }

    public LocalDate getNewDate() {
        return newDate;
    }

    public void setNewDate(LocalDate newDate) {
        this.newDate = newDate;
    }

    public LocalTime getNewTime() {
        return newTime;
    }

    public void setNewTime(LocalTime newTime) {
        this.newTime = newTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
