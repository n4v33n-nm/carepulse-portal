package com.carepulse.dto;

public class DoctorConnectStatsDTO {

    private long totalConversations;
    private long openConversations;
    private long answeredConversations;
    private long resolvedConversations;

    public DoctorConnectStatsDTO() {
    }

    public DoctorConnectStatsDTO(long totalConversations, long openConversations, long answeredConversations, long resolvedConversations) {
        this.totalConversations = totalConversations;
        this.openConversations = openConversations;
        this.answeredConversations = answeredConversations;
        this.resolvedConversations = resolvedConversations;
    }

    public long getTotalConversations() {
        return totalConversations;
    }

    public void setTotalConversations(long totalConversations) {
        this.totalConversations = totalConversations;
    }

    public long getOpenConversations() {
        return openConversations;
    }

    public void setOpenConversations(long openConversations) {
        this.openConversations = openConversations;
    }

    public long getAnsweredConversations() {
        return answeredConversations;
    }

    public void setAnsweredConversations(long answeredConversations) {
        this.answeredConversations = answeredConversations;
    }

    public long getResolvedConversations() {
        return resolvedConversations;
    }

    public void setResolvedConversations(long resolvedConversations) {
        this.resolvedConversations = resolvedConversations;
    }
}

