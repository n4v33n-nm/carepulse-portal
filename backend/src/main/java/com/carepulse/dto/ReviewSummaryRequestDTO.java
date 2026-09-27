package com.carepulse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ReviewSummaryRequestDTO {

    @NotBlank(message = "Action is required")
    @Pattern(regexp = "^(APPROVE|EDIT|REJECT)$", message = "Action must be APPROVE, EDIT, or REJECT")
    private String action; // APPROVE, EDIT, REJECT

    private String editedSummary;

    public ReviewSummaryRequestDTO() {
    }

    public ReviewSummaryRequestDTO(String action, String editedSummary) {
        this.action = action;
        this.editedSummary = editedSummary;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEditedSummary() {
        return editedSummary;
    }

    public void setEditedSummary(String editedSummary) {
        this.editedSummary = editedSummary;
    }
}
