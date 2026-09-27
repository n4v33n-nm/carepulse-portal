package com.carepulse.controller;

import com.carepulse.dto.UserProfileUpdateRequest;
import com.carepulse.entity.Patient;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.PatientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository patientRepository;
    private final com.carepulse.service.UserService userService;
    private final com.carepulse.service.CaregiverService caregiverService;

    public PatientController(PatientRepository patientRepository,
                             com.carepulse.service.UserService userService,
                             com.carepulse.service.CaregiverService caregiverService) {
        this.patientRepository = patientRepository;
        this.userService = userService;
        this.caregiverService = caregiverService;
    }

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientRepository.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id, org.springframework.security.core.Authentication authentication) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));

        com.carepulse.entity.User caller = userService.getUserByEmail(authentication.getName());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(caller.getRole());
        boolean isDoctor = "DOCTOR".equalsIgnoreCase(caller.getRole());
        boolean isSelf = patient.getUser().getEmail().equalsIgnoreCase(caller.getEmail());
        boolean isCaregiver = caregiverService.isAuthorizedCaregiver(patient.getId(), caller.getEmail());

        if (!isAdmin && !isDoctor && !isSelf && !isCaregiver) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to view this patient profile");
        }

        return ResponseEntity.ok(patient);
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Patient> updateProfile(
            @PathVariable Long id,
            org.springframework.security.core.Authentication authentication,
            @RequestBody UserProfileUpdateRequest req) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));

        com.carepulse.entity.User caller = userService.getUserByEmail(authentication.getName());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(caller.getRole());
        boolean isSelf = patient.getUser().getEmail().equalsIgnoreCase(caller.getEmail());

        if (!isAdmin && !isSelf) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to modify this patient profile");
        }

        if (req.getFullName() != null) patient.setFullName(req.getFullName());
        if (req.getPhone() != null) patient.setPhone(req.getPhone());
        if (req.getAddress() != null) patient.setAddress(req.getAddress());
        if (req.getBloodGroup() != null) patient.setBloodGroup(req.getBloodGroup());
        if (req.getEmergencyContact() != null) patient.setEmergencyContact(req.getEmergencyContact());

        return ResponseEntity.ok(patientRepository.save(patient));
    }
}
