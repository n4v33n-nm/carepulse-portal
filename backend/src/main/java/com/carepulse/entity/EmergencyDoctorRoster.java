package com.carepulse.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
    name = "emergency_doctor_roster",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_emergency_roster_doc_date_shift", columnNames = {"doctor_id", "roster_date", "shift_name"})
    },
    indexes = {
        @Index(name = "idx_edr_roster_date", columnList = "roster_date"),
        @Index(name = "idx_edr_doctor_id", columnList = "doctor_id"),
        @Index(name = "idx_edr_duty_status", columnList = "duty_status"),
        @Index(name = "idx_edr_avail_status", columnList = "doctor_availability_status")
    }
)
public class EmergencyDoctorRoster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "roster_date", nullable = false)
    private LocalDate rosterDate;

    @Column(name = "shift_name", nullable = false, length = 30)
    private String shiftName = "MORNING"; // MORNING, EVENING, NIGHT, CUSTOM

    @Column(name = "shift_start", nullable = false)
    private LocalTime shiftStart;

    @Column(name = "shift_end", nullable = false)
    private LocalTime shiftEnd;

    @Column(name = "duty_status", nullable = false, length = 30)
    private String dutyStatus = "EMERGENCY_DUTY"; // EMERGENCY_DUTY, NOT_ASSIGNED

    @Column(name = "doctor_availability_status", nullable = false, length = 30)
    private String doctorAvailabilityStatus = "AVAILABLE"; // AVAILABLE, BUSY, IN_CONSULTATION, OFF_DUTY, ON_LEAVE

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public EmergencyDoctorRoster() {
    }

    public EmergencyDoctorRoster(Doctor doctor, LocalDate rosterDate, String shiftName, LocalTime shiftStart, LocalTime shiftEnd) {
        this.doctor = doctor;
        this.rosterDate = rosterDate;
        this.shiftName = shiftName;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.dutyStatus = "EMERGENCY_DUTY";
        this.doctorAvailabilityStatus = "AVAILABLE";
    }

    public EmergencyDoctorRoster(Doctor doctor, LocalDate rosterDate, String shiftName, LocalTime shiftStart, LocalTime shiftEnd, String dutyStatus, String doctorAvailabilityStatus) {
        this.doctor = doctor;
        this.rosterDate = rosterDate;
        this.shiftName = shiftName;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.dutyStatus = dutyStatus != null ? dutyStatus : "EMERGENCY_DUTY";
        this.doctorAvailabilityStatus = doctorAvailabilityStatus != null ? doctorAvailabilityStatus : "AVAILABLE";
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Determines whether the given date and time falls within this shift window.
     * Accurately handles normal shifts and cross-midnight shifts (e.g., 20:00 to 08:00 next morning).
     */
    public boolean isActiveAt(LocalDate checkDate, LocalTime checkTime) {
        if (checkDate == null || checkTime == null || rosterDate == null || shiftStart == null || shiftEnd == null) {
            return false;
        }

        LocalDateTime startDateTime = LocalDateTime.of(rosterDate, shiftStart);
        LocalDateTime endDateTime;

        if (shiftStart.isBefore(shiftEnd)) {
            endDateTime = LocalDateTime.of(rosterDate, shiftEnd);
        } else {
            // Crosses midnight or 24-hr shift
            endDateTime = LocalDateTime.of(rosterDate.plusDays(1), shiftEnd);
        }

        LocalDateTime checkDateTime = LocalDateTime.of(checkDate, checkTime);
        return (!checkDateTime.isBefore(startDateTime)) && checkDateTime.isBefore(endDateTime);
    }

    /**
     * Determines whether the given time falls within this shift.
     * Supports regular shifts (e.g., 08:00 - 14:00) as well as cross-midnight shifts (e.g., 20:00 - 08:00).
     */
    public boolean isActiveAt(LocalTime time) {
        if (time == null || shiftStart == null || shiftEnd == null) {
            return false;
        }
        if (shiftStart.isBefore(shiftEnd)) {
            return (!time.isBefore(shiftStart)) && time.isBefore(shiftEnd);
        } else if (shiftStart.isAfter(shiftEnd)) {
            // Crosses midnight, e.g. 20:00 - 08:00
            return (!time.isBefore(shiftStart)) || time.isBefore(shiftEnd);
        } else {
            // 24-hour shift where start == end
            return true;
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public LocalDate getRosterDate() {
        return rosterDate;
    }

    public void setRosterDate(LocalDate rosterDate) {
        this.rosterDate = rosterDate;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public LocalTime getShiftStart() {
        return shiftStart;
    }

    public void setShiftStart(LocalTime shiftStart) {
        this.shiftStart = shiftStart;
    }

    public LocalTime getShiftEnd() {
        return shiftEnd;
    }

    public void setShiftEnd(LocalTime shiftEnd) {
        this.shiftEnd = shiftEnd;
    }

    public String getDutyStatus() {
        return dutyStatus;
    }

    public void setDutyStatus(String dutyStatus) {
        this.dutyStatus = dutyStatus;
    }

    public String getDoctorAvailabilityStatus() {
        return doctorAvailabilityStatus;
    }

    public void setDoctorAvailabilityStatus(String doctorAvailabilityStatus) {
        this.doctorAvailabilityStatus = doctorAvailabilityStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
