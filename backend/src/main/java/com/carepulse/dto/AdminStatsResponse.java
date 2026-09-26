package com.carepulse.dto;

import java.util.Map;

public class AdminStatsResponse {

    private long totalPatients;
    private long totalDoctors;
    private long totalAppointments;
    private long activeUsers;
    private Map<String, Long> appointmentsByStatus;

    public AdminStatsResponse() {
    }

    public AdminStatsResponse(long totalPatients, long totalDoctors, long totalAppointments, long activeUsers, Map<String, Long> appointmentsByStatus) {
        this.totalPatients = totalPatients;
        this.totalDoctors = totalDoctors;
        this.totalAppointments = totalAppointments;
        this.activeUsers = activeUsers;
        this.appointmentsByStatus = appointmentsByStatus;
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

    public long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public Map<String, Long> getAppointmentsByStatus() {
        return appointmentsByStatus;
    }

    public void setAppointmentsByStatus(Map<String, Long> appointmentsByStatus) {
        this.appointmentsByStatus = appointmentsByStatus;
    }
}
