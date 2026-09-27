package com.carepulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class EmergencyStatusUpdateRequestDTO {

    @NotBlank(message = "Status cannot be blank")
    private String status; // IN_PROGRESS, COMPLETED, CANCELLED

    @Size(max = 2000, message = "Doctor notes cannot exceed 2000 characters")
    private String doctorNotes;

    public EmergencyStatusUpdateRequestDTO() {
    }

    public EmergencyStatusUpdateRequestDTO(String status, String doctorNotes) {
        this.status = status;
        this.doctorNotes = doctorNotes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDoctorNotes() {
        return doctorNotes;
    }

    public void setDoctorNotes(String doctorNotes) {
        this.doctorNotes = doctorNotes;
    }
}
