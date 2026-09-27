package com.carepulse.dto;

public class DoctorWorkloadDTO {

    private Long doctorId;
    private String doctorName;
    private String specialization;
    private int normalAppointmentsToday;
    private int emergencyRequestsToday;
    private int totalWorkloadToday;
    private int activeEmergencyCases;
    private String currentAvailabilityStatus;
    private boolean isEmergencyDutyToday;
    private String dutyShift;

    public DoctorWorkloadDTO() {
    }

    public DoctorWorkloadDTO(Long doctorId, String doctorName, String specialization,
                             int normalAppointmentsToday, int emergencyRequestsToday,
                             int totalWorkloadToday, int activeEmergencyCases,
                             String currentAvailabilityStatus, boolean isEmergencyDutyToday,
                             String dutyShift) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.normalAppointmentsToday = normalAppointmentsToday;
        this.emergencyRequestsToday = emergencyRequestsToday;
        this.totalWorkloadToday = totalWorkloadToday;
        this.activeEmergencyCases = activeEmergencyCases;
        this.currentAvailabilityStatus = currentAvailabilityStatus;
        this.isEmergencyDutyToday = isEmergencyDutyToday;
        this.dutyShift = dutyShift;
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

    public int getNormalAppointmentsToday() {
        return normalAppointmentsToday;
    }

    public void setNormalAppointmentsToday(int normalAppointmentsToday) {
        this.normalAppointmentsToday = normalAppointmentsToday;
    }

    public int getEmergencyRequestsToday() {
        return emergencyRequestsToday;
    }

    public void setEmergencyRequestsToday(int emergencyRequestsToday) {
        this.emergencyRequestsToday = emergencyRequestsToday;
    }

    public int getTotalWorkloadToday() {
        return totalWorkloadToday;
    }

    public void setTotalWorkloadToday(int totalWorkloadToday) {
        this.totalWorkloadToday = totalWorkloadToday;
    }

    public int getActiveEmergencyCases() {
        return activeEmergencyCases;
    }

    public void setActiveEmergencyCases(int activeEmergencyCases) {
        this.activeEmergencyCases = activeEmergencyCases;
    }

    public String getCurrentAvailabilityStatus() {
        return currentAvailabilityStatus;
    }

    public void setCurrentAvailabilityStatus(String currentAvailabilityStatus) {
        this.currentAvailabilityStatus = currentAvailabilityStatus;
    }

    public boolean isEmergencyDutyToday() {
        return isEmergencyDutyToday;
    }

    public void setEmergencyDutyToday(boolean emergencyDutyToday) {
        isEmergencyDutyToday = emergencyDutyToday;
    }

    public String getDutyShift() {
        return dutyShift;
    }

    public void setDutyShift(String dutyShift) {
        this.dutyShift = dutyShift;
    }
}
