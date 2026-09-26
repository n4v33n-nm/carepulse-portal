package com.carepulse.controller;

import com.carepulse.dto.MedicalRecordRequest;
import com.carepulse.entity.MedicalRecord;
import com.carepulse.service.MedicalRecordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    public MedicalRecordController(MedicalRecordService medicalRecordService) {
        this.medicalRecordService = medicalRecordService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<MedicalRecord> createRecord(
            Authentication authentication,
            @Valid @RequestBody MedicalRecordRequest request) {
        MedicalRecord record = medicalRecordService.createRecord(authentication.getName(), request);
        return ResponseEntity.ok(record);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<List<MedicalRecord>> getMyRecords(Authentication authentication) {
        return ResponseEntity.ok(medicalRecordService.getRecordsForPatient(authentication.getName()));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<MedicalRecord>> getRecordsByPatientId(
            @PathVariable Long patientId,
            Authentication authentication) {
        return ResponseEntity.ok(medicalRecordService.getRecordsForPatientId(patientId, authentication.getName()));
    }

    @GetMapping("/doctor/my")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<MedicalRecord>> getRecordsByDoctor(Authentication authentication) {
        return ResponseEntity.ok(medicalRecordService.getRecordsByDoctor(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecord> getRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(medicalRecordService.getRecordById(id));
    }
}
