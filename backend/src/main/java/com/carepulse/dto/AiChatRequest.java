package com.carepulse.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

public class AiChatRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;

    private List<Map<String, String>> history; // [{role: "user", content: "..."}, {role: "assistant", content: "..."}]

    public AiChatRequest() {
    }

    public AiChatRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Map<String, String>> getHistory() {
        return history;
    }

    public void setHistory(List<Map<String, String>> history) {
        this.history = history;
    }
}
