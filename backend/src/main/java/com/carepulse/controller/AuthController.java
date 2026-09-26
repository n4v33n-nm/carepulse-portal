package com.carepulse.controller;

import com.carepulse.dto.AuthRequest;
import com.carepulse.dto.AuthResponse;
import com.carepulse.dto.DoctorRegisterRequest;
import com.carepulse.dto.PatientRegisterRequest;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.Patient;
import com.carepulse.entity.User;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.PatientRepository;
import com.carepulse.service.AuditLogService;
import com.carepulse.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AuditLogService auditLogService;

    public AuthController(UserService userService,
                          PatientRepository patientRepository,
                          DoctorRepository doctorRepository,
                          AuditLogService auditLogService) {
        this.userService = userService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.auditLogService = auditLogService;
    }

    @PostMapping("/register/patient")
    public ResponseEntity<AuthResponse> registerPatient(@Valid @RequestBody PatientRegisterRequest request) {
        AuthResponse response = userService.registerPatient(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register/doctor")
    public ResponseEntity<AuthResponse> registerDoctor(@Valid @RequestBody DoctorRegisterRequest request) {
        AuthResponse response = userService.registerDoctor(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        String email = authentication.getName();
        User user = userService.getUserByEmail(email);

        String fullName = user.getEmail();
        Long patientId = null;
        Long doctorId = null;

        if ("PATIENT".equals(user.getRole())) {
            Patient patient = patientRepository.findByUser(user).orElse(null);
            if (patient != null) {
                fullName = patient.getFullName();
                patientId = patient.getId();
            }
        } else if ("DOCTOR".equals(user.getRole())) {
            Doctor doctor = doctorRepository.findByUser(user).orElse(null);
            if (doctor != null) {
                fullName = doctor.getFullName();
                doctorId = doctor.getId();
            }
        } else if ("ADMIN".equals(user.getRole())) {
            fullName = "System Administrator";
        }

        AuthResponse resp = new AuthResponse(
                null,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                fullName,
                patientId,
                doctorId,
                user.getCommunicationPreference()
        );
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/communication-preference")
    public ResponseEntity<Map<String, String>> updateCommunicationPreference(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        String preference = request.get("preference");
        userService.updateCommunicationPreference(authentication.getName(), preference);
        return ResponseEntity.ok(Map.of("message", "Preference updated successfully", "preference", preference));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(Authentication authentication) {
        if (authentication != null) {
            auditLogService.log(authentication.getName(), "LOGOUT", "Auth", "User logged out");
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
