package com.carepulse.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class DoctorLeaveRequest {

    @NotNull(message = "Start date is mandatory")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is mandatory")
    private LocalDate endDate;

    @NotBlank(message = "Reason for leave is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    public DoctorLeaveRequest() {
    }

    public DoctorLeaveRequest(LocalDate startDate, LocalDate endDate, String reason) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

