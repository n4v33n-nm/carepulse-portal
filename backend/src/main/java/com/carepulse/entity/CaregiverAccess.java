package com.carepulse.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "caregiver_access")
public class CaregiverAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "caregiver_email", nullable = false, length = 120)
    private String caregiverEmail;

    @Column(name = "caregiver_name", length = 120)
    private String caregiverName;

    @Column(name = "relationship", length = 60)
    private String relationship;

    @Column(nullable = false, length = 60)
    private String permission = "VIEW_ALL"; // VIEW_APPOINTMENTS, VIEW_MEDICAL_RECORDS, VIEW_PRESCRIPTIONS, VIEW_ALL

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE"; // ACTIVE, REVOKED

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public CaregiverAccess() {
    }

    public CaregiverAccess(Patient patient, String caregiverEmail, String caregiverName, String relationship, String permission) {
        this.patient = patient;
        this.caregiverEmail = caregiverEmail;
        this.caregiverName = caregiverName;
        this.relationship = relationship;
        this.permission = permission != null ? permission : "VIEW_ALL";
        this.status = "ACTIVE";
    }

    @PrePersist
    protected void onCreate() {
        this.grantedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(LocalDateTime grantedAt) {
        this.grantedAt = grantedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }
}
