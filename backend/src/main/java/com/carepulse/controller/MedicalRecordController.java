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
    private final com.carepulse.service.UserService userService;

    public MedicalRecordController(MedicalRecordService medicalRecordService, com.carepulse.service.UserService userService) {
        this.medicalRecordService = medicalRecordService;
        this.userService = userService;
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MedicalRecord>> getRecordsByPatientId(
            @PathVariable Long patientId,
            Authentication authentication) {
        com.carepulse.entity.User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(medicalRecordService.getRecordsForPatientId(patientId, user.getEmail(), user.getRole()));
    }

    @GetMapping("/doctor/my")
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<List<MedicalRecord>> getRecordsByDoctor(Authentication authentication) {
        return ResponseEntity.ok(medicalRecordService.getRecordsByDoctor(authentication.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MedicalRecord> getRecordById(@PathVariable Long id, Authentication authentication) {
        com.carepulse.entity.User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(medicalRecordService.getRecordById(id, user.getEmail(), user.getRole()));
    }

    @PostMapping("/{id}/ai-summary-draft")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MedicalRecord> generateAiDraftSummary(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(medicalRecordService.generateAiDraftSummary(id, authentication.getName()));
    }

    @PatchMapping("/{id}/ai-summary-review")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<MedicalRecord> reviewAiSummary(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody com.carepulse.dto.ReviewSummaryRequestDTO request) {
        return ResponseEntity.ok(medicalRecordService.reviewAiSummary(id, authentication.getName(), request));
    }
}
