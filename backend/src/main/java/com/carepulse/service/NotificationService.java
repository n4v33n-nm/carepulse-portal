package com.carepulse.service;

import com.carepulse.entity.Notification;
import com.carepulse.entity.User;
import com.carepulse.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification createNotification(User user, String title, String message, String type) {
        Notification notification = new Notification(user, title, message, type);
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId, String role) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new com.carepulse.exception.ResourceNotFoundException("Notification not found with ID: " + notificationId));
        if (!"ADMIN".equalsIgnoreCase(role) && (n.getUser() == null || !n.getUser().getId().equals(userId))) {
            throw new org.springframework.security.access.AccessDeniedException("You do not have permission to mark this notification as read");
        }
        n.setRead(true);
        notificationRepository.save(n);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
