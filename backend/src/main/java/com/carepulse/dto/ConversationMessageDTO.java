package com.carepulse.dto;

import java.time.LocalDateTime;

public class ConversationMessageDTO {

    private Long id;
    private Long senderId;
    private String senderRole;
    private String senderName;
    private String content;
    private LocalDateTime sentAt;
    private boolean read;

    public ConversationMessageDTO() {
    }

    public ConversationMessageDTO(Long id, Long senderId, String senderRole, String senderName, String content, LocalDateTime sentAt, boolean read) {
        this.id = id;
        this.senderId = senderId;
        this.senderRole = senderRole;
        this.senderName = senderName;
        this.content = content;
        this.sentAt = sentAt;
        this.read = read;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}

