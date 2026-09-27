package com.carepulse.service;

import com.carepulse.dto.EmergencyRequestCreateDTO;
import com.carepulse.dto.EmergencyRequestResponseDTO;
import com.carepulse.dto.EmergencyStatusUpdateRequestDTO;
import com.carepulse.entity.*;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.EmergencyRequestNotFoundException;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.exception.UnauthorizedException;
import com.carepulse.repository.DoctorRepository;
import com.carepulse.repository.EmergencyDoctorRosterRepository;
import com.carepulse.repository.EmergencyRequestRepository;
import com.carepulse.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmergencyRequestService {

    private final EmergencyRequestRepository requestRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final EmergencyDoctorRosterRepository rosterRepository;
    private final EmergencyDoctorAllocationService allocationService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public EmergencyRequestService(EmergencyRequestRepository requestRepository,
                                  PatientRepository patientRepository,
                                  DoctorRepository doctorRepository,
                                  EmergencyDoctorRosterRepository rosterRepository,
                                  EmergencyDoctorAllocationService allocationService,
                                  NotificationService notificationService,
                                  AuditLogService auditLogService) {
        this.requestRepository = requestRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.rosterRepository = rosterRepository;
        this.allocationService = allocationService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public EmergencyRequestResponseDTO createEmergencyRequest(String patientEmail, EmergencyRequestCreateDTO reqDto) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for email: " + patientEmail));

        EmergencyRequest request = new EmergencyRequest(
                patient,
                reqDto.getCategory(),
                reqDto.getDescription()
        );

        // Run allocation algorithm
        EmergencyRequest allocatedRequest = allocationService.allocateDoctor(request);

        // Audit log creation
        auditLogService.log(
                patientEmail,
                "EMERGENCY_REQUEST_CREATED",
                "EmergencyRequest:" + allocatedRequest.getId(),
                "Emergency assistance requested. Category: " + allocatedRequest.getCategory() + ", Status: " + allocatedRequest.getStatus()
        );

        if ("ASSIGNED".equalsIgnoreCase(allocatedRequest.getStatus()) && allocatedRequest.getAssignedDoctor() != null) {
            Doctor doctor = allocatedRequest.getAssignedDoctor();

            // Notify Doctor
            notificationService.createNotification(
                    doctor.getUser(),
                    "New Emergency Request Assigned",
                    "Urgent: Emergency request assigned for patient " + patient.getFullName() +
                            ". Category: " + allocatedRequest.getCategory() +
                            ". Immediate clinical attention required.",
                    "EMERGENCY"
            );

            // Notify Patient
            notificationService.createNotification(
                    patient.getUser(),
                    "Emergency Doctor Assigned",
                    "Emergency request assigned to Dr. " + doctor.getFullName() +
                            " (" + doctor.getSpecialization() + "). Please remain available for rapid clinical response.",
                    "EMERGENCY"
            );

            auditLogService.log(
                    doctor.getUser().getEmail(),
                    "EMERGENCY_DOCTOR_ASSIGNED",
                    "EmergencyRequest:" + allocatedRequest.getId(),
                    "Doctor " + doctor.getFullName() + " assigned to Emergency Request #" + allocatedRequest.getId()
            );
        } else if ("NO_DOCTOR_AVAILABLE".equalsIgnoreCase(allocatedRequest.getStatus())) {
            notificationService.createNotification(
                    patient.getUser(),
                    "No Emergency Doctor Available",
                    "No emergency-duty doctor is currently available. If this is a life-threatening situation, please contact local emergency medical services immediately.",
                    "EMERGENCY"
            );

            auditLogService.log(
                    patientEmail,
                    "EMERGENCY_NO_DOCTOR_AVAILABLE",
                    "EmergencyRequest:" + allocatedRequest.getId(),
                    "Emergency request resulted in NO_DOCTOR_AVAILABLE state"
            );
        }

        return EmergencyRequestResponseDTO.fromEntity(allocatedRequest);
    }

    public List<EmergencyRequestResponseDTO> getPatientEmergencyRequests(String patientEmail) {
        Patient patient = patientRepository.findByUserEmail(patientEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for email: " + patientEmail));

        return requestRepository.findByPatientOrderByRequestTimeDesc(patient)
                .stream()
                .map(EmergencyRequestResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<EmergencyRequestResponseDTO> getAssignedRequestsForDoctor(String doctorEmail) {
        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for email: " + doctorEmail));

        return requestRepository.findByAssignedDoctorOrderByRequestTimeDesc(doctor)
                .stream()
                .map(EmergencyRequestResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public EmergencyRequestResponseDTO getEmergencyRequestById(Long id, String currentUserEmail, String role) {
        EmergencyRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EmergencyRequestNotFoundException("Emergency request not found with ID: " + id));

        // Privacy and Access Control Enforcement
        if ("PATIENT".equalsIgnoreCase(role)) {
            if (!request.getPatient().getUser().getEmail().equalsIgnoreCase(currentUserEmail)) {
                throw new UnauthorizedException("You are not authorized to view another patient's emergency request");
            }
        } else if ("DOCTOR".equalsIgnoreCase(role)) {
            if (request.getAssignedDoctor() != null &&
                    !request.getAssignedDoctor().getUser().getEmail().equalsIgnoreCase(currentUserEmail)) {
                throw new UnauthorizedException("You are not authorized to view another doctor's assigned emergency request");
            }
        }

        return EmergencyRequestResponseDTO.fromEntity(request);
    }

    @Transactional
    public EmergencyRequestResponseDTO updateRequestStatus(Long id, String doctorEmail, EmergencyStatusUpdateRequestDTO req) {
        EmergencyRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EmergencyRequestNotFoundException("Emergency request not found with ID: " + id));

        Doctor doctor = doctorRepository.findByUserEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for email: " + doctorEmail));

        if (request.getAssignedDoctor() == null || !request.getAssignedDoctor().getId().equals(doctor.getId())) {
            throw new UnauthorizedException("You are not the assigned doctor for this emergency request");
        }

        String newStatus = req.getStatus().toUpperCase();
        request.setStatus(newStatus);
        if (req.getDoctorNotes() != null) {
            request.setDoctorNotes(req.getDoctorNotes());
        }

        EmergencyRequest updated = requestRepository.save(request);

        // When emergency case is COMPLETED or CANCELLED, reset doctor's availability if no other active cases
        if ("COMPLETED".equalsIgnoreCase(newStatus) || "CANCELLED".equalsIgnoreCase(newStatus)) {
            long remainingActiveCases = requestRepository.countByAssignedDoctorAndStatusIn(
                    doctor,
                    List.of("ASSIGNED", "IN_PROGRESS")
            );

            if (remainingActiveCases == 0) {
                doctor.setAvailabilityStatus("AVAILABLE");
                doctorRepository.save(doctor);

                LocalDate today = LocalDate.now();
                LocalDate yesterday = today.minusDays(1);
                List<EmergencyDoctorRoster> rosters = rosterRepository.findTodayDutyForDoctor(doctor.getId(), today, yesterday);
                for (EmergencyDoctorRoster r : rosters) {
                    if ("IN_CONSULTATION".equalsIgnoreCase(r.getDoctorAvailabilityStatus()) ||
                        "BUSY".equalsIgnoreCase(r.getDoctorAvailabilityStatus())) {
                        r.setDoctorAvailabilityStatus("AVAILABLE");
                        rosterRepository.save(r);
                    }
                }
            }

            notificationService.createNotification(
                    request.getPatient().getUser(),
                    "Emergency Case " + newStatus,
                    "Your emergency case has been marked as " + newStatus + " by Dr. " + doctor.getFullName() + ".",
                    "EMERGENCY"
            );
        }

        auditLogService.log(
                doctorEmail,
                "EMERGENCY_STATUS_UPDATED",
                "EmergencyRequest:" + id,
                "Doctor " + doctor.getFullName() + " updated Emergency Request #" + id + " status to " + newStatus
        );

        return EmergencyRequestResponseDTO.fromEntity(updated);
    }

    @Transactional
    public EmergencyRequestResponseDTO cancelPatientRequest(Long id, String patientEmail) {
        EmergencyRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EmergencyRequestNotFoundException("Emergency request not found with ID: " + id));

        if (!request.getPatient().getUser().getEmail().equalsIgnoreCase(patientEmail)) {
            throw new UnauthorizedException("You can only cancel your own emergency requests");
        }

        if ("COMPLETED".equalsIgnoreCase(request.getStatus()) || "CANCELLED".equalsIgnoreCase(request.getStatus())) {
            throw new BadRequestException("Request is already in state: " + request.getStatus());
        }

        request.setStatus("CANCELLED");
        EmergencyRequest updated = requestRepository.save(request);

        Doctor doctor = request.getAssignedDoctor();
        if (doctor != null) {
            long remainingActiveCases = requestRepository.countByAssignedDoctorAndStatusIn(
                    doctor,
                    List.of("ASSIGNED", "IN_PROGRESS")
            );

            if (remainingActiveCases == 0) {
                doctor.setAvailabilityStatus("AVAILABLE");
                doctorRepository.save(doctor);

                LocalDate today = LocalDate.now();
                LocalDate yesterday = today.minusDays(1);
                List<EmergencyDoctorRoster> rosters = rosterRepository.findTodayDutyForDoctor(doctor.getId(), today, yesterday);
                for (EmergencyDoctorRoster r : rosters) {
                    r.setDoctorAvailabilityStatus("AVAILABLE");
                    rosterRepository.save(r);
                }
            }

            notificationService.createNotification(
                    doctor.getUser(),
                    "Emergency Request Cancelled",
                    "Emergency Request #" + id + " was cancelled by the patient.",
                    "EMERGENCY"
            );
        }

        auditLogService.log(
                patientEmail,
                "EMERGENCY_REQUEST_CANCELLED",
                "EmergencyRequest:" + id,
                "Patient cancelled Emergency Request #" + id
        );

        return EmergencyRequestResponseDTO.fromEntity(updated);
    }

    public List<EmergencyRequestResponseDTO> getAllEmergencyRequests() {
        return requestRepository.findByOrderByRequestTimeDesc()
                .stream()
                .map(EmergencyRequestResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getEmergencyStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("WAITING", requestRepository.countByStatus("WAITING"));
        stats.put("ASSIGNED", requestRepository.countByStatus("ASSIGNED"));
        stats.put("IN_PROGRESS", requestRepository.countByStatus("IN_PROGRESS"));
        stats.put("COMPLETED", requestRepository.countByStatus("COMPLETED"));
        stats.put("CANCELLED", requestRepository.countByStatus("CANCELLED"));
        stats.put("NO_DOCTOR_AVAILABLE", requestRepository.countByStatus("NO_DOCTOR_AVAILABLE"));
        return stats;
    }
}
