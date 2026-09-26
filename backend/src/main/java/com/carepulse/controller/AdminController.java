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

    public AdminController(UserRepository userRepository,
                           PatientRepository patientRepository,
                           DoctorRepository doctorRepository,
                           AppointmentRepository appointmentRepository,
                           UserService userService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.userService = userService;
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
}
