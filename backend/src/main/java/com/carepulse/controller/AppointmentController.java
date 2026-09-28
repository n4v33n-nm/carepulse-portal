package com.carepulse.controller;

import com.carepulse.dto.AppointmentRequest;
import com.carepulse.dto.AppointmentStatusUpdateRequest;
import com.carepulse.entity.Appointment;
import com.carepulse.entity.User;
import com.carepulse.service.AppointmentService;
import com.carepulse.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserService userService;
    private final com.carepulse.service.AppointmentWaitlistService waitlistService;
    private final com.carepulse.service.AppointmentWaitTimeService waitTimeService;
    private final com.carepulse.service.DoctorService doctorService;

    public AppointmentController(AppointmentService appointmentService,
                                 UserService userService,
                                 com.carepulse.service.AppointmentWaitlistService waitlistService,
                                 com.carepulse.service.AppointmentWaitTimeService waitTimeService,
                                 com.carepulse.service.DoctorService doctorService) {
        this.appointmentService = appointmentService;
        this.userService = userService;
        this.waitlistService = waitlistService;
        this.waitTimeService = waitTimeService;
        this.doctorService = doctorService;
    }

    @PostMapping
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<Appointment> bookAppointment(
            Authentication authentication,
            @Valid @RequestBody AppointmentRequest request) {
        Appointment appointment = appointmentService.bookAppointment(authentication.getName(), request);
        return ResponseEntity.ok(appointment);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Appointment>> getMyAppointments(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        if ("DOCTOR".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.ok(appointmentService.getAppointmentsForDoctor(user.getEmail()));
        } else if ("PATIENT".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.ok(appointmentService.getAppointmentsForPatient(user.getEmail()));
        } else {
            return ResponseEntity.ok(appointmentService.getAllAppointments());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable Long id, Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(appointmentService.getAppointmentById(id, user.getEmail(), user.getRole()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Appointment> updateStatus(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody AppointmentStatusUpdateRequest request) {
        User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(appointmentService.updateAppointmentStatus(id, user.getEmail(), user.getRole(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Appointment> cancelAppointment(
            @PathVariable Long id,
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "Cancelled by user") String reason) {
        User user = userService.getUserByEmail(authentication.getName());
        AppointmentStatusUpdateRequest req = new AppointmentStatusUpdateRequest();
        req.setStatus("CANCELLED");
        req.setCancellationReason(reason);
        return ResponseEntity.ok(appointmentService.updateAppointmentStatus(id, user.getEmail(), user.getRole(), req));
    }

    @PutMapping("/{id}/reschedule")
    public ResponseEntity<Appointment> rescheduleAppointment(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody com.carepulse.dto.AppointmentRescheduleRequestDTO request) {
        User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(appointmentService.rescheduleAppointment(id, user.getEmail(), user.getRole(), request));
    }

    @GetMapping("/available-slots")
    public ResponseEntity<List<java.time.LocalTime>> getAvailableSlotsForDoctor(
            @RequestParam Long doctorId,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        return ResponseEntity.ok(doctorService.getAvailableTimeSlots(doctorId, date));
    }

    @PostMapping("/waitlist")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<com.carepulse.dto.AppointmentWaitlistResponseDTO> joinWaitlist(
            Authentication authentication,
            @Valid @RequestBody com.carepulse.dto.JoinWaitlistRequestDTO request) {
        return ResponseEntity.ok(waitlistService.joinWaitlist(authentication.getName(), request));
    }

    @GetMapping("/waitlist")
    public ResponseEntity<List<com.carepulse.dto.AppointmentWaitlistResponseDTO>> getWaitlist(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.ok(waitlistService.getAllWaitlistEntries());
        }
        return ResponseEntity.ok(waitlistService.getMyWaitlistEntries(user.getEmail()));
    }

    @DeleteMapping("/waitlist/{id}")
    public ResponseEntity<java.util.Map<String, String>> cancelWaitlist(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        waitlistService.cancelWaitlistEntry(id, user.getEmail(), user.getRole());
        return ResponseEntity.ok(java.util.Map.of("message", "Waitlist entry cancelled successfully"));
    }

    @GetMapping("/{id}/wait-time")
    public ResponseEntity<com.carepulse.dto.AppointmentWaitTimeResponseDTO> getAppointmentWaitTime(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        // Verify ownership/permission via appointmentService
        appointmentService.getAppointmentById(id, user.getEmail(), user.getRole());
        return ResponseEntity.ok(waitTimeService.calculateWaitTime(id));
    }
}
