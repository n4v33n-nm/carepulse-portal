package com.carepulse.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public class DoctorMatchResponseDTO {

    private Long doctorId;
    private String doctorName;
    private String specialization;
    private String qualification;
    private Integer experienceYears;
    private Double consultationFee;
    private String hospitalAffiliation;
    private Double rating;
    private int matchScore; // 0 - 100
    private List<String> matchReasons;
    private List<LocalTime> availableSlots;
    private boolean emergencyDutyToday;
    private int todayAppointmentsCount;
    private int todayEmergencyRequestsCount;
    private int totalWorkloadToday;
    private String availabilityStatus; // AVAILABLE, BUSY, IN_CONSULTATION, ON_LEAVE, OFF_DUTY
    private String disclaimer;

    public DoctorMatchResponseDTO() {
    }

    public DoctorMatchResponseDTO(Long doctorId, String doctorName, String specialization,
                                 String qualification, Integer experienceYears, Double consultationFee,
                                 String hospitalAffiliation, Double rating, int matchScore,
                                 List<String> matchReasons, List<LocalTime> availableSlots,
                                 boolean emergencyDutyToday, int todayAppointmentsCount,
                                 int todayEmergencyRequestsCount, int totalWorkloadToday,
                                 String availabilityStatus, String disclaimer) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.specialization = specialization;
        this.qualification = qualification;
        this.experienceYears = experienceYears;
        this.consultationFee = consultationFee;
        this.hospitalAffiliation = hospitalAffiliation;
        this.rating = rating;
        this.matchScore = matchScore;
        this.matchReasons = matchReasons;
        this.availableSlots = availableSlots;
        this.emergencyDutyToday = emergencyDutyToday;
        this.todayAppointmentsCount = todayAppointmentsCount;
        this.todayEmergencyRequestsCount = todayEmergencyRequestsCount;
        this.totalWorkloadToday = totalWorkloadToday;
        this.availabilityStatus = availabilityStatus;
        this.disclaimer = disclaimer;
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

    public String getQualification() {
        return qualification;
    }

    public void setQualification(String qualification) {
        this.qualification = qualification;
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

    public String getHospitalAffiliation() {
        return hospitalAffiliation;
    }

    public void setHospitalAffiliation(String hospitalAffiliation) {
        this.hospitalAffiliation = hospitalAffiliation;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public int getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(int matchScore) {
        this.matchScore = matchScore;
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }

    public void setMatchReasons(List<String> matchReasons) {
        this.matchReasons = matchReasons;
    }

    public List<LocalTime> getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(List<LocalTime> availableSlots) {
        this.availableSlots = availableSlots;
    }

    public boolean isEmergencyDutyToday() {
        return emergencyDutyToday;
    }

    public void setEmergencyDutyToday(boolean emergencyDutyToday) {
        this.emergencyDutyToday = emergencyDutyToday;
    }

    public int getTodayAppointmentsCount() {
        return todayAppointmentsCount;
    }

    public void setTodayAppointmentsCount(int todayAppointmentsCount) {
        this.todayAppointmentsCount = todayAppointmentsCount;
    }

    public int getTodayEmergencyRequestsCount() {
        return todayEmergencyRequestsCount;
    }

    public void setTodayEmergencyRequestsCount(int todayEmergencyRequestsCount) {
        this.todayEmergencyRequestsCount = todayEmergencyRequestsCount;
    }

    public int getTotalWorkloadToday() {
        return totalWorkloadToday;
    }

    public void setTotalWorkloadToday(int totalWorkloadToday) {
        this.totalWorkloadToday = totalWorkloadToday;
    }

    public String getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
