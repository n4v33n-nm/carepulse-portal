package com.carepulse.dto;

import java.util.List;

public class AiChatResponse {

    private String response;
    private String empathyTone; // SIMPLE, SUPPORTIVE, PROFESSIONAL
    private List<String> suggestedQuestions;
    private String disclaimer = "This AI provides general informational support and does not provide medical diagnosis or replace professional medical advice.";

    public AiChatResponse() {
    }

    public AiChatResponse(String response, String empathyTone, List<String> suggestedQuestions) {
        this.response = response;
        this.empathyTone = empathyTone;
        this.suggestedQuestions = suggestedQuestions;
        this.disclaimer = "This AI provides general informational support and does not provide medical diagnosis or replace professional medical advice.";
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getEmpathyTone() {
        return empathyTone;
    }

    public void setEmpathyTone(String empathyTone) {
        this.empathyTone = empathyTone;
    }

    public List<String> getSuggestedQuestions() {
        return suggestedQuestions;
    }

    public void setSuggestedQuestions(List<String> suggestedQuestions) {
        this.suggestedQuestions = suggestedQuestions;
    }

    public String getDisclaimer() {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer) {
        this.disclaimer = disclaimer;
    }
}
