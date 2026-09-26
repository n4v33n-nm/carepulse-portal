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

    public PatientController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));
        return ResponseEntity.ok(patient);
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<Patient> updateProfile(
            @PathVariable Long id,
            @RequestBody UserProfileUpdateRequest req) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));

        if (req.getFullName() != null) patient.setFullName(req.getFullName());
        if (req.getPhone() != null) patient.setPhone(req.getPhone());
        if (req.getAddress() != null) patient.setAddress(req.getAddress());
        if (req.getBloodGroup() != null) patient.setBloodGroup(req.getBloodGroup());
        if (req.getEmergencyContact() != null) patient.setEmergencyContact(req.getEmergencyContact());

        return ResponseEntity.ok(patientRepository.save(patient));
    }
}
