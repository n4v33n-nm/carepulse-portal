package com.carepulse.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class UserProfileUpdateRequest {

    @Size(max = 120, message = "Full name cannot exceed 120 characters")
    private String fullName;

    @Size(max = 30, message = "Phone cannot exceed 30 characters")
    private String phone;

    @Pattern(regexp = "SIMPLE|SUPPORTIVE|PROFESSIONAL", message = "Communication preference must be SIMPLE, SUPPORTIVE, or PROFESSIONAL")
    private String communicationPreference; // SIMPLE, SUPPORTIVE, PROFESSIONAL
    
    // Patient specific
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Size(max = 60, message = "Emergency contact cannot exceed 60 characters")
    private String emergencyContact;

    @Size(max = 10, message = "Blood group cannot exceed 10 characters")
    private String bloodGroup;

    // Doctor specific
    private String bio;

    @Positive(message = "Consultation fee must be positive")
    private Double consultationFee;

    @Pattern(regexp = "AVAILABLE|BUSY|ON_LEAVE", message = "Availability status must be AVAILABLE, BUSY, or ON_LEAVE")
    private String availabilityStatus; // AVAILABLE, BUSY, ON_LEAVE

    public UserProfileUpdateRequest() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCommunicationPreference() {
        return communicationPreference;
    }

    public void setCommunicationPreference(String communicationPreference) {
        this.communicationPreference = communicationPreference;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(Double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
}
