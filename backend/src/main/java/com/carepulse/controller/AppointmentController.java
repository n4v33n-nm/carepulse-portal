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

    public AppointmentController(AppointmentService appointmentService, UserService userService) {
        this.appointmentService = appointmentService;
        this.userService = userService;
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
}
