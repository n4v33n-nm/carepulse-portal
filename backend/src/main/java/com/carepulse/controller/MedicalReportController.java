package com.carepulse.controller;

import com.carepulse.dto.MedicalReportResponseDTO;
import com.carepulse.entity.MedicalReport;
import com.carepulse.entity.User;
import com.carepulse.service.MedicalReportService;
import com.carepulse.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class MedicalReportController {

    private final MedicalReportService medicalReportService;
    private final UserService userService;

    public MedicalReportController(MedicalReportService medicalReportService, UserService userService) {
        this.medicalReportService = medicalReportService;
        this.userService = userService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MedicalReportResponseDTO> uploadReport(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "reportType", required = false, defaultValue = "OTHER") String reportType,
            @RequestParam(value = "reportDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reportDate,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "patientId", required = false) Long patientId) {

        User user = userService.getUserByEmail(authentication.getName());
        MedicalReportResponseDTO response = medicalReportService.uploadReport(
                user.getEmail(),
                user.getRole(),
                file,
                title,
                reportType,
                reportDate,
                description,
                doctorId,
                patientId
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT') or hasRole('ADMIN')")
    public ResponseEntity<List<MedicalReportResponseDTO>> getMyReports(Authentication authentication) {
        return ResponseEntity.ok(medicalReportService.getReportsForPatient(authentication.getName()));
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MedicalReportResponseDTO>> getReportsByPatientId(
            @PathVariable Long patientId,
            Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(medicalReportService.getReportsByPatientId(patientId, user.getEmail(), user.getRole()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MedicalReportResponseDTO> getReportById(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(medicalReportService.getReportById(id, user.getEmail(), user.getRole()));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadReport(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        Resource resource = medicalReportService.loadReportFileResource(id, user.getEmail(), user.getRole());
        MedicalReport report = medicalReportService.getReportEntity(id);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(report.getFileType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.getOriginalFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/view")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> viewReportInline(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        Resource resource = medicalReportService.loadReportFileResource(id, user.getEmail(), user.getRole());
        MedicalReport report = medicalReportService.getReportEntity(id);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(report.getFileType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + report.getOriginalFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReport(
            @PathVariable Long id,
            Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        medicalReportService.deleteReport(id, user.getEmail(), user.getRole());
        return ResponseEntity.noContent().build();
    }
}

