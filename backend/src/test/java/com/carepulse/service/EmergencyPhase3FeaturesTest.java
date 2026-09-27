package com.carepulse.service;

import com.carepulse.dto.DoctorEmergencyWorkloadDTO;
import com.carepulse.dto.EmergencyAnalyticsDTO;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.EmergencyDoctorRoster;
import com.carepulse.entity.EmergencyRequest;
import com.carepulse.entity.Patient;
import com.carepulse.entity.User;
import com.carepulse.repository.AppointmentRepository;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.EmergencyDoctorRosterRepository;
import com.carepulse.repository.EmergencyRequestRepository;
import com.carepulse.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyPhase3FeaturesTest {

    @Mock
    private EmergencyRequestRepository requestRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private EmergencyDoctorRosterRepository rosterRepository;

    @Mock
    private EmergencyDoctorAllocationService allocationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EmergencyRequestService emergencyRequestService;

    private Patient patient;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emergencyRequestService, "assignmentTimeoutMinutes", 10);

        User uPatient = new User("patient@carepulse.com", "pass", "PATIENT");
        uPatient.setId(10L);
        patient = new Patient(uPatient, "Jane Doe", "+1234567890", LocalDate.of(1992, 3, 15), "Female");
        patient.setId(10L);

        User uDoctor = new User("doctor@carepulse.com", "pass", "DOCTOR");
        uDoctor.setId(1L);
        doctor = new Doctor(uDoctor, "Dr. Sarah Jenkins", "+1111111111", "Cardiology", "MD", 12);
        doctor.setId(1L);
        doctor.setAvailabilityStatus("AVAILABLE");
    }

    @Test
    @DisplayName("Timeout Recovery: Unstarted assignments older than timeout are returned to WAITING queue")
    void testAssignmentTimeoutRecovery() {
        EmergencyRequest timedOutReq = new EmergencyRequest(patient, "Cardiology", "Acute chest pain");
        timedOutReq.setId(101L);
        timedOutReq.setStatus("ASSIGNED");
        timedOutReq.setAssignedDoctor(doctor);
        timedOutReq.setAssignedTime(LocalDateTime.now().minusMinutes(15)); // 15 mins ago, timeout is 10 mins

        when(requestRepository.findByStatusAndAssignedTimeBefore(eq("ASSIGNED"), any(LocalDateTime.class)))
                .thenReturn(List.of(timedOutReq));
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(i -> i.getArgument(0));
        when(requestRepository.countByAssignedDoctorAndStatusIn(eq(doctor), anyList())).thenReturn(0L);

        int recoveredCount = emergencyRequestService.recoverTimedOutAssignments();

        assertEquals(1, recoveredCount);
        assertEquals("WAITING", timedOutReq.getStatus());
        assertNull(timedOutReq.getAssignedDoctor());
        assertNull(timedOutReq.getAssignedTime());

        verify(notificationService).createNotification(
                eq(doctor.getUser()),
                contains("Timeout"),
                anyString(),
                eq("EMERGENCY")
        );
        verify(notificationService).createNotification(
                eq(patient.getUser()),
                contains("Re-queued"),
                anyString(),
                eq("EMERGENCY")
        );
        verify(auditLogService).log(eq("SYSTEM"), eq("EMERGENCY_REQUEST_REQUEUED"), anyString(), anyString());
        verify(allocationService).allocateDoctor(eq(timedOutReq));
    }

    @Test
    @DisplayName("Admin Analytics: Computes accurate statistics and doctor workload across database records")
    void testGetEmergencyAnalytics() {
        LocalDate today = LocalDate.now();

        when(requestRepository.countByRequestTimeBetween(any(), any())).thenReturn(12L);
        when(requestRepository.countByStatusAndRequestTimeBetween(eq("WAITING"), any(), any())).thenReturn(1L);
        when(requestRepository.countByStatusAndRequestTimeBetween(eq("ASSIGNED"), any(), any())).thenReturn(3L);
        when(requestRepository.countByStatusAndRequestTimeBetween(eq("IN_PROGRESS"), any(), any())).thenReturn(2L);
        when(requestRepository.countByStatusAndRequestTimeBetween(eq("COMPLETED"), any(), any())).thenReturn(6L);
        when(requestRepository.countByStatusAndRequestTimeBetween(eq("CANCELLED"), any(), any())).thenReturn(0L);
        when(requestRepository.countByStatusAndRequestTimeBetween(eq("NO_DOCTOR_AVAILABLE"), any(), any())).thenReturn(0L);

        when(doctorRepository.findAll()).thenReturn(List.of(doctor));

        EmergencyDoctorRoster roster = new EmergencyDoctorRoster(
                doctor, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        when(rosterRepository.findByRosterDate(today)).thenReturn(List.of(roster));
        when(rosterRepository.countByDoctorIdAndRosterDateBetween(eq(doctor.getId()), any(), any())).thenReturn(3L);
        when(requestRepository.countByAssignedDoctorAndRequestTimeBetween(eq(doctor), any(), any())).thenReturn(3L);
        when(requestRepository.countByAssignedDoctorAndStatusAndRequestTimeBetween(eq(doctor), eq("COMPLETED"), any(), any())).thenReturn(2L);
        when(requestRepository.countByAssignedDoctorAndStatusInAndRequestTimeBetween(eq(doctor), anyList(), any(), any())).thenReturn(1L);

        EmergencyAnalyticsDTO analytics = emergencyRequestService.getEmergencyAnalytics(today);

        assertNotNull(analytics);
        assertEquals(12L, analytics.getTotalRequests());
        assertEquals(1L, analytics.getWaitingRequests());
        assertEquals(3L, analytics.getAssignedRequests());
        assertEquals(2L, analytics.getInProgressRequests());
        assertEquals(6L, analytics.getCompletedRequests());

        assertNotNull(analytics.getDoctorWorkloads());
        assertEquals(1, analytics.getDoctorWorkloads().size());
        DoctorEmergencyWorkloadDTO docWorkload = analytics.getDoctorWorkloads().get(0);
        assertEquals("Dr. Sarah Jenkins", docWorkload.getDoctorName());
        assertTrue(docWorkload.isEmergencyDutyToday());
        assertEquals("MORNING", docWorkload.getShiftName());
        assertEquals(3L, docWorkload.getTodayTotalCases());
        assertEquals(2L, docWorkload.getTodayCompletedCases());
        assertEquals(1L, docWorkload.getTodayActiveCases());
    }
}
