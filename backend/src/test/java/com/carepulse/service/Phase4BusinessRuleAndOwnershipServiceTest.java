package com.carepulse.service;

import com.carepulse.dto.AppointmentRequest;
import com.carepulse.dto.AppointmentStatusUpdateRequest;
import com.carepulse.dto.CaregiverRequest;
import com.carepulse.entity.*;
import com.carepulse.exception.BadRequestException;
import com.carepulse.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class Phase4BusinessRuleAndOwnershipServiceTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private CaregiverService caregiverService;

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    @DisplayName("Business Rule: Booking an on-leave doctor throws BadRequestException")
    void testBookingOnLeaveDoctorThrowsException() {
        Doctor doctor = doctorRepository.findAll().get(0);
        String originalStatus = doctor.getAvailabilityStatus();
        try {
            doctor.setAvailabilityStatus("ON_LEAVE");
            doctorRepository.save(doctor);

            AppointmentRequest req = new AppointmentRequest();
            req.setDoctorId(doctor.getId());
            req.setAppointmentDate(LocalDate.now().plusDays(2));
            req.setAppointmentTime(LocalTime.of(11, 0));
            req.setReason("General Consultation");

            assertThrows(BadRequestException.class, () ->
                    appointmentService.bookAppointment("john.doe@example.com", req));
        } finally {
            doctor.setAvailabilityStatus(originalStatus);
            doctorRepository.save(doctor);
        }
    }

    @Test
    @DisplayName("Business Rule: Double-booking same doctor and slot throws BadRequestException")
    void testDoubleBookingDoctorThrowsException() {
        Doctor doctor = doctorRepository.findAll().get(0);
        LocalDate futureDate = LocalDate.now().plusDays(5);
        LocalTime slotTime = LocalTime.of(14, 0);

        AppointmentRequest req1 = new AppointmentRequest();
        req1.setDoctorId(doctor.getId());
        req1.setAppointmentDate(futureDate);
        req1.setAppointmentTime(slotTime);
        req1.setReason("First Consultation");

        Appointment first = appointmentService.bookAppointment("john.doe@example.com", req1);
        assertNotNull(first.getId());

        // Second booking attempt for same doctor, date, and slot
        AppointmentRequest req2 = new AppointmentRequest();
        req2.setDoctorId(doctor.getId());
        req2.setAppointmentDate(futureDate);
        req2.setAppointmentTime(slotTime);
        req2.setReason("Conflicting Consultation");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                appointmentService.bookAppointment("emma.watson@example.com", req2));

        assertTrue(ex.getMessage().contains("no longer available") || ex.getMessage().contains("conflict"));
    }

    @Test
    @DisplayName("Ownership: Patient cannot mark appointment COMPLETED (AccessDeniedException)")
    void testPatientCannotCompleteAppointment() {
        Doctor doctor = doctorRepository.findAll().get(0);
        LocalDate futureDate = LocalDate.now().plusDays(4);
        LocalTime slotTime = LocalTime.of(15, 30);

        AppointmentRequest req = new AppointmentRequest();
        req.setDoctorId(doctor.getId());
        req.setAppointmentDate(futureDate);
        req.setAppointmentTime(slotTime);
        req.setReason("Routine Visit");

        Appointment apt = appointmentService.bookAppointment("john.doe@example.com", req);

        AppointmentStatusUpdateRequest statusReq = new AppointmentStatusUpdateRequest();
        statusReq.setStatus("COMPLETED");

        // Attempting to complete with patient identity
        assertThrows(AccessDeniedException.class, () ->
                appointmentService.updateAppointmentStatus(apt.getId(), "john.doe@example.com", "PATIENT", statusReq));
    }

    @Test
    @DisplayName("Caregiver: Revoked caregiver access immediately denies access")
    void testCaregiverAccessRevocationLifecycle() {
        Patient patient = patientRepository.findByUserEmail("john.doe@example.com").orElseThrow();

        CaregiverRequest req = new CaregiverRequest();
        req.setCaregiverEmail("caregiver.test@carepulse.com");
        req.setCaregiverName("Guardian Mary");
        req.setRelationship("Mother");
        req.setPermission("VIEW_ALL");

        CaregiverAccess access = caregiverService.grantAccess("john.doe@example.com", req);
        assertTrue(caregiverService.isAuthorizedCaregiver(patient.getId(), "caregiver.test@carepulse.com"));

        // Revoke access
        caregiverService.revokeAccess(access.getId(), "john.doe@example.com");

        // Must immediately reflect as false
        assertFalse(caregiverService.isAuthorizedCaregiver(patient.getId(), "caregiver.test@carepulse.com"),
                "Revoked caregiver access must immediately become invalid");
    }

    @Test
    @DisplayName("Ownership: User cannot mark another user's notification as read")
    void testNotificationReadOwnershipEnforcement() {
        User user1 = userRepository.findByEmail("admin@carepulse.com").orElseThrow();
        User user2 = userRepository.findByEmail("john.doe@example.com").orElseThrow();

        Notification notification = notificationService.createNotification(user1, "System Notice", "Test Alert", "INFO");

        // user2 attempting to mark user1's notification as read
        assertThrows(AccessDeniedException.class, () ->
                notificationService.markAsRead(notification.getId(), user2.getId(), "PATIENT"));
    }
}
