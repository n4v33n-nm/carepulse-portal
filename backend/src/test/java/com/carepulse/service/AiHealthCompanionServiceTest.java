package com.carepulse.service;

import com.carepulse.dto.AiChatRequest;
import com.carepulse.dto.AiChatResponse;
import com.carepulse.entity.User;
import com.carepulse.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AiHealthCompanionServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AiHealthCompanionService aiService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("patient@carepulse.com", "password", "PATIENT");
        testUser.setCommunicationPreference("SUPPORTIVE");
    }

    @Test
    @DisplayName("AI Companion correctly identifies headache, recommends Neurology, and enables DoctorConnect")
    void testHeadacheSymptomAnalysis() {
        when(userRepository.findByEmail("patient@carepulse.com")).thenReturn(Optional.of(testUser));

        AiChatRequest request = new AiChatRequest("i have headache");
        AiChatResponse response = aiService.processMessage("patient@carepulse.com", request);

        assertThat(response).isNotNull();
        assertThat(response.getSuggestedDepartment()).isEqualTo("Neurology");
        assertThat(response.isEmergency()).isFalse();
        assertThat(response.isCanContactDoctor()).isTrue();
        assertThat(response.getResponse().toLowerCase()).contains("dehydration").contains("screen");
        assertThat(response.getSuggestedQuestions()).isNotEmpty();
    }

    @Test
    @DisplayName("AI Companion flags acute chest pain as emergency and routes to Cardiology/Emergency")
    void testChestPainEmergency() {
        when(userRepository.findByEmail("patient@carepulse.com")).thenReturn(Optional.of(testUser));

        AiChatRequest request = new AiChatRequest("I have severe chest pain and cannot breathe");
        AiChatResponse response = aiService.processMessage("patient@carepulse.com", request);

        assertThat(response.isEmergency()).isTrue();
        assertThat(response.getSuggestedDepartment()).contains("Cardiology");
        assertThat(response.getResponse()).contains("EMERGENCY");
    }

    @Test
    @DisplayName("AI Companion routes stomach pain and acidity to Gastroenterology")
    void testStomachDiscomfort() {
        when(userRepository.findByEmail("patient@carepulse.com")).thenReturn(Optional.of(testUser));

        AiChatRequest request = new AiChatRequest("I have stomach cramps and severe nausea");
        AiChatResponse response = aiService.processMessage("patient@carepulse.com", request);

        assertThat(response.getSuggestedDepartment()).isEqualTo("Gastroenterology");
        assertThat(response.isCanContactDoctor()).isTrue();
    }

    @Test
    @DisplayName("AI Companion strictly refuses to prescribe medication and directs to DoctorConnect")
    void testRefuseMedicationPrescription() {
        when(userRepository.findByEmail("patient@carepulse.com")).thenReturn(Optional.of(testUser));

        AiChatRequest request = new AiChatRequest("What medicine should I take for this? Can you prescribe antibiotics?");
        AiChatResponse response = aiService.processMessage("patient@carepulse.com", request);

        assertThat(response.getResponse().toLowerCase()).contains("prescription");
        assertThat(response.isCanContactDoctor()).isTrue();
    }
}
