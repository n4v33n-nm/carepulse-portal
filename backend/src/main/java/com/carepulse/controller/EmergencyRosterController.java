package com.carepulse.controller;

import com.carepulse.dto.DoctorStatusUpdateRequestDTO;
import com.carepulse.dto.EmergencyRequestResponseDTO;
import com.carepulse.dto.EmergencyRosterRequestDTO;
import com.carepulse.dto.EmergencyRosterResponseDTO;
import com.carepulse.entity.Doctor;
import com.carepulse.service.EmergencyRequestService;
import com.carepulse.service.EmergencyRosterService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
public class EmergencyRosterController {

    private final EmergencyRosterService emergencyRosterService;
    private final EmergencyRequestService emergencyRequestService;

    public EmergencyRosterController(EmergencyRosterService emergencyRosterService,
                                   EmergencyRequestService emergencyRequestService) {
        this.emergencyRosterService = emergencyRosterService;
        this.emergencyRequestService = emergencyRequestService;
    }

    // ==========================================
    // DOCTOR EMERGENCY DUTY & STATUS ENDPOINTS
    // ==========================================

    @GetMapping("/api/emergency-roster/my-duty")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<List<EmergencyRosterResponseDTO>> getMyEmergencyDuty(Authentication authentication) {
        List<EmergencyRosterResponseDTO> duties = emergencyRosterService.getDoctorTodayDuty(authentication.getName());
        return ResponseEntity.ok(duties);
    }

    @GetMapping("/api/doctor/emergency-duty/today")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<com.carepulse.dto.DoctorTodayDutyDTO> getDoctorTodayDutyToday(Authentication authentication) {
        com.carepulse.dto.DoctorTodayDutyDTO duty = emergencyRosterService.getDoctorTodayDutySummary(authentication.getName());
        return ResponseEntity.ok(duty);
    }

    @PutMapping("/api/emergency-roster/my-status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Doctor> updateMyAvailabilityStatus(
            Authentication authentication,
            @Valid @RequestBody DoctorStatusUpdateRequestDTO request) {
        Doctor doctor = emergencyRosterService.updateDoctorStatus(authentication.getName(), request);
        return ResponseEntity.ok(doctor);
    }

    // ==========================================
    // ADMIN EMERGENCY ROSTER MANAGEMENT ENDPOINTS
    // ==========================================

    @GetMapping("/api/admin/emergency-roster/doctors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.carepulse.dto.DoctorRosterOptionDTO>> getDoctorsForRoster() {
        List<com.carepulse.dto.DoctorRosterOptionDTO> doctors = emergencyRosterService.getDoctorsForRoster();
        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/api/admin/emergency-roster")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmergencyRosterResponseDTO>> getRosterByDate(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<EmergencyRosterResponseDTO> rosters = emergencyRosterService.getRosterByDate(date);
        return ResponseEntity.ok(rosters);
    }

    @GetMapping("/api/admin/emergency-roster/range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmergencyRosterResponseDTO>> getRosterRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        List<EmergencyRosterResponseDTO> rosters = emergencyRosterService.getRosterByDateRange(start, end);
        return ResponseEntity.ok(rosters);
    }

    @PostMapping("/api/admin/emergency-roster")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmergencyRosterResponseDTO> createRosterEntry(
            Authentication authentication,
            @Valid @RequestBody EmergencyRosterRequestDTO request) {
        EmergencyRosterResponseDTO response = emergencyRosterService.createRosterEntry(request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/api/admin/emergency-roster/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmergencyRosterResponseDTO> updateRosterEntry(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody EmergencyRosterRequestDTO request) {
        EmergencyRosterResponseDTO response = emergencyRosterService.updateRosterEntry(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/api/admin/emergency-roster/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> deleteRosterEntry(
            @PathVariable Long id,
            Authentication authentication) {
        emergencyRosterService.deleteRosterEntry(id, authentication.getName());
        return ResponseEntity.ok(Map.of("message", "Emergency roster entry deleted successfully"));
    }

    @GetMapping("/api/admin/emergency-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmergencyRequestResponseDTO>> getAllEmergencyRequests() {
        List<EmergencyRequestResponseDTO> requests = emergencyRequestService.getAllEmergencyRequests();
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/api/admin/emergency-roster/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EmergencyRosterResponseDTO>> generateEmergencyRoster(
            Authentication authentication,
            @RequestBody(required = false) com.carepulse.dto.EmergencyRosterGenerateRequestDTO request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date;
        Integer doctorsPerShift = null;
        if (request != null) {
            if (request.getRosterDate() != null) targetDate = request.getRosterDate();
            if (request.getDoctorsPerShift() != null) doctorsPerShift = request.getDoctorsPerShift();
        }
        if (targetDate == null) {
            targetDate = LocalDate.now().plusDays(1); // default to tomorrow
        }
        List<EmergencyRosterResponseDTO> generated = emergencyRosterService.autoGenerateRoster(
                targetDate, doctorsPerShift, authentication.getName());
        return ResponseEntity.ok(generated);
    }

    @GetMapping("/api/admin/emergency-analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.carepulse.dto.EmergencyAnalyticsDTO> getEmergencyAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        com.carepulse.dto.EmergencyAnalyticsDTO analytics = emergencyRequestService.getEmergencyAnalytics(date);
        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/api/admin/emergency-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getEmergencyStats() {
        Map<String, Long> stats = emergencyRequestService.getEmergencyStats();
        return ResponseEntity.ok(stats);
    }
}
