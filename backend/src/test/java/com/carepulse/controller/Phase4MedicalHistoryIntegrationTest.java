package com.carepulse.controller;

import com.carepulse.dto.AppointmentStatusUpdateRequest;
import com.carepulse.dto.MedicalRecordRequest;
import com.carepulse.dto.PrescriptionRequest;
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
public class Phase4MedicalHistoryIntegrationTest {

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
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

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
        patientUser = userRepository.save(new User("patient_p4_" + ts + "@carepulse.com", passwordEncoder.encode("secret"), "PATIENT"));
        patient = new Patient();
        patient.setUser(patientUser);
        patient.setFullName("Patient Phase Four");
        patient.setPhone("+15551234567");
        patient.setGender("Male");
        patient.setBloodGroup("A+");
        patient.setAddress("No 4 Health Ave");
        patient.setDateOfBirth(LocalDate.of(1990, 5, 15));
        patient = patientRepository.save(patient);

        patientToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(patientUser.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_PATIENT"))),
                "PATIENT", patientUser.getId());

        // 2. Authorized Doctor (has confirmed appointment with patient)
        authorizedDoctorUser = userRepository.save(new User("doc_auth_" + ts + "@carepulse.com", passwordEncoder.encode("secret"), "DOCTOR"));
        authorizedDoctor = new Doctor();
        authorizedDoctor.setUser(authorizedDoctorUser);
        authorizedDoctor.setFullName("Dr. Authorized Phase4");
        authorizedDoctor.setSpecialization("Cardiology");
        authorizedDoctor.setExperienceYears(12);
        authorizedDoctor.setConsultationFee(140.0);
        authorizedDoctor.setAvailabilityStatus("AVAILABLE");
        authorizedDoctor = doctorRepository.save(authorizedDoctor);

        authorizedDoctorToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(authorizedDoctorUser.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))),
                "DOCTOR", authorizedDoctorUser.getId());

        // 3. Unauthorized Doctor (no relationship with patient)
        unauthorizedDoctorUser = userRepository.save(new User("doc_unauth_" + ts + "@carepulse.com", passwordEncoder.encode("secret"), "DOCTOR"));
        unauthorizedDoctor = new Doctor();
        unauthorizedDoctor.setUser(unauthorizedDoctorUser);
        unauthorizedDoctor.setFullName("Dr. Unauth Phase4");
        unauthorizedDoctor.setSpecialization("Dermatology");
        unauthorizedDoctor.setExperienceYears(5);
        unauthorizedDoctor.setConsultationFee(100.0);
        unauthorizedDoctor.setAvailabilityStatus("AVAILABLE");
        unauthorizedDoctor = doctorRepository.save(unauthorizedDoctor);

        unauthorizedDoctorToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(unauthorizedDoctorUser.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DOCTOR"))),
                "DOCTOR", unauthorizedDoctorUser.getId());

        // 4. Admin
        adminUser = userRepository.save(new User("admin_p4_" + ts + "@carepulse.com", passwordEncoder.encode("secret"), "ADMIN"));
        adminToken = jwtTokenProvider.generateToken(
                new UsernamePasswordAuthenticationToken(adminUser.getEmail(), null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))),
                "ADMIN", adminUser.getId());

        // Create an active appointment establishing physician-patient relationship
        Appointment appt = new Appointment(patient, authorizedDoctor, LocalDate.now().plusDays(1), LocalTime.of(10, 0), "Cardiology follow-up");
        appt.setStatus("CONFIRMED");
        appointmentRepository.save(appt);
    }

    @Test
    @DisplayName("Patient can view their own medical records, prescriptions, and appointment history")
    public void testPatientCanViewOwnMedicalHistory() throws Exception {
        // Seed a medical record and prescription for this patient
        MedicalRecord rec = new MedicalRecord(patient, authorizedDoctor, LocalDate.now(), "Hypertension Stage 1", "Elevated BP, mild headache", "Lifestyle modification and ACE inhibitor", "Patient advised to monitor BP daily.");
        medicalRecordRepository.save(rec);

        Prescription pres = new Prescription(patient, authorizedDoctor, rec, "Lisinopril", "10mg", "Once daily", "30 days", "Take in the morning", LocalDate.now());
        prescriptionRepository.save(pres);

        // Patient fetches their records
        mockMvc.perform(get("/api/records/my")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosis").value("Hypertension Stage 1"))
                .andExpect(jsonPath("$[0].consultationNotes").value("Patient advised to monitor BP daily."));

        // Patient fetches their prescriptions
        mockMvc.perform(get("/api/prescriptions/my")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].medicineName").value("Lisinopril"))
                .andExpect(jsonPath("$[0].dosage").value("10mg"));

        // Patient fetches their appointments
        mockMvc.perform(get("/api/appointments/my")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").value("Cardiology follow-up"));
    }

    @Test
    @DisplayName("Authorized doctor can access patient medical history; unauthorized doctor is denied with 403")
    public void testDoctorAccessAuthorization() throws Exception {
        MedicalRecord rec = new MedicalRecord(patient, authorizedDoctor, LocalDate.now(), "Angina", "Chest discomfort on exertion", "Nitroglycerin PRN", "Schedule stress test.");
        medicalRecordRepository.save(rec);

        // 1. Authorized doctor can access
        mockMvc.perform(get("/api/records/patient/" + patient.getId())
                        .header("Authorization", "Bearer " + authorizedDoctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosis").value("Angina"));

        // 2. Unauthorized doctor is denied with 403 Forbidden
        mockMvc.perform(get("/api/records/patient/" + patient.getId())
                        .header("Authorization", "Bearer " + unauthorizedDoctorToken))
                .andExpect(status().isForbidden());

        // 3. Unauthorized doctor cannot access prescriptions either
        mockMvc.perform(get("/api/prescriptions/patient/" + patient.getId())
                        .header("Authorization", "Bearer " + unauthorizedDoctorToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Doctor completing appointment with diagnosis and prescription persists both in PostgreSQL")
    public void testDoctorCompleteConsultationWithClinicalDocumentation() throws Exception {
        Appointment appt = new Appointment(patient, authorizedDoctor, LocalDate.now(), LocalTime.of(14, 0), "Chest pain consultation");
        appt.setStatus("CONFIRMED");
        appointmentRepository.save(appt);

        AppointmentStatusUpdateRequest req = new AppointmentStatusUpdateRequest();
        req.setStatus("COMPLETED");
        req.setConsultationNotes("Patient examined. Heart sounds normal, S1/S2 audible. EKG sinus rhythm.");
        req.setDiagnosis("Atypical Chest Wall Pain");
        req.setSymptoms("Sharp pain on inspiration, localized tenderness");
        req.setTreatment("Rest, heat pack, and analgesics");
        req.setMedicineName("Ibuprofen");
        req.setDosage("400mg");
        req.setFrequency("Three times daily with food");
        req.setDuration("5 days");
        req.setInstructions("Do not take on empty stomach.");

        mockMvc.perform(put("/api/appointments/" + appt.getId() + "/status")
                        .header("Authorization", "Bearer " + authorizedDoctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.consultationNotes").value(req.getConsultationNotes()));

        // Verify MedicalRecord was persisted in DB
        var records = medicalRecordRepository.findByPatientOrderByRecordDateDesc(patient);
        assertThat(records).isNotEmpty();
        assertThat(records.get(0).getDiagnosis()).isEqualTo("Atypical Chest Wall Pain");
        assertThat(records.get(0).getConsultationNotes()).isEqualTo(req.getConsultationNotes());

        // Verify Prescription was persisted in DB
        var prescriptions = prescriptionRepository.findByPatientOrderByIssuedDateDesc(patient);
        assertThat(prescriptions).isNotEmpty();
        assertThat(prescriptions.get(0).getMedicineName()).isEqualTo("Ibuprofen");
        assertThat(prescriptions.get(0).getDosage()).isEqualTo("400mg");
    }

    @Test
    @DisplayName("Patients cannot author or modify medical records or prescriptions directly (role-based enforcement)")
    public void testPatientCannotCreateOrModifyRecords() throws Exception {
        MedicalRecordRequest recReq = new MedicalRecordRequest();
        recReq.setPatientId(patient.getId());
        recReq.setDiagnosis("Self-diagnosed Condition");
        recReq.setRecordDate(LocalDate.now());

        // Attempt by patient to post to /api/records should be 403 Forbidden
        mockMvc.perform(post("/api/records")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recReq)))
                .andExpect(status().isForbidden());

        PrescriptionRequest presReq = new PrescriptionRequest();
        presReq.setPatientId(patient.getId());
        presReq.setMedicineName("Restricted Medication");
        presReq.setDosage("50mg");
        presReq.setFrequency("Daily");
        presReq.setDuration("30 days");

        // Attempt by patient to post to /api/prescriptions should be 403 Forbidden
        mockMvc.perform(post("/api/prescriptions")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin has authorized audit access to patient medical records")
    public void testAdminAccessToPatientRecords() throws Exception {
        MedicalRecord rec = new MedicalRecord(patient, authorizedDoctor, LocalDate.now(), "Arrhythmia Evaluation", "Palpitations", "Holter monitor", "Refer to electrophysiologist.");
        medicalRecordRepository.save(rec);

        mockMvc.perform(get("/api/records/patient/" + patient.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].diagnosis").value("Arrhythmia Evaluation"));
    }
}
