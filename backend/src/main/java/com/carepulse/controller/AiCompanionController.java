package com.carepulse.controller;

import com.carepulse.dto.AiChatRequest;
import com.carepulse.dto.AiChatResponse;
import com.carepulse.service.AiHealthCompanionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiCompanionController {

    private final AiHealthCompanionService aiHealthCompanionService;

    public AiCompanionController(AiHealthCompanionService aiHealthCompanionService) {
        this.aiHealthCompanionService = aiHealthCompanionService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            Authentication authentication,
            @Valid @RequestBody AiChatRequest request) {
        String email = authentication != null ? authentication.getName() : null;
        AiChatResponse response = aiHealthCompanionService.processMessage(email, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/disclaimer")
    public ResponseEntity<Map<String, String>> getDisclaimer() {
        return ResponseEntity.ok(Map.of("disclaimer", aiHealthCompanionService.getDisclaimer()));
    }
}
