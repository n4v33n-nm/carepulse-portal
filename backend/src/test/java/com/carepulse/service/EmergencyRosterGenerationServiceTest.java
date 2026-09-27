package com.carepulse.service;

import com.carepulse.dto.EmergencyRosterResponseDTO;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.EmergencyDoctorRoster;
import com.carepulse.entity.User;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.InvalidEmergencyRosterException;
import com.carepulse.repository.AppointmentRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyRosterGenerationServiceTest {

    @Mock
    private EmergencyDoctorRosterRepository rosterRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private EmergencyRequestRepository requestRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EmergencyRosterGenerationService rosterGenerationService;

    private Doctor doc1;
    private Doctor doc2;
    private Doctor doc3;
    private Doctor doc4;
    private Doctor docLeave;

    private LocalDate tomorrow;

    @BeforeEach
    void setUp() {
        tomorrow = LocalDate.now().plusDays(1);

        ReflectionTestUtils.setField(rosterGenerationService, "morningStartStr", "08:00");
        ReflectionTestUtils.setField(rosterGenerationService, "morningEndStr", "14:00");
        ReflectionTestUtils.setField(rosterGenerationService, "eveningStartStr", "14:00");
        ReflectionTestUtils.setField(rosterGenerationService, "eveningEndStr", "20:00");
        ReflectionTestUtils.setField(rosterGenerationService, "nightStartStr", "20:00");
        ReflectionTestUtils.setField(rosterGenerationService, "nightEndStr", "08:00");
        ReflectionTestUtils.setField(rosterGenerationService, "defaultDoctorsPerShift", 1);

        User u1 = new User("doc1@carepulse.com", "pass", "DOCTOR");
        u1.setId(1L);
        doc1 = new Doctor(u1, "Dr. Arun Kumar", "+1111111111", "General Medicine", "MBBS", 10);
        doc1.setId(1L);
        doc1.setAvailabilityStatus("AVAILABLE");

        User u2 = new User("doc2@carepulse.com", "pass", "DOCTOR");
        u2.setId(2L);
        doc2 = new Doctor(u2, "Dr. Priya Sharma", "+2222222222", "Cardiology", "MD", 12);
        doc2.setId(2L);
        doc2.setAvailabilityStatus("AVAILABLE");

        User u3 = new User("doc3@carepulse.com", "pass", "DOCTOR");
        u3.setId(3L);
        doc3 = new Doctor(u3, "Dr. Kumar Sangakara", "+3333333333", "Neurology", "MD", 8);
        doc3.setId(3L);
        doc3.setAvailabilityStatus("AVAILABLE");

        User u4 = new User("doc4@carepulse.com", "pass", "DOCTOR");
        u4.setId(4L);
        doc4 = new Doctor(u4, "Dr. Meena Iyer", "+4444444444", "Pediatrics", "MD", 6);
        doc4.setId(4L);
        doc4.setAvailabilityStatus("AVAILABLE");

        User u5 = new User("leave@carepulse.com", "pass", "DOCTOR");
        u5.setId(5L);
        docLeave = new Doctor(u5, "Dr. On Leave", "+5555555555", "Orthopedics", "MS", 15);
        docLeave.setId(5L);
        docLeave.setAvailabilityStatus("ON_LEAVE");
    }

    @Test
    @DisplayName("1. Successfully generates roster for tomorrow across MORNING, EVENING, and NIGHT")
    void testGenerateRosterForTomorrowSuccess() {
        when(doctorRepository.findAll()).thenReturn(List.of(doc1, doc2, doc3, doc4));
        when(rosterRepository.findByRosterDate(tomorrow)).thenReturn(Collections.emptyList());
        when(rosterRepository.findByRosterDateAndDutyStatus(any(LocalDate.class), eq("EMERGENCY_DUTY")))
                .thenReturn(Collections.emptyList());
        when(rosterRepository.countByDoctorIdAndRosterDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);
        when(appointmentRepository.countByDoctorIdAndAppointmentDate(anyLong(), any(LocalDate.class)))
                .thenReturn(0L);
        when(rosterRepository.save(any(EmergencyDoctorRoster.class))).thenAnswer(i -> {
            EmergencyDoctorRoster r = i.getArgument(0);
            r.setId(100L + (long) (Math.random() * 1000));
            return r;
        });

        List<EmergencyRosterResponseDTO> result = rosterGenerationService.generateRosterForDate(tomorrow, 1, "admin@carepulse.com");

        assertNotNull(result);
        assertEquals(3, result.size()); // 1 doctor per shift for MORNING, EVENING, NIGHT
        List<String> shifts = result.stream().map(EmergencyRosterResponseDTO::getShiftName).toList();
        assertTrue(shifts.contains("MORNING"));
        assertTrue(shifts.contains("EVENING"));
        assertTrue(shifts.contains("NIGHT"));

        verify(auditLogService).log(eq("admin@carepulse.com"), eq("ROSTER_GENERATED"), anyString(), anyString());
    }

    @Test
    @DisplayName("2. Rotation algorithm prefers doctors with fewer past 7 days shifts")
    void testRotationPrefersDoctorWithFewerRecentShifts() {
        // Dr. Arun (doc1) had 5 shifts in past 7 days
        // Dr. Priya (doc2) had 1 shift in past 7 days
        // Dr. Kumar (doc3) had 2 shifts in past 7 days
        when(doctorRepository.findAll()).thenReturn(List.of(doc1, doc2, doc3));
        when(rosterRepository.findByRosterDate(tomorrow)).thenReturn(Collections.emptyList());
        when(rosterRepository.findByRosterDateAndDutyStatus(any(LocalDate.class), eq("EMERGENCY_DUTY")))
                .thenReturn(Collections.emptyList());

        when(rosterRepository.countByDoctorIdAndRosterDateBetween(eq(doc1.getId()), any(LocalDate.class), any(LocalDate.class))).thenReturn(5L);
        when(rosterRepository.countByDoctorIdAndRosterDateBetween(eq(doc2.getId()), any(LocalDate.class), any(LocalDate.class))).thenReturn(1L);
        when(rosterRepository.countByDoctorIdAndRosterDateBetween(eq(doc3.getId()), any(LocalDate.class), any(LocalDate.class))).thenReturn(2L);

        when(appointmentRepository.countByDoctorIdAndAppointmentDate(anyLong(), any(LocalDate.class))).thenReturn(0L);
        when(rosterRepository.save(any(EmergencyDoctorRoster.class))).thenAnswer(i -> {
            EmergencyDoctorRoster r = i.getArgument(0);
            r.setId(200L);
            return r;
        });

        List<EmergencyRosterResponseDTO> result = rosterGenerationService.generateRosterForDate(tomorrow, 1, "admin@carepulse.com");

        // The first shift (MORNING) should select Dr. Priya (doc2) because she has the lowest past shift count (1)
        EmergencyRosterResponseDTO morningShift = result.stream()
                .filter(r -> "MORNING".equals(r.getShiftName()))
                .findFirst()
                .orElse(null);

        assertNotNull(morningShift);
        assertEquals(doc2.getId(), morningShift.getDoctorId());
        assertEquals("Dr. Priya Sharma", morningShift.getDoctorName());
    }

    @Test
    @DisplayName("3. Doctor marked ON_LEAVE is skipped during roster generation")
    void testDoctorOnLeaveIsExcluded() {
        // Only doc1 (AVAILABLE) and docLeave (ON_LEAVE)
        when(doctorRepository.findAll()).thenReturn(List.of(doc1, docLeave));
        when(rosterRepository.findByRosterDate(tomorrow)).thenReturn(Collections.emptyList());
        when(rosterRepository.findByRosterDateAndDutyStatus(any(LocalDate.class), eq("EMERGENCY_DUTY")))
                .thenReturn(Collections.emptyList());
        when(rosterRepository.countByDoctorIdAndRosterDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(appointmentRepository.countByDoctorIdAndAppointmentDate(anyLong(), any(LocalDate.class))).thenReturn(0L);
        when(rosterRepository.save(any(EmergencyDoctorRoster.class))).thenAnswer(i -> i.getArgument(0));

        List<EmergencyRosterResponseDTO> result = rosterGenerationService.generateRosterForDate(tomorrow, 1, "admin@carepulse.com");

        // docLeave should never appear in any assigned shift
        boolean docLeaveAssigned = result.stream().anyMatch(r -> r.getDoctorId().equals(docLeave.getId()));
        assertFalse(docLeaveAssigned);
    }

    @Test
    @DisplayName("4. Night Shift rest rule: Doctor who worked previous night cannot be assigned to MORNING shift")
    void testNightShiftRestRuleEnforced() {
        // Dr. Arun (doc1) worked NIGHT shift yesterday
        // Dr. Priya (doc2) did not work NIGHT shift yesterday
        EmergencyDoctorRoster prevNightRoster = new EmergencyDoctorRoster(
                doc1, tomorrow.minusDays(1), "NIGHT", LocalTime.of(20, 0), LocalTime.of(8, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );

        when(doctorRepository.findAll()).thenReturn(List.of(doc1, doc2));
        when(rosterRepository.findByRosterDate(tomorrow)).thenReturn(Collections.emptyList());
        when(rosterRepository.findByRosterDateAndDutyStatus(tomorrow.minusDays(1), "EMERGENCY_DUTY"))
                .thenReturn(List.of(prevNightRoster));
        when(rosterRepository.countByDoctorIdAndRosterDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(appointmentRepository.countByDoctorIdAndAppointmentDate(anyLong(), any(LocalDate.class))).thenReturn(0L);
        when(rosterRepository.save(any(EmergencyDoctorRoster.class))).thenAnswer(i -> i.getArgument(0));

        List<EmergencyRosterResponseDTO> result = rosterGenerationService.generateRosterForDate(tomorrow, 1, "admin@carepulse.com");

        EmergencyRosterResponseDTO morningShift = result.stream()
                .filter(r -> "MORNING".equals(r.getShiftName()))
                .findFirst()
                .orElse(null);

        assertNotNull(morningShift);
        // Dr. Priya must get MORNING because Dr. Arun worked NIGHT shift yesterday
        assertEquals(doc2.getId(), morningShift.getDoctorId());
    }

    @Test
    @DisplayName("5. Reject past date for roster generation")
    void testPastDateRejected() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        assertThrows(BadRequestException.class, () ->
                rosterGenerationService.generateRosterForDate(yesterday, 1, "admin@carepulse.com")
        );
    }

    @Test
    @DisplayName("6. Existing roster entries are not duplicated")
    void testExistingRosterNotDuplicated() {
        EmergencyDoctorRoster existingMorning = new EmergencyDoctorRoster(
                doc1, tomorrow, "MORNING", LocalTime.of(8, 0), LocalTime.of(14, 0), "EMERGENCY_DUTY", "AVAILABLE"
        );
        existingMorning.setId(99L);

        when(doctorRepository.findAll()).thenReturn(List.of(doc1, doc2, doc3));
        when(rosterRepository.findByRosterDate(tomorrow)).thenReturn(List.of(existingMorning));
        when(rosterRepository.findByRosterDateAndDutyStatus(any(LocalDate.class), eq("EMERGENCY_DUTY")))
                .thenReturn(Collections.emptyList());
        when(rosterRepository.countByDoctorIdAndRosterDateBetween(anyLong(), any(LocalDate.class), any(LocalDate.class))).thenReturn(0L);
        when(appointmentRepository.countByDoctorIdAndAppointmentDate(anyLong(), any(LocalDate.class))).thenReturn(0L);
        when(rosterRepository.save(any(EmergencyDoctorRoster.class))).thenAnswer(i -> i.getArgument(0));

        // Requesting 1 doctor per shift; MORNING already has 1 doctor (doc1)
        List<EmergencyRosterResponseDTO> result = rosterGenerationService.generateRosterForDate(tomorrow, 1, "admin@carepulse.com");

        // MORNING should not be re-generated or duplicated
        boolean morningRecreated = result.stream().anyMatch(r -> "MORNING".equals(r.getShiftName()));
        assertFalse(morningRecreated);
        // Only EVENING and NIGHT should be generated
        assertEquals(2, result.size());
    }
}
