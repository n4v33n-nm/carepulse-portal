package com.carepulse.service;

import com.carepulse.entity.Appointment;
import com.carepulse.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ScheduledTasksService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasksService.class);

    private final AppointmentWaitlistService waitlistService;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    public ScheduledTasksService(AppointmentWaitlistService waitlistService,
                                 AppointmentRepository appointmentRepository,
                                 NotificationService notificationService) {
        this.waitlistService = waitlistService;
        this.appointmentRepository = appointmentRepository;
        this.notificationService = notificationService;
    }

    // Run every 30 minutes to clean up past waitlist entries
    @Scheduled(fixedRate = 1800000)
    public void runWaitlistCleanup() {
        try {
            int expiredCount = waitlistService.expirePastWaitlists();
            if (expiredCount > 0) {
                log.info("Background job expired {} past waitlist entries", expiredCount);
            }
        } catch (Exception e) {
            log.error("Error during waitlist expiration scheduled task", e);
        }
    }

    // Run daily at 08:00 AM to send reminders for today's confirmed appointments
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyAppointmentReminders() {
        try {
            LocalDate today = LocalDate.now();
            List<Appointment> todayAppts = appointmentRepository.findByAppointmentDateAndStatus(today, "CONFIRMED");
            for (Appointment a : todayAppts) {
                String timeStr = a.getAppointmentTime().toString();
                notificationService.createNotification(
                        a.getPatient().getUser(),
                        "Appointment Reminder: Today at " + timeStr,
                        "Reminder: You have a scheduled consultation today with Dr. " +
                                a.getDoctor().getFullName() + " at " + timeStr + ". Please be on time!",
                        "APPOINTMENT"
                );
            }
            if (!todayAppts.isEmpty()) {
                log.info("Sent {} appointment reminders for today", todayAppts.size());
            }
        } catch (Exception e) {
            log.error("Error sending daily appointment reminders", e);
        }
    }
}
