package com.carepulse.service;

import com.carepulse.entity.Doctor;
import com.carepulse.entity.EmergencyDoctorRoster;
import com.carepulse.entity.EmergencyRequest;
import com.carepulse.entity.Patient;
import com.carepulse.entity.User;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.EmergencyDoctorRosterRepository;
import com.carepulse.repository.EmergencyRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyDoctorAllocationServiceTest {

    @Mock
    private EmergencyDoctorRosterRepository rosterRepository;

    @Mock
    private EmergencyRequestRepository requestRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private EmergencyDoctorAllocationService allocationService;

    private Patient patient;
    private Doctor docCardio;
    private Doctor docGeneral;
    private Doctor docDerma;

    private LocalDate today;
    private LocalTime morningTime;

    @BeforeEach
    void setUp() {
        today = LocalDate.of(2026, 9, 27);
        morningTime = LocalTime.of(10, 0);

        User patientUser = new User("patient@carepulse.com", "pass", "PATIENT");
        patientUser.setId(10L);
        patient = new Patient(patientUser, "John Doe", "+1234567890", LocalDate.of(1990, 1, 1), "Male");
        patient.setId(10L);

        User doc1User = new User("cardio@carepulse.com", "pass", "DOCTOR");
        doc1User.setId(1L);
        docCardio = new Doctor(doc1User, "Dr. Sarah Jenkins", "+1111111111", "Cardiology", "MD", 12);
        docCardio.setId(1L);
        docCardio.setAvailabilityStatus("AVAILABLE");

        User doc2User = new User("general@carepulse.com", "pass", "DOCTOR");
        doc2User.setId(2L);
        docGeneral = new Doctor(doc2User, "Dr. Priya Sharma", "+2222222222", "General Medicine", "MBBS", 10);
        docGeneral.setId(2L);
        docGeneral.setAvailabilityStatus("AVAILABLE");

        User doc3User = new User("derma@carepulse.com", "pass", "DOCTOR");
        doc3User.setId(3L);
        docDerma = new Doctor(doc3User, "Dr. Marcus Vance", "+3333333333", "Dermatology", "MD", 8);
        docDerma.setId(3L);
        docDerma.setAvailabilityStatus("AVAILABLE");
    }

    @Test
    @DisplayName("3. Available emergency-duty doctor gets assigned successfully")
    void testAvailableEmergencyDoctorGetsAssigned() {
        EmergencyDoctorRoster roster = new EmergencyDoctorRoster(
                docGeneral, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        roster.setId(100L);

        when(rosterRepository.findAvailableEmergencyRosters(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(roster));
        when(rosterRepository.findByIdWithLock(100L)).thenReturn(Optional.of(roster));
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyRequest request = new EmergencyRequest(patient, "General", "Chest tightness");

        EmergencyRequest result = allocationService.allocateDoctor(request, today, morningTime);

        assertNotNull(result.getAssignedDoctor());
        assertEquals("Dr. Priya Sharma", result.getAssignedDoctor().getFullName());
        assertEquals("ASSIGNED", result.getStatus());
        assertEquals("IN_CONSULTATION", roster.getDoctorAvailabilityStatus());
        assertEquals("IN_CONSULTATION", docGeneral.getAvailabilityStatus());
    }

    @Test
    @DisplayName("4. Non-emergency-duty doctor is not selected")
    void testNonEmergencyDutyDoctorNotSelected() {
        // Doctor is on roster but duty_status is NOT_ASSIGNED
        EmergencyDoctorRoster roster = new EmergencyDoctorRoster(
                docGeneral, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "NOT_ASSIGNED", "AVAILABLE"
        );
        roster.setId(101L);

        when(rosterRepository.findAvailableEmergencyRosters(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(roster));
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyRequest request = new EmergencyRequest(patient, "General", "Routine emergency");

        EmergencyRequest result = allocationService.allocateDoctor(request, today, morningTime);

        assertEquals("NO_DOCTOR_AVAILABLE", result.getStatus());
        assertNull(result.getAssignedDoctor());
    }

    @Test
    @DisplayName("5. Busy doctor is not selected")
    void testBusyDoctorNotSelected() {
        // Doctor status is BUSY
        EmergencyDoctorRoster roster = new EmergencyDoctorRoster(
                docGeneral, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "BUSY"
        );
        roster.setId(102L);

        when(rosterRepository.findAvailableEmergencyRosters(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(roster));
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyRequest request = new EmergencyRequest(patient, "General", "Fever");

        EmergencyRequest result = allocationService.allocateDoctor(request, today, morningTime);

        assertEquals("NO_DOCTOR_AVAILABLE", result.getStatus());
        assertNull(result.getAssignedDoctor());
    }

    @Test
    @DisplayName("6. Off-duty / On-leave doctor is not selected")
    void testOffDutyDoctorNotSelected() {
        docGeneral.setAvailabilityStatus("OFF_DUTY");
        EmergencyDoctorRoster roster = new EmergencyDoctorRoster(
                docGeneral, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "OFF_DUTY"
        );
        roster.setId(103L);

        when(rosterRepository.findAvailableEmergencyRosters(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(roster));
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyRequest request = new EmergencyRequest(patient, "General", "Fever");

        EmergencyRequest result = allocationService.allocateDoctor(request, today, morningTime);

        assertEquals("NO_DOCTOR_AVAILABLE", result.getStatus());
        assertNull(result.getAssignedDoctor());
    }

    @Test
    @DisplayName("7. No available doctor returns NO_DOCTOR_AVAILABLE state")
    void testNoAvailableDoctorReturnsNoDoctorAvailable() {
        when(rosterRepository.findAvailableEmergencyRosters(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyRequest request = new EmergencyRequest(patient, "General", "Severe abdominal pain");

        EmergencyRequest result = allocationService.allocateDoctor(request, today, morningTime);

        assertEquals("NO_DOCTOR_AVAILABLE", result.getStatus());
        assertNull(result.getAssignedDoctor());
    }

    @Test
    @DisplayName("8. Correct specialization is preferred when available")
    void testSpecializationMatchingPreference() {
        // Both Dr. Jenkins (Cardiology) and Dr. Sharma (General) are available on morning duty
        EmergencyDoctorRoster rCardio = new EmergencyDoctorRoster(
                docCardio, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        rCardio.setId(101L);

        EmergencyDoctorRoster rGeneral = new EmergencyDoctorRoster(
                docGeneral, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        rGeneral.setId(102L);

        when(rosterRepository.findAvailableEmergencyRosters(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(rGeneral, rCardio)); // even if general is listed first
        when(rosterRepository.findByIdWithLock(101L)).thenReturn(Optional.of(rCardio));
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Patient requests Cardiology emergency
        EmergencyRequest request = new EmergencyRequest(patient, "Cardiology", "Sudden acute chest pressure");

        EmergencyRequest result = allocationService.allocateDoctor(request, today, morningTime);

        assertNotNull(result.getAssignedDoctor());
        assertEquals("Dr. Sarah Jenkins", result.getAssignedDoctor().getFullName());
        assertEquals("Cardiology", result.getAssignedDoctor().getSpecialization());
    }

    @Test
    @DisplayName("Specialization fallback: Assigns best available doctor if requested specialization is not on duty")
    void testSpecializationFallbackToAvailableDoctor() {
        // Only General Medicine doctor is available; patient requests Neurology
        EmergencyDoctorRoster rGeneral = new EmergencyDoctorRoster(
                docGeneral, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        rGeneral.setId(102L);

        when(rosterRepository.findAvailableEmergencyRosters(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(rGeneral));
        when(rosterRepository.findByIdWithLock(102L)).thenReturn(Optional.of(rGeneral));
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyRequest request = new EmergencyRequest(patient, "Neurology", "Sudden dizziness");

        EmergencyRequest result = allocationService.allocateDoctor(request, today, morningTime);

        assertNotNull(result.getAssignedDoctor());
        assertEquals("Dr. Priya Sharma", result.getAssignedDoctor().getFullName());
        assertEquals("ASSIGNED", result.getStatus());
    }

    @Test
    @DisplayName("Workload balancing: Selects available doctor with lowest emergency caseload")
    void testLowestWorkloadPreference() {
        EmergencyDoctorRoster r1 = new EmergencyDoctorRoster(
                docCardio, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        r1.setId(101L);

        EmergencyDoctorRoster r2 = new EmergencyDoctorRoster(
                docGeneral, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        r2.setId(102L);

        when(rosterRepository.findAvailableEmergencyRosters(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(r1, r2));
        // Doctor Cardio has 3 active emergency cases; Doctor General has 1 active case
        when(requestRepository.countByAssignedDoctorAndStatusIn(eq(docCardio), anyList())).thenReturn(3L);
        when(requestRepository.countByAssignedDoctorAndStatusIn(eq(docGeneral), anyList())).thenReturn(1L);

        when(rosterRepository.findByIdWithLock(102L)).thenReturn(Optional.of(r2));
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyRequest request = new EmergencyRequest(patient, "Other", "Urgent assistance");

        EmergencyRequest result = allocationService.allocateDoctor(request, today, morningTime);

        assertNotNull(result.getAssignedDoctor());
        assertEquals("Dr. Priya Sharma", result.getAssignedDoctor().getFullName());
    }

    @Test
    @DisplayName("Cross-midnight Night Shift active window validation")
    void testCrossMidnightNightShiftActive() {
        // Night shift 20:00 to 08:00
        EmergencyDoctorRoster rNight = new EmergencyDoctorRoster(
                docGeneral, today, "NIGHT", LocalTime.of(20, 0), LocalTime.of(8, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        rNight.setId(105L);

        assertTrue(rNight.isActiveAt(LocalTime.of(22, 0))); // 10 PM
        assertTrue(rNight.isActiveAt(LocalTime.of(2, 30)));  // 2:30 AM
        assertFalse(rNight.isActiveAt(LocalTime.of(12, 0))); // 12 PM (noon)
    }

    @Test
    @DisplayName("14. Concurrency Protection: Re-checks availability under lock if claimed concurrently")
    void testConcurrentDoubleAssignmentProtection() {
        EmergencyDoctorRoster r1 = new EmergencyDoctorRoster(
                docCardio, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        r1.setId(101L);

        EmergencyDoctorRoster r2 = new EmergencyDoctorRoster(
                docGeneral, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        r2.setId(102L);

        when(rosterRepository.findAvailableEmergencyRosters(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(r1, r2));

        // Simulate that when r1 lock is acquired, another thread already updated r1 to IN_CONSULTATION!
        EmergencyDoctorRoster claimedR1 = new EmergencyDoctorRoster(
                docCardio, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "IN_CONSULTATION"
        );
        claimedR1.setId(101L);
        when(rosterRepository.findByIdWithLock(101L)).thenReturn(Optional.of(claimedR1));

        // Then r2 lock is acquired and r2 is still AVAILABLE
        when(rosterRepository.findByIdWithLock(102L)).thenReturn(Optional.of(r2));
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmergencyRequest request = new EmergencyRequest(patient, "Cardiology", "Urgent dizziness");

        EmergencyRequest result = allocationService.allocateDoctor(request, today, morningTime);

        // Doctor 1 was preferred for Cardiology, but was claimed concurrently.
        // Doctor 2 (General) should be assigned as fallback!
        assertNotNull(result.getAssignedDoctor());
        assertEquals("Dr. Priya Sharma", result.getAssignedDoctor().getFullName());
        assertEquals("ASSIGNED", result.getStatus());
    }
}
