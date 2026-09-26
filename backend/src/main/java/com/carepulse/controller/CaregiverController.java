package com.carepulse.controller;

import com.carepulse.dto.CaregiverRequest;
import com.carepulse.entity.CaregiverAccess;
import com.carepulse.service.CaregiverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/caregivers")
public class CaregiverController {

    private final CaregiverService caregiverService;

    public CaregiverController(CaregiverService caregiverService) {
        this.caregiverService = caregiverService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<CaregiverAccess> grantAccess(
            Authentication authentication,
            @Valid @RequestBody CaregiverRequest request) {
        return ResponseEntity.ok(caregiverService.grantAccess(authentication.getName(), request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<List<CaregiverAccess>> getMyCaregivers(Authentication authentication) {
        return ResponseEntity.ok(caregiverService.getCaregiversForPatient(authentication.getName()));
    }

    @GetMapping("/accessible-patients")
    public ResponseEntity<List<CaregiverAccess>> getAccessiblePatients(Authentication authentication) {
        return ResponseEntity.ok(caregiverService.getPatientsForCaregiver(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<CaregiverAccess> revokeAccess(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(caregiverService.revokeAccess(id, authentication.getName()));
    }
}
