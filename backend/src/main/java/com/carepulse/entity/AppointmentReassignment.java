package com.carepulse.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_reassignments", indexes = {
        @Index(name = "idx_reassign_appt", columnList = "appointment_id"),
        @Index(name = "idx_reassign_orig_doc", columnList = "original_doctor_id"),
        @Index(name = "idx_reassign_repl_doc", columnList = "replacement_doctor_id"),
        @Index(name = "idx_reassign_consent", columnList = "patient_consent_status")
})
public class AppointmentReassignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_leave_id")
    private DoctorLeave doctorLeave;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "original_doctor_id", nullable = false)
    private Doctor originalDoctor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "replacement_doctor_id", nullable = false)
    private Doctor replacementDoctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reassigned_by_id", nullable = false)
    private User reassignedByAdmin;

    @Column(name = "patient_consent_status", nullable = false, length = 30)
    private String patientConsentStatus = "PROPOSED"; // PROPOSED, ACCEPTED, REJECTED

    @Column(name = "patient_notes", length = 500)
    private String patientNotes;

    @Column(name = "admin_notes", length = 500)
    private String adminNotes;

    @Column(name = "reassigned_at", nullable = false)
    private LocalDateTime reassignedAt;

    @Column(name = "patient_responded_at")
    private LocalDateTime patientRespondedAt;

    public AppointmentReassignment() {
    }

    public AppointmentReassignment(Appointment appointment, DoctorLeave doctorLeave, Doctor originalDoctor,
                                   Doctor replacementDoctor, User reassignedByAdmin, String adminNotes) {
        this.appointment = appointment;
        this.doctorLeave = doctorLeave;
        this.originalDoctor = originalDoctor;
        this.replacementDoctor = replacementDoctor;
        this.reassignedByAdmin = reassignedByAdmin;
        this.adminNotes = adminNotes;
        this.patientConsentStatus = "PROPOSED";
        this.reassignedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.reassignedAt == null) {
            this.reassignedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public DoctorLeave getDoctorLeave() {
        return doctorLeave;
    }

    public void setDoctorLeave(DoctorLeave doctorLeave) {
        this.doctorLeave = doctorLeave;
    }

    public Doctor getOriginalDoctor() {
        return originalDoctor;
    }

    public void setOriginalDoctor(Doctor originalDoctor) {
        this.originalDoctor = originalDoctor;
    }

    public Doctor getReplacementDoctor() {
        return replacementDoctor;
    }

    public void setReplacementDoctor(Doctor replacementDoctor) {
        this.replacementDoctor = replacementDoctor;
    }

    public User getReassignedByAdmin() {
        return reassignedByAdmin;
    }

    public void setReassignedByAdmin(User reassignedByAdmin) {
        this.reassignedByAdmin = reassignedByAdmin;
    }

    public String getPatientConsentStatus() {
        return patientConsentStatus;
    }

    public void setPatientConsentStatus(String patientConsentStatus) {
        this.patientConsentStatus = patientConsentStatus;
    }

    public String getPatientNotes() {
        return patientNotes;
    }

    public void setPatientNotes(String patientNotes) {
        this.patientNotes = patientNotes;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public LocalDateTime getReassignedAt() {
        return reassignedAt;
    }

    public void setReassignedAt(LocalDateTime reassignedAt) {
        this.reassignedAt = reassignedAt;
    }

    public LocalDateTime getPatientRespondedAt() {
        return patientRespondedAt;
    }

    public void setPatientRespondedAt(LocalDateTime patientRespondedAt) {
        this.patientRespondedAt = patientRespondedAt;
    }
}

