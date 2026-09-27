package com.carepulse.config;

import com.carepulse.entity.*;
import com.carepulse.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorAvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final CaregiverAccessRepository caregiverAccessRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmergencyDoctorRosterRepository emergencyDoctorRosterRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PatientRepository patientRepository,
                           DoctorRepository doctorRepository,
                           DoctorAvailabilityRepository availabilityRepository,
                           AppointmentRepository appointmentRepository,
                           MedicalRecordRepository medicalRecordRepository,
                           PrescriptionRepository prescriptionRepository,
                           CaregiverAccessRepository caregiverAccessRepository,
                           NotificationRepository notificationRepository,
                           AuditLogRepository auditLogRepository,
                           EmergencyDoctorRosterRepository emergencyDoctorRosterRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.caregiverAccessRepository = caregiverAccessRepository;
        this.notificationRepository = notificationRepository;
        this.auditLogRepository = auditLogRepository;
        this.emergencyDoctorRosterRepository = emergencyDoctorRosterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            seedEmergencyRosterIfEmpty();
            return; // Data already seeded
        }

        System.out.println("Seeding CarePulse demo data into PostgreSQL...");

        // 1. Admin
        User adminUser = new User("admin@carepulse.com", passwordEncoder.encode("Admin@123"), "ADMIN");
        adminUser.setCommunicationPreference("PROFESSIONAL");
        userRepository.save(adminUser);
        auditLogRepository.save(new AuditLog("admin@carepulse.com", "SYSTEM_INIT", "System", "CarePulse platform initialized"));

        // 2. Doctors
        // Doctor 1: Cardiology
        User doc1User = new User("dr.jenkins@carepulse.com", passwordEncoder.encode("Doctor@123"), "DOCTOR");
        doc1User.setCommunicationPreference("PROFESSIONAL");
        userRepository.save(doc1User);

        Doctor doc1 = new Doctor(doc1User, "Dr. Sarah Jenkins", "+1 (555) 234-5678", "Cardiology", "MD, FACC, Harvard Medical", 12);
        doc1.setBio("Board-certified cardiologist specializing in preventive cardiovascular care, heart rhythm disorders, and hypertensive management. Dedicated to compassionate, evidence-based patient coordination.");
        doc1.setConsultationFee(1500.0);
        doc1.setRating(4.9);
        doc1.setAvailabilityStatus("AVAILABLE");
        doctorRepository.save(doc1);

        // Doctor 2: Dermatology
        User doc2User = new User("dr.vance@carepulse.com", passwordEncoder.encode("Doctor@123"), "DOCTOR");
        doc2User.setCommunicationPreference("PROFESSIONAL");
        userRepository.save(doc2User);

        Doctor doc2 = new Doctor(doc2User, "Dr. Marcus Vance", "+1 (555) 345-6789", "Dermatology", "MD, FAAD, Johns Hopkins", 8);
        doc2.setBio("Specialist in clinical dermatology, cutaneous oncology, eczema, and advanced phototherapy treatments. Passionate about empowering patients with personalized skin care therapies.");
        doc2.setConsultationFee(1200.0);
        doc2.setRating(4.8);
        doc2.setAvailabilityStatus("AVAILABLE");
        doctorRepository.save(doc2);

        // Doctor 3: General Medicine
        User doc3User = new User("dr.sharma@carepulse.com", passwordEncoder.encode("Doctor@123"), "DOCTOR");
        doc3User.setCommunicationPreference("SUPPORTIVE");
        userRepository.save(doc3User);

        Doctor doc3 = new Doctor(doc3User, "Dr. Priya Sharma", "+1 (555) 456-7890", "General Medicine", "MBBS, MD (Internal Medicine), Stanford", 10);
        doc3.setBio("Internal medicine consultant focusing on holistic family healthcare, metabolic disorders, lifestyle coaching, and chronic illness management with empathy and precision.");
        doc3.setConsultationFee(800.0);
        doc3.setRating(5.0);
        doc3.setAvailabilityStatus("AVAILABLE");
        doctorRepository.save(doc3);

        // Seed Doctor Availabilities (Mon - Fri 09:00 - 17:00, lunch 13:00 - 14:00)
        List<Doctor> doctors = List.of(doc1, doc2, doc3);
        List<String> weekdays = List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
        for (Doctor doc : doctors) {
            for (String day : weekdays) {
                DoctorAvailability da = new DoctorAvailability(
                        doc,
                        day,
                        LocalTime.of(9, 0),
                        LocalTime.of(17, 0),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 0),
                        30
                );
                if ("SUNDAY".equals(day)) {
                    da.setAvailable(false);
                }
                availabilityRepository.save(da);
            }
        }

        // 3. Patients
        // Patient 1: John Doe
        User pat1User = new User("john.doe@example.com", passwordEncoder.encode("Patient@123"), "PATIENT");
        pat1User.setCommunicationPreference("SUPPORTIVE");
        userRepository.save(pat1User);

        Patient pat1 = new Patient(pat1User, "John Doe", "+1 (555) 111-2222", LocalDate.of(1988, 5, 14), "Male");
        pat1.setAddress("742 Evergreen Terrace, Springfield, IL");
        pat1.setBloodGroup("O+");
        pat1.setEmergencyContact("+1 (555) 999-0001 (Emma Watson, Spouse)");
        patientRepository.save(pat1);

        // Patient 2: Emma Watson
        User pat2User = new User("emma.watson@example.com", passwordEncoder.encode("Patient@123"), "PATIENT");
        pat2User.setCommunicationPreference("SIMPLE");
        userRepository.save(pat2User);

        Patient pat2 = new Patient(pat2User, "Emma Watson", "+1 (555) 222-3333", LocalDate.of(1992, 9, 21), "Female");
        pat2.setAddress("124 Elm Street, Cambridge, MA");
        pat2.setBloodGroup("A+");
        pat2.setEmergencyContact("+1 (555) 111-2222 (John Doe)");
        patientRepository.save(pat2);

        // Patient 3: Robert Chen
        User pat3User = new User("robert.chen@example.com", passwordEncoder.encode("Patient@123"), "PATIENT");
        pat3User.setCommunicationPreference("PROFESSIONAL");
        userRepository.save(pat3User);

        Patient pat3 = new Patient(pat3User, "Robert Chen", "+1 (555) 333-4444", LocalDate.of(1975, 11, 3), "Male");
        pat3.setAddress("450 Park Avenue, Seattle, WA");
        pat3.setBloodGroup("B+");
        pat3.setEmergencyContact("+1 (555) 888-7777 (Lily Chen, Daughter)");
        patientRepository.save(pat3);

        // 4. Appointments
        LocalDate today = LocalDate.now();
        // Upcoming Confirmed Appointment for John with Dr. Jenkins tomorrow
        Appointment appt1 = new Appointment(pat1, doc1, today.plusDays(1), LocalTime.of(10, 0), "Routine cardiac stress check and blood pressure review");
        appt1.setStatus("CONFIRMED");
        appt1.setConsultationNotes("Patient reports mild exertion fatigue. ECG review scheduled.");
        appointmentRepository.save(appt1);

        // Upcoming Pending Appointment for Emma with Dr. Vance in 3 days
        Appointment appt2 = new Appointment(pat2, doc2, today.plusDays(3), LocalTime.of(11, 30), "Persistent skin rash on forearms and allergy assessment");
        appt2.setStatus("PENDING");
        appointmentRepository.save(appt2);

        // Completed Appointment for John with Dr. Sharma last week
        Appointment appt3 = new Appointment(pat1, doc3, today.minusDays(7), LocalTime.of(14, 0), "Annual preventive health examination and lipid panel review");
        appt3.setStatus("COMPLETED");
        appt3.setConsultationNotes("Vitals stable. BP 122/80 mmHg. Advised moderate aerobic exercise and prescribed multivitamin supplement.");
        appointmentRepository.save(appt3);

        // Completed Appointment for Robert with Dr. Jenkins 2 weeks ago
        Appointment appt4 = new Appointment(pat3, doc1, today.minusDays(14), LocalTime.of(15, 30), "Palpitation monitoring and Holter report evaluation");
        appt4.setStatus("COMPLETED");
        appt4.setConsultationNotes("Sinus rhythm maintained. Palpitations correlated with elevated caffeine intake. Reduced stimulant guidelines shared.");
        appointmentRepository.save(appt4);

        // 5. Medical Records
        MedicalRecord rec1 = new MedicalRecord(
                pat1,
                doc3,
                today.minusDays(7),
                "Mild Essential Hypertension & Hyperlipidemia",
                "Occasional morning headaches, mild exertional shortness of breath",
                "Lifestyle modifications: Mediterranean diet, 30 mins brisk walking 5 days/week. Atorvastatin 10mg once daily.",
                "Follow-up in 3 months with fasting lipid profile and basic metabolic panel."
        );
        medicalRecordRepository.save(rec1);

        MedicalRecord rec2 = new MedicalRecord(
                pat3,
                doc1,
                today.minusDays(14),
                "Benign Ventricular Ectopy",
                "Transient palpitations post-lunch",
                "Electrolyte panel normal. Advised hydration and reduction of espresso consumption to 1 cup/day.",
                "Patient reassured; Holter revealed 0.4% burden which is benign."
        );
        medicalRecordRepository.save(rec2);

        // 6. Prescriptions
        Prescription pres1 = new Prescription(
                pat1,
                doc3,
                rec1,
                "Atorvastatin",
                "10mg",
                "Once daily at bedtime",
                "90 days",
                "Take with or without food. Avoid excessive grapefruit juice. Report muscle ache if persistent.",
                today.minusDays(7)
        );
        prescriptionRepository.save(pres1);

        Prescription pres2 = new Prescription(
                pat1,
                doc3,
                rec1,
                "Omega-3 Fish Oil",
                "1000mg",
                "Twice daily with meals",
                "60 days",
                "Supports cardiovascular wellness and triglyceride control.",
                today.minusDays(7)
        );
        prescriptionRepository.save(pres2);

        Prescription pres3 = new Prescription(
                pat3,
                doc1,
                rec2,
                "Magnesium Glycinate",
                "200mg",
                "Once daily in evening",
                "30 days",
                "Support neuromuscular relaxation and sleep hygiene.",
                today.minusDays(14)
        );
        prescriptionRepository.save(pres3);

        // 7. Caregiver Access
        CaregiverAccess cg1 = new CaregiverAccess(
                pat1,
                "emma.watson@example.com",
                "Emma Watson",
                "Spouse",
                "VIEW_ALL"
        );
        caregiverAccessRepository.save(cg1);

        // 8. Notifications
        notificationRepository.save(new Notification(
                pat1User,
                "Upcoming Appointment Tomorrow",
                "Friendly reminder: Your consultation with Dr. Sarah Jenkins is scheduled for tomorrow at 10:00 AM. Please arrive 10 minutes early.",
                "APPOINTMENT"
        ));
        notificationRepository.save(new Notification(
                pat1User,
                "New Prescription Added",
                "Dr. Priya Sharma prescribed Atorvastatin 10mg. Check your Prescriptions tab for full instructions.",
                "PRESCRIPTION"
        ));
        notificationRepository.save(new Notification(
                doc1User,
                "Confirmed Consultation Tomorrow",
                "You have an upcoming consultation with John Doe scheduled tomorrow at 10:00 AM.",
                "APPOINTMENT"
        ));
        notificationRepository.save(new Notification(
                doc2User,
                "New Appointment Request",
                "Patient Emma Watson requested an appointment on " + today.plusDays(3) + " at 11:30 AM. Please confirm or reschedule.",
                "APPOINTMENT"
        ));

        // 9. Initial Audit Logs
        auditLogRepository.save(new AuditLog("john.doe@example.com", "LOGIN", "Auth", "Patient John Doe logged in"));
        auditLogRepository.save(new AuditLog("john.doe@example.com", "APPOINTMENT_CREATED", "Appointment:" + appt1.getId(), "Booked appointment with Dr. Sarah Jenkins"));
        auditLogRepository.save(new AuditLog("dr.jenkins@carepulse.com", "APPOINTMENT_STATUS_UPDATED", "Appointment:" + appt1.getId(), "Status set to CONFIRMED"));
        auditLogRepository.save(new AuditLog("dr.sharma@carepulse.com", "PRESCRIPTION_CREATED", "Prescription:" + pres1.getId(), "Prescribed Atorvastatin 10mg"));

        // 10. Emergency Doctor Roster
        seedEmergencyRosterIfEmpty();

        System.out.println("CarePulse demo data successfully seeded!");
    }

    private void seedEmergencyRosterIfEmpty() {
        if (emergencyDoctorRosterRepository.count() > 0) {
            return;
        }

        List<Doctor> doctors = doctorRepository.findAll();
        if (doctors.size() < 2) {
            return;
        }

        System.out.println("Seeding Emergency Doctor Duty Roster demo data...");

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        Doctor docJenkins = doctors.stream().filter(d -> d.getFullName().contains("Jenkins")).findFirst().orElse(doctors.get(0));
        Doctor docVance = doctors.stream().filter(d -> d.getFullName().contains("Vance")).findFirst().orElse(doctors.size() > 1 ? doctors.get(1) : doctors.get(0));
        Doctor docSharma = doctors.stream().filter(d -> d.getFullName().contains("Sharma")).findFirst().orElse(doctors.size() > 2 ? doctors.get(2) : doctors.get(0));

        // TODAY ROSTER:
        // Morning (08:00 - 14:00): Dr. Priya Sharma (General Medicine) & Dr. Sarah Jenkins (Cardiology)
        emergencyDoctorRosterRepository.save(new EmergencyDoctorRoster(
                docSharma,
                today,
                "MORNING",
                LocalTime.of(8, 0),
                LocalTime.of(14, 0),
                "EMERGENCY_DUTY",
                "AVAILABLE"
        ));

        emergencyDoctorRosterRepository.save(new EmergencyDoctorRoster(
                docJenkins,
                today,
                "MORNING",
                LocalTime.of(8, 0),
                LocalTime.of(14, 0),
                "EMERGENCY_DUTY",
                "AVAILABLE"
        ));

        // Evening (14:00 - 20:00): Dr. Marcus Vance (Dermatology) & Dr. Sarah Jenkins (Cardiology)
        emergencyDoctorRosterRepository.save(new EmergencyDoctorRoster(
                docVance,
                today,
                "EVENING",
                LocalTime.of(14, 0),
                LocalTime.of(20, 0),
                "EMERGENCY_DUTY",
                "AVAILABLE"
        ));

        emergencyDoctorRosterRepository.save(new EmergencyDoctorRoster(
                docJenkins,
                today,
                "EVENING",
                LocalTime.of(14, 0),
                LocalTime.of(20, 0),
                "EMERGENCY_DUTY",
                "AVAILABLE"
        ));

        // Night (20:00 - 08:00): Dr. Priya Sharma (General Medicine)
        emergencyDoctorRosterRepository.save(new EmergencyDoctorRoster(
                docSharma,
                today,
                "NIGHT",
                LocalTime.of(20, 0),
                LocalTime.of(8, 0),
                "EMERGENCY_DUTY",
                "AVAILABLE"
        ));

        // TOMORROW ROSTER (Demonstrating daily dynamic rotation):
        // Morning: Dr. Marcus Vance
        emergencyDoctorRosterRepository.save(new EmergencyDoctorRoster(
                docVance,
                tomorrow,
                "MORNING",
                LocalTime.of(8, 0),
                LocalTime.of(14, 0),
                "EMERGENCY_DUTY",
                "AVAILABLE"
        ));

        // Evening: Dr. Priya Sharma
        emergencyDoctorRosterRepository.save(new EmergencyDoctorRoster(
                docSharma,
                tomorrow,
                "EVENING",
                LocalTime.of(14, 0),
                LocalTime.of(20, 0),
                "EMERGENCY_DUTY",
                "AVAILABLE"
        ));

        auditLogRepository.save(new AuditLog("admin@carepulse.com", "EMERGENCY_ROSTER_INITIALIZED", "EmergencyRoster", "Default emergency duty roster seeded for today and tomorrow"));
    }
}
