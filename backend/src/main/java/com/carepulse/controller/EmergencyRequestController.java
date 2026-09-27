package com.carepulse.controller;

import com.carepulse.dto.EmergencyRequestCreateDTO;
import com.carepulse.dto.EmergencyRequestResponseDTO;
import com.carepulse.dto.EmergencyStatusUpdateRequestDTO;
import com.carepulse.service.EmergencyRequestService;
import com.carepulse.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/emergency-requests")
public class EmergencyRequestController {

    private final EmergencyRequestService emergencyRequestService;
    private final UserService userService;

    public EmergencyRequestController(EmergencyRequestService emergencyRequestService, UserService userService) {
        this.emergencyRequestService = emergencyRequestService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<EmergencyRequestResponseDTO> createEmergencyRequest(
            Authentication authentication,
            @Valid @RequestBody EmergencyRequestCreateDTO request) {
        EmergencyRequestResponseDTO response = emergencyRequestService.createEmergencyRequest(authentication.getName(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<EmergencyRequestResponseDTO>> getMyEmergencyRequests(Authentication authentication) {
        List<EmergencyRequestResponseDTO> requests = emergencyRequestService.getPatientEmergencyRequests(authentication.getName());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/assigned")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<EmergencyRequestResponseDTO>> getAssignedEmergencyRequests(Authentication authentication) {
        List<EmergencyRequestResponseDTO> requests = emergencyRequestService.getAssignedRequestsForDoctor(authentication.getName());
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmergencyRequestResponseDTO> getEmergencyRequestById(
            @PathVariable Long id,
            Authentication authentication) {
        String role = userService.getUserByEmail(authentication.getName()).getRole();
        EmergencyRequestResponseDTO response = emergencyRequestService.getEmergencyRequestById(id, authentication.getName(), role);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<EmergencyRequestResponseDTO> cancelEmergencyRequest(
            @PathVariable Long id,
            Authentication authentication) {
        EmergencyRequestResponseDTO response = emergencyRequestService.cancelPatientRequest(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PATCH, RequestMethod.PUT})
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
    public ResponseEntity<EmergencyRequestResponseDTO> updateEmergencyStatus(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody EmergencyStatusUpdateRequestDTO request) {
        EmergencyRequestResponseDTO response = emergencyRequestService.updateRequestStatus(id, authentication.getName(), request);
        return ResponseEntity.ok(response);
    }
}
