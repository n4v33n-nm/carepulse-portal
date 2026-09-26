package com.carepulse.service;

import com.carepulse.dto.*;
import com.carepulse.entity.Doctor;
import com.carepulse.entity.Patient;
import com.carepulse.entity.User;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.PatientRepository;
import com.carepulse.repository.UserRepository;
import com.carepulse.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                       PatientRepository patientRepository,
                       DoctorRepository doctorRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       AuditLogService auditLogService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AuthResponse registerPatient(PatientRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new BadRequestException("An account with email " + request.getEmail() + " already exists");
        }

        User user = new User(
                request.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(request.getPassword()),
                "PATIENT"
        );
        if (request.getCommunicationPreference() != null) {
            user.setCommunicationPreference(request.getCommunicationPreference());
        }
        User savedUser = userRepository.save(user);

        Patient patient = new Patient(
                savedUser,
                request.getFullName(),
                request.getPhone(),
                request.getDateOfBirth(),
                request.getGender()
        );
        patient.setAddress(request.getAddress());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setEmergencyContact(request.getEmergencyContact());
        Patient savedPatient = patientRepository.save(patient);

        auditLogService.log(savedUser.getEmail(), "REGISTER", "Patient", "Patient registered with ID: " + savedPatient.getId());
        notificationService.createNotification(savedUser, "Welcome to CarePulse!", "Your patient account has been created successfully. You can now book appointments and manage your medical records.", "SYSTEM");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );
        String token = tokenProvider.generateToken(authentication, savedUser.getRole(), savedUser.getId());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedPatient.getFullName(),
                savedPatient.getId(),
                null,
                savedUser.getCommunicationPreference()
        );
    }

    @Transactional
    public AuthResponse registerDoctor(DoctorRegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new BadRequestException("An account with email " + request.getEmail() + " already exists");
        }

        User user = new User(
                request.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(request.getPassword()),
                "DOCTOR"
        );
        if (request.getCommunicationPreference() != null) {
            user.setCommunicationPreference(request.getCommunicationPreference());
        }
        User savedUser = userRepository.save(user);

        Doctor doctor = new Doctor(
                savedUser,
                request.getFullName(),
                request.getPhone(),
                request.getSpecialization(),
                request.getQualification(),
                request.getExperienceYears()
        );
        doctor.setBio(request.getBio());
        if (request.getConsultationFee() != null) {
            doctor.setConsultationFee(request.getConsultationFee());
        }
        Doctor savedDoctor = doctorRepository.save(doctor);

        auditLogService.log(savedUser.getEmail(), "REGISTER", "Doctor", "Doctor registered with ID: " + savedDoctor.getId());
        notificationService.createNotification(savedUser, "Welcome to CarePulse Portal", "Your doctor profile has been created. Set your weekly availability to begin accepting appointments.", "SYSTEM");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );
        String token = tokenProvider.generateToken(authentication, savedUser.getRole(), savedUser.getId());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedDoctor.getFullName(),
                null,
                savedDoctor.getId(),
                savedUser.getCommunicationPreference()
        );
    }

    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BadRequestException("Account is disabled. Please contact system administrator.");
        }

        String fullName = user.getEmail();
        Long patientId = null;
        Long doctorId = null;

        if ("PATIENT".equals(user.getRole())) {
            Patient patient = patientRepository.findByUser(user).orElse(null);
            if (patient != null) {
                fullName = patient.getFullName();
                patientId = patient.getId();
            }
        } else if ("DOCTOR".equals(user.getRole())) {
            Doctor doctor = doctorRepository.findByUser(user).orElse(null);
            if (doctor != null) {
                fullName = doctor.getFullName();
                doctorId = doctor.getId();
            }
        } else if ("ADMIN".equals(user.getRole())) {
            fullName = "System Administrator";
        }

        String token = tokenProvider.generateToken(authentication, user.getRole(), user.getId());
        auditLogService.log(user.getEmail(), "LOGIN", "Auth", "User logged in successfully");

        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                fullName,
                patientId,
                doctorId,
                user.getCommunicationPreference()
        );
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional
    public User updateCommunicationPreference(String email, String preference) {
        User user = getUserByEmail(email);
        user.setCommunicationPreference(preference);
        return userRepository.save(user);
    }

    @Transactional
    public void toggleUserActive(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(!user.isActive());
        userRepository.save(user);
        auditLogService.log("ADMIN", "USER_STATUS_TOGGLE", "User:" + userId, "User active status set to: " + user.isActive());
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
