package com.carepulse.service;

import com.carepulse.dto.*;
import com.carepulse.entity.*;
import com.carepulse.exception.BadRequestException;
import com.carepulse.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class Phase5SmartCoordinationTest {

    @Autowired
    private SmartDoctorMatchingService matchingService;

    @Autowired
    private AppointmentWaitTimeService waitTimeService;

    @Autowired
    private AppointmentWaitlistService waitlistService;

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private CaregiverService caregiverService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentWaitlistRepository waitlistRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    private Doctor doctor1;
    private Patient patient1;
    private Patient patient2;

    @BeforeEach
    void setUp() {
        // Use existing seeded doctors and patients
        doctor1 = doctorRepository.findAll().stream()
                .filter(d -> d.getUser() != null && "dr.arun@carepulse.com".equalsIgnoreCase(d.getUser().getEmail()))
                .findFirst()
                .orElseGet(() -> doctorRepository.findAll().get(0));

        patient1 = patientRepository.findAll().stream()
                .filter(p -> p.getUser() != null && "john.doe@example.com".equalsIgnoreCase(p.getUser().getEmail()))
                .findFirst()
                .orElseGet(() -> patientRepository.findAll().get(0));

        patient2 = patientRepository.findAll().stream()
                .filter(p -> p.getUser() != null && "emma.watson@example.com".equalsIgnoreCase(p.getUser().getEmail()))
                .findFirst()
                .orElseGet(() -> patientRepository.findAll().get(1));
    }

    @Test
    void testSmartDoctorMatchingProvidesExplainableRecommendations() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<DoctorMatchResponseDTO> matches = matchingService.matchDoctors(doctor1.getSpecialization(), tomorrow, null);

        assertNotNull(matches);
        assertFalse(matches.isEmpty(), "Should find matching doctors for specialization");

        DoctorMatchResponseDTO topMatch = matches.get(0);
        assertTrue(topMatch.getMatchScore() > 0, "Top match score should be positive");
        assertNotNull(topMatch.getMatchReasons(), "Should provide explainable match reasons");
        assertFalse(topMatch.getMatchReasons().isEmpty(), "Match reasons should not be empty");
        assertNotNull(topMatch.getDisclaimer(), "Should include mandatory non-diagnostic disclaimer");
        assertTrue(topMatch.getDisclaimer().contains("does not provide medical triage or clinical diagnosis"));
    }

    @Test
    void testDynamicSlotGenerationAndCancelledSlotReuse() {
        LocalDate date = LocalDate.now().plusDays(3);
        List<LocalTime> initialSlots = doctorService.getAvailableTimeSlots(doctor1.getId(), date);
        assertFalse(initialSlots.isEmpty(), "Doctor should have available slots on regular workday");

        LocalTime bookedTime = initialSlots.get(0);

        // Book slot
        AppointmentRequest req = new AppointmentRequest(doctor1.getId(), date, bookedTime, "General checkup");
        Appointment booked = appointmentService.bookAppointment(patient1.getUser().getEmail(), req);
        assertNotNull(booked);

        // Verify slot is no longer available
        List<LocalTime> slotsAfterBooking = doctorService.getAvailableTimeSlots(doctor1.getId(), date);
        assertFalse(slotsAfterBooking.contains(bookedTime), "Booked slot must no longer appear in available slots");

        // Cancel appointment
        AppointmentStatusUpdateRequest cancelReq = new AppointmentStatusUpdateRequest();
        cancelReq.setStatus("CANCELLED");
        cancelReq.setCancellationReason("Schedule conflict");
        appointmentService.updateAppointmentStatus(booked.getId(), patient1.getUser().getEmail(), "PATIENT", cancelReq);

        // Section 3: Cancelled slot is immediately reused & available again
        List<LocalTime> slotsAfterCancel = doctorService.getAvailableTimeSlots(doctor1.getId(), date);
        assertTrue(slotsAfterCancel.contains(bookedTime), "Cancelled slot must be reusable and available again");
    }

    @Test
    void testWaitlistLifecycleAndNotificationOnCancellation() {
        LocalDate date = LocalDate.now().plusDays(4);
        LocalTime slotTime = LocalTime.of(10, 0);

        // Patient 1 books the slot
        AppointmentRequest req = new AppointmentRequest(doctor1.getId(), date, slotTime, "Initial Consultation");
        Appointment appt = appointmentService.bookAppointment(patient1.getUser().getEmail(), req);

        // Patient 2 joins the waitlist for that doctor and date
        JoinWaitlistRequestDTO waitlistReq = new JoinWaitlistRequestDTO(
                doctor1.getId(), doctor1.getSpecialization(), date, slotTime, "Prefer 10 AM slot"
        );
        AppointmentWaitlistResponseDTO waitlistEntry = waitlistService.joinWaitlist(patient2.getUser().getEmail(), waitlistReq);
        assertNotNull(waitlistEntry);
        assertEquals("WAITING", waitlistEntry.getStatus());

        // Prevent duplicate active waitlist entry
        assertThrows(BadRequestException.class, () -> {
            waitlistService.joinWaitlist(patient2.getUser().getEmail(), waitlistReq);
        });

        // Patient 1 cancels the appointment
        AppointmentStatusUpdateRequest cancelReq = new AppointmentStatusUpdateRequest();
        cancelReq.setStatus("CANCELLED");
        cancelReq.setCancellationReason("Emergency travel");
        appointmentService.updateAppointmentStatus(appt.getId(), patient1.getUser().getEmail(), "PATIENT", cancelReq);

        // Section 4: Waitlisted patient is automatically updated to NOTIFIED
        AppointmentWaitlist updatedWaitlist = waitlistRepository.findById(waitlistEntry.getId()).orElseThrow();
        assertEquals("NOTIFIED", updatedWaitlist.getStatus(), "Waitlisted patient must be notified when slot opens up");
    }

    @Test
    void testAppointmentWaitTimeEstimation() {
        LocalDate date = LocalDate.now().plusDays(5);
        LocalTime slot1 = LocalTime.of(9, 0);
        LocalTime slot2 = LocalTime.of(9, 30);

        // Book first appointment
        appointmentService.bookAppointment(patient1.getUser().getEmail(),
                new AppointmentRequest(doctor1.getId(), date, slot1, "Patient 1 Visit"));

        // Book second appointment
        Appointment appt2 = appointmentService.bookAppointment(patient2.getUser().getEmail(),
                new AppointmentRequest(doctor1.getId(), date, slot2, "Patient 2 Visit"));

        // Calculate wait time for second appointment
        AppointmentWaitTimeResponseDTO waitTime = waitTimeService.calculateWaitTime(appt2.getId());
        assertNotNull(waitTime);
        assertEquals(1, waitTime.getPatientsAheadCount(), "Should have exactly 1 patient ahead");
        assertTrue(waitTime.getEstimatedWaitMinutes() >= 30, "Estimated wait should be at least average slot duration (30m)");
        assertNotNull(waitTime.getExplanation(), "Should contain explainable breakdown");
        assertTrue(waitTime.getExplanation().contains("1 patient(s) scheduled ahead"));
    }

    @Test
    void testDoctorWorkloadBalancingCalculation() {
        List<DoctorWorkloadDTO> workloads = doctorService.getDoctorWorkloadsToday();
        assertNotNull(workloads);
        assertFalse(workloads.isEmpty(), "Should return workloads for all doctors");

        DoctorWorkloadDTO docWorkload = doctorService.getDoctorWorkload(doctor1.getId());
        assertNotNull(docWorkload);
        assertEquals(doctor1.getId(), docWorkload.getDoctorId());
        assertEquals(docWorkload.getNormalAppointmentsToday() + docWorkload.getEmergencyRequestsToday(),
                docWorkload.getTotalWorkloadToday(), "Total workload must equal normal appointments + emergency requests");
    }

    @Test
    void testAiDraftSummaryAndDoctorReviewVerification() {
        // Create medical record
        MedicalRecord record = new MedicalRecord(
                patient1, doctor1, LocalDate.now(), "Hypertension Stage 1",
                "Headaches, mild dizziness", "Amlodipine 5mg once daily", "Patient to monitor BP daily"
        );
        MedicalRecord saved = medicalRecordRepository.save(record);

        // 1. Doctor requests AI draft summary
        MedicalRecord withDraft = medicalRecordService.generateAiDraftSummary(saved.getId(), doctor1.getUser().getEmail());
        assertNotNull(withDraft.getAiDraftSummary(), "AI draft summary should be generated");
        assertEquals("PENDING_REVIEW", withDraft.getSummaryStatus(), "Status should be PENDING_REVIEW");
        assertNull(withDraft.getClinicalSummary(), "Clinical summary should remain unfinalized before review");

        // 2. Doctor reviews and approves with edits
        String finalizedSummary = "Verified Clinical Summary: Stage 1 essential hypertension controlled. Amlodipine 5mg prescribed.";
        ReviewSummaryRequestDTO reviewReq = new ReviewSummaryRequestDTO("APPROVE", finalizedSummary);
        MedicalRecord finalized = medicalRecordService.reviewAiSummary(saved.getId(), doctor1.getUser().getEmail(), reviewReq);

        assertEquals("APPROVED", finalized.getSummaryStatus());
        assertEquals(finalizedSummary, finalized.getClinicalSummary());
    }

    @Test
    void testWaitlistExpirationScheduledTask() {
        // Create an expired waitlist entry for yesterday
        AppointmentWaitlist pastWaitlist = new AppointmentWaitlist(
                patient1, doctor1, doctor1.getSpecialization(), LocalDate.now().minusDays(2), LocalTime.of(10, 0), "Past entry"
        );
        pastWaitlist = waitlistRepository.save(pastWaitlist);

        int expiredCount = waitlistService.expirePastWaitlists();
        assertTrue(expiredCount >= 1, "Should expire past waitlist entries");

        AppointmentWaitlist refreshed = waitlistRepository.findById(pastWaitlist.getId()).orElseThrow();
        assertEquals("EXPIRED", refreshed.getStatus());
    }
}
