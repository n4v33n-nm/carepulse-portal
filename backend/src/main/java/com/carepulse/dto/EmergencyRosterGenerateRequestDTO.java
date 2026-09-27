package com.carepulse.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;

public class EmergencyRosterGenerateRequestDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate rosterDate;

    private Integer doctorsPerShift; // optional override, defaults to system config

    public EmergencyRosterGenerateRequestDTO() {
    }

    public EmergencyRosterGenerateRequestDTO(LocalDate rosterDate) {
        this.rosterDate = rosterDate;
    }

    public EmergencyRosterGenerateRequestDTO(LocalDate rosterDate, Integer doctorsPerShift) {
        this.rosterDate = rosterDate;
        this.doctorsPerShift = doctorsPerShift;
    }

    public LocalDate getRosterDate() {
        return rosterDate;
    }

    public void setRosterDate(LocalDate rosterDate) {
        this.rosterDate = rosterDate;
    }

    public Integer getDoctorsPerShift() {
        return doctorsPerShift;
    }

    public void setDoctorsPerShift(Integer doctorsPerShift) {
        this.doctorsPerShift = doctorsPerShift;
    }
}
