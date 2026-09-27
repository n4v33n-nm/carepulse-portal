package com.carepulse.controller;

import com.carepulse.dto.DoctorAvailabilityRequest;
import com.carepulse.dto.UserProfileUpdateRequest;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.DoctorAvailability;
import com.carepulse.service.DoctorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final com.carepulse.service.UserService userService;

    public DoctorController(DoctorService doctorService, com.carepulse.service.UserService userService) {
        this.doctorService = doctorService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<Doctor>> getAllDoctors(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(doctorService.getAllDoctors(specialization, query));
    }

    @GetMapping("/specializations")
    public ResponseEntity<List<String>> getSpecializations() {
        return ResponseEntity.ok(doctorService.getAllSpecializations());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Doctor> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Doctor> updateDoctorProfile(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication,
            @RequestBody UserProfileUpdateRequest request) {
        Doctor doctor = doctorService.getDoctorById(id);
        com.carepulse.entity.User caller = userService.getUserByEmail(authentication.getName());
        if (!"ADMIN".equalsIgnoreCase(caller.getRole()) && !doctor.getUser().getEmail().equalsIgnoreCase(caller.getEmail())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to modify another doctor's profile");
        }
        return ResponseEntity.ok(doctorService.updateDoctorProfile(id, request));
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<List<DoctorAvailability>> getAvailability(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorAvailability(id));
    }

    @PostMapping("/{id}/availability")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<DoctorAvailability>> updateAvailability(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication,
            @RequestBody List<DoctorAvailabilityRequest> requests) {
        Doctor doctor = doctorService.getDoctorById(id);
        com.carepulse.entity.User caller = userService.getUserByEmail(authentication.getName());
        if (!"ADMIN".equalsIgnoreCase(caller.getRole()) && !doctor.getUser().getEmail().equalsIgnoreCase(caller.getEmail())) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to modify another doctor's availability");
        }
        return ResponseEntity.ok(doctorService.saveOrUpdateAvailability(id, requests));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<List<LocalTime>> getAvailableSlots(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(doctorService.getAvailableTimeSlots(id, date));
    }
}
