package com.carepulse.service;

import com.carepulse.dto.*;
import com.carepulse.entity.*;
import com.carepulse.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EmergencyEndToEndWorkflowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private EmergencyDoctorRosterRepository rosterRepository;

    @Autowired
    private EmergencyRequestRepository requestRepository;

    @Autowired
    private EmergencyRosterService rosterService;

    @Test
    @DisplayName("Section 25: Real 22-Step End-To-End Emergency Workflow Test")
    void testComplete22StepEmergencyWorkflow() throws Exception {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        // STEP 1: Login Admin (simulated via Spring Security MockMvc User admin@carepulse.com)

        // Clean up any pre-existing test roster entries for tomorrow to ensure deterministic fresh generation
        List<EmergencyDoctorRoster> existingTomorrow = rosterRepository.findByRosterDate(tomorrow);
        if (!existingTomorrow.isEmpty()) {
            rosterRepository.deleteAll(existingTomorrow);
        }

        // STEP 2: Open Emergency Duty Roster
        MvcResult rosterResult = mockMvc.perform(get("/api/admin/emergency-roster")
                        .param("date", tomorrow.toString())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@carepulse.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        // STEP 3: Generate roster for tomorrow
        EmergencyRosterGenerateRequestDTO generateReq = new EmergencyRosterGenerateRequestDTO(tomorrow, 1);
        MvcResult genResult = mockMvc.perform(post("/api/admin/emergency-roster/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(generateReq))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@carepulse.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        // STEP 4: Verify real doctors appear in the generated roster
        List<EmergencyRosterResponseDTO> generatedRoster = objectMapper.readValue(
                genResult.getResponse().getContentAsString(),
                new TypeReference<List<EmergencyRosterResponseDTO>>() {}
        );
        assertNotNull(generatedRoster, "Generated roster must not be null");
        assertFalse(generatedRoster.isEmpty(), "Generated roster must contain real scheduled shifts");
        System.out.println("STEP 4: Generated " + generatedRoster.size() + " emergency duty entries for tomorrow.");

        // STEP 5: Refresh browser / query again
        MvcResult refreshedResult = mockMvc.perform(get("/api/admin/emergency-roster")
                        .param("date", tomorrow.toString())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin@carepulse.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn();

        // STEP 6: Verify roster still exists after refresh
        List<EmergencyRosterResponseDTO> persistentRoster = objectMapper.readValue(
                refreshedResult.getResponse().getContentAsString(),
                new TypeReference<List<EmergencyRosterResponseDTO>>() {}
        );
        assertEquals(generatedRoster.size(), persistentRoster.size(), "Persistent roster size must match generated roster");
        assertTrue(persistentRoster.stream().allMatch(r -> r.getRosterDate().equals(tomorrow)));

        // STEP 7: Login as one assigned Doctor
        EmergencyRosterResponseDTO chosenShift = persistentRoster.get(0);
        Doctor assignedDoc = doctorRepository.findById(chosenShift.getDoctorId()).orElseThrow();
        String doctorEmail = assignedDoc.getUser().getEmail();
        assertNotNull(doctorEmail, "Assigned doctor must have a valid user email");

        // Clean up any pre-existing active requests for this doctor from earlier test runs
        List<EmergencyRequest> lingeringCases = requestRepository.findByAssignedDoctorOrderByRequestTimeDesc(assignedDoc);
        for (EmergencyRequest lingering : lingeringCases) {
            if ("ASSIGNED".equals(lingering.getStatus()) || "IN_PROGRESS".equals(lingering.getStatus())) {
                lingering.setStatus("COMPLETED");
                lingering.setCompletedTime(java.time.LocalDateTime.now());
                requestRepository.save(lingering);
            }
        }

        // STEP 8: Verify Emergency Duty = YES
        // Ensure today has an active roster shift for this doctor so real-time emergency duty applies
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        // Clean up any pre-existing test shift for this doctor on today
        rosterRepository.findByDoctorIdAndRosterDateAndShiftName(assignedDoc.getId(), today, "TODAY_TEST_SHIFT")
                .ifPresent(rosterRepository::delete);

        rosterRepository.save(new EmergencyDoctorRoster(
                assignedDoc,
                today,
                "TODAY_TEST_SHIFT",
                now.minusMinutes(30),
                now.plusHours(4),
                "EMERGENCY_DUTY",
                "AVAILABLE"
        ));

        DoctorTodayDutyDTO doctorTodayDuty = rosterService.getDoctorTodayDutySummary(doctorEmail);
        assertTrue(doctorTodayDuty.isEmergencyDuty(), "STEP 8: Doctor emergency duty must be YES");

        // STEP 9: Set/verify doctor status = AVAILABLE
        DoctorStatusUpdateRequestDTO statusUpdate = new DoctorStatusUpdateRequestDTO("AVAILABLE");
        mockMvc.perform(put("/api/emergency-roster/my-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(doctorEmail).roles("DOCTOR")))
                .andExpect(status().isOk());

        DoctorTodayDutyDTO verifiedDuty = rosterService.getDoctorTodayDutySummary(doctorEmail);
        assertEquals("AVAILABLE", verifiedDuty.getStatus(), "STEP 9: Doctor status must be AVAILABLE");
        long initialCompletedCases = verifiedDuty.getTodayCompletedCases();

        // STEP 10: Login as Patient
        List<Patient> patients = patientRepository.findAll();
        Patient patient = patients.isEmpty() ? null : patients.get(0);
        assertNotNull(patient, "A registered patient must exist in the database");
        String patientEmail = patient.getUser().getEmail();

        // STEP 11 & 12: Click Emergency Assistance & Submit request
        EmergencyRequestCreateDTO reqDTO = new EmergencyRequestCreateDTO();
        reqDTO.setCategory(assignedDoc.getSpecialization());
        reqDTO.setDescription("Acute emergency symptom evaluation");
        reqDTO.setPriority("URGENT");

        MvcResult emergencyResult = mockMvc.perform(post("/api/emergency-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqDTO))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(patientEmail).roles("PATIENT")))
                .andExpect(status().isOk())
                .andReturn();

        // STEP 13: Verify backend assigns appropriate doctor
        EmergencyRequestResponseDTO createdReq = objectMapper.readValue(
                emergencyResult.getResponse().getContentAsString(),
                EmergencyRequestResponseDTO.class
        );
        assertNotNull(createdReq.getId(), "Emergency request ID must not be null");
        assertEquals("ASSIGNED", createdReq.getStatus(), "STEP 13: Request must be ASSIGNED");
        assertNotNull(createdReq.getDoctorId(), "Assigned doctor ID must not be null");
        assertEquals(assignedDoc.getId(), createdReq.getDoctorId(), "Assigned doctor must match candidate");

        // STEP 14 & 15: Login as Doctor and verify request appears
        MvcResult docRequestsResult = mockMvc.perform(get("/api/emergency-requests/assigned")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(doctorEmail).roles("DOCTOR")))
                .andExpect(status().isOk())
                .andReturn();

        List<EmergencyRequestResponseDTO> docRequests = objectMapper.readValue(
                docRequestsResult.getResponse().getContentAsString(),
                new TypeReference<List<EmergencyRequestResponseDTO>>() {}
        );
        assertTrue(docRequests.stream().anyMatch(r -> r.getId().equals(createdReq.getId())),
                "STEP 15: Assigned emergency request must appear in doctor's queue");

        // STEP 16: Doctor starts request
        EmergencyStatusUpdateRequestDTO inProgressUpdate = new EmergencyStatusUpdateRequestDTO("IN_PROGRESS", "Starting consultation");
        MvcResult startResult = mockMvc.perform(put("/api/emergency-requests/" + createdReq.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inProgressUpdate))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(doctorEmail).roles("DOCTOR")))
                .andExpect(status().isOk())
                .andReturn();

        // STEP 17: Verify patient status changes to IN_PROGRESS
        EmergencyRequestResponseDTO startedReq = objectMapper.readValue(
                startResult.getResponse().getContentAsString(),
                EmergencyRequestResponseDTO.class
        );
        assertEquals("IN_PROGRESS", startedReq.getStatus(), "STEP 17: Status must transition to IN_PROGRESS");
        assertNotNull(startedReq.getStartedTime(), "Started time timestamp must be populated");

        // STEP 18: Doctor completes request
        EmergencyStatusUpdateRequestDTO completeUpdate = new EmergencyStatusUpdateRequestDTO("COMPLETED", "Consultation completed. Patient stabilized.");
        MvcResult completeResult = mockMvc.perform(put("/api/emergency-requests/" + createdReq.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeUpdate))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(doctorEmail).roles("DOCTOR")))
                .andExpect(status().isOk())
                .andReturn();

        // STEP 19: Verify patient sees COMPLETED
        EmergencyRequestResponseDTO completedReq = objectMapper.readValue(
                completeResult.getResponse().getContentAsString(),
                EmergencyRequestResponseDTO.class
        );
        assertEquals("COMPLETED", completedReq.getStatus(), "STEP 19: Status must transition to COMPLETED");
        assertNotNull(completedReq.getCompletedTime(), "Completed time timestamp must be populated");

        // STEP 20: Verify doctor availability returns appropriately
        DoctorTodayDutyDTO dutyAfterCompletion = rosterService.getDoctorTodayDutySummary(doctorEmail);
        assertEquals("AVAILABLE", dutyAfterCompletion.getStatus(), "STEP 20: Doctor status must return to AVAILABLE");
        assertEquals(initialCompletedCases + 1, dutyAfterCompletion.getTodayCompletedCases(), "Doctor must show incremented completed emergency cases count");

        // STEP 21: Create another emergency request
        EmergencyRequestCreateDTO secondReqDTO = new EmergencyRequestCreateDTO();
        secondReqDTO.setCategory("GENERAL");
        secondReqDTO.setDescription("Follow-up urgent assessment");
        secondReqDTO.setPriority("NORMAL");

        MvcResult secondResult = mockMvc.perform(post("/api/emergency-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondReqDTO))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(patientEmail).roles("PATIENT")))
                .andExpect(status().isOk())
                .andReturn();

        // STEP 22: Verify another appropriate doctor is selected if available
        EmergencyRequestResponseDTO secondCreatedReq = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(),
                EmergencyRequestResponseDTO.class
        );
        assertNotNull(secondCreatedReq.getId());
        assertTrue("ASSIGNED".equals(secondCreatedReq.getStatus()) || "WAITING".equals(secondCreatedReq.getStatus()));
        System.out.println("STEP 22: Second emergency request resolved with status: " + secondCreatedReq.getStatus() +
                (secondCreatedReq.getDoctorName() != null ? " to " + secondCreatedReq.getDoctorName() : ""));
    }
}
