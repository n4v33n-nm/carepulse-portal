package com.carepulse.service;

import com.carepulse.dto.*;
import com.carepulse.entity.*;
import com.carepulse.exception.BadRequestException;
import com.carepulse.exception.ResourceNotFoundException;
import com.carepulse.exception.UnauthorizedException;
import com.carepulse.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DoctorConnectService {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public DoctorConnectService(ConversationRepository conversationRepository,
                                ConversationMessageRepository messageRepository,
                                PatientRepository patientRepository,
                                DoctorRepository doctorRepository,
                                AppointmentRepository appointmentRepository,
                                UserRepository userRepository,
                                NotificationService notificationService,
                                AuditLogService auditLogService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ConversationResponseDTO createConversation(String userEmail, CreateConversationRequest request) {
        Patient patient = patientRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for email: " + userEmail));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + request.getAppointmentId()));
            if (!appointment.getPatient().getId().equals(patient.getId())) {
                throw new BadRequestException("Appointment does not belong to the authenticated patient");
            }
        }

        Conversation conversation = new Conversation(patient, doctor, appointment, request.getSubject().trim());
        conversation = conversationRepository.save(conversation);

        User sender = patient.getUser();
        ConversationMessage initialMessage = new ConversationMessage(
                conversation,
                sender,
                "PATIENT",
                patient.getFullName(),
                request.getMessage().trim()
        );
        messageRepository.save(initialMessage);
        conversation.getMessages().add(initialMessage);

        // Notify Doctor
        if (doctor.getUser() != null) {
            notificationService.createNotification(
                    doctor.getUser(),
                    "New Patient Question",
                    "Patient " + patient.getFullName() + " submitted an inquiry: \"" + conversation.getSubject() + "\"",
                    "COMMUNICATION"
            );
        }

        auditLogService.log(
                userEmail,
                "CREATE_PATIENT_QUERY",
                "Conversation #" + conversation.getId(),
                "Patient created DoctorConnect query to Dr. " + doctor.getFullName()
        );

        return mapToDTO(conversation, "PATIENT");
    }

    @Transactional
    public List<ConversationResponseDTO> getUserConversations(String userEmail, String role, String statusFilter) {
        List<Conversation> list;

        if ("PATIENT".equalsIgnoreCase(role)) {
            Patient patient = patientRepository.findByUserEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));
            list = conversationRepository.findByPatientOrderByUpdatedAtDesc(patient);
        } else if ("DOCTOR".equalsIgnoreCase(role)) {
            Doctor doctor = doctorRepository.findByUserEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
            if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                list = conversationRepository.findByDoctorAndStatusOrderByUpdatedAtDesc(doctor, statusFilter.toUpperCase());
            } else {
                list = conversationRepository.findByDoctorOrderByUpdatedAtDesc(doctor);
            }
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                list = conversationRepository.findByStatusOrderByUpdatedAtDesc(statusFilter.toUpperCase());
            } else {
                list = conversationRepository.findAllByOrderByUpdatedAtDesc();
            }
        } else {
            throw new UnauthorizedException("Invalid role for DoctorConnect");
        }

        return list.stream()
                .map(c -> mapToDTO(c, role))
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationResponseDTO getConversationById(Long id, String userEmail, String role) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + id));

        verifyAccess(conversation, userEmail, role);

        // Mark incoming messages as read
        String currentRole = role.toUpperCase();
        messageRepository.markIncomingMessagesAsRead(conversation, currentRole);

        return mapToDTO(conversation, role);
    }

    @Transactional
    public ConversationMessageDTO sendMessage(Long conversationId, String userEmail, String role, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + conversationId));

        verifyAccess(conversation, userEmail, role);

        User sender = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String senderRole = role.toUpperCase();
        String senderName;
        if ("PATIENT".equals(senderRole)) {
            senderName = conversation.getPatient().getFullName();
            conversation.setStatus("OPEN");
            // Notify doctor
            if (conversation.getDoctor().getUser() != null) {
                notificationService.createNotification(
                        conversation.getDoctor().getUser(),
                        "Follow-up Patient Message",
                        "Patient " + senderName + " replied on: \"" + conversation.getSubject() + "\"",
                        "COMMUNICATION"
                );
            }
        } else if ("DOCTOR".equals(senderRole)) {
            String docName = conversation.getDoctor().getFullName();
            senderName = docName.startsWith("Dr.") ? docName : "Dr. " + docName;
            conversation.setStatus("ANSWERED");
            // Notify patient
            if (conversation.getPatient().getUser() != null) {
                notificationService.createNotification(
                        conversation.getPatient().getUser(),
                        "Doctor Response Received",
                        senderName + " replied to your inquiry: \"" + conversation.getSubject() + "\"",
                        "COMMUNICATION"
                );
            }
        } else {
            senderName = "CarePulse Clinical Admin";
        }

        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        ConversationMessage message = new ConversationMessage(
                conversation,
                sender,
                senderRole,
                senderName,
                request.getContent().trim()
        );
        conversation.addMessage(message);
        message = messageRepository.save(message);
        conversationRepository.save(conversation);

        auditLogService.log(
                userEmail,
                "SEND_DOCTOR_CONNECT_MESSAGE",
                "Conversation #" + conversationId,
                senderRole + " sent message on conversation"
        );

        return new ConversationMessageDTO(
                message.getId(),
                sender.getId(),
                message.getSenderRole(),
                message.getSenderName(),
                message.getContent(),
                message.getSentAt(),
                message.isRead()
        );
    }

    @Transactional
    public ConversationResponseDTO updateStatus(Long conversationId, String userEmail, String role, String newStatus) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with ID: " + conversationId));

        verifyAccess(conversation, userEmail, role);

        String upperStatus = newStatus.toUpperCase().trim();
        if (!List.of("OPEN", "ANSWERED", "RESOLVED").contains(upperStatus)) {
            throw new BadRequestException("Invalid status: " + newStatus + ". Must be OPEN, ANSWERED, or RESOLVED");
        }

        conversation.setStatus(upperStatus);
        conversation.setUpdatedAt(LocalDateTime.now());
        conversation = conversationRepository.save(conversation);

        auditLogService.log(
                userEmail,
                "UPDATE_CONVERSATION_STATUS",
                "Conversation #" + conversationId,
                "Status updated to " + upperStatus
        );

        return mapToDTO(conversation, role);
    }

    @Transactional(readOnly = true)
    public DoctorConnectStatsDTO getStats() {
        long total = conversationRepository.count();
        long open = conversationRepository.countByStatus("OPEN");
        long answered = conversationRepository.countByStatus("ANSWERED");
        long resolved = conversationRepository.countByStatus("RESOLVED");
        return new DoctorConnectStatsDTO(total, open, answered, resolved);
    }

    private void verifyAccess(Conversation conversation, String userEmail, String role) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return; // Admins have supervisory access
        }
        if ("PATIENT".equalsIgnoreCase(role)) {
            if (!conversation.getPatient().getUser().getEmail().equalsIgnoreCase(userEmail)) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not have permission to view this conversation");
            }
            return;
        }
        if ("DOCTOR".equalsIgnoreCase(role)) {
            if (!conversation.getDoctor().getUser().getEmail().equalsIgnoreCase(userEmail)) {
                throw new org.springframework.security.access.AccessDeniedException("Access denied: You do not have permission to view this conversation");
            }
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException("Access denied to conversation");
    }

    private ConversationResponseDTO mapToDTO(Conversation c, String viewerRole) {
        ConversationResponseDTO dto = new ConversationResponseDTO();
        dto.setId(c.getId());
        dto.setSubject(c.getSubject());
        dto.setStatus(c.getStatus());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());

        // Patient info
        Patient p = c.getPatient();
        dto.setPatientId(p.getId());
        dto.setPatientName(p.getFullName());
        dto.setPatientGender(p.getGender());
        dto.setPatientBloodGroup(p.getBloodGroup());
        dto.setPatientDob(p.getDateOfBirth());
        if (p.getUser() != null) {
            dto.setPatientEmail(p.getUser().getEmail());
        }
        dto.setPatientPhone(p.getPhone());

        // Doctor info
        Doctor d = c.getDoctor();
        dto.setDoctorId(d.getId());
        dto.setDoctorName(d.getFullName());
        dto.setDoctorSpecialization(d.getSpecialization());

        // Appointment info if linked
        if (c.getAppointment() != null) {
            dto.setAppointmentId(c.getAppointment().getId());
            dto.setAppointmentDate(c.getAppointment().getAppointmentDate());
        }

        // Messages
        List<ConversationMessage> msgs = c.getMessages();
        if (msgs != null && !msgs.isEmpty()) {
            dto.setMessages(msgs.stream().map(m -> new ConversationMessageDTO(
                    m.getId(),
                    m.getSender() != null ? m.getSender().getId() : null,
                    m.getSenderRole(),
                    m.getSenderName(),
                    m.getContent(),
                    m.getSentAt(),
                    m.isRead()
            )).collect(Collectors.toList()));

            // Last message snippet
            ConversationMessage lastMsg = msgs.get(msgs.size() - 1);
            String snippet = lastMsg.getContent();
            if (snippet.length() > 80) {
                snippet = snippet.substring(0, 77) + "...";
            }
            dto.setLastMessageSnippet(snippet);
        } else {
            dto.setMessages(new ArrayList<>());
            dto.setLastMessageSnippet("");
        }

        // Unread count
        long unread = messageRepository.countByConversationAndSenderRoleNotAndReadFalse(c, viewerRole.toUpperCase());
        dto.setUnreadMessagesCount(unread);

        return dto;
    }
}

