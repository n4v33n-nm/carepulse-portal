package com.carepulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DoctorLeaveReviewRequest {

    @NotBlank(message = "Review status is required")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "Status must be either APPROVED or REJECTED")
    private String status;

    @Size(max = 1000, message = "Admin notes cannot exceed 1000 characters")
    private String adminNotes;

    public DoctorLeaveReviewRequest() {
    }

    public DoctorLeaveReviewRequest(String status, String adminNotes) {
        this.status = status;
        this.adminNotes = adminNotes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }
}

