package com.carepulse.controller;

import com.carepulse.dto.CreateConversationRequest;
import com.carepulse.dto.SendMessageRequest;
import com.carepulse.entity.*;
import com.carepulse.repository.*;
import com.carepulse.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DoctorConnectIntegrationTest {

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

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository messageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String patientToken;
    private String doctorToken;
    private String unauthorizedDoctorToken;
    private String adminToken;

    private Patient testPatient;
    private Doctor testDoctor;
    private Doctor otherDoctor;

    @BeforeEach
    void setUp() {
        // Ensure Patient exists
        User patientUser = userRepository.findByEmail("connect.patient@carepulse.com").orElseGet(() -> {
            User u = new User("connect.patient@carepulse.com", passwordEncoder.encode("password123"), "PATIENT");
            return userRepository.save(u);
        });
        testPatient = patientRepository.findByUserEmail(patientUser.getEmail()).orElseGet(() -> {
            Patient p = new Patient();
            p.setUser(patientUser);
            p.setFullName("John Doe Test");
            p.setPhone("+15551234567");
            p.setBloodGroup("O+");
            p.setGender("Male");
            p.setDateOfBirth(LocalDate.of(1990, 5, 15));
            return patientRepository.save(p);
        });

        // Ensure Doctor 1 exists
        User docUser = userRepository.findByEmail("connect.doc1@carepulse.com").orElseGet(() -> {
            User u = new User("connect.doc1@carepulse.com", passwordEncoder.encode("password123"), "DOCTOR");
            return userRepository.save(u);
        });
        testDoctor = doctorRepository.findByUserEmail(docUser.getEmail()).orElseGet(() -> {
            Doctor d = new Doctor();
            d.setUser(docUser);
            d.setFullName("Dr. Robert Smith");
            d.setSpecialization("Cardiology");
            d.setPhone("+15559876543");
            d.setAvailabilityStatus("AVAILABLE");
            return doctorRepository.save(d);
        });

        // Ensure Doctor 2 exists
        User docUser2 = userRepository.findByEmail("connect.doc2@carepulse.com").orElseGet(() -> {
            User u = new User("connect.doc2@carepulse.com", passwordEncoder.encode("password123"), "DOCTOR");
            return userRepository.save(u);
        });
        otherDoctor = doctorRepository.findByUserEmail(docUser2.getEmail()).orElseGet(() -> {
            Doctor d = new Doctor();
            d.setUser(docUser2);
            d.setFullName("Dr. Alice Walker");
            d.setSpecialization("Dermatology");
            d.setPhone("+15559876544");
            d.setAvailabilityStatus("AVAILABLE");
            return doctorRepository.save(d);
        });

        // Ensure Admin exists
        User adminUser = userRepository.findByEmail("connect.admin@carepulse.com").orElseGet(() -> {
            User u = new User("connect.admin@carepulse.com", passwordEncoder.encode("password123"), "ADMIN");
            return userRepository.save(u);
        });

        org.springframework.security.core.Authentication patientAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(patientUser.getEmail(), null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_PATIENT")));
        patientToken = "Bearer " + jwtTokenProvider.generateToken(patientAuth, "PATIENT", patientUser.getId());

        org.springframework.security.core.Authentication docAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(docUser.getEmail(), null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_DOCTOR")));
        doctorToken = "Bearer " + jwtTokenProvider.generateToken(docAuth, "DOCTOR", docUser.getId());

        org.springframework.security.core.Authentication otherDocAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(docUser2.getEmail(), null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_DOCTOR")));
        unauthorizedDoctorToken = "Bearer " + jwtTokenProvider.generateToken(otherDocAuth, "DOCTOR", docUser2.getId());

        org.springframework.security.core.Authentication adminAuth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(adminUser.getEmail(), null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        adminToken = "Bearer " + jwtTokenProvider.generateToken(adminAuth, "ADMIN", adminUser.getId());
    }

    @Test
    @DisplayName("Complete End-to-End DoctorConnect Workflow with Security Access Control")
    void testDoctorConnectFullLifecycle() throws Exception {
        // 1. Patient initiates conversation with Dr. Robert Smith
        CreateConversationRequest createReq = new CreateConversationRequest(
                testDoctor.getId(),
                null,
                "Chest Tightness Post-Exercise",
                "Hello Dr. Smith, I felt slight tightness in my chest after jogging yesterday morning. Should I reduce intensity?"
        );

        MvcResult createResult = mockMvc.perform(post("/api/doctor-connect/conversations")
                        .header("Authorization", patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.doctorName").value("Dr. Robert Smith"))
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andReturn();

        Map<?, ?> createdData = objectMapper.readValue(createResult.getResponse().getContentAsString(), Map.class);
        Long conversationId = ((Number) createdData.get("id")).longValue();

        // Verify persistence in PostgreSQL
        Conversation convDb = conversationRepository.findById(conversationId).orElse(null);
        assertThat(convDb).isNotNull();
        assertThat(convDb.getSubject()).isEqualTo("Chest Tightness Post-Exercise");
        assertThat(convDb.getStatus()).isEqualTo("OPEN");
        assertThat(convDb.getMessages()).hasSize(1);

        // Verify Doctor received in-app notification
        boolean notified = notificationRepository.findAll().stream()
                .anyMatch(n -> n.getUser().getId().equals(testDoctor.getUser().getId()) &&
                               n.getMessage().contains("Chest Tightness Post-Exercise"));
        assertThat(notified).isTrue();

        // 2. Doctor views their inquiries
        mockMvc.perform(get("/api/doctor-connect/conversations")
                        .header("Authorization", doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(conversationId))
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        // 3. Security: Unauthorized Doctor (Dr. Alice Walker) attempts to read conversation -> Must be 403 Forbidden
        mockMvc.perform(get("/api/doctor-connect/conversations/" + conversationId)
                        .header("Authorization", unauthorizedDoctorToken))
                .andExpect(status().isForbidden());

        // 4. Authorized Doctor views conversation and responds
        mockMvc.perform(get("/api/doctor-connect/conversations/" + conversationId)
                        .header("Authorization", doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId));

        SendMessageRequest docReply = new SendMessageRequest(
                "Please suspend strenuous exertion immediately and schedule an ECG evaluation. Monitor resting pulse."
        );

        mockMvc.perform(post("/api/doctor-connect/conversations/" + conversationId + "/messages")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(docReply)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderRole").value("DOCTOR"))
                .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("suspend strenuous exertion")));

        // Status should now be ANSWERED
        Conversation afterReply = conversationRepository.findById(conversationId).orElseThrow();
        assertThat(afterReply.getStatus()).isEqualTo("ANSWERED");
        assertThat(afterReply.getMessages()).hasSize(2);

        // Verify Patient received notification of doctor reply
        boolean patientNotified = notificationRepository.findAll().stream()
                .anyMatch(n -> n.getUser().getId().equals(testPatient.getUser().getId()) &&
                               n.getMessage().contains("replied to your inquiry"));
        assertThat(patientNotified).isTrue();

        // 5. Patient sends follow-up question
        SendMessageRequest patientFollowUp = new SendMessageRequest(
                "Thank you Doctor. I have booked an appointment for tomorrow. My resting pulse is 72 bpm."
        );
        mockMvc.perform(post("/api/doctor-connect/conversations/" + conversationId + "/messages")
                        .header("Authorization", patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patientFollowUp)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.senderRole").value("PATIENT"));

        // Status should transition back to OPEN
        Conversation afterFollowUp = conversationRepository.findById(conversationId).orElseThrow();
        assertThat(afterFollowUp.getStatus()).isEqualTo("OPEN");

        // 6. Doctor marks conversation as RESOLVED
        mockMvc.perform(patch("/api/doctor-connect/conversations/" + conversationId + "/status")
                        .header("Authorization", doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "RESOLVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        Conversation resolvedConv = conversationRepository.findById(conversationId).orElseThrow();
        assertThat(resolvedConv.getStatus()).isEqualTo("RESOLVED");

        // 7. Admin views all conversations and stats
        mockMvc.perform(get("/api/doctor-connect/stats")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalConversations").isNumber())
                .andExpect(jsonPath("$.resolvedConversations").isNumber());
    }
}

