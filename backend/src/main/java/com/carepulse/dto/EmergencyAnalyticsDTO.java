package com.carepulse.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmergencyAnalyticsDTO {

    private LocalDate date;
    private long totalRequests;
    private long waitingRequests;
    private long assignedRequests;
    private long inProgressRequests;
    private long completedRequests;
    private long cancelledRequests;
    private long noDoctorAvailableRequests;
    private List<DoctorEmergencyWorkloadDTO> doctorWorkloads = new ArrayList<>();

    public EmergencyAnalyticsDTO() {
    }

    public EmergencyAnalyticsDTO(LocalDate date, long totalRequests, long waitingRequests,
                                 long assignedRequests, long inProgressRequests,
                                 long completedRequests, long cancelledRequests,
                                 long noDoctorAvailableRequests,
                                 List<DoctorEmergencyWorkloadDTO> doctorWorkloads) {
        this.date = date;
        this.totalRequests = totalRequests;
        this.waitingRequests = waitingRequests;
        this.assignedRequests = assignedRequests;
        this.inProgressRequests = inProgressRequests;
        this.completedRequests = completedRequests;
        this.cancelledRequests = cancelledRequests;
        this.noDoctorAvailableRequests = noDoctorAvailableRequests;
        this.doctorWorkloads = doctorWorkloads != null ? doctorWorkloads : new ArrayList<>();
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public long getWaitingRequests() {
        return waitingRequests;
    }

    public void setWaitingRequests(long waitingRequests) {
        this.waitingRequests = waitingRequests;
    }

    public long getAssignedRequests() {
        return assignedRequests;
    }

    public void setAssignedRequests(long assignedRequests) {
        this.assignedRequests = assignedRequests;
    }

    public long getInProgressRequests() {
        return inProgressRequests;
    }

    public void setInProgressRequests(long inProgressRequests) {
        this.inProgressRequests = inProgressRequests;
    }

    public long getCompletedRequests() {
        return completedRequests;
    }

    public void setCompletedRequests(long completedRequests) {
        this.completedRequests = completedRequests;
    }

    public long getCancelledRequests() {
        return cancelledRequests;
    }

    public void setCancelledRequests(long cancelledRequests) {
        this.cancelledRequests = cancelledRequests;
    }

    public long getNoDoctorAvailableRequests() {
        return noDoctorAvailableRequests;
    }

    public void setNoDoctorAvailableRequests(long noDoctorAvailableRequests) {
        this.noDoctorAvailableRequests = noDoctorAvailableRequests;
    }

    public List<DoctorEmergencyWorkloadDTO> getDoctorWorkloads() {
        return doctorWorkloads;
    }

    public void setDoctorWorkloads(List<DoctorEmergencyWorkloadDTO> doctorWorkloads) {
        this.doctorWorkloads = doctorWorkloads;
    }
}
