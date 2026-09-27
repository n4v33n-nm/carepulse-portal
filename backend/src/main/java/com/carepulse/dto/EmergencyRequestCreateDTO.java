package com.carepulse.dto;

import jakarta.validation.constraints.Size;

public class EmergencyRequestCreateDTO {

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category = "General";

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    private String priority = "NORMAL"; // NORMAL, URGENT

    public EmergencyRequestCreateDTO() {
    }

    public EmergencyRequestCreateDTO(String category, String description) {
        this.category = category;
        this.description = description;
        this.priority = "NORMAL";
    }

    public EmergencyRequestCreateDTO(String category, String description, String priority) {
        this.category = category;
        this.description = description;
        this.priority = priority != null && !priority.isBlank() ? priority.toUpperCase() : "NORMAL";
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
