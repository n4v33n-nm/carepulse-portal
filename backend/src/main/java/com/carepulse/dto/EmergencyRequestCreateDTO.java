package com.carepulse.dto;

import jakarta.validation.constraints.Size;

public class EmergencyRequestCreateDTO {

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    private String category = "General";

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    public EmergencyRequestCreateDTO() {
    }

    public EmergencyRequestCreateDTO(String category, String description) {
        this.category = category;
        this.description = description;
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
}
