package com.carepulse.service;

import com.carepulse.dto.DoctorStatusUpdateRequestDTO;
import com.carepulse.dto.EmergencyRosterRequestDTO;
import com.carepulse.dto.EmergencyRosterResponseDTO;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.EmergencyDoctorRoster;
import com.carepulse.entity.User;
import com.carepulse.exception.InvalidEmergencyRosterException;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyRosterServiceTest {

    @Mock
    private EmergencyDoctorRosterRepository rosterRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private EmergencyRequestRepository requestRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EmergencyRosterService rosterService;

    private Doctor doctor;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.of(2026, 9, 27);
        User user = new User("dr.jenkins@carepulse.com", "pass", "DOCTOR");
        user.setId(1L);
        doctor = new Doctor(user, "Dr. Sarah Jenkins", "+1555234567", "Cardiology", "MD", 12);
        doctor.setId(1L);
        doctor.setAvailabilityStatus("AVAILABLE");
    }

    @Test
    @DisplayName("12. Admin can create emergency roster entry")
    void testAdminCreateRosterEntry() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(rosterRepository.existsByDoctorIdAndRosterDateAndShiftName(1L, today, "MORNING")).thenReturn(false);

        EmergencyDoctorRoster savedRoster = new EmergencyDoctorRoster(
                doctor, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        savedRoster.setId(10L);
        when(rosterRepository.save(any(EmergencyDoctorRoster.class))).thenReturn(savedRoster);

        EmergencyRosterRequestDTO req = new EmergencyRosterRequestDTO(1L, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0));

        EmergencyRosterResponseDTO response = rosterService.createRosterEntry(req, "admin@carepulse.com");

        assertNotNull(response);
        assertEquals(1L, response.getDoctorId());
        assertEquals("MORNING", response.getShiftName());
        assertEquals(today, response.getRosterDate());
        verify(auditLogService).log(eq("admin@carepulse.com"), eq("ADMIN_ASSIGNED_EMERGENCY_DUTY"), anyString(), anyString());
    }

    @Test
    @DisplayName("9. Duplicate roster assignment is prevented")
    void testDuplicateRosterAssignmentPrevented() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(rosterRepository.existsByDoctorIdAndRosterDateAndShiftName(1L, today, "MORNING")).thenReturn(true);

        EmergencyRosterRequestDTO req = new EmergencyRosterRequestDTO(1L, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0));

        assertThrows(InvalidEmergencyRosterException.class, () ->
                rosterService.createRosterEntry(req, "admin@carepulse.com")
        );
    }

    @Test
    @DisplayName("12. Admin can update and delete roster entry")
    void testAdminUpdateAndDeleteRoster() {
        EmergencyDoctorRoster roster = new EmergencyDoctorRoster(
                doctor, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        roster.setId(20L);

        when(rosterRepository.findById(20L)).thenReturn(Optional.of(roster));
        when(rosterRepository.save(any(EmergencyDoctorRoster.class))).thenReturn(roster);

        EmergencyRosterRequestDTO updateReq = new EmergencyRosterRequestDTO();
        updateReq.setDutyStatus("NOT_ASSIGNED");
        EmergencyRosterResponseDTO updated = rosterService.updateRosterEntry(20L, updateReq, "admin@carepulse.com");

        assertEquals("NOT_ASSIGNED", updated.getDutyStatus());

        // Test delete
        rosterService.deleteRosterEntry(20L, "admin@carepulse.com");
        verify(rosterRepository).delete(roster);
        verify(auditLogService).log(eq("admin@carepulse.com"), eq("ADMIN_REMOVED_EMERGENCY_DUTY"), anyString(), anyString());
    }

    @Test
    @DisplayName("Doctor can view their duty today and update their availability status")
    void testDoctorDutyAndStatusUpdate() {
        when(doctorRepository.findByUserEmail("dr.jenkins@carepulse.com")).thenReturn(Optional.of(doctor));

        EmergencyDoctorRoster duty = new EmergencyDoctorRoster(
                doctor, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        when(rosterRepository.findTodayDutyForDoctor(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(duty));

        List<EmergencyRosterResponseDTO> dutyList = rosterService.getDoctorTodayDuty("dr.jenkins@carepulse.com");
        assertFalse(dutyList.isEmpty());
        assertEquals("MORNING", dutyList.get(0).getShiftName());

        // Update status to BUSY
        DoctorStatusUpdateRequestDTO statusReq = new DoctorStatusUpdateRequestDTO("BUSY");
        Doctor updatedDoc = rosterService.updateDoctorStatus("dr.jenkins@carepulse.com", statusReq);

        assertEquals("BUSY", updatedDoc.getAvailabilityStatus());
        verify(doctorRepository).save(doctor);
    }

    @Test
    @DisplayName("Admin can fetch active doctors for emergency roster dropdown")
    void testGetDoctorsForRoster() {
        when(doctorRepository.findAll()).thenReturn(List.of(doctor));

        List<com.carepulse.dto.DoctorRosterOptionDTO> options = rosterService.getDoctorsForRoster();

        assertNotNull(options);
        assertEquals(1, options.size());
        assertEquals("Dr. Sarah Jenkins", options.get(0).getName());
        assertEquals("Cardiology", options.get(0).getSpecialization());
        assertEquals("AVAILABLE", options.get(0).getStatus());
    }

    @Test
    @DisplayName("Doctor can query structured today's emergency duty summary")
    void testGetDoctorTodayDutySummary() {
        when(doctorRepository.findByUserEmail("dr.jenkins@carepulse.com")).thenReturn(Optional.of(doctor));

        EmergencyDoctorRoster duty = new EmergencyDoctorRoster(
                doctor, today, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        when(rosterRepository.findTodayDutyForDoctor(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(duty));
        when(requestRepository.countByAssignedDoctorAndStatusIn(eq(doctor), anyList())).thenReturn(2L);

        com.carepulse.dto.DoctorTodayDutyDTO summary = rosterService.getDoctorTodayDutySummary("dr.jenkins@carepulse.com");

        assertNotNull(summary);
        assertTrue(summary.getIsEmergencyDuty());
        assertEquals("MORNING", summary.getShift());
        assertEquals("08:00", summary.getShiftStart());
        assertEquals("14:00", summary.getShiftEnd());
        assertEquals("AVAILABLE", summary.getStatus());
        assertEquals(2L, summary.getEmergencyRequestsCount());
    }
}
