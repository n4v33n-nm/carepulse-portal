package com.carepulse.dto;

import java.time.LocalTime;

public class ReplacementDoctorCandidateDTO {

    private Long doctorId;
    private String doctorName;
    private String specialization;
    private Integer experienceYears;
    private Double consultationFee;
    private boolean availableOnDay;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private boolean slotCollision;
    private boolean onApprovedLeave;
    private long dayAppointmentsCount;
    private boolean recommended;
    private String matchExplanation;

    public ReplacementDoctorCandidateDTO() {
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public Integer getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(Integer experienceYears) {
        this.experienceYears = experienceYears;
    }

    public Double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(Double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public boolean isAvailableOnDay() {
        return availableOnDay;
    }

    public void setAvailableOnDay(boolean availableOnDay) {
        this.availableOnDay = availableOnDay;
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

    public boolean isSlotCollision() {
        return slotCollision;
    }

    public void setSlotCollision(boolean slotCollision) {
        this.slotCollision = slotCollision;
    }

    public boolean isOnApprovedLeave() {
        return onApprovedLeave;
    }

    public void setOnApprovedLeave(boolean onApprovedLeave) {
        this.onApprovedLeave = onApprovedLeave;
    }

    public long getDayAppointmentsCount() {
        return dayAppointmentsCount;
    }

    public void setDayAppointmentsCount(long dayAppointmentsCount) {
        this.dayAppointmentsCount = dayAppointmentsCount;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public String getMatchExplanation() {
        return matchExplanation;
    }

    public void setMatchExplanation(String matchExplanation) {
        this.matchExplanation = matchExplanation;
    }
}

