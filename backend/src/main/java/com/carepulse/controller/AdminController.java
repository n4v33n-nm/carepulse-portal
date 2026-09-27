package com.carepulse.controller;

import com.carepulse.dto.AdminStatsResponse;
import com.carepulse.entity.User;
import com.carepulse.repository.AppointmentRepository;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.PatientRepository;
import com.carepulse.repository.UserRepository;
import com.carepulse.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserService userService;
    private final com.carepulse.service.DoctorService doctorService;
    private final com.carepulse.repository.EmergencyRequestRepository emergencyRequestRepository;
    private final com.carepulse.repository.AppointmentWaitlistRepository waitlistRepository;

    public AdminController(UserRepository userRepository,
                           PatientRepository patientRepository,
                           DoctorRepository doctorRepository,
                           AppointmentRepository appointmentRepository,
                           UserService userService,
                           com.carepulse.service.DoctorService doctorService,
                           com.carepulse.repository.EmergencyRequestRepository emergencyRequestRepository,
                           com.carepulse.repository.AppointmentWaitlistRepository waitlistRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.userService = userService;
        this.doctorService = doctorService;
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.waitlistRepository = waitlistRepository;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminStatsResponse> getDashboardStats() {
        long totalPatients = patientRepository.count();
        long totalDoctors = doctorRepository.count();
        long totalAppointments = appointmentRepository.count();
        long activeUsers = userRepository.countByActiveTrue();

        Map<String, Long> statusMap = new HashMap<>();
        statusMap.put("PENDING", appointmentRepository.countByStatus("PENDING"));
        statusMap.put("CONFIRMED", appointmentRepository.countByStatus("CONFIRMED"));
        statusMap.put("COMPLETED", appointmentRepository.countByStatus("COMPLETED"));
        statusMap.put("CANCELLED", appointmentRepository.countByStatus("CANCELLED"));

        AdminStatsResponse response = new AdminStatsResponse(
                totalPatients,
                totalDoctors,
                totalAppointments,
                activeUsers,
                statusMap
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<Map<String, String>> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserActive(id);
        return ResponseEntity.ok(Map.of("message", "User status updated successfully"));
    }

    @GetMapping("/analytics")
    public ResponseEntity<com.carepulse.dto.AdminAnalyticsDTO> getPlatformAnalytics() {
        java.time.LocalDate today = java.time.LocalDate.now();
        long totalPatients = patientRepository.count();
        long totalDoctors = doctorRepository.count();
        long totalAppointments = appointmentRepository.count();
        long todayAppointments = appointmentRepository.countByAppointmentDate(today);
        long completedAppointments = appointmentRepository.countByStatus("COMPLETED");
        long cancelledAppointments = appointmentRepository.countByStatus("CANCELLED");
        long activeWaitlist = waitlistRepository.countByStatus("WAITING");

        long totalEmergencies = emergencyRequestRepository.count();
        long assignedEmergencies = emergencyRequestRepository.countByStatus("ASSIGNED");
        long inProgressEmergencies = emergencyRequestRepository.countByStatus("IN_PROGRESS");
        long completedEmergencies = emergencyRequestRepository.countByStatus("COMPLETED");
        long waitingEmergencies = emergencyRequestRepository.countByStatus("WAITING");
        long noDocEmergencies = emergencyRequestRepository.countByStatus("NO_DOCTOR_AVAILABLE");

        // Calculate average emergency assignment seconds
        List<com.carepulse.entity.EmergencyRequest> allRequests = emergencyRequestRepository.findAll();
        double avgAssignmentSeconds = allRequests.stream()
                .filter(r -> r.getAssignedTime() != null && r.getRequestTime() != null)
                .mapToLong(r -> java.time.Duration.between(r.getRequestTime(), r.getAssignedTime()).getSeconds())
                .average()
                .orElse(0.0);

        // 7-day appointment trends
        Map<String, Long> trends = new java.util.LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate d = today.minusDays(i);
            trends.put(d.toString(), appointmentRepository.countByAppointmentDate(d));
        }

        List<com.carepulse.dto.DoctorWorkloadDTO> workloads = doctorService.getDoctorWorkloadsToday();

        com.carepulse.dto.AdminAnalyticsDTO analytics = new com.carepulse.dto.AdminAnalyticsDTO(
                totalPatients,
                totalDoctors,
                totalAppointments,
                todayAppointments,
                completedAppointments,
                cancelledAppointments,
                activeWaitlist,
                totalEmergencies,
                assignedEmergencies,
                inProgressEmergencies,
                completedEmergencies,
                waitingEmergencies,
                noDocEmergencies,
                avgAssignmentSeconds,
                workloads,
                trends
        );

        return ResponseEntity.ok(analytics);
    }
}
