package com.carepulse.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CaregiverRequest {

    @NotBlank(message = "Caregiver email is required")
    @Email(message = "Invalid email format")
    private String caregiverEmail;

    private String caregiverName;
    private String relationship;

    @NotBlank(message = "Permission is required")
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
