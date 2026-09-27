package com.carepulse.dto;

import jakarta.validation.constraints.NotBlank;

public class DoctorStatusUpdateRequestDTO {

    @NotBlank(message = "Availability status is required")
    private String availabilityStatus; // AVAILABLE, BUSY, IN_CONSULTATION, OFF_DUTY, ON_LEAVE

    public DoctorStatusUpdateRequestDTO() {
    }

    public DoctorStatusUpdateRequestDTO(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
}
