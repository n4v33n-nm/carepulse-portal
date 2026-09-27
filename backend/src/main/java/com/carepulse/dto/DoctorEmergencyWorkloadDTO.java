package com.carepulse.dto;

public class DoctorEmergencyWorkloadDTO {

    private Long doctorId;
    private String doctorName;
    private String specialization;
    private String currentStatus;
    private boolean isEmergencyDutyToday;
    private String shiftName;
    private String shiftHours;
    private long todayTotalCases;
    private long todayCompletedCases;
    private long todayActiveCases;
    private long totalPast7DaysShifts;

    public DoctorEmergencyWorkloadDTO() {
    }

    public DoctorEmergencyWorkloadDTO(Long doctorId, String doctorName, String specialization,
                                      String currentStatus, boolean isEmergencyDutyToday,
                                      String shiftName, String shiftHours,
                                      long todayTotalCases, long todayCompletedCases,
                                      long todayActiveCases, long totalPast7DaysShifts) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.currentStatus = currentStatus;
        this.isEmergencyDutyToday = isEmergencyDutyToday;
        this.shiftName = shiftName;
        this.shiftHours = shiftHours;
        this.todayTotalCases = todayTotalCases;
        this.todayCompletedCases = todayCompletedCases;
        this.todayActiveCases = todayActiveCases;
        this.totalPast7DaysShifts = totalPast7DaysShifts;
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

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public boolean isEmergencyDutyToday() {
        return isEmergencyDutyToday;
    }

    public void setEmergencyDutyToday(boolean emergencyDutyToday) {
        isEmergencyDutyToday = emergencyDutyToday;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public String getShiftHours() {
        return shiftHours;
    }

    public void setShiftHours(String shiftHours) {
        this.shiftHours = shiftHours;
    }

    public long getTodayTotalCases() {
        return todayTotalCases;
    }

    public void setTodayTotalCases(long todayTotalCases) {
        this.todayTotalCases = todayTotalCases;
    }

    public long getTodayCompletedCases() {
        return todayCompletedCases;
    }

    public void setTodayCompletedCases(long todayCompletedCases) {
        this.todayCompletedCases = todayCompletedCases;
    }

    public long getTodayActiveCases() {
        return todayActiveCases;
    }

    public void setTodayActiveCases(long todayActiveCases) {
        this.todayActiveCases = todayActiveCases;
    }

    public long getTotalPast7DaysShifts() {
        return totalPast7DaysShifts;
    }

    public void setTotalPast7DaysShifts(long totalPast7DaysShifts) {
        this.totalPast7DaysShifts = totalPast7DaysShifts;
    }
}
