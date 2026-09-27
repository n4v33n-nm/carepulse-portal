package com.carepulse.dto;

import java.util.List;
import java.util.Map;

public class AdminAnalyticsDTO {

    private long totalPatients;
    private long totalDoctors;
    private long totalAppointments;
    private long todayAppointments;
    private long completedAppointments;
    private long cancelledAppointments;
    private long activeWaitlistEntries;
    private long totalEmergencyRequests;
    private long assignedEmergencyRequests;
    private long inProgressEmergencyRequests;
    private long completedEmergencyRequests;
    private long waitingEmergencyRequests;
    private long noDoctorAvailableEmergencyRequests;
    private Double averageEmergencyAssignmentSeconds;
    private List<DoctorWorkloadDTO> doctorWorkloads;
    private Map<String, Long> appointmentTrendsPast7Days;

    public AdminAnalyticsDTO() {
    }

    public AdminAnalyticsDTO(long totalPatients, long totalDoctors, long totalAppointments,
                             long todayAppointments, long completedAppointments,
                             long cancelledAppointments, long activeWaitlistEntries,
                             long totalEmergencyRequests, long assignedEmergencyRequests,
                             long inProgressEmergencyRequests, long completedEmergencyRequests,
                             long waitingEmergencyRequests, long noDoctorAvailableEmergencyRequests,
                             Double averageEmergencyAssignmentSeconds,
                             List<DoctorWorkloadDTO> doctorWorkloads,
                             Map<String, Long> appointmentTrendsPast7Days) {
        this.totalPatients = totalPatients;
        this.totalDoctors = totalDoctors;
        this.totalAppointments = totalAppointments;
        this.todayAppointments = todayAppointments;
        this.completedAppointments = completedAppointments;
        this.cancelledAppointments = cancelledAppointments;
        this.activeWaitlistEntries = activeWaitlistEntries;
        this.totalEmergencyRequests = totalEmergencyRequests;
        this.assignedEmergencyRequests = assignedEmergencyRequests;
        this.inProgressEmergencyRequests = inProgressEmergencyRequests;
        this.completedEmergencyRequests = completedEmergencyRequests;
        this.waitingEmergencyRequests = waitingEmergencyRequests;
        this.noDoctorAvailableEmergencyRequests = noDoctorAvailableEmergencyRequests;
        this.averageEmergencyAssignmentSeconds = averageEmergencyAssignmentSeconds;
        this.doctorWorkloads = doctorWorkloads;
        this.appointmentTrendsPast7Days = appointmentTrendsPast7Days;
    }

    public long getTotalPatients() {
        return totalPatients;
    }

    public void setTotalPatients(long totalPatients) {
        this.totalPatients = totalPatients;
    }

    public long getTotalDoctors() {
        return totalDoctors;
    }

    public void setTotalDoctors(long totalDoctors) {
        this.totalDoctors = totalDoctors;
    }

    public long getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(long totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public long getTodayAppointments() {
        return todayAppointments;
    }

    public void setTodayAppointments(long todayAppointments) {
        this.todayAppointments = todayAppointments;
    }

    public long getCompletedAppointments() {
        return completedAppointments;
    }

    public void setCompletedAppointments(long completedAppointments) {
        this.completedAppointments = completedAppointments;
    }

    public long getCancelledAppointments() {
        return cancelledAppointments;
    }

    public void setCancelledAppointments(long cancelledAppointments) {
        this.cancelledAppointments = cancelledAppointments;
    }

    public long getActiveWaitlistEntries() {
        return activeWaitlistEntries;
    }

    public void setActiveWaitlistEntries(long activeWaitlistEntries) {
        this.activeWaitlistEntries = activeWaitlistEntries;
    }

    public long getTotalEmergencyRequests() {
        return totalEmergencyRequests;
    }

    public void setTotalEmergencyRequests(long totalEmergencyRequests) {
        this.totalEmergencyRequests = totalEmergencyRequests;
    }

    public long getAssignedEmergencyRequests() {
        return assignedEmergencyRequests;
    }

    public void setAssignedEmergencyRequests(long assignedEmergencyRequests) {
        this.assignedEmergencyRequests = assignedEmergencyRequests;
    }

    public long getInProgressEmergencyRequests() {
        return inProgressEmergencyRequests;
    }

    public void setInProgressEmergencyRequests(long inProgressEmergencyRequests) {
        this.inProgressEmergencyRequests = inProgressEmergencyRequests;
    }

    public long getCompletedEmergencyRequests() {
        return completedEmergencyRequests;
    }

    public void setCompletedEmergencyRequests(long completedEmergencyRequests) {
        this.completedEmergencyRequests = completedEmergencyRequests;
    }

    public long getWaitingEmergencyRequests() {
        return waitingEmergencyRequests;
    }

    public void setWaitingEmergencyRequests(long waitingEmergencyRequests) {
        this.waitingEmergencyRequests = waitingEmergencyRequests;
    }

    public long getNoDoctorAvailableEmergencyRequests() {
        return noDoctorAvailableEmergencyRequests;
    }

    public void setNoDoctorAvailableEmergencyRequests(long noDoctorAvailableEmergencyRequests) {
        this.noDoctorAvailableEmergencyRequests = noDoctorAvailableEmergencyRequests;
    }

    public Double getAverageEmergencyAssignmentSeconds() {
        return averageEmergencyAssignmentSeconds;
    }

    public void setAverageEmergencyAssignmentSeconds(Double averageEmergencyAssignmentSeconds) {
        this.averageEmergencyAssignmentSeconds = averageEmergencyAssignmentSeconds;
    }

    public List<DoctorWorkloadDTO> getDoctorWorkloads() {
        return doctorWorkloads;
    }

    public void setDoctorWorkloads(List<DoctorWorkloadDTO> doctorWorkloads) {
        this.doctorWorkloads = doctorWorkloads;
    }

    public Map<String, Long> getAppointmentTrendsPast7Days() {
        return appointmentTrendsPast7Days;
    }

    public void setAppointmentTrendsPast7Days(Map<String, Long> appointmentTrendsPast7Days) {
        this.appointmentTrendsPast7Days = appointmentTrendsPast7Days;
    }
}
