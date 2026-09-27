package com.carepulse.service;

import com.carepulse.dto.DoctorEmergencyWorkloadDTO;
import com.carepulse.dto.EmergencyAnalyticsDTO;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmergencyRequestService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyRequestService.class);

    private final EmergencyRequestRepository requestRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final EmergencyDoctorRosterRepository rosterRepository;
    private final EmergencyDoctorAllocationService allocationService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Value("${emergency.assignment.timeout.minutes:10}")
    private int assignmentTimeoutMinutes;

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

        String priority = (reqDto.getPriority() != null && !reqDto.getPriority().isBlank())
                ? reqDto.getPriority().trim().toUpperCase() : "NORMAL";
        if (!"URGENT".equals(priority) && !"NORMAL".equals(priority)) {
            priority = "NORMAL";
        }

        EmergencyRequest request = new EmergencyRequest(
                patient,
                reqDto.getCategory(),
                reqDto.getDescription(),
                priority
        );

        // Run deterministic allocation algorithm
        EmergencyRequest allocatedRequest = allocationService.allocateDoctor(request);

        // Audit log creation
        auditLogService.log(
                patientEmail,
                "EMERGENCY_REQUEST_CREATED",
                "EmergencyRequest:" + allocatedRequest.getId(),
                "Emergency assistance requested. Priority: " + priority + ", Category: " + allocatedRequest.getCategory() + ", Status: " + allocatedRequest.getStatus()
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
        } else if ("WAITING".equalsIgnoreCase(allocatedRequest.getStatus())) {
            // Requirement 8: All doctors occupied, placed in waiting queue
            notificationService.createNotification(
                    patient.getUser(),
                    "Emergency Request Waiting in Queue",
                    "All emergency-duty doctors are currently occupied. You have been placed in the triage queue (Priority: " + priority + "). If this is a life-threatening emergency, please call 911 / 112 immediately.",
                    "EMERGENCY"
            );

            // Notify on-duty doctors of waiting emergency queue
            LocalDate today = LocalDate.now();
            List<EmergencyDoctorRoster> activeRosters = rosterRepository.findAvailableEmergencyRosters(today, today.minusDays(1));
            for (EmergencyDoctorRoster r : activeRosters) {
                if (r.getDoctor() != null && r.getDoctor().getUser() != null) {
                    notificationService.createNotification(
                            r.getDoctor().getUser(),
                            "Emergency Request Waiting in Queue",
                            "New emergency request is waiting in the triage queue for category: " + allocatedRequest.getCategory(),
                            "EMERGENCY"
                    );
                }
            }
        } else if ("NO_DOCTOR_AVAILABLE".equalsIgnoreCase(allocatedRequest.getStatus())) {
            notificationService.createNotification(
                    patient.getUser(),
                    "No Emergency Doctor Available",
                    "No emergency-duty doctor is currently rostered for this shift. If this is a life-threatening situation, please contact local emergency medical services (911 / 112) immediately.",
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

        String currentStatus = request.getStatus() != null ? request.getStatus().toUpperCase() : "WAITING";
        String newStatus = req.getStatus() != null ? req.getStatus().toUpperCase() : "";

        // State Machine Validation (Requirement 6)
        if ("COMPLETED".equals(currentStatus)) {
            throw new BadRequestException("Invalid transition: Emergency case is already COMPLETED and cannot be modified.");
        }
        if ("CANCELLED".equals(currentStatus)) {
            throw new BadRequestException("Invalid transition: Emergency case is CANCELLED and cannot be modified.");
        }
        if ("NO_DOCTOR_AVAILABLE".equals(currentStatus)) {
            throw new BadRequestException("Invalid transition: Emergency case has NO_DOCTOR_AVAILABLE.");
        }

        if ("IN_PROGRESS".equals(newStatus)) {
            if (!"ASSIGNED".equals(currentStatus)) {
                throw new BadRequestException("Invalid transition: Cannot start emergency case unless it is in ASSIGNED status. Current status: " + currentStatus);
            }
        } else if ("COMPLETED".equals(newStatus)) {
            if (!"IN_PROGRESS".equals(currentStatus) && !"ASSIGNED".equals(currentStatus)) {
                throw new BadRequestException("Invalid transition: Cannot complete emergency case unless it is ASSIGNED or IN_PROGRESS. Current status: " + currentStatus);
            }
        } else if (!"CANCELLED".equals(newStatus)) {
            throw new BadRequestException("Unsupported status transition to: " + newStatus);
        }

        request.setStatus(newStatus);
        if (req.getDoctorNotes() != null) {
            request.setDoctorNotes(req.getDoctorNotes());
        }

        // When doctor starts case (BUSY -> IN_CONSULTATION, Requirement 6 & 14)
        if ("IN_PROGRESS".equals(newStatus)) {
            request.setStartedTime(LocalDateTime.now());

            doctor.setAvailabilityStatus("IN_CONSULTATION");
            doctorRepository.save(doctor);

            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            List<EmergencyDoctorRoster> rosters = rosterRepository.findTodayDutyForDoctor(doctor.getId(), today, yesterday);
            for (EmergencyDoctorRoster r : rosters) {
                r.setDoctorAvailabilityStatus("IN_CONSULTATION");
                rosterRepository.save(r);
            }

            notificationService.createNotification(
                    request.getPatient().getUser(),
                    "Emergency In Progress",
                    "Dr. " + doctor.getFullName() + " has started handling your emergency case.",
                    "EMERGENCY"
            );

            auditLogService.log(
                    doctorEmail,
                    "EMERGENCY_REQUEST_STARTED",
                    "EmergencyRequest:" + id,
                    "Doctor " + doctor.getFullName() + " started handling Emergency Request #" + id
            );
        }

        // When emergency case is COMPLETED (IN_CONSULTATION -> AVAILABLE, Requirement 6 & 14)
        if ("COMPLETED".equalsIgnoreCase(newStatus)) {
            request.setCompletedTime(LocalDateTime.now());

            long remainingActiveCases = requestRepository.countByAssignedDoctorAndStatusIn(
                    doctor,
                    List.of("ASSIGNED", "IN_PROGRESS")
            ) - 1; // excluding this current request which is now completed

            if (remainingActiveCases <= 0) {
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
                    request.getPatient().getUser(),
                    "Emergency Completed",
                    "Emergency request completed by Dr. " + doctor.getFullName() + ".",
                    "EMERGENCY"
            );

            auditLogService.log(
                    doctorEmail,
                    "EMERGENCY_REQUEST_COMPLETED",
                    "EmergencyRequest:" + id,
                    "Doctor " + doctor.getFullName() + " completed Emergency Request #" + id
            );
        }

        if ("CANCELLED".equalsIgnoreCase(newStatus)) {
            long remainingActiveCases = requestRepository.countByAssignedDoctorAndStatusIn(
                    doctor,
                    List.of("ASSIGNED", "IN_PROGRESS")
            ) - 1;

            if (remainingActiveCases <= 0) {
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
        }

        EmergencyRequest updated = requestRepository.save(request);

        // Requirement 8: If doctor became AVAILABLE upon completion, check waiting emergency queue
        if (("COMPLETED".equalsIgnoreCase(newStatus) || "CANCELLED".equalsIgnoreCase(newStatus))
                && "AVAILABLE".equalsIgnoreCase(doctor.getAvailabilityStatus())) {
            Optional<EmergencyRequest> waitingAssigned = allocationService.assignWaitingRequestToDoctor(
                    doctor, LocalDate.now(), LocalTime.now());

            if (waitingAssigned.isPresent()) {
                EmergencyRequest nextCase = waitingAssigned.get();
                notificationService.createNotification(
                        doctor.getUser(),
                        "New Emergency Case from Queue",
                        "Patient " + nextCase.getPatient().getFullName() + " waiting in queue was assigned to you.",
                        "EMERGENCY"
                );
                notificationService.createNotification(
                        nextCase.getPatient().getUser(),
                        "Emergency Doctor Assigned",
                        "Dr. " + doctor.getFullName() + " is now assigned to your emergency case from the queue.",
                        "EMERGENCY"
                );
                auditLogService.log(
                        doctorEmail,
                        "EMERGENCY_DOCTOR_ASSIGNED",
                        "EmergencyRequest:" + nextCase.getId(),
                        "Assigned waiting request #" + nextCase.getId() + " to newly available Doctor " + doctor.getFullName()
                );
            }
        }

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

                // Check waiting queue for newly available doctor
                allocationService.assignWaitingRequestToDoctor(doctor, LocalDate.now(), LocalTime.now());
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

    /**
     * Requirement 11: Emergency request assignment timeout and recovery logic.
     * Requeues unstarted requests if physician does not start within configurable timeout.
     */
    @Scheduled(fixedDelayString = "${emergency.recovery.interval.ms:60000}")
    @Transactional
    public int recoverTimedOutAssignments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(assignmentTimeoutMinutes);
        List<EmergencyRequest> timedOut = requestRepository.findByStatusAndAssignedTimeBefore("ASSIGNED", cutoff);

        int count = 0;
        for (EmergencyRequest req : timedOut) {
            Doctor prevDoc = req.getAssignedDoctor();
            req.setStatus("WAITING");
            req.setAssignedDoctor(null);
            req.setAssignedTime(null);
            EmergencyRequest requeued = requestRepository.save(req);

            if (prevDoc != null) {
                long remaining = requestRepository.countByAssignedDoctorAndStatusIn(
                        prevDoc, List.of("ASSIGNED", "IN_PROGRESS"));
                if (remaining == 0) {
                    prevDoc.setAvailabilityStatus("AVAILABLE");
                    doctorRepository.save(prevDoc);

                    List<EmergencyDoctorRoster> rosters = rosterRepository.findTodayDutyForDoctor(
                            prevDoc.getId(), LocalDate.now(), LocalDate.now().minusDays(1));
                    for (EmergencyDoctorRoster r : rosters) {
                        r.setDoctorAvailabilityStatus("AVAILABLE");
                        rosterRepository.save(r);
                    }
                }

                notificationService.createNotification(
                        prevDoc.getUser(),
                        "Emergency Case Timeout",
                        "Emergency Request #" + req.getId() + " was returned to queue due to response timeout (" + assignmentTimeoutMinutes + " mins).",
                        "EMERGENCY"
                );
            }

            notificationService.createNotification(
                    req.getPatient().getUser(),
                    "Emergency Request Re-queued",
                    "Assigned physician was occupied and did not start consultation within " + assignmentTimeoutMinutes + " minutes. Your request has been returned to the priority queue for immediate reallocation.",
                    "EMERGENCY"
            );

            auditLogService.log(
                    "SYSTEM",
                    "EMERGENCY_REQUEST_REQUEUED",
                    "EmergencyRequest:" + req.getId(),
                    "Requeued request #" + req.getId() + " after assignment response timeout of " + assignmentTimeoutMinutes + " minutes."
            );

            // Attempt immediate reallocation
            allocationService.allocateDoctor(requeued);
            count++;
        }
        if (count > 0) {
            log.info("Recovered and requeued {} timed-out emergency assignments", count);
        }
        return count;
    }

    /**
     * Requirement 12: Admin Emergency Analytics with real database records.
     */
    public EmergencyAnalyticsDTO getEmergencyAnalytics(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

        long total = requestRepository.countByRequestTimeBetween(dayStart, dayEnd);
        long waiting = requestRepository.countByStatusAndRequestTimeBetween("WAITING", dayStart, dayEnd);
        long assigned = requestRepository.countByStatusAndRequestTimeBetween("ASSIGNED", dayStart, dayEnd);
        long inProgress = requestRepository.countByStatusAndRequestTimeBetween("IN_PROGRESS", dayStart, dayEnd);
        long completed = requestRepository.countByStatusAndRequestTimeBetween("COMPLETED", dayStart, dayEnd);
        long cancelled = requestRepository.countByStatusAndRequestTimeBetween("CANCELLED", dayStart, dayEnd);
        long noDoctor = requestRepository.countByStatusAndRequestTimeBetween("NO_DOCTOR_AVAILABLE", dayStart, dayEnd);

        List<Doctor> allDoctors = doctorRepository.findAll().stream()
                .filter(d -> d.getUser() != null && d.getUser().isActive() && "DOCTOR".equalsIgnoreCase(d.getUser().getRole()))
                .sorted(Comparator.comparing(Doctor::getFullName))
                .collect(Collectors.toList());

        List<EmergencyDoctorRoster> todayRosters = rosterRepository.findByRosterDate(targetDate);
        LocalDate weekStart = targetDate.minusDays(7);

        List<DoctorEmergencyWorkloadDTO> doctorWorkloads = new ArrayList<>();
        for (Doctor doc : allDoctors) {
            List<EmergencyDoctorRoster> docRostersOnDate = todayRosters.stream()
                    .filter(r -> r.getDoctor().getId().equals(doc.getId()))
                    .collect(Collectors.toList());

            boolean isDuty = !docRostersOnDate.isEmpty() && docRostersOnDate.stream().anyMatch(r -> "EMERGENCY_DUTY".equalsIgnoreCase(r.getDutyStatus()));

            String shiftName = docRostersOnDate.isEmpty() ? "NONE" : docRostersOnDate.stream().map(EmergencyDoctorRoster::getShiftName).collect(Collectors.joining(", "));
            String shiftHours = docRostersOnDate.isEmpty() ? "N/A" : docRostersOnDate.stream()
                    .map(r -> (r.getShiftStart() != null ? r.getShiftStart().toString().substring(0, 5) : "") + " - " + (r.getShiftEnd() != null ? r.getShiftEnd().toString().substring(0, 5) : ""))
                    .collect(Collectors.joining("; "));

            long todayCases = requestRepository.countByAssignedDoctorAndRequestTimeBetween(doc, dayStart, dayEnd);
            long todayDone = requestRepository.countByAssignedDoctorAndStatusAndRequestTimeBetween(doc, "COMPLETED", dayStart, dayEnd);
            long todayActive = requestRepository.countByAssignedDoctorAndStatusInAndRequestTimeBetween(doc, List.of("ASSIGNED", "IN_PROGRESS"), dayStart, dayEnd);
            long past7Shifts = rosterRepository.countByDoctorIdAndRosterDateBetween(doc.getId(), weekStart, targetDate);

            doctorWorkloads.add(new DoctorEmergencyWorkloadDTO(
                    doc.getId(),
                    doc.getFullName(),
                    doc.getSpecialization(),
                    doc.getAvailabilityStatus(),
                    isDuty,
                    shiftName,
                    shiftHours,
                    todayCases,
                    todayDone,
                    todayActive,
                    past7Shifts
            ));
        }

        return new EmergencyAnalyticsDTO(
                targetDate,
                total,
                waiting,
                assigned,
                inProgress,
                completed,
                cancelled,
                noDoctor,
                doctorWorkloads
        );
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
