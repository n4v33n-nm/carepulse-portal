package com.carepulse.service;

import com.carepulse.dto.DoctorAvailabilityRequest;
import com.carepulse.dto.UserProfileUpdateRequest;
import com.carepulse.entity.Appointment;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.DoctorAvailability;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.AppointmentRepository;
import com.carepulse.repository.DoctorAvailabilityRepository;
import com.carepulse.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final com.carepulse.repository.EmergencyDoctorRosterRepository emergencyRosterRepository;
    private final com.carepulse.repository.EmergencyRequestRepository emergencyRequestRepository;

    public DoctorService(DoctorRepository doctorRepository,
                         DoctorAvailabilityRepository availabilityRepository,
                         AppointmentRepository appointmentRepository,
                         com.carepulse.repository.EmergencyDoctorRosterRepository emergencyRosterRepository,
                         com.carepulse.repository.EmergencyRequestRepository emergencyRequestRepository) {
        this.doctorRepository = doctorRepository;
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
        this.emergencyRosterRepository = emergencyRosterRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
    }

    public List<Doctor> getAllDoctors(String specialization, String query) {
        if (specialization != null && !specialization.isBlank()) {
            return doctorRepository.findBySpecializationContainingIgnoreCase(specialization.trim());
        }
        if (query != null && !query.isBlank()) {
            return doctorRepository.findByFullNameContainingIgnoreCase(query.trim());
        }
        return doctorRepository.findAll();
    }

    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));
    }

    public Doctor getDoctorByEmail(String email) {
        return doctorRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for email: " + email));
    }

    public List<String> getAllSpecializations() {
        return doctorRepository.findAll().stream()
                .map(Doctor::getSpecialization)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Transactional
    public Doctor updateDoctorProfile(Long doctorId, UserProfileUpdateRequest req) {
        Doctor doctor = getDoctorById(doctorId);
        if (req.getFullName() != null) doctor.setFullName(req.getFullName());
        if (req.getPhone() != null) doctor.setPhone(req.getPhone());
        if (req.getBio() != null) doctor.setBio(req.getBio());
        if (req.getConsultationFee() != null) doctor.setConsultationFee(req.getConsultationFee());
        if (req.getAvailabilityStatus() != null) doctor.setAvailabilityStatus(req.getAvailabilityStatus());
        return doctorRepository.save(doctor);
    }

    public List<DoctorAvailability> getDoctorAvailability(Long doctorId) {
        return availabilityRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public List<DoctorAvailability> saveOrUpdateAvailability(Long doctorId, List<DoctorAvailabilityRequest> requests) {
        Doctor doctor = getDoctorById(doctorId);
        availabilityRepository.deleteByDoctor(doctor);

        List<DoctorAvailability> savedList = new ArrayList<>();
        for (DoctorAvailabilityRequest req : requests) {
            DoctorAvailability da = new DoctorAvailability(
                    doctor,
                    req.getDayOfWeek().toUpperCase(),
                    req.getStartTime(),
                    req.getEndTime(),
                    req.getBreakStartTime(),
                    req.getBreakEndTime(),
                    req.getSlotDurationMinutes() != null ? req.getSlotDurationMinutes() : 30
            );
            da.setAvailable(req.isAvailable());
            savedList.add(availabilityRepository.save(da));
        }
        return savedList;
    }

    public List<LocalTime> getAvailableTimeSlots(Long doctorId, LocalDate date) {
        Doctor doctor = getDoctorById(doctorId);
        String status = doctor.getAvailabilityStatus() != null ? doctor.getAvailabilityStatus() : "AVAILABLE";
        if ("ON_LEAVE".equalsIgnoreCase(status) || "OFF_DUTY".equalsIgnoreCase(status)) {
            return Collections.emptyList();
        }

        DayOfWeek dow = date.getDayOfWeek();
        String dayOfWeekStr = dow.name();

        List<DoctorAvailability> availabilities = availabilityRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeekStr);
        if (availabilities.isEmpty()) {
            return Collections.emptyList();
        }

        // Get booked appointments for that day
        List<Appointment> bookedAppointments = appointmentRepository.findByDoctorAndAppointmentDateOrderByAppointmentTimeAsc(doctor, date);
        Set<LocalTime> bookedTimes = bookedAppointments.stream()
                .filter(a -> !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                .map(Appointment::getAppointmentTime)
                .collect(Collectors.toSet());

        List<LocalTime> availableSlots = new ArrayList<>();
        LocalTime now = LocalTime.now();
        boolean isToday = date.isEqual(LocalDate.now());

        for (DoctorAvailability avail : availabilities) {
            if (!avail.isAvailable()) continue;

            LocalTime current = avail.getStartTime();
            LocalTime end = avail.getEndTime();
            int duration = avail.getSlotDurationMinutes() != null ? avail.getSlotDurationMinutes() : 30;

            while (current.plusMinutes(duration).isBefore(end) || current.plusMinutes(duration).equals(end)) {
                // If today, exclude slots that have already passed
                if (isToday && current.isBefore(now)) {
                    current = current.plusMinutes(duration);
                    continue;
                }

                // Check if inside break
                boolean inBreak = false;
                if (avail.getBreakStartTime() != null && avail.getBreakEndTime() != null) {
                    if ((current.equals(avail.getBreakStartTime()) || current.isAfter(avail.getBreakStartTime()))
                            && current.isBefore(avail.getBreakEndTime())) {
                        inBreak = true;
                    }
                }

                if (!inBreak && !bookedTimes.contains(current)) {
                    availableSlots.add(current);
                }
                current = current.plusMinutes(duration);
            }
        }

        return availableSlots;
    }

    public List<com.carepulse.dto.DoctorWorkloadDTO> getDoctorWorkloadsToday() {
        LocalDate today = LocalDate.now();
        List<Doctor> doctors = doctorRepository.findAll();
        List<com.carepulse.dto.DoctorWorkloadDTO> result = new ArrayList<>();

        for (Doctor doc : doctors) {
            result.add(calculateDoctorWorkload(doc, today));
        }

        result.sort(Comparator.comparingInt(com.carepulse.dto.DoctorWorkloadDTO::getTotalWorkloadToday)
                .thenComparing(com.carepulse.dto.DoctorWorkloadDTO::getDoctorName));
        return result;
    }

    public com.carepulse.dto.DoctorWorkloadDTO getDoctorWorkload(Long doctorId) {
        Doctor doctor = getDoctorById(doctorId);
        return calculateDoctorWorkload(doctor, LocalDate.now());
    }

    private com.carepulse.dto.DoctorWorkloadDTO calculateDoctorWorkload(Doctor doctor, LocalDate date) {
        List<Appointment> dayAppts = appointmentRepository.findByDoctorAndAppointmentDateOrderByAppointmentTimeAsc(doctor, date);
        int normalCount = (int) dayAppts.stream()
                .filter(a -> !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                .count();

        int emergencyCount = (int) emergencyRequestRepository.countEmergencyCasesForDoctorOnDate(doctor.getId(), date);
        int activeEmergencies = (int) emergencyRequestRepository.countActiveEmergenciesForDoctor(doctor.getId());

        List<com.carepulse.entity.EmergencyDoctorRoster> rosters = emergencyRosterRepository.findByDoctorIdAndRosterDate(doctor.getId(), date);
        boolean onEmergencyDuty = rosters.stream().anyMatch(r -> "EMERGENCY_DUTY".equalsIgnoreCase(r.getDutyStatus()));
        String shift = onEmergencyDuty && !rosters.isEmpty() ?
                rosters.get(0).getShiftName() + " (" + rosters.get(0).getShiftStart() + " - " + rosters.get(0).getShiftEnd() + ")" : "NONE";

        String status = doctor.getAvailabilityStatus() != null ? doctor.getAvailabilityStatus() : "AVAILABLE";
        int totalWorkload = normalCount + emergencyCount;

        return new com.carepulse.dto.DoctorWorkloadDTO(
                doctor.getId(),
                doctor.getFullName(),
                doctor.getSpecialization(),
                normalCount,
                emergencyCount,
                totalWorkload,
                activeEmergencies,
                status,
                onEmergencyDuty,
                shift
        );
    }
}
