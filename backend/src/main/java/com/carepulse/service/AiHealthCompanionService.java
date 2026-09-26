package com.carepulse.service;

import com.carepulse.dto.AiChatRequest;
import com.carepulse.dto.AiChatResponse;
import com.carepulse.entity.User;
import com.carepulse.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class AiHealthCompanionService {

    private final UserRepository userRepository;
    private static final String MANDATORY_DISCLAIMER =
            "This AI provides general informational support and does not provide medical diagnosis or replace professional medical advice.";

    public AiHealthCompanionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public String getDisclaimer() {
        return MANDATORY_DISCLAIMER;
    }

    public AiChatResponse processMessage(String userEmail, AiChatRequest request) {
        String message = request.getMessage().trim().toLowerCase();
        
        // Retrieve empathy preference
        String tone = "SUPPORTIVE";
        if (userEmail != null) {
            tone = userRepository.findByEmail(userEmail)
                    .map(User::getCommunicationPreference)
                    .orElse("SUPPORTIVE");
        }

        String responseText;
        List<String> suggestedQuestions;

        // Check for diagnosis or dangerous medical self-diagnosis attempts
        if (message.contains("diagnose") || message.contains("what disease do i have") || message.contains("do i have cancer") || message.contains("cure me")) {
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "I cannot diagnose illnesses. Please consult a qualified doctor on CarePulse Portal for any symptoms.";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "Clinical Protocol Notice: As an AI health coordinator, I am not authorized to evaluate pathology or establish clinical diagnoses. Please book a consultation with our board-certified medical specialists.";
            } else {
                responseText = "I completely understand you might be worried about symptoms, but as an AI health companion, I cannot provide a medical diagnosis. Your health is very important—please book a consultation with one of our doctors so they can examine you safely!";
            }
            suggestedQuestions = Arrays.asList(
                    "How do I find the right specialist?",
                    "What questions should I ask my doctor?",
                    "How to prepare for my upcoming appointment?"
            );
            return new AiChatResponse(responseText, tone, suggestedQuestions);
        }

        // Preparation before appointment
        if (message.contains("prepare") || message.contains("before appointment") || message.contains("consultation prep")) {
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "1. Write down your symptoms.\n2. Bring previous test reports.\n3. List all current medicines.\n4. Arrive 10 minutes early.";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "Recommended Consultation Preparation:\n• Chronological timeline of onset, frequency, and severity of symptoms.\n• Complete pharmaceutical inventory (prescriptions, OTC drugs, dosages).\n• Diagnostic reports from past 6-12 months.\n• Prioritized list of clinical inquiries for the physician.";
            } else {
                responseText = "To make the most of your visit with your doctor:\n• Note down when your symptoms started and what triggers them.\n• Bring a list or photo of current medications & supplements.\n• Upload or carry previous lab results and medical records.\n• Jot down 2-3 key questions you really want answered during your time together!";
            }
            suggestedQuestions = Arrays.asList(
                    "What information should I tell my doctor?",
                    "How can I organize my medical records?",
                    "Can a caregiver view my consultation notes?"
            );
            return new AiChatResponse(responseText, tone, suggestedQuestions);
        }

        // What information to tell the doctor
        if (message.contains("tell my doctor") || message.contains("what to tell") || message.contains("discuss")) {
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "Tell your doctor:\n- What hurts and when it started\n- Any allergies you have\n- Medicines you currently take\n- Any recent changes in sleep, diet, or stress.";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "Essential Clinical Disclosure Checklist:\n1. Primary complaint and symptom progression.\n2. Known pharmacological or environmental allergies.\n3. Relevant family medical history (cardiac, endocrine, oncological).\n4. Lifestyle variables including occupational stressors and nutritional patterns.";
            } else {
                responseText = "Doctors appreciate clarity and honesty! Be sure to share:\n• Exactly how you feel in your own words, even if it feels minor.\n• Any allergies or unpleasant side-effects you've experienced.\n• Any lifestyle shifts, changes in appetite, or stress levels.\nRemember: no question is silly when it comes to your wellbeing.";
            }
            suggestedQuestions = Arrays.asList(
                    "How do I prepare questions for my consultation?",
                    "How should I track my blood pressure or vitals?",
                    "Where do I see my doctor's prescriptions?"
            );
            return new AiChatResponse(responseText, tone, suggestedQuestions);
        }

        // Organizing medical records
        if (message.contains("record") || message.contains("organize") || message.contains("history")) {
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "CarePulse saves your medical records automatically in the 'Medical Records' tab. You can view diagnosis, prescriptions, and date-wise health timelines anytime.";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "Record Management Protocol: Access your 'Medical Records' module for a chronological health timeline. Records include physician diagnostic notes, symptom logs, therapeutic plans, and digital prescriptions with exportable records.";
            } else {
                responseText = "Keeping your records organized is easy with CarePulse! Head over to the 'Medical Records' tab in your patient dashboard. You will see a chronological health timeline of every visit, doctor's notes, and issued prescriptions all in one place.";
            }
            suggestedQuestions = Arrays.asList(
                    "Can my family member view my records?",
                    "How do I book a follow-up appointment?",
                    "What should I prepare before my doctor appointment?"
            );
            return new AiChatResponse(responseText, tone, suggestedQuestions);
        }

        // Missed medicine / prescription advice
        if (message.contains("missed") || message.contains("dose") || message.contains("medicine") || message.contains("prescription")) {
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "Check your prescription card in CarePulse for frequency. Never take a double dose. Contact your prescribing doctor for specific medicine advice.";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "Pharmacotherapy Guidance: As a general rule, do not administer compensating double dosages without physician authorization. Refer to the active dosage interval in your 'Prescriptions' tab and message the attending physician.";
            } else {
                responseText = "If you missed a dose, check the specific dosage and instructions listed in your 'Prescriptions' tab. As a general rule of thumb, do not double up on medication to make up for a missed dose. If in doubt, reach out directly to your doctor!";
            }
            suggestedQuestions = Arrays.asList(
                    "How do I view my active prescriptions?",
                    "What should I tell my doctor about side effects?",
                    "How can I book a follow-up consultation?"
            );
            return new AiChatResponse(responseText, tone, suggestedQuestions);
        }

        // Caregiver / Family access
        if (message.contains("caregiver") || message.contains("family") || message.contains("access")) {
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "Go to 'Caregiver Access' to add a trusted person. You can choose what they see and revoke access at any time.";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "Proxy Access Administration: You retain sovereign control over designated proxy delegates via the 'Caregiver Access' sub-system. Configurable permissions include Appointment viewing, Medical Record inspection, and Prescription auditing.";
            } else {
                responseText = "Caregiver access allows a family member or trusted loved one to stay informed and help manage your care! In the 'Caregiver Access' section, simply enter their email and pick which permissions (appointments, records, or prescriptions) to share. You can revoke it anytime!";
            }
            suggestedQuestions = Arrays.asList(
                    "How do I add my caregiver?",
                    "How do I revoke caregiver access?",
                    "How can I prepare questions for my consultation?"
            );
            return new AiChatResponse(responseText, tone, suggestedQuestions);
        }

        // Default intelligent health coordination response
        if ("SIMPLE".equalsIgnoreCase(tone)) {
            responseText = "Hello! I am your CarePulse Health Companion. I can help you prepare for appointments, organize your medical records, and understand your health timeline. What would you like help with?";
        } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
            responseText = "CarePulse Coordination Engine Active. I assist with consultation pre-planning, clinical questionnaire formulation, health chronology auditing, and caregiver delegate governance. How may I assist your coordination today?";
        } else {
            responseText = "Hello! I'm your CarePulse AI Health Companion. I'm here to support you with practical guidance—like getting ready for upcoming appointments, organizing your questions, and navigating your medical records. How can I help make your healthcare journey smoother today?";
        }

        suggestedQuestions = Arrays.asList(
                "What should I prepare before my doctor appointment?",
                "What information should I tell my doctor?",
                "How can I organize my medical records?",
                "How does caregiver access work?"
        );

        return new AiChatResponse(responseText, tone, suggestedQuestions);
    }
}
