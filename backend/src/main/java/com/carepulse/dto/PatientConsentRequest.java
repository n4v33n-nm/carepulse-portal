package com.carepulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PatientConsentRequest {

    @NotBlank(message = "Action is mandatory")
    @Pattern(regexp = "^(ACCEPT|REJECT)$", message = "Action must be either ACCEPT or REJECT")
    private String action;

    @Size(max = 500, message = "Patient notes cannot exceed 500 characters")
    private String patientNotes;

    public PatientConsentRequest() {
    }

    public PatientConsentRequest(String action, String patientNotes) {
        this.action = action;
        this.patientNotes = patientNotes;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPatientNotes() {
        return patientNotes;
    }

    public void setPatientNotes(String patientNotes) {
        this.patientNotes = patientNotes;
    }
}

