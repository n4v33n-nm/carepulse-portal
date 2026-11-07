package com.carepulse.controller;

import com.carepulse.entity.*;
import com.carepulse.repository.*;
import com.carepulse.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class Phase5MedicalReportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalReportRepository medicalReportRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User patientUser;
    private Patient patient;
    private String patientToken;

    private User authorizedDoctorUser;
    private Doctor authorizedDoctor;
    private String authorizedDoctorToken;

    private User unauthorizedDoctorUser;
    private Doctor unauthorizedDoctor;
    private String unauthorizedDoctorToken;

    private User adminUser;
    private String adminToken;

    @BeforeEach
    public void setup() {
        long ts = System.currentTimeMillis();

        // 1. Patient
        patientUser = userRepository.save(new User("patient_p5_" + ts + "@carepulse.com", passwordEncoder.encode("secret"), "PATIENT"));
        patient = new Patient();
        patient.setUser(patientUser);
        patient.setFullName("Patient Phase Five");
        patient.setPhone("+15559876543");
        patient.setGender("Female");
        patient.setBloodGroup("B+");
        patient.setAddress("42 Health Parkway");
        patient.setDateOfBirth(LocalDate.of(1992, 8, 20));
        patient = patientRepository.save(patient);

        patientToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(patientUser.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))),
                "PATIENT", patientUser.getId());

        // 2. Authorized Doctor
        authorizedDoctorUser = userRepository.save(new User("doc_auth5_" + ts + "@carepulse.com", passwordEncoder.encode("secret"), "DOCTOR"));
        authorizedDoctor = new Doctor();
        authorizedDoctor.setUser(authorizedDoctorUser);
        authorizedDoctor.setFullName("Dr. Gregory House");
        authorizedDoctor.setSpecialization("Diagnostic Medicine");
        authorizedDoctor.setExperienceYears(20);
        authorizedDoctor.setConsultationFee(200.0);
        authorizedDoctor.setAvailabilityStatus("AVAILABLE");
        authorizedDoctor = doctorRepository.save(authorizedDoctor);

        authorizedDoctorToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(authorizedDoctorUser.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))),
                "DOCTOR", authorizedDoctorUser.getId());

        // 3. Unauthorized Doctor
        unauthorizedDoctorUser = userRepository.save(new User("doc_unauth5_" + ts + "@carepulse.com", passwordEncoder.encode("secret"), "DOCTOR"));
        unauthorizedDoctor = new Doctor();
        unauthorizedDoctor.setUser(unauthorizedDoctorUser);
        unauthorizedDoctor.setFullName("Dr. Robert Chase");
        unauthorizedDoctor.setSpecialization("Surgery");
        unauthorizedDoctor.setExperienceYears(10);
        unauthorizedDoctor.setConsultationFee(150.0);
        unauthorizedDoctor.setAvailabilityStatus("AVAILABLE");
        unauthorizedDoctor = doctorRepository.save(unauthorizedDoctor);

        unauthorizedDoctorToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(unauthorizedDoctorUser.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))),
                "DOCTOR", unauthorizedDoctorUser.getId());

        // 4. Admin
        adminUser = userRepository.save(new User("admin_p5_" + ts + "@carepulse.com", passwordEncoder.encode("secret"), "ADMIN"));
        adminToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(adminUser.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))),
                "ADMIN", adminUser.getId());

        // Authorize Doctor via appointment
        Appointment appt = new Appointment(patient, authorizedDoctor, LocalDate.now().plusDays(2), LocalTime.of(11, 0), "Diagnostic evaluation");
        appt.setStatus("CONFIRMED");
        appointmentRepository.save(appt);
    }

    @Test
    @DisplayName("Patient can securely upload a valid PDF medical report")
    public void testPatientCanUploadValidReport() throws Exception {
        byte[] pdfContent = "%PDF-1.4 sample blood count test document content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cbc_report.pdf",
                "application/pdf",
                pdfContent
        );

        mockMvc.perform(multipart("/api/reports/upload")
                        .file(file)
                        .param("title", "Comprehensive Metabolic Panel")
                        .param("reportType", "BLOOD_TEST")
                        .param("reportDate", LocalDate.now().toString())
                        .param("description", "Routine annual blood test results")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Comprehensive Metabolic Panel"))
                .andExpect(jsonPath("$.reportType").value("BLOOD_TEST"))
                .andExpect(jsonPath("$.originalFilename").value("cbc_report.pdf"))
                .andExpect(jsonPath("$.patientId").value(patient.getId()));

        var reports = medicalReportRepository.findByPatientOrderByReportDateDesc(patient);
        assertThat(reports).hasSize(1);
        assertThat(reports.get(0).getTitle()).isEqualTo("Comprehensive Metabolic Panel");
        assertThat(reports.get(0).getFileType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Backend rejects unauthorized executable or script file extensions with 400 Bad Request")
    public void testBackendRejectsInvalidFileExtensions() throws Exception {
        MockMultipartFile maliciousFile = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "malicious script payload".getBytes()
        );

        mockMvc.perform(multipart("/api/reports/upload")
                        .file(maliciousFile)
                        .param("title", "Harmful File")
                        .param("reportType", "OTHER")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Authorized doctor can view and download assigned patient's report; unauthorized doctor is denied with 403")
    public void testDoctorAccessAndDownloadAuthorization() throws Exception {
        // Upload report as patient
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "chest_xray.png",
                "image/png",
                "image byte stream".getBytes()
        );

        String responseStr = mockMvc.perform(multipart("/api/reports/upload")
                        .file(file)
                        .param("title", "Chest X-Ray PA View")
                        .param("reportType", "SCAN_REPORT")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long reportId = medicalReportRepository.findByPatientOrderByReportDateDesc(patient).get(0).getId();

        // 1. Authorized doctor can list reports for patient
        mockMvc.perform(get("/api/reports/patient/" + patient.getId())
                        .header("Authorization", "Bearer " + authorizedDoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Chest X-Ray PA View"));

        // 2. Authorized doctor can download report file
        mockMvc.perform(get("/api/reports/" + reportId + "/download")
                        .header("Authorization", "Bearer " + authorizedDoctorToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"chest_xray.png\""));

        // 3. Unauthorized doctor cannot view report metadata (403 Forbidden)
        mockMvc.perform(get("/api/reports/patient/" + patient.getId())
                        .header("Authorization", "Bearer " + unauthorizedDoctorToken))
                .andExpect(status().isForbidden());

        // 4. Unauthorized doctor cannot download report file (403 Forbidden)
        mockMvc.perform(get("/api/reports/" + reportId + "/download")
                        .header("Authorization", "Bearer " + unauthorizedDoctorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request cannot access or download reports directly")
    public void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(get("/api/reports/my"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/reports/1/download"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Patient can delete their own uploaded report")
    public void testPatientCanDeleteReport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "to_delete.pdf",
                "application/pdf",
                "temp report".getBytes()
        );

        mockMvc.perform(multipart("/api/reports/upload")
                        .file(file)
                        .param("title", "Temporary Lab Report")
                        .param("reportType", "LAB_REPORT")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk());

        Long reportId = medicalReportRepository.findByPatientOrderByReportDateDesc(patient).get(0).getId();

        mockMvc.perform(delete("/api/reports/" + reportId)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isNoContent());

        assertThat(medicalReportRepository.findById(reportId)).isEmpty();
    }
}

