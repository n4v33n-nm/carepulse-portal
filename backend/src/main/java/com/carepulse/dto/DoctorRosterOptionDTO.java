package com.carepulse.dto;

import com.carepulse.entity.Doctor;

public class DoctorRosterOptionDTO {

    private Long id;
    private String name;
    private String specialization;
    private String qualification;
    private String status;
    private String phone;

    public DoctorRosterOptionDTO() {
    }

    public DoctorRosterOptionDTO(Long id, String name, String specialization, String qualification, String status, String phone) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.qualification = qualification;
        this.status = status;
        this.phone = phone;
    }

    public static DoctorRosterOptionDTO fromEntity(Doctor doctor) {
        if (doctor == null) return null;
        return new DoctorRosterOptionDTO(
                doctor.getId(),
                doctor.getFullName(),
                doctor.getSpecialization(),
                doctor.getQualification(),
                doctor.getAvailabilityStatus(),
                doctor.getPhone()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
