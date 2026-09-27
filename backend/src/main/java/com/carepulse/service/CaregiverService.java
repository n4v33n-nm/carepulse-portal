package com.carepulse.service;

import com.carepulse.dto.CaregiverRequest;
import com.carepulse.entity.CaregiverAccess;
import com.carepulse.entity.Patient;
import com.carepulse.entity.User;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.repository.CaregiverAccessRepository;
import com.carepulse.repository.PatientRepository;
import com.carepulse.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CaregiverService {

    private final CaregiverAccessRepository caregiverAccessRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public CaregiverService(CaregiverAccessRepository caregiverAccessRepository,
                            PatientRepository patientRepository,
                            UserRepository userRepository,
                            NotificationService notificationService,
                            AuditLogService auditLogService) {
        this.caregiverAccessRepository = caregiverAccessRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CaregiverAccess grantAccess(String patientEmail, CaregiverRequest request) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        if (patientEmail.equalsIgnoreCase(request.getCaregiverEmail().trim())) {
            throw new BadRequestException("You cannot assign yourself as your own caregiver");
        }

        // Check if active access already exists
        caregiverAccessRepository.findByPatientIdAndCaregiverEmailAndStatus(
                patient.getId(), request.getCaregiverEmail().toLowerCase().trim(), "ACTIVE"
        ).ifPresent(existing -> {
            throw new BadRequestException("Caregiver access is already active for " + request.getCaregiverEmail());
        });

        CaregiverAccess access = new CaregiverAccess(
                patient,
                request.getCaregiverEmail().toLowerCase().trim(),
                request.getCaregiverName(),
                request.getRelationship(),
                request.getPermission()
        );

        CaregiverAccess saved = caregiverAccessRepository.save(access);

        // Notify user if registered
        userRepository.findByEmail(request.getCaregiverEmail().toLowerCase().trim()).ifPresent(caregiverUser -> {
            notificationService.createNotification(
                    caregiverUser,
                    "Caregiver Access Granted",
                    patient.getFullName() + " granted you caregiver access with permission: " + access.getPermission(),
                    "CAREGIVER"
            );
        });

        notificationService.createNotification(
                patient.getUser(),
                "Caregiver Added",
                "You granted " + request.getCaregiverEmail() + " access (" + request.getPermission() + "). You can revoke access anytime.",
                "CAREGIVER"
        );

        auditLogService.log(patientEmail, "CAREGIVER_GRANTED", "Caregiver:" + saved.getId(), "Granted to: " + request.getCaregiverEmail());

        return saved;
    }

    public boolean isAuthorizedCaregiver(Long patientId, String caregiverEmail) {
        if (patientId == null || caregiverEmail == null || caregiverEmail.isBlank()) {
            return false;
        }
        return caregiverAccessRepository.findByPatientIdAndCaregiverEmailAndStatus(
                patientId, caregiverEmail.toLowerCase().trim(), "ACTIVE"
        ).isPresent();
    }

    public List<CaregiverAccess> getCaregiversForPatient(String patientEmail) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
        return caregiverAccessRepository.findByPatientOrderByGrantedAtDesc(patient);
    }

    public List<CaregiverAccess> getPatientsForCaregiver(String caregiverEmail) {
        return caregiverAccessRepository.findByCaregiverEmailAndStatus(caregiverEmail.toLowerCase().trim(), "ACTIVE");
    }

    @Transactional
    public CaregiverAccess revokeAccess(Long accessId, String patientEmail) {
        CaregiverAccess access = caregiverAccessRepository.findById(accessId)
                .orElseThrow(() -> new ResourceNotFoundException("Caregiver access record not found"));

        if (!access.getPatient().getUser().getEmail().equalsIgnoreCase(patientEmail)) {
            throw new BadRequestException("You can only revoke caregiver access granted by your account");
        }

        access.setStatus("REVOKED");
        access.setRevokedAt(LocalDateTime.now());
        CaregiverAccess saved = caregiverAccessRepository.save(access);

        notificationService.createNotification(
                access.getPatient().getUser(),
                "Caregiver Access Revoked",
                "Access for " + access.getCaregiverEmail() + " has been revoked.",
                "CAREGIVER"
        );

        auditLogService.log(patientEmail, "CAREGIVER_REVOKED", "Caregiver:" + saved.getId(), "Revoked access for: " + access.getCaregiverEmail());

        return saved;
    }
}
