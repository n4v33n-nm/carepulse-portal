package com.carepulse.controller;

import com.carepulse.dto.*;
import com.carepulse.service.DoctorLeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor-leave")
public class DoctorLeaveController {

    private final DoctorLeaveService doctorLeaveService;

    public DoctorLeaveController(DoctorLeaveService doctorLeaveService) {
        this.doctorLeaveService = doctorLeaveService;
    }

    /**
     * 1. Doctor submits a new leave request
     */
    @PostMapping("/requests")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<DoctorLeaveResponseDTO> submitLeaveRequest(
            Authentication authentication,
            @Valid @RequestBody DoctorLeaveRequest request) {
        DoctorLeaveResponseDTO response = doctorLeaveService.submitLeaveRequest(authentication.getName(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 2. Doctor views their leave history
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<DoctorLeaveResponseDTO>> getMyLeaves(Authentication authentication) {
        List<DoctorLeaveResponseDTO> leaves = doctorLeaveService.getMyLeaves(authentication.getName());
        return ResponseEntity.ok(leaves);
    }

    /**
     * 3. Admin views all leave requests with optional status filter
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DoctorLeaveResponseDTO>> getAllLeaves(
            @RequestParam(required = false) String status) {
        List<DoctorLeaveResponseDTO> leaves = doctorLeaveService.getAllLeaves(status);
        return ResponseEntity.ok(leaves);
    }

    /**
     * 4. Doctor or Admin views a single leave request
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<DoctorLeaveResponseDTO> getLeaveById(
            @PathVariable Long id,
            Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_USER")
                .replace("ROLE_", "");

        DoctorLeaveResponseDTO response = doctorLeaveService.getLeaveById(id, authentication.getName(), role);
        return ResponseEntity.ok(response);
    }

    /**
     * 5. Admin approves or rejects a leave request
     */
    @PutMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorLeaveResponseDTO> reviewLeaveRequest(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody DoctorLeaveReviewRequest request) {
        DoctorLeaveResponseDTO response = doctorLeaveService.reviewLeaveRequest(id, authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 6. Admin inspects appointments affected by leave
     */
    @GetMapping("/{id}/affected-appointments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppointmentReassignmentDTO>> getAffectedAppointments(@PathVariable Long id) {
        List<AppointmentReassignmentDTO> appointments = doctorLeaveService.getAffectedAppointments(id);
        return ResponseEntity.ok(appointments);
    }

    /**
     * 7. Admin finds and scores replacement doctor candidates for an appointment
     */
    @GetMapping("/appointments/{appointmentId}/candidates")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReplacementDoctorCandidateDTO>> findReplacementDoctorCandidates(
            @PathVariable Long appointmentId) {
        List<ReplacementDoctorCandidateDTO> candidates = doctorLeaveService.findReplacementDoctorCandidates(appointmentId);
        return ResponseEntity.ok(candidates);
    }

    /**
     * 8. Admin proposes a replacement doctor for an affected appointment
     */
    @PostMapping("/appointments/{appointmentId}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppointmentReassignmentDTO> proposeReplacementDoctor(
            @PathVariable Long appointmentId,
            Authentication authentication,
            @Valid @RequestBody ProposeReassignmentRequest request) {
        AppointmentReassignmentDTO response = doctorLeaveService.proposeReplacementDoctor(
                appointmentId, request.getReplacementDoctorId(), authentication.getName(), request.getAdminNotes());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * 9. Patient views their pending reassignments
     */
    @GetMapping("/reassignments/my-pending")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<AppointmentReassignmentDTO>> getPatientPendingReassignments(
            Authentication authentication) {
        List<AppointmentReassignmentDTO> pending = doctorLeaveService.getPatientPendingReassignments(authentication.getName());
        return ResponseEntity.ok(pending);
    }

    /**
     * 10. Patient consents (accepts/rejects) a proposed replacement doctor
     */
    @PostMapping("/reassignments/{id}/consent")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<AppointmentReassignmentDTO> patientRespondToReassignment(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody PatientConsentRequest request) {
        AppointmentReassignmentDTO response = doctorLeaveService.patientRespondToReassignment(
                id, authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * 11. Doctor views reassignments where they are original or replacement doctor
     */
    @GetMapping("/reassignments/doctor-history")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<AppointmentReassignmentDTO>> getDoctorReassignedHistory(
            Authentication authentication) {
        List<AppointmentReassignmentDTO> history = doctorLeaveService.getDoctorReassignedHistory(authentication.getName());
        return ResponseEntity.ok(history);
    }
}

