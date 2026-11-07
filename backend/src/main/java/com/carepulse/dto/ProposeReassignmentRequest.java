package com.carepulse.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProposeReassignmentRequest {

    @NotNull(message = "Replacement doctor ID is mandatory")
    private Long replacementDoctorId;

    @Size(max = 500, message = "Admin notes cannot exceed 500 characters")
    private String adminNotes;

    public ProposeReassignmentRequest() {
    }

    public ProposeReassignmentRequest(Long replacementDoctorId, String adminNotes) {
        this.replacementDoctorId = replacementDoctorId;
        this.adminNotes = adminNotes;
    }

    public Long getReplacementDoctorId() {
        return replacementDoctorId;
    }

    public void setReplacementDoctorId(Long replacementDoctorId) {
        this.replacementDoctorId = replacementDoctorId;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
}

