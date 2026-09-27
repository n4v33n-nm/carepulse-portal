package com.carepulse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CaregiverRequest {

    @NotBlank(message = "Caregiver email is required")
    @Email(message = "Invalid email format")
    @Size(max = 120, message = "Email cannot exceed 120 characters")
    private String caregiverEmail;

    @Size(max = 120, message = "Caregiver name cannot exceed 120 characters")
    private String caregiverName;

    @Size(max = 60, message = "Relationship cannot exceed 60 characters")
    private String relationship;

    @NotBlank(message = "Permission is required")
    @Pattern(regexp = "VIEW_APPOINTMENTS|VIEW_MEDICAL_RECORDS|VIEW_PRESCRIPTIONS|VIEW_ALL",
            message = "Permission must be VIEW_APPOINTMENTS, VIEW_MEDICAL_RECORDS, VIEW_PRESCRIPTIONS, or VIEW_ALL")
    private String permission; // VIEW_APPOINTMENTS, VIEW_MEDICAL_RECORDS, VIEW_PRESCRIPTIONS, VIEW_ALL

    public CaregiverRequest() {
    }

    public String getCaregiverEmail() {
        return caregiverEmail;
    }

    public void setCaregiverEmail(String caregiverEmail) {
        this.caregiverEmail = caregiverEmail;
    }

    public String getCaregiverName() {
        return caregiverName;
    }

    public void setCaregiverName(String caregiverName) {
        this.caregiverName = caregiverName;
    }

    public String getRelationship() {
        return relationship;
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }
}
