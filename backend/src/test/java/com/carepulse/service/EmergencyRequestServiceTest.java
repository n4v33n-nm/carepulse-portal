package com.carepulse.service;

import com.carepulse.dto.EmergencyRequestCreateDTO;
import com.carepulse.dto.EmergencyRequestResponseDTO;
import com.carepulse.dto.EmergencyStatusUpdateRequestDTO;
import com.carepulse.entity.*;
import com.carepulse.exception.UnauthorizedException;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyRequestServiceTest {

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

    private Patient patient1;
    private Patient patient2;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        User u1 = new User("patient1@carepulse.com", "pass", "PATIENT");
        u1.setId(10L);
        patient1 = new Patient(u1, "Patient One", "+111111111", LocalDate.of(1990, 1, 1), "Male");
        patient1.setId(10L);

        User u2 = new User("patient2@carepulse.com", "pass", "PATIENT");
        u2.setId(20L);
        patient2 = new Patient(u2, "Patient Two", "+222222222", LocalDate.of(1995, 5, 5), "Female");
        patient2.setId(20L);

        User docUser = new User("doctor@carepulse.com", "pass", "DOCTOR");
        docUser.setId(1L);
        doctor = new Doctor(docUser, "Dr. Sarah Jenkins", "+999999999", "Cardiology", "MD", 12);
        doctor.setId(1L);
    }

    @Test
    @DisplayName("1. Patient can create emergency request and doctor is assigned with notifications")
    void testPatientCreatesEmergencyRequestSuccess() {
        when(patientRepository.findByUserEmail("patient1@carepulse.com")).thenReturn(Optional.of(patient1));

        EmergencyRequest allocated = new EmergencyRequest(patient1, "Cardiology", "Acute chest pain");
        allocated.setId(50L);
        allocated.setAssignedDoctor(doctor);
        allocated.setStatus("ASSIGNED");
        allocated.setAssignedTime(LocalDateTime.now());

        when(allocationService.allocateDoctor(any(EmergencyRequest.class))).thenReturn(allocated);

        EmergencyRequestCreateDTO reqDto = new EmergencyRequestCreateDTO("Cardiology", "Acute chest pain");
        EmergencyRequestResponseDTO result = emergencyRequestService.createEmergencyRequest("patient1@carepulse.com", reqDto);

        assertNotNull(result);
        assertEquals(50L, result.getId());
        assertEquals("ASSIGNED", result.getStatus());
        assertEquals(doctor.getId(), result.getDoctorId());

        // Verify doctor and patient notifications were sent
        verify(notificationService, times(2)).createNotification(any(User.class), anyString(), anyString(), eq("EMERGENCY"));
        verify(auditLogService, atLeastOnce()).log(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("10. Unauthorized patient cannot access another patient's emergency request")
    void testUnauthorizedPatientCannotAccessAnotherPatientRequest() {
        EmergencyRequest req = new EmergencyRequest(patient1, "General", "Dizziness");
        req.setId(60L);

        when(requestRepository.findById(60L)).thenReturn(Optional.of(req));

        // patient2 tries to view patient1's request
        assertThrows(UnauthorizedException.class, () ->
                emergencyRequestService.getEmergencyRequestById(60L, "patient2@carepulse.com", "PATIENT")
        );
    }

    @Test
    @DisplayName("13. Emergency request status transitions work (ASSIGNED -> IN_PROGRESS -> COMPLETED)")
    void testEmergencyRequestStatusTransitions() {
        EmergencyRequest req = new EmergencyRequest(patient1, "Cardiology", "Severe chest pain");
        req.setId(70L);
        req.setAssignedDoctor(doctor);
        req.setStatus("ASSIGNED");

        when(requestRepository.findById(70L)).thenReturn(Optional.of(req));
        when(doctorRepository.findByUserEmail("doctor@carepulse.com")).thenReturn(Optional.of(doctor));
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(i -> i.getArgument(0));

        // Transition 1: Doctor sets IN_PROGRESS
        EmergencyStatusUpdateRequestDTO inProgressReq = new EmergencyStatusUpdateRequestDTO("IN_PROGRESS", "Patient undergoing ECG");
        EmergencyRequestResponseDTO res1 = emergencyRequestService.updateRequestStatus(70L, "doctor@carepulse.com", inProgressReq);
        assertEquals("IN_PROGRESS", res1.getStatus());

        // Transition 2: Doctor sets COMPLETED
        when(requestRepository.countByAssignedDoctorAndStatusIn(eq(doctor), anyList())).thenReturn(0L);
        EmergencyStatusUpdateRequestDTO completedReq = new EmergencyStatusUpdateRequestDTO("COMPLETED", "Treated successfully, vitals normal");
        EmergencyRequestResponseDTO res2 = emergencyRequestService.updateRequestStatus(70L, "doctor@carepulse.com", completedReq);
        assertEquals("COMPLETED", res2.getStatus());

        // Doctor should be restored to AVAILABLE since 0 active emergency cases remain
        assertEquals("AVAILABLE", doctor.getAvailabilityStatus());
        verify(doctorRepository).save(doctor);
    }

    @Test
    @DisplayName("Patient can cancel their pending/assigned emergency request")
    void testPatientCanCancelRequest() {
        EmergencyRequest req = new EmergencyRequest(patient1, "General", "Mild symptoms subsided");
        req.setId(80L);
        req.setStatus("WAITING");

        when(requestRepository.findById(80L)).thenReturn(Optional.of(req));
        when(requestRepository.save(any(EmergencyRequest.class))).thenAnswer(i -> i.getArgument(0));

        EmergencyRequestResponseDTO result = emergencyRequestService.cancelPatientRequest(80L, "patient1@carepulse.com");

        assertEquals("CANCELLED", result.getStatus());
        verify(auditLogService).log(eq("patient1@carepulse.com"), eq("EMERGENCY_REQUEST_CANCELLED"), anyString(), anyString());
    }
}
