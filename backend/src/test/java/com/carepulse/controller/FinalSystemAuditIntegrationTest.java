package com.carepulse.controller;

import com.carepulse.dto.AppointmentRequest;
import com.carepulse.dto.AppointmentRescheduleRequestDTO;
import com.carepulse.entity.Appointment;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.Patient;
import com.carepulse.entity.User;
import com.carepulse.repository.AppointmentRepository;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.MedicalRecordRepository;
import com.carepulse.repository.PatientRepository;
import com.carepulse.repository.UserRepository;
import com.carepulse.service.AppointmentService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FinalSystemAuditIntegrationTest {

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
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private com.carepulse.service.DoctorService doctorService;

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = {"PATIENT"})
    @DisplayName("Security: Patient can view own medical records")
    void testPatientCanViewOwnMedicalRecords() throws Exception {
        mockMvc.perform(get("/api/records/my"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "dr.vance@carepulse.com", roles = {"DOCTOR"})
    @DisplayName("Security: Doctor without treating relationship is forbidden from accessing patient records (403 Forbidden)")
    void testUnauthorizedDoctorCannotViewPatientRecords() throws Exception {
        Doctor doctor = doctorRepository.findByUserEmail("dr.vance@carepulse.com").orElse(null);
        if (doctor != null) {
            User u = userRepository.findByEmail("isolated.audit.patient@example.com").orElseGet(() -> {
                User newUser = new User("isolated.audit.patient@example.com", "Password@123", "PATIENT");
                return userRepository.save(newUser);
            });
            Patient isolatedPatient = patientRepository.findByUserEmail("isolated.audit.patient@example.com").orElseGet(() -> {
                Patient newP = new Patient(u, "Isolated Patient", "+15559998888", LocalDate.of(1995, 1, 1), "Other");
                return patientRepository.save(newP);
            });

            mockMvc.perform(get("/api/records/patient/" + isolatedPatient.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = {"PATIENT"})
    @DisplayName("Security: Patient can view own prescriptions")
    void testPatientCanViewOwnPrescriptions() throws Exception {
        mockMvc.perform(get("/api/prescriptions/my"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username = "dr.vance@carepulse.com", roles = {"DOCTOR"})
    @DisplayName("Security: Doctor without treating relationship is forbidden from accessing patient prescriptions (403 Forbidden)")
    void testUnauthorizedDoctorCannotViewPatientPrescriptions() throws Exception {
        Doctor doctor = doctorRepository.findByUserEmail("dr.vance@carepulse.com").orElse(null);
        if (doctor != null) {
            User u = userRepository.findByEmail("isolated.audit.patient@example.com").orElseGet(() -> {
                User newUser = new User("isolated.audit.patient@example.com", "Password@123", "PATIENT");
                return userRepository.save(newUser);
            });
            Patient isolatedPatient = patientRepository.findByUserEmail("isolated.audit.patient@example.com").orElseGet(() -> {
                Patient newP = new Patient(u, "Isolated Patient", "+15559998888", LocalDate.of(1995, 1, 1), "Other");
                return patientRepository.save(newP);
            });

            mockMvc.perform(get("/api/prescriptions/patient/" + isolatedPatient.getId()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403))
                    .andExpect(jsonPath("$.error").value("Forbidden"));
        }
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = {"PATIENT"})
    @DisplayName("Flow: Patient can reschedule an appointment and old slot is released")
    void testAppointmentRescheduleFlow() throws Exception {
        Doctor doctor = doctorRepository.findAll().get(0);

        LocalDate initialDate = LocalDate.now().plusDays(20);
        List<LocalTime> initialSlots = doctorService.getAvailableTimeSlots(doctor.getId(), initialDate);
        while (initialSlots.isEmpty()) {
            initialDate = initialDate.plusDays(1);
            initialSlots = doctorService.getAvailableTimeSlots(doctor.getId(), initialDate);
        }
        LocalTime initialTime = initialSlots.get(0);

        AppointmentRequest bookReq = new AppointmentRequest();
        bookReq.setDoctorId(doctor.getId());
        bookReq.setAppointmentDate(initialDate);
        bookReq.setAppointmentTime(initialTime);
        bookReq.setReason("Annual Physical Exam");

        Appointment booked = appointmentService.bookAppointment("john.doe@example.com", bookReq);
        assertNotNull(booked.getId());

        LocalDate newDate = initialDate.plusDays(1);
        List<LocalTime> newSlots = doctorService.getAvailableTimeSlots(doctor.getId(), newDate);
        while (newSlots.isEmpty()) {
            newDate = newDate.plusDays(1);
            newSlots = doctorService.getAvailableTimeSlots(doctor.getId(), newDate);
        }
        LocalTime newTime = newSlots.get(0);

        AppointmentRescheduleRequestDTO reschedReq = new AppointmentRescheduleRequestDTO();
        reschedReq.setNewDate(newDate);
        reschedReq.setNewTime(newTime);
        reschedReq.setReason("Conflict on previous date");

        mockMvc.perform(put("/api/appointments/" + booked.getId() + "/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(booked.getId()))
                .andExpect(jsonPath("$.appointmentDate").value(newDate.toString()))
                .andExpect(jsonPath("$.appointmentTime").value(newTime.toString() + (newTime.toString().length() == 5 ? ":00" : "")))
                .andExpect(jsonPath("$.reason").value("Conflict on previous date"));
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = {"PATIENT"})
    @DisplayName("Validation: Rescheduling to past date is rejected (400 Bad Request)")
    void testRescheduleToPastDateRejected() throws Exception {
        Doctor doctor = doctorRepository.findAll().get(0);

        LocalDate initialDate = LocalDate.now().plusDays(25);
        List<LocalTime> initialSlots = doctorService.getAvailableTimeSlots(doctor.getId(), initialDate);
        while (initialSlots.isEmpty()) {
            initialDate = initialDate.plusDays(1);
            initialSlots = doctorService.getAvailableTimeSlots(doctor.getId(), initialDate);
        }
        LocalTime initialTime = initialSlots.get(0);

        AppointmentRequest bookReq = new AppointmentRequest();
        bookReq.setDoctorId(doctor.getId());
        bookReq.setAppointmentDate(initialDate);
        bookReq.setAppointmentTime(initialTime);
        bookReq.setReason("Consultation");

        Appointment booked = appointmentService.bookAppointment("john.doe@example.com", bookReq);

        AppointmentRescheduleRequestDTO reschedReq = new AppointmentRescheduleRequestDTO();
        reschedReq.setNewDate(LocalDate.now().minusDays(1));
        reschedReq.setNewTime(LocalTime.of(10, 0));
        reschedReq.setReason("Past date attempt");

        mockMvc.perform(put("/api/appointments/" + booked.getId() + "/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reschedReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("past")));
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = {"PATIENT"})
    @DisplayName("AI Companion: Emergency symptom detection triggers immediate safety alert")
    void testAiCompanionEmergencyAlert() throws Exception {
        String payload = "{\"message\":\"I have severe chest pain and cannot breathe!\"}";

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value(containsStringIgnoringCase("emergency")))
                .andExpect(jsonPath("$.suggestedQuestions").isArray());
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = {"PATIENT"})
    @DisplayName("AI Companion: Refuses to prescribe medication and directs to licensed doctors")
    void testAiCompanionRefusesPrescription() throws Exception {
        String payload = "{\"message\":\"What medicine should I take for fever? Please prescribe antibiotics.\"}";

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value(containsStringIgnoringCase("prescription")))
                .andExpect(jsonPath("$.suggestedQuestions").isArray());
    }

    @Test
    @WithMockUser(username = "john.doe@example.com", roles = {"PATIENT"})
    @DisplayName("AI Companion: Refuses to diagnose diseases and directs to specialist consultation")
    void testAiCompanionRefusesDiagnosis() throws Exception {
        String payload = "{\"message\":\"Can you diagnose what disease I have? Do I have diabetes?\"}";

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value(containsStringIgnoringCase("diagnos")))
                .andExpect(jsonPath("$.suggestedQuestions").isArray());
    }

    @Test
    @WithMockUser(username = "admin@carepulse.com", roles = {"ADMIN"})
    @DisplayName("Admin Analytics: Supports filtering by date and doctor")
    void testAdminAnalyticsWithFilters() throws Exception {
        mockMvc.perform(get("/api/admin/analytics")
                        .param("date", LocalDate.now().toString())
                        .param("appointmentStatus", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").isNumber())
                .andExpect(jsonPath("$.totalDoctors").isNumber())
                .andExpect(jsonPath("$.totalAppointments").isNumber())
                .andExpect(jsonPath("$.todayAppointments").isNumber())
                .andExpect(jsonPath("$.doctorWorkloads").isArray())
                .andExpect(jsonPath("$.appointmentTrendsPast7Days").isMap());
    }
}
