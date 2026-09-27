package com.carepulse.service;

import com.carepulse.dto.AppointmentWaitTimeResponseDTO;
import com.carepulse.entity.Appointment;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.DoctorAvailability;
import com.carepulse.entity.EmergencyDoctorRoster;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.AppointmentRepository;
import com.carepulse.repository.DoctorAvailabilityRepository;
import com.carepulse.repository.EmergencyDoctorRosterRepository;
import com.carepulse.repository.EmergencyRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AppointmentWaitTimeService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final EmergencyDoctorRosterRepository emergencyRosterRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;

    public AppointmentWaitTimeService(AppointmentRepository appointmentRepository,
                                      DoctorAvailabilityRepository availabilityRepository,
                                      EmergencyDoctorRosterRepository emergencyRosterRepository,
                                      EmergencyRequestRepository emergencyRequestRepository) {
        this.appointmentRepository = appointmentRepository;
        this.availabilityRepository = availabilityRepository;
        this.emergencyRosterRepository = emergencyRosterRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
    }

    public AppointmentWaitTimeResponseDTO calculateWaitTime(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + appointmentId));

        Doctor doctor = appointment.getDoctor();
        LocalDate date = appointment.getAppointmentDate();
        LocalTime apptTime = appointment.getAppointmentTime();

        // 1. Determine average consultation duration from DoctorAvailability
        int avgDurationMinutes = 30; // default
        String dayOfWeekStr = date.getDayOfWeek().name();
        List<DoctorAvailability> avails = availabilityRepository.findByDoctorIdAndDayOfWeek(doctor.getId(), dayOfWeekStr);
        if (!avails.isEmpty() && avails.get(0).getSlotDurationMinutes() != null && avails.get(0).getSlotDurationMinutes() > 0) {
            avgDurationMinutes = avails.get(0).getSlotDurationMinutes();
        }

        // 2. Count active appointments ahead of this appointment on that day
        List<Appointment> dayAppointments = appointmentRepository.findByDoctorAndAppointmentDateOrderByAppointmentTimeAsc(doctor, date);
        int patientsAhead = 0;
        for (Appointment a : dayAppointments) {
            if (a.getId().equals(appointmentId)) {
                break; // We reached this appointment in time order
            }
            if (!"CANCELLED".equalsIgnoreCase(a.getStatus()) && !"COMPLETED".equalsIgnoreCase(a.getStatus())) {
                patientsAhead++;
            }
        }

        // 3. Check doctor emergency duty & active emergency cases
        List<EmergencyDoctorRoster> rosters = emergencyRosterRepository.findByDoctorIdAndRosterDate(doctor.getId(), date);
        boolean onEmergencyDuty = rosters.stream().anyMatch(r -> "EMERGENCY_DUTY".equalsIgnoreCase(r.getDutyStatus()));
        long activeEmergencies = emergencyRequestRepository.countActiveEmergenciesForDoctor(doctor.getId());

        int emergencyBufferMinutes = 0;
        if (onEmergencyDuty && activeEmergencies > 0) {
            emergencyBufferMinutes = (int) activeEmergencies * 20;
        }

        int totalWaitMinutes = (patientsAhead * avgDurationMinutes) + emergencyBufferMinutes;
        LocalTime estimatedStart = apptTime.plusMinutes(totalWaitMinutes);

        String doctorStatus = doctor.getAvailabilityStatus() != null ? doctor.getAvailabilityStatus() : "AVAILABLE";

        StringBuilder explanation = new StringBuilder();
        if (patientsAhead == 0) {
            explanation.append("You are first in queue for your scheduled consultation window.");
        } else {
            explanation.append(patientsAhead).append(" patient(s) scheduled ahead (~").append(avgDurationMinutes).append(" mins each).");
        }

        if (emergencyBufferMinutes > 0) {
            explanation.append(" Physician is currently handling ").append(activeEmergencies)
                    .append(" emergency case(s) (+").append(emergencyBufferMinutes).append(" min buffer).");
        }

        if ("IN_CONSULTATION".equalsIgnoreCase(doctorStatus)) {
            explanation.append(" Doctor is currently in consultation with a patient.");
        }

        return new AppointmentWaitTimeResponseDTO(
                appointmentId,
                doctor.getId(),
                doctor.getFullName(),
                date,
                apptTime,
                patientsAhead,
                avgDurationMinutes,
                totalWaitMinutes,
                estimatedStart,
                doctorStatus,
                onEmergencyDuty,
                explanation.toString()
        );
    }
}
