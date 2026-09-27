package com.carepulse.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public class EmergencyRosterRequestDTO {

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Roster date is required")
    private LocalDate rosterDate;

    private String shiftName = "MORNING"; // MORNING, EVENING, NIGHT, CUSTOM

    private LocalTime shiftStart;

    private LocalTime shiftEnd;

    private String dutyStatus = "EMERGENCY_DUTY"; // EMERGENCY_DUTY, NOT_ASSIGNED

    private String doctorAvailabilityStatus = "AVAILABLE"; // AVAILABLE, BUSY, IN_CONSULTATION, OFF_DUTY, ON_LEAVE

    public EmergencyRosterRequestDTO() {
    }

    public EmergencyRosterRequestDTO(Long doctorId, LocalDate rosterDate, String shiftName, LocalTime shiftStart, LocalTime shiftEnd) {
        this.doctorId = doctorId;
        this.rosterDate = rosterDate;
        this.shiftName = shiftName;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.dutyStatus = "EMERGENCY_DUTY";
        this.doctorAvailabilityStatus = "AVAILABLE";
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
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
}
