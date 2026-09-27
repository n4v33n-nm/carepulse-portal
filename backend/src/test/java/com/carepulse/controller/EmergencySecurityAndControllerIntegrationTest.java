package com.carepulse.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmergencySecurityAndControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("2. Unauthenticated user cannot create emergency request (401 / 403)")
    void testUnauthenticatedUserCannotCreateEmergencyRequest() throws Exception {
        mockMvc.perform(post("/api/emergency-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"Cardiology\",\"description\":\"Chest pain\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated user cannot access admin emergency roster")
    void testUnauthenticatedUserCannotAccessAdminRoster() throws Exception {
        mockMvc.perform(get("/api/admin/emergency-roster"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "doctor@carepulse.com", roles = {"DOCTOR"})
    @DisplayName("11. Unauthorized doctor cannot manage emergency roster (403 Forbidden)")
    void testDoctorCannotManageEmergencyRoster() throws Exception {
        mockMvc.perform(post("/api/admin/emergency-roster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorId\":1,\"rosterDate\":\"2026-09-27\",\"shiftName\":\"MORNING\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "patient@carepulse.com", roles = {"PATIENT"})
    @DisplayName("Patient cannot access doctor's assigned emergency queue (403 Forbidden)")
    void testPatientCannotAccessDoctorAssignedQueue() throws Exception {
        mockMvc.perform(get("/api/emergency-requests/assigned"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@carepulse.com", roles = {"ADMIN"})
    @DisplayName("Admin can view emergency stats and roster")
    void testAdminCanViewEmergencyStatsAndRoster() throws Exception {
        mockMvc.perform(get("/api/admin/emergency-stats"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/emergency-roster"))
                .andExpect(status().isOk());
    }
}
