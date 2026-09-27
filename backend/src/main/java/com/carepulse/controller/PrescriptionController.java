package com.carepulse.controller;

import com.carepulse.dto.PrescriptionRequest;
import com.carepulse.entity.Prescription;
import com.carepulse.service.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final com.carepulse.service.UserService userService;

    public PrescriptionController(PrescriptionService prescriptionService, com.carepulse.service.UserService userService) {
        this.prescriptionService = prescriptionService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<Prescription> createPrescription(
            Authentication authentication,
            @Valid @RequestBody PrescriptionRequest request) {
        Prescription prescription = prescriptionService.createPrescription(authentication.getName(), request);
        return ResponseEntity.ok(prescription);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<List<Prescription>> getMyPrescriptions(Authentication authentication) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionsForPatient(authentication.getName()));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Prescription>> getPrescriptionsForPatient(
            @PathVariable Long patientId,
            Authentication authentication) {
        com.carepulse.entity.User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(prescriptionService.getPrescriptionsForPatientId(patientId, user.getEmail(), user.getRole()));
    }

    @GetMapping("/doctor/my")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<Prescription>> getPrescriptionsByDoctor(Authentication authentication) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionsByDoctor(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Prescription> getPrescriptionById(@PathVariable Long id, Authentication authentication) {
        com.carepulse.entity.User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(prescriptionService.getPrescriptionById(id, user.getEmail(), user.getRole()));
    }
}
