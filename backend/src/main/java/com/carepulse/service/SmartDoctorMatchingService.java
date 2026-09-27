package com.carepulse.service;

import com.carepulse.dto.DoctorMatchResponseDTO;
import com.carepulse.entity.Appointment;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.DoctorAvailability;
import com.carepulse.entity.EmergencyDoctorRoster;
import com.carepulse.repository.AppointmentRepository;
import com.carepulse.repository.DoctorAvailabilityRepository;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.EmergencyDoctorRosterRepository;
import com.carepulse.repository.EmergencyRequestRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SmartDoctorMatchingService {

    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final EmergencyDoctorRosterRepository emergencyRosterRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;
    private final DoctorService doctorService;

    public static final String MATCHING_DISCLAIMER =
            "This matching engine provides scheduling and availability recommendations based on doctor specialties, workload, and clinic schedules. It does not provide medical triage or clinical diagnosis.";

    public SmartDoctorMatchingService(DoctorRepository doctorRepository,
                                      DoctorAvailabilityRepository availabilityRepository,
                                      AppointmentRepository appointmentRepository,
                                      EmergencyDoctorRosterRepository emergencyRosterRepository,
                                      EmergencyRequestRepository emergencyRequestRepository,
                                      DoctorService doctorService) {
        this.doctorRepository = doctorRepository;
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
        this.emergencyRosterRepository = emergencyRosterRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.doctorService = doctorService;
    }

    public List<DoctorMatchResponseDTO> matchDoctors(String requestedSpecialization, LocalDate preferredDate, LocalTime preferredTime) {
        LocalDate targetDate = (preferredDate != null) ? preferredDate : LocalDate.now();
        List<Doctor> allDoctors = doctorRepository.findAll();

        List<DoctorMatchResponseDTO> results = new ArrayList<>();

        for (Doctor doctor : allDoctors) {
            // Check basic eligibility
            String status = doctor.getAvailabilityStatus() != null ? doctor.getAvailabilityStatus() : "AVAILABLE";
            if ("ON_LEAVE".equalsIgnoreCase(status)) {
                continue; // Exclude doctors on leave
            }

            int score = 40; // Base score
            List<String> reasons = new ArrayList<>();

            // 1. Specialization Matching
            boolean specMatches = false;
            if (requestedSpecialization != null && !requestedSpecialization.isBlank()) {
                if (doctor.getSpecialization() != null &&
                        doctor.getSpecialization().equalsIgnoreCase(requestedSpecialization.trim())) {
                    score += 35;
                    specMatches = true;
                    reasons.add(doctor.getSpecialization() + " specialization exact match");
                } else if (doctor.getSpecialization() != null &&
                        doctor.getSpecialization().toLowerCase().contains(requestedSpecialization.toLowerCase().trim())) {
                    score += 25;
                    specMatches = true;
                    reasons.add(doctor.getSpecialization() + " specialization related match");
                } else {
                    score -= 30; // Not matching requested specialty
                    continue; // Skip doctors not matching requested specialization
                }
            } else {
                reasons.add("General availability match");
            }

            // 2. Doctor Weekly Schedule / Working Hours on Target Day
            DayOfWeek dow = targetDate.getDayOfWeek();
            List<DoctorAvailability> availList = availabilityRepository.findByDoctorIdAndDayOfWeek(doctor.getId(), dow.name());
            boolean isWorkingToday = availList.stream().anyMatch(DoctorAvailability::isAvailable);
            if (isWorkingToday) {
                score += 15;
                reasons.add("Scheduled for regular clinic hours on " + dow);
            } else {
                score -= 20;
                reasons.add("No standard clinic hours on " + dow);
            }

            // 3. Dynamic Slots & Collision Check
            List<LocalTime> availableSlots = doctorService.getAvailableTimeSlots(doctor.getId(), targetDate);
            if (!availableSlots.isEmpty()) {
                score += 10;
                reasons.add(availableSlots.size() + " appointment slots available");
            } else {
                score -= 25;
                reasons.add("All scheduled slots booked for this date");
            }

            // 4. Requested Time Match
            if (preferredTime != null) {
                if (availableSlots.contains(preferredTime)) {
                    score += 20;
                    reasons.add("Requested slot " + preferredTime + " is available");
                } else {
                    score -= 10;
                    reasons.add("Requested time " + preferredTime + " is not open");
                }
            }

            // 5. Workload Balancing (Appointments)
            List<Appointment> todayAppointments = appointmentRepository.findByDoctorAndAppointmentDateOrderByAppointmentTimeAsc(doctor, targetDate);
            long nonCancelledCount = todayAppointments.stream()
                    .filter(a -> !"CANCELLED".equalsIgnoreCase(a.getStatus()))
                    .count();

            if (nonCancelledCount == 0) {
                score += 10;
                reasons.add("Low workload: No active appointments today");
            } else if (nonCancelledCount <= 3) {
                score += 5;
                reasons.add("Moderate workload: " + nonCancelledCount + " appointments today");
            } else {
                score -= 10;
                reasons.add("High workload: " + nonCancelledCount + " appointments today");
            }

            // 6. Emergency Duty Status
            List<EmergencyDoctorRoster> rosters = emergencyRosterRepository.findByDoctorIdAndRosterDate(doctor.getId(), targetDate);
            boolean onEmergencyDuty = rosters.stream().anyMatch(r -> "EMERGENCY_DUTY".equalsIgnoreCase(r.getDutyStatus()));
            int activeEmergencyCases = (int) emergencyRequestRepository.countActiveEmergenciesForDoctor(doctor.getId());
            int todayEmergencyTotal = (int) emergencyRequestRepository.countEmergencyCasesForDoctorOnDate(doctor.getId(), targetDate);

            if (onEmergencyDuty) {
                EmergencyDoctorRoster r = rosters.get(0);
                if (activeEmergencyCases > 0) {
                    score -= 15;
                    reasons.add("Active emergency caseload: " + activeEmergencyCases + " active emergency patient(s)");
                } else {
                    reasons.add("On emergency rotation today (" + r.getShiftName() + " shift)");
                }
            }

            // 7. Experience / Rating Bonus
            if (doctor.getRating() != null && doctor.getRating() >= 4.5) {
                score += 5;
            }

            int finalScore = Math.max(0, Math.min(100, score));
            int totalWorkload = (int) nonCancelledCount + todayEmergencyTotal;

            DoctorMatchResponseDTO dto = new DoctorMatchResponseDTO(
                    doctor.getId(),
                    doctor.getFullName(),
                    doctor.getSpecialization(),
                    doctor.getQualification(),
                    doctor.getYearsOfExperience(),
                    doctor.getConsultationFee(),
                    doctor.getHospitalAffiliation(),
                    doctor.getRating(),
                    finalScore,
                    reasons,
                    availableSlots,
                    onEmergencyDuty,
                    (int) nonCancelledCount,
                    todayEmergencyTotal,
                    totalWorkload,
                    status,
                    MATCHING_DISCLAIMER
            );

            results.add(dto);
        }

        // Sort descending by matchScore, then by totalWorkload ascending, then by doctorId ascending
        results.sort(Comparator.comparingInt(DoctorMatchResponseDTO::getMatchScore).reversed()
                .thenComparingInt(DoctorMatchResponseDTO::getTotalWorkloadToday)
                .thenComparing(DoctorMatchResponseDTO::getDoctorId));

        return results;
    }
}
