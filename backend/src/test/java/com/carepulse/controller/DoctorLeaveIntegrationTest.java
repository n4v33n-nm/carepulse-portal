package com.carepulse.controller;

import com.carepulse.dto.DoctorLeaveRequest;
import com.carepulse.dto.DoctorLeaveReviewRequest;
import com.carepulse.dto.PatientConsentRequest;
import com.carepulse.dto.ProposeReassignmentRequest;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DoctorLeaveIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorLeaveRepository doctorLeaveRepository;

    @Autowired
    private AppointmentReassignmentRepository reassignmentRepository;

    @Autowired
    private DoctorAvailabilityRepository doctorAvailabilityRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String doctorToken;
    private String replacementDoctorToken;
    private String adminToken;
    private String patientToken;
    private String otherPatientToken;

    private Doctor primaryDoctor;
    private Doctor replacementDoctor;
    private Patient testPatient;
    private Patient otherPatient;
    private Appointment testAppointment;

    @BeforeEach
    void setUp() {
        // Doctor 1 (Primary - Cardiology)
        User docUser1 = userRepository.findByEmail("leave.doc1@carepulse.com").orElseGet(() ->
                userRepository.save(new User("leave.doc1@carepulse.com", passwordEncoder.encode("Pass123"), "DOCTOR")));
        primaryDoctor = doctorRepository.findByUserEmail(docUser1.getEmail()).orElseGet(() -> {
            Doctor d = new Doctor();
            d.setUser(docUser1);
            d.setFullName("Dr. Gregory House");
            d.setSpecialization("Cardiology");
            d.setExperienceYears(15);
            d.setConsultationFee(150.0);
            d.setAvailabilityStatus("AVAILABLE");
            return doctorRepository.save(d);
        });

        // Doctor 2 (Replacement Candidate - Cardiology)
        User docUser2 = userRepository.findByEmail("leave.doc2@carepulse.com").orElseGet(() ->
                userRepository.save(new User("leave.doc2@carepulse.com", passwordEncoder.encode("Pass123"), "DOCTOR")));
        replacementDoctor = doctorRepository.findByUserEmail(docUser2.getEmail()).orElseGet(() -> {
            Doctor d = new Doctor();
            d.setUser(docUser2);
            d.setFullName("Dr. Allison Cameron");
            d.setSpecialization("Cardiology");
            d.setExperienceYears(8);
            d.setConsultationFee(120.0);
            d.setAvailabilityStatus("AVAILABLE");
            return doctorRepository.save(d);
        });

        // Ensure Doctor 2 has availability on the appointment day
        LocalDate targetDate = LocalDate.now().plusDays(5);
        String dayOfWeek = targetDate.getDayOfWeek().name();
        if (doctorAvailabilityRepository.findByDoctorIdAndDayOfWeek(replacementDoctor.getId(), dayOfWeek).isEmpty()) {
            DoctorAvailability da = new DoctorAvailability(
                    replacementDoctor, dayOfWeek, LocalTime.of(9, 0), LocalTime.of(17, 0),
                    LocalTime.of(13, 0), LocalTime.of(14, 0), 30
            );
            doctorAvailabilityRepository.save(da);
        }

        // Admin User
        User adminUser = userRepository.findByEmail("leave.admin@carepulse.com").orElseGet(() ->
                userRepository.save(new User("leave.admin@carepulse.com", passwordEncoder.encode("Pass123"), "ADMIN")));

        // Patient 1
        User patUser1 = userRepository.findByEmail("leave.patient1@carepulse.com").orElseGet(() ->
                userRepository.save(new User("leave.patient1@carepulse.com", passwordEncoder.encode("Pass123"), "PATIENT")));
        testPatient = patientRepository.findByUserEmail(patUser1.getEmail()).orElseGet(() -> {
            Patient p = new Patient();
            p.setUser(patUser1);
            p.setFullName("James Wilson");
            p.setPhone("+15551112222");
            p.setDateOfBirth(LocalDate.of(1985, 4, 10));
            return patientRepository.save(p);
        });

        // Patient 2 (Unauthorized)
        User patUser2 = userRepository.findByEmail("leave.patient2@carepulse.com").orElseGet(() ->
                userRepository.save(new User("leave.patient2@carepulse.com", passwordEncoder.encode("Pass123"), "PATIENT")));
        otherPatient = patientRepository.findByUserEmail(patUser2.getEmail()).orElseGet(() -> {
            Patient p = new Patient();
            p.setUser(patUser2);
            p.setFullName("Lisa Cuddy");
            p.setPhone("+15553334444");
            p.setDateOfBirth(LocalDate.of(1980, 8, 20));
            return patientRepository.save(p);
        });

        // Create an appointment for testPatient with primaryDoctor on targetDate
        testAppointment = new Appointment(
                testPatient, primaryDoctor, targetDate, LocalTime.of(10, 0), "Routine Cardiology Follow-up");
        testAppointment.setStatus("CONFIRMED");
        testAppointment = appointmentRepository.save(testAppointment);

        // Generate Tokens
        doctorToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(docUser1.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))),
                "DOCTOR", docUser1.getId());

        replacementDoctorToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(docUser2.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))),
                "DOCTOR", docUser2.getId());

        adminToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(adminUser.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))),
                "ADMIN", adminUser.getId());

        patientToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(patUser1.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))),
                "PATIENT", patUser1.getId());

        otherPatientToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(patUser2.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))),
                "PATIENT", patUser2.getId());
    }

    @Test
    @DisplayName("Doctor can submit a leave request and view it in their history")
    void testDoctorSubmitLeaveRequest() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(4);
        LocalDate endDate = LocalDate.now().plusDays(6);

        DoctorLeaveRequest req = new DoctorLeaveRequest(startDate, endDate, "Attending medical symposium");

        MvcResult result = mockMvc.perform(post("/api/doctor-leave/requests")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.doctorName").value("Dr. Gregory House"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.affectedAppointmentsCount").isNumber())
                .andReturn();

        // Verify doctor can query their leaves
        mockMvc.perform(get("/api/doctor-leave/my")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].reason").value("Attending medical symposium"));
    }

    @Test
    @DisplayName("Submitting leave with invalid dates is rejected with 400 Bad Request")
    void testInvalidLeaveDateValidation() throws Exception {
        // End date before start date
        DoctorLeaveRequest invalidDates = new DoctorLeaveRequest(
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(5), "Invalid range");

        mockMvc.perform(post("/api/doctor-leave/requests")
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDates)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Full workflow: Admin approves leave, discovers candidates, reassigns, and patient accepts")
    void testFullLeaveAndReassignmentWorkflow() throws Exception {
        LocalDate startDate = LocalDate.now().plusDays(4);
        LocalDate endDate = LocalDate.now().plusDays(6);

        // 1. Doctor submits leave covering appointment date
        DoctorLeave leave = new DoctorLeave(primaryDoctor, startDate, endDate, "Medical Conference");
        leave = doctorLeaveRepository.save(leave);

        // 2. Admin reviews and APPROVES leave
        DoctorLeaveReviewRequest reviewReq = new DoctorLeaveReviewRequest("APPROVED", "Approved for continuing medical education");

        mockMvc.perform(put("/api/doctor-leave/" + leave.getId() + "/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.adminNotes").value("Approved for continuing medical education"));

        // Verify leave record updated in DB
        DoctorLeave updatedLeave = doctorLeaveRepository.findById(leave.getId()).orElseThrow();
        assertThat(updatedLeave.getStatus()).isEqualTo("APPROVED");

        // 3. Admin views affected appointments
        mockMvc.perform(get("/api/doctor-leave/" + leave.getId() + "/affected-appointments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].appointmentId").value(testAppointment.getId()))
                .andExpect(jsonPath("$[0].patientName").value("James Wilson"));

        // 4. Admin finds replacement candidates
        mockMvc.perform(get("/api/doctor-leave/appointments/" + testAppointment.getId() + "/candidates")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.doctorId == " + replacementDoctor.getId() + ")].specialization").value("Cardiology"))
                .andExpect(jsonPath("$[?(@.doctorId == " + replacementDoctor.getId() + ")].recommended").value(true));

        // 5. Admin proposes replacement doctor
        ProposeReassignmentRequest proposeReq = new ProposeReassignmentRequest(
                replacementDoctor.getId(), "Cardiology specialist available at the same time");

        MvcResult reassignResult = mockMvc.perform(post("/api/doctor-leave/appointments/" + testAppointment.getId() + "/reassign")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(proposeReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientConsentStatus").value("PROPOSED"))
                .andExpect(jsonPath("$.replacementDoctorName").value("Dr. Allison Cameron"))
                .andReturn();

        String responseJson = reassignResult.getResponse().getContentAsString();
        Map<String, Object> map = objectMapper.readValue(responseJson, Map.class);
        Long reassignmentId = ((Number) map.get("id")).longValue();

        // Verify patient received notification
        boolean notificationExists = notificationRepository.findAll().stream()
                .anyMatch(n -> n.getUser().getId().equals(testPatient.getUser().getId()) &&
                        n.getType().equals("REASSIGNMENT_PROPOSED"));
        assertThat(notificationExists).isTrue();

        // 6. Unauthorized patient cannot consent
        PatientConsentRequest consentReq = new PatientConsentRequest("ACCEPT", "Accepting replacement doctor");
        mockMvc.perform(post("/api/doctor-leave/reassignments/" + reassignmentId + "/consent")
                        .header("Authorization", "Bearer " + otherPatientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consentReq)))
                .andExpect(status().isForbidden());

        // 7. Authorized patient ACCEPTS replacement
        mockMvc.perform(post("/api/doctor-leave/reassignments/" + reassignmentId + "/consent")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consentReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientConsentStatus").value("ACCEPTED"));

        // 8. Verify appointment now points to replacement doctor and is CONFIRMED
        Appointment reloadedAppt = appointmentRepository.findById(testAppointment.getId()).orElseThrow();
        assertThat(reloadedAppt.getDoctor().getId()).isEqualTo(replacementDoctor.getId());
        assertThat(reloadedAppt.getStatus()).isEqualTo("CONFIRMED");

        // 9. Verify reassignment history visible to replacement doctor
        mockMvc.perform(get("/api/doctor-leave/reassignments/doctor-history")
                        .header("Authorization", "Bearer " + replacementDoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].replacementDoctorName").value("Dr. Allison Cameron"));
    }

    @Test
    @DisplayName("Patient rejects reassignment: status is REJECTED and original appointment is not modified")
    void testPatientRejectsReassignment() throws Exception {
        LocalDate targetDate = LocalDate.now().plusDays(5);

        // Propose reassignment
        AppointmentReassignment reassignment = new AppointmentReassignment(
                testAppointment, null, primaryDoctor, replacementDoctor,
                userRepository.findByEmail("leave.admin@carepulse.com").orElseThrow(), "Test notes");
        reassignment = reassignmentRepository.save(reassignment);

        PatientConsentRequest rejectReq = new PatientConsentRequest("REJECT", "I prefer to reschedule when Dr. House returns");

        mockMvc.perform(post("/api/doctor-leave/reassignments/" + reassignment.getId() + "/consent")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rejectReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientConsentStatus").value("REJECTED"))
                .andExpect(jsonPath("$.patientNotes").value("I prefer to reschedule when Dr. House returns"));

        // Verify appointment was NOT modified to replacement doctor
        Appointment reloadedAppt = appointmentRepository.findById(testAppointment.getId()).orElseThrow();
        assertThat(reloadedAppt.getDoctor().getId()).isEqualTo(primaryDoctor.getId());
    }
}
