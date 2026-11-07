package com.carepulse.controller;

import com.carepulse.dto.*;
import com.carepulse.service.DoctorConnectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctor-connect")
public class DoctorConnectController {

    private final DoctorConnectService doctorConnectService;

    public DoctorConnectController(DoctorConnectService doctorConnectService) {
        this.doctorConnectService = doctorConnectService;
    }

    private String getRole(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null || auth.getAuthorities().isEmpty()) {
            return "PATIENT";
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if (a.startsWith("ROLE_")) {
                return a.substring(5);
            }
        }
        return "PATIENT";
    }

    @PostMapping("/conversations")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ConversationResponseDTO> createConversation(
            Authentication authentication,
            @Valid @RequestBody CreateConversationRequest request) {
        ConversationResponseDTO created = doctorConnectService.createConversation(authentication.getName(), request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/conversations")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<List<ConversationResponseDTO>> getConversations(
            Authentication authentication,
            @RequestParam(required = false) String status) {
        String role = getRole(authentication);
        List<ConversationResponseDTO> list = doctorConnectService.getUserConversations(authentication.getName(), role, status);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/conversations/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ConversationResponseDTO> getConversationById(
            Authentication authentication,
            @PathVariable Long id) {
        String role = getRole(authentication);
        ConversationResponseDTO conv = doctorConnectService.getConversationById(id, authentication.getName(), role);
        return ResponseEntity.ok(conv);
    }

    @PostMapping("/conversations/{id}/messages")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ConversationMessageDTO> sendMessage(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        String role = getRole(authentication);
        ConversationMessageDTO msg = doctorConnectService.sendMessage(id, authentication.getName(), role, request);
        return new ResponseEntity<>(msg, HttpStatus.CREATED);
    }

    @PatchMapping("/conversations/{id}/status")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ConversationResponseDTO> updateStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String role = getRole(authentication);
        ConversationResponseDTO updated = doctorConnectService.updateStatus(id, authentication.getName(), role, status);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<DoctorConnectStatsDTO> getStats() {
        return ResponseEntity.ok(doctorConnectService.getStats());
    }
}

