package com.carepulse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;

public class DoctorTodayDutyDTO {

    @JsonProperty("isEmergencyDuty")
    private boolean isEmergencyDuty;

    private String shift;
    private String shiftStart;
    private String shiftEnd;
    private String status;
    private long emergencyRequestsCount;
    private long todayTotalCases;
    private long todayCompletedCases;
    private long todayActiveCases;
    private Long rosterId;
    private List<EmergencyRosterResponseDTO> allTodayShifts = new ArrayList<>();

    public DoctorTodayDutyDTO() {
    }

    public DoctorTodayDutyDTO(boolean isEmergencyDuty, String shift, String shiftStart, String shiftEnd, String status, long emergencyRequestsCount, Long rosterId) {
        this.isEmergencyDuty = isEmergencyDuty;
        this.shift = shift;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.status = status;
        this.emergencyRequestsCount = emergencyRequestsCount;
        this.rosterId = rosterId;
        this.todayTotalCases = emergencyRequestsCount;
        this.todayActiveCases = emergencyRequestsCount;
    }

    public DoctorTodayDutyDTO(boolean isEmergencyDuty, String shift, String shiftStart, String shiftEnd, String status,
                              long emergencyRequestsCount, long todayTotalCases, long todayCompletedCases, long todayActiveCases, Long rosterId) {
        this.isEmergencyDuty = isEmergencyDuty;
        this.shift = shift;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.status = status;
        this.emergencyRequestsCount = emergencyRequestsCount;
        this.todayTotalCases = todayTotalCases;
        this.todayCompletedCases = todayCompletedCases;
        this.todayActiveCases = todayActiveCases;
        this.rosterId = rosterId;
    }

    public boolean isEmergencyDuty() {
        return isEmergencyDuty;
    }

    public boolean getIsEmergencyDuty() {
        return isEmergencyDuty;
    }

    public void setEmergencyDuty(boolean emergencyDuty) {
        isEmergencyDuty = emergencyDuty;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getShiftStart() {
        return shiftStart;
    }

    public void setShiftStart(String shiftStart) {
        this.shiftStart = shiftStart;
    }

    public String getShiftEnd() {
        return shiftEnd;
    }

    public void setShiftEnd(String shiftEnd) {
        this.shiftEnd = shiftEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getEmergencyRequestsCount() {
        return emergencyRequestsCount;
    }

    public void setEmergencyRequestsCount(long emergencyRequestsCount) {
        this.emergencyRequestsCount = emergencyRequestsCount;
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

    public Long getRosterId() {
        return rosterId;
    }

    public void setRosterId(Long rosterId) {
        this.rosterId = rosterId;
    }

    public List<EmergencyRosterResponseDTO> getAllTodayShifts() {
        return allTodayShifts;
    }

    public void setAllTodayShifts(List<EmergencyRosterResponseDTO> allTodayShifts) {
        this.allTodayShifts = allTodayShifts;
    }
}
