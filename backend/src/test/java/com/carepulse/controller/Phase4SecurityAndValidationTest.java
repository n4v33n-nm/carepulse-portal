package com.carepulse.controller;

import com.carepulse.entity.Doctor;
import com.carepulse.entity.Patient;
import com.carepulse.entity.User;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.PatientRepository;
import com.carepulse.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Phase4SecurityAndValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Test
    @DisplayName("Security: Public health check endpoint returns UP and database connectivity")
    void testHealthCheckEndpointPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Security: Successful login returns token and never exposes password")
    void testSuccessfulLogin() throws Exception {
        String loginPayload = "{\"email\":\"admin@carepulse.com\",\"password\":\"Admin@123\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("admin@carepulse.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("Security: Invalid login credentials returns 401 and clean message")
    void testInvalidPasswordLogin() throws Exception {
        String loginPayload = "{\"email\":\"admin@carepulse.com\",\"password\":\"WrongPassword!\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Data Security: User entity serialization never includes password hash in JSON")
    void testUserEntityNeverExposesPasswordInJson() throws Exception {
        User user = userRepository.findByEmail("admin@carepulse.com").orElseThrow();
        String json = objectMapper.writeValueAsString(user);
        assertFalse(json.contains("\"password\""), "Serialized User JSON must never contain password field!");
    }

    @Test
    @WithMockUser(username = "patient@carepulse.com", roles = {"PATIENT"})
    @DisplayName("RBAC: Patient is forbidden from accessing Admin Dashboard (403 Forbidden)")
    void testPatientCannotAccessAdminDashboard() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "dr.jenkins@carepulse.com", roles = {"DOCTOR"})
    @DisplayName("RBAC: Doctor is forbidden from accessing Admin User Management (403 Forbidden)")
    void testDoctorCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "dr.jenkins@carepulse.com", roles = {"DOCTOR"})
    @DisplayName("Ownership: Doctor cannot modify another doctor's profile (403 Forbidden)")
    void testDoctorCannotModifyAnotherDoctorProfile() throws Exception {
        List<Doctor> doctors = doctorRepository.findAll();
        Doctor otherDoctor = doctors.stream()
                .filter(d -> !d.getUser().getEmail().equalsIgnoreCase("dr.jenkins@carepulse.com"))
                .findFirst()
                .orElse(null);

        if (otherDoctor != null) {
            String updatePayload = "{\"bio\":\"Malicious update\",\"consultationFee\":5000.0}";
            mockMvc.perform(put("/api/doctors/" + otherDoctor.getId() + "/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePayload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = {"PATIENT"})
    @DisplayName("Ownership: Patient cannot modify another patient's profile (403 Forbidden)")
    void testPatientCannotModifyAnotherPatientProfile() throws Exception {
        List<Patient> patients = patientRepository.findAll();
        Patient otherPatient = patients.stream()
                .filter(p -> !p.getUser().getEmail().equalsIgnoreCase("john.doe@example.com"))
                .findFirst()
                .orElse(null);

        if (otherPatient != null) {
            String updatePayload = "{\"address\":\"Compromised address\",\"emergencyContact\":\"999\"}";
            mockMvc.perform(put("/api/patients/" + otherPatient.getId() + "/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updatePayload))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
        }
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = {"PATIENT"})
    @DisplayName("Validation: Booking appointment on past date is rejected (400 Bad Request)")
    void testBookingAppointmentOnPastDateRejected() throws Exception {
        Doctor doctor = doctorRepository.findAll().get(0);
        String payload = String.format("{\"doctorId\":%d,\"appointmentDate\":\"2020-01-01\",\"appointmentTime\":\"10:00:00\",\"reason\":\"Checkup\"}", doctor.getId());

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = {"PATIENT"})
    @DisplayName("Business Rule: Booking appointment for past time slot today is rejected (400 Bad Request)")
    void testBookingAppointmentForPastTimeTodayRejected() throws Exception {
        Doctor doctor = doctorRepository.findAll().get(0);
        LocalDate today = LocalDate.now();
        LocalTime pastTime = LocalTime.of(0, 1); // 00:01 AM today is always past during normal operations

        String payload = String.format("{\"doctorId\":%d,\"appointmentDate\":\"%s\",\"appointmentTime\":\"%s\",\"reason\":\"Checkup\"}",
                doctor.getId(), today.toString(), pastTime.toString());

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("past")));
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = {"PATIENT"})
    @DisplayName("Validation: DTO field errors return structured fieldErrors map")
    void testDtoFieldErrorsStructuredResponse() throws Exception {
        // Missing doctorId and empty reason
        String invalidPayload = "{\"appointmentDate\":\"2099-01-01\",\"appointmentTime\":\"10:00:00\"}";

        mockMvc.perform(post("/api/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Error"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }
}
