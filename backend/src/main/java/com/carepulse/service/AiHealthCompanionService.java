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
        String rawMessage = request.getMessage().trim();
        String message = rawMessage.toLowerCase();

        // Retrieve empathy preference
        String tone = "SUPPORTIVE";
        if (userEmail != null) {
            tone = userRepository.findByEmail(userEmail)
                    .map(User::getCommunicationPreference)
                    .orElse("SUPPORTIVE");
        }

        // 1. ACUTE EMERGENCY / LIFE-THREATENING SYMPTOMS
        if (message.contains("chest pain") || message.contains("heart attack") || message.contains("stroke") ||
            message.contains("severe bleeding") || message.contains("unconscious") || message.contains("can't breathe") ||
            message.contains("cannot breathe") || message.contains("shortness of breath") || message.contains("emergency") ||
            message.contains("choking") || message.contains("sudden numbness")) {

            String responseText;
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "⚠️ EMERGENCY ALERT: These symptoms sound like a potential medical emergency!\n\n" +
                        "• Do NOT wait for an online appointment.\n" +
                        "• Call local emergency services (911 / 112) immediately.\n" +
                        "• Or trigger 'Emergency Assistance' at the top of your CarePulse portal right away.";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "⚠️ CRITICAL PROTOCOL ALERT: Acute Medical Emergency Indication.\n\n" +
                        "The reported symptoms (cardiovascular or acute respiratory distress) represent potential life-threatening pathology. Clinical AI assistants are strictly prohibited from evaluating acute distress.\n\n" +
                        "Recommended Protocol:\n" +
                        "1. Dispatch local emergency response services immediately.\n" +
                        "2. Trigger CarePulse Rapid Emergency Physician Allocation.\n" +
                        "3. Maintain patient at rest with unobstructed airway.";
            } else {
                responseText = "⚠️ EMERGENCY SAFETY WARNING: Your health and safety come first!\n\n" +
                        "If you or someone with you is experiencing severe chest pain, trouble breathing, sudden weakness, or intense acute pain, please call emergency services (911/112) immediately.\n\n" +
                        "You can also click the red 'Emergency Assistance' button in CarePulse to be rapidly assigned an on-duty hospital physician right now.";
            }

            List<String> suggestedQuestions = Arrays.asList(
                    "How do I request emergency assistance?",
                    "Where can I find emergency duty doctors?",
                    "How does emergency doctor allocation work?"
            );

            return new AiChatResponse(
                    responseText,
                    tone,
                    suggestedQuestions,
                    "Cardiology & Emergency Medicine",
                    true,
                    false,
                    "Acute Emergency Symptoms"
            );
        }

        // 2. MEDICATION PRESCRIPTION OR DOSING REQUESTS
        if (message.contains("prescribe") || message.contains("what medicine should i take") ||
            message.contains("give me antibiotics") || message.contains("which pill") || message.contains("which medicine for") ||
            message.contains("what tablet") || message.contains("can i take paracetamol") || message.contains("can i take ibuprofen")) {

            String responseText;
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "I cannot write a prescription or give exact pill dosages. Only a licensed doctor can prescribe drugs safely.\n\n" +
                        "Please use CarePulse to message your doctor or book a quick consultation to obtain a valid digital prescription.";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "Clinical Protocol Notice: Pharmacotherapeutic boundaries prohibit AI assistants from issuing medication prescriptions, drug substitution advice, or dosing regimens.\n\n" +
                        "Recommendation: Consult a board-certified physician via CarePulse DoctorConnect or schedule an appointment for clinical evaluation and a digital prescription.";
            } else {
                responseText = "I completely understand you're looking for relief, but as an AI health companion, I cannot issue a prescription or recommend specific drug dosages.\n\n" +
                        "Every prescription medication needs to be tailored to your health history, allergies, and current vitals. To obtain a safe prescription, you can easily ask your appointed doctor directly through DoctorConnect!";
            }

            List<String> suggestedQuestions = Arrays.asList(
                    "How do I ask my doctor via DoctorConnect?",
                    "How do I view my existing prescriptions?",
                    "What should I tell my doctor about side effects?"
            );

            return new AiChatResponse(
                    responseText,
                    tone,
                    suggestedQuestions,
                    "General Medicine",
                    false,
                    true,
                    "Medication Inquiry"
            );
        }

        // 3. DEFINITIVE DIAGNOSIS REQUESTS
        if (message.contains("diagnose") || message.contains("what disease do i have") || message.contains("do i have cancer") || message.contains("cure me")) {
            String responseText;
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "I cannot give medical diagnoses. Please consult a qualified doctor on CarePulse to check your symptoms safely.";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "Clinical Regulatory Boundary: Diagnostic formulation requires physical examination, pathological evaluation, and diagnostic imaging. Please schedule a consultation with our licensed medical specialists.";
            } else {
                responseText = "I hear your concern, but as an AI health coordinator, I cannot provide a clinical diagnosis. Your symptoms deserve proper, careful examination by a qualified doctor.\n\n" +
                        "You can book a visit with one of our specialists or ask your appointed doctor via DoctorConnect!";
            }

            List<String> suggestedQuestions = Arrays.asList(
                    "How do I find the right specialist on CarePulse?",
                    "What questions should I ask during consultation?",
                    "How to prepare my symptom timeline?"
            );

            return new AiChatResponse(
                    responseText,
                    tone,
                    suggestedQuestions,
                    "General Medicine",
                    false,
                    true,
                    "Diagnostic Query"
            );
        }

        // 4. HEADACHE / MIGRAINE / NEUROLOGICAL SYMPTOMS (User's specific case!)
        if (message.contains("headache") || message.contains("migraine") || message.contains("head hurt") ||
            message.contains("head is pounding") || message.contains("dizzy") || message.contains("dizziness") ||
            message.contains("head pressure") || message.contains("temple pain")) {

            String responseText;
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "I understand you have a headache. Here is safe general information:\n\n" +
                        "• Common causes: Tension, dehydration, eye strain from screens, lack of sleep, or sinus pressure.\n" +
                        "• Helpful steps: Drink a large glass of water, rest in a quiet dark room, and take a screen break.\n" +
                        "• Red Flags: If you have sudden 'worst headache of your life', high fever with stiff neck, or trouble speaking, seek emergency care right away.\n\n" +
                        "Would you like to ask your doctor about this in DoctorConnect?";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "Clinical Evaluation Context: Cephalea / Headache Analysis.\n\n" +
                        "Etiological Considerations:\n" +
                        "• Primary: Tension-type headache, cervicogenic strain, migraine with/without aura.\n" +
                        "• Secondary: Dehydration, asthenopia (digital eye strain), circadian disruption, sinus congestion.\n\n" +
                        "Conservative Comfort Measures:\n" +
                        "• Oral rehydration therapy (minimum 500-750ml water).\n" +
                        "• Environmental photic & acoustic reduction (darkened quiet room).\n" +
                        "• Cool or warm sub-occipital compress.\n\n" +
                        "Clinical Red Flags (Require Immediate Urgent Care):\n" +
                        "• Sudden explosive 'thunderclap' onset, nuchal rigidity (stiff neck) with pyrexia (fever), focal neurological deficit, or cranial trauma.\n\n" +
                        "Recommended Department: Neurology or General Internal Medicine.";
            } else {
                responseText = "I'm sorry you're dealing with a headache! Here is helpful, safe guidance to support you:\n\n" +
                        "🔍 Common Everyday Causes:\n" +
                        "• Dehydration (one of the most frequent causes)\n" +
                        "• Tension & neck muscle stiffness (often from poor posture or desk work)\n" +
                        "• Eye strain from screens or bright light\n" +
                        "• Stress, skipped meals, or irregular sleep\n\n" +
                        "💡 Non-Medical Comfort Steps:\n" +
                        "1. Drink 1-2 glasses of cool water.\n" +
                        "2. Rest for 20-30 minutes in a quiet, dimly lit room.\n" +
                        "3. Apply a cold cloth across your forehead or back of your neck.\n" +
                        "4. Take a break from all phone, tablet, and computer screens.\n\n" +
                        "⚠️ When to Seek Urgent Medical Care:\n" +
                        "If this headache came on suddenly with severe intensity ('thunderclap'), is paired with a stiff neck and fever, or causes confusion, weakness, or vision changes, please seek emergency care.\n\n" +
                        "Would you like to consult your doctor or send a follow-up question via DoctorConnect?";
            }

            List<String> suggestedQuestions = Arrays.asList(
                    "Send question to doctor via DoctorConnect",
                    "What should I tell my neurologist?",
                    "How to prepare for my doctor appointment?",
                    "What triggers migraine headaches?"
            );

            return new AiChatResponse(
                    responseText,
                    tone,
                    suggestedQuestions,
                    "Neurology",
                    false,
                    true,
                    "Headache & Cephalea"
            );
        }

        // 5. STOMACH / ABDOMINAL / DIGESTIVE
        if (message.contains("stomach") || message.contains("tummy") || message.contains("abdomen") ||
            message.contains("nausea") || message.contains("vomit") || message.contains("diarrhea") ||
            message.contains("acidity") || message.contains("heartburn") || message.contains("bloating") ||
            message.contains("cramp") || message.contains("indigestion") || message.contains("belly")) {

            String responseText;
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "I understand your stomach is uncomfortable. Here is safe guidance:\n\n" +
                        "• Common causes: Food irritation, mild gastritis, indigestion, or stomach bug.\n" +
                        "• Care tips: Sip small amounts of water or oral rehydration fluids. Stick to bland foods (rice, bananas, toast). Avoid spicy or fatty food.\n" +
                        "• Red Flags: Severe pain in lower right belly, high fever, or blood in vomit/stool require immediate doctor care.\n\n" +
                        "You can discuss this with a doctor on CarePulse.";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "Gastrointestinal Evaluation Context:\n\n" +
                        "Differential Factors: Gastritis, functional dyspepsia, viral gastroenteritis, or gastroesophageal reflux (GERD).\n\n" +
                        "Supportive Management:\n" +
                        "• Fractionated hydration (electrolyte rehydration solutions).\n" +
                        "• Dietary modification: bland non-irritating diet (BRAT protocol).\n" +
                        "• Restriction of ethanol, caffeine, and capsaicin-containing foods.\n\n" +
                        "Red Flags: Acute localized abdominal guarding (specifically right lower quadrant), hematemesis, melena, or unremitting emesis.\n\n" +
                        "Recommended Specialty: Gastroenterology / Internal Medicine.";
            } else {
                responseText = "I understand you're experiencing stomach discomfort. Here is safe medical guidance:\n\n" +
                        "🔍 Common Causes:\n" +
                        "• Indigestion, acid reflux, or food sensitivities\n" +
                        "• Mild viral gastroenteritis ('stomach flu')\n" +
                        "• Dehydration or stress-related gut sensitivity\n\n" +
                        "💡 Comfort & Self-Care Measures:\n" +
                        "• Sip small sips of clear fluids (water, electrolyte solutions, herbal tea)\n" +
                        "• Eat light, bland foods (plain rice, bananas, toast, applesauce)\n" +
                        "• Avoid spicy, greasy foods, dairy, and caffeine for the next 24 hours\n\n" +
                        "⚠️ Urgent Warning Signs:\n" +
                        "Seek immediate medical attention if you experience severe localized pain (especially bottom right), persistent high fever, or blood in stool or vomit.\n\n" +
                        "You can easily consult a Gastroenterologist or send this to your doctor on DoctorConnect!";
            }

            List<String> suggestedQuestions = Arrays.asList(
                    "Send question to doctor via DoctorConnect",
                    "What foods are gentlest on the stomach?",
                    "What symptoms should I track for my doctor?",
                    "When does stomach pain need an in-person visit?"
            );

            return new AiChatResponse(
                    responseText,
                    tone,
                    suggestedQuestions,
                    "Gastroenterology",
                    false,
                    true,
                    "Gastrointestinal Discomfort"
            );
        }

        // 6. FEVER / COLD / COUGH / RESPIRATORY
        if (message.contains("fever") || message.contains("temperature") || message.contains("cold") ||
            message.contains("cough") || message.contains("sore throat") || message.contains("flu") ||
            message.contains("runny nose") || message.contains("phlegm") || message.contains("congestion") ||
            message.contains("sneezing") || message.contains("shivering")) {

            String responseText;
            if ("SIMPLE".equalsIgnoreCase(tone)) {
                responseText = "I understand you have fever or cold symptoms. Here is safe guidance:\n\n" +
                        "• Common causes: Common viral cold, mild flu, or seasonal changes.\n" +
                        "• Tips: Rest, drink lots of warm fluids (soup, warm water), and do warm saline gargles for your throat.\n" +
                        "• Red Flags: Fever above 103°F (39.5°C), lasting over 3 days, or difficulty breathing needs immediate doctor review.\n\n" +
                        "Would you like to message a doctor in DoctorConnect?";
            } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
                responseText = "Respiratory & Pyrexial Clinical Overview:\n\n" +
                        "Clinical Considerations: Viral upper respiratory tract infection (URTI), influenza syndrome, or acute pharyngitis.\n\n" +
                        "Supportive Regimen:\n" +
                        "• Maintenance of fluid homeostasis (hyper-hydration).\n" +
                        "• Rest to facilitate physiological immune defense.\n" +
                        "• Saline hypertonic gargling for pharyngeal edema.\n\n" +
                        "Warning Thresholds: Dyspnea (shortness of breath), oxygen saturation < 94%, persistent pyrexia > 72 hours, or hemoptysis.\n\n" +
                        "Recommended Specialty: General Medicine / Pulmonology.";
            } else {
                responseText = "I'm sorry you're feeling under the weather with fever or cold symptoms! Here is safe, helpful advice:\n\n" +
                        "🔍 Common Causes:\n" +
                        "• Seasonal viral infections or common cold\n" +
                        "• Upper respiratory irritation or pharyngitis\n\n" +
                        "💡 Non-Medicinal Supportive Care:\n" +
                        "• Drink plenty of warm liquids (water, clear broths, herbal teas)\n" +
                        "• Get plenty of restful sleep to let your immune system work\n" +
                        "• Warm salt water gargles for throat irritation\n" +
                        "• Steam inhalation to help clear nasal congestion\n\n" +
                        "⚠️ When to See a Doctor Immediately:\n" +
                        "If fever exceeds 103°F, persists for more than 3 consecutive days, or is accompanied by difficulty breathing, chest pain, or extreme fatigue.\n\n" +
                        "Would you like to message a General Physician or book an appointment on CarePulse?";
            }

            List<String> suggestedQuestions = Arrays.asList(
                    "Send question to doctor via DoctorConnect",
                    "How to log temperature readings for my doctor?",
                    "What questions should I ask my physician?",
                    "How to book a General Medicine appointment?"
            );

            return new AiChatResponse(
                    responseText,
                    tone,
                    suggestedQuestions,
                    "General Medicine",
                    false,
                    true,
                    "Fever & Upper Respiratory Symptoms"
            );
        }

        // 7. SKIN / RASH / ITCHING / DERMATOLOGICAL
        if (message.contains("rash") || message.contains("skin") || message.contains("itching") ||
            message.contains("itchy") || message.contains("acne") || message.contains("allergy") ||
            message.contains("hives") || message.contains("eczema") || message.contains("red spots") ||
            message.contains("blister")) {

            String responseText = "I understand you're experiencing a skin irritation or rash. Here is safe dermatological guidance:\n\n" +
                    "🔍 Common Causes:\n" +
                    "• Contact dermatitis (reaction to soap, detergent, perfume, or plant)\n" +
                    "• Mild seasonal or food allergy\n" +
                    "• Dry skin or eczema flare-up\n\n" +
                    "💡 Gentle Care Recommendations:\n" +
                    "• Avoid scratching to prevent skin barrier breakdown or secondary bacterial infection\n" +
                    "• Wash gently with lukewarm water and mild, fragrance-free cleanser\n" +
                    "• Apply plain, fragrance-free moisturizer\n" +
                    "• Take note of any new soaps, detergents, cosmetics, or foods introduced recently\n\n" +
                    "⚠️ Red Flags:\n" +
                    "If the rash spreads rapidly, is accompanied by facial/lip swelling, fever, or difficulty breathing, seek immediate emergency care.\n\n" +
                    "Recommended Specialty: Dermatology.";

            List<String> suggestedQuestions = Arrays.asList(
                    "Send question to doctor via DoctorConnect",
                    "How should I describe my rash to a dermatologist?",
                    "How to find a Dermatologist on CarePulse?",
                    "What common allergens cause skin hives?"
            );

            return new AiChatResponse(
                    responseText,
                    tone,
                    suggestedQuestions,
                    "Dermatology",
                    false,
                    true,
                    "Dermatological Symptom"
            );
        }

        // 8. BONE / JOINT / MUSCLE / BACK PAIN / ORTHOPEDIC
        if (message.contains("back pain") || message.contains("knee pain") || message.contains("joint") ||
            message.contains("muscle") || message.contains("sprain") || message.contains("shoulder") ||
            message.contains("neck pain") || message.contains("arthritis") || message.contains("leg pain") ||
            message.contains("swelling") || message.contains("ankle")) {

            String responseText = "I understand you're experiencing joint, muscle, or back discomfort. Here is safe orthopedic guidance:\n\n" +
                    "🔍 Common Causes:\n" +
                    "• Muscular strain from prolonged sitting, lifting, or poor posture\n" +
                    "• Mild tendon or ligament sprain\n" +
                    "• Joint overuse or mechanical irritation\n\n" +
                    "💡 Safe Supportive Steps (RICE principle):\n" +
                    "• Rest the affected area and avoid strenuous loading or heavy lifting\n" +
                    "• Apply an ice pack wrapped in a towel for 15-20 minutes to soothe acute swelling\n" +
                    "• Keep gentle, comfortable posture and avoid sudden twisting movements\n\n" +
                    "⚠️ Red Flags:\n" +
                    "Severe pain following high-impact trauma, visible joint deformity, inability to bear any weight, or back pain with leg numbness or bladder/bowel changes requires urgent medical review.\n\n" +
                    "Recommended Specialty: Orthopedics / Physical Therapy.";

            List<String> suggestedQuestions = Arrays.asList(
                    "Send question to doctor via DoctorConnect",
                    "How to prepare for an orthopedic consultation?",
                    "What gentle mobility exercises are safe?",
                    "How to find an Orthopedic specialist on CarePulse?"
            );

            return new AiChatResponse(
                    responseText,
                    tone,
                    suggestedQuestions,
                    "Orthopedics",
                    false,
                    true,
                    "Musculoskeletal / Orthopedic Strain"
            );
        }

        // 9. APPOINTMENT PREPARATION
        if (message.contains("prepare") || message.contains("before appointment") || message.contains("consultation prep")) {
            String responseText = "Here is a complete checklist to prepare for your doctor visit:\n\n" +
                    "1. Timeline of Symptoms: Note down when your symptoms started, what makes them better or worse.\n" +
                    "2. Medications List: Have photos or a list of all current prescriptions, supplements, and vitamins.\n" +
                    "3. Previous Reports: Bring recent lab tests, scans, or doctor consultation notes.\n" +
                    "4. Top 3 Questions: Write down the most important questions you want answered during your consultation.\n\n" +
                    "All your consultation notes and records will be saved automatically in your CarePulse Medical Records tab!";

            List<String> suggestedQuestions = Arrays.asList(
                    "What information should I tell my doctor?",
                    "How can I organize my medical records?",
                    "How to view my doctor's available slots?"
            );

            return new AiChatResponse(responseText, tone, suggestedQuestions, "General Medicine", false, false, "Consultation Preparation");
        }

        // 10. WHAT TO TELL DOCTOR
        if (message.contains("tell my doctor") || message.contains("what to tell") || message.contains("discuss")) {
            String responseText = "Doctors appreciate clarity and honesty! Be sure to share:\n\n" +
                    "• Exactly what hurts or feels off, in your own words\n" +
                    "• When the symptom started and how often it happens\n" +
                    "• Any allergies or unpleasant side-effects you've experienced in the past\n" +
                    "• Any recent life changes, diet shifts, or stress levels\n\n" +
                    "Remember: no question is too minor when it comes to your health!";

            List<String> suggestedQuestions = Arrays.asList(
                    "Send question to doctor via DoctorConnect",
                    "How to track blood pressure or vitals?",
                    "Where do I see my doctor's prescriptions?"
            );

            return new AiChatResponse(responseText, tone, suggestedQuestions, "General Medicine", false, false, "Doctor Communication Guidance");
        }

        // 11. GENERAL HEALTH QUESTION / FALLBACK
        String responseText;
        if ("SIMPLE".equalsIgnoreCase(tone)) {
            responseText = "Hello! I am your CarePulse Health Companion.\n\n" +
                    "I can help you understand your symptoms, find the right medical department, prepare questions for your doctor, and organize your health records.\n\n" +
                    "You can tell me about any symptoms (e.g., headache, fever, cough, stomach pain) or ask how to consult your doctor.";
        } else if ("PROFESSIONAL".equalsIgnoreCase(tone)) {
            responseText = "CarePulse Clinical Coordination Engine Active.\n\n" +
                    "System Capabilities:\n" +
                    "• Triage symptom profiling and department specialization mapping.\n" +
                    "• Pre-consultation clinical questionnaire formulation.\n" +
                    "• Health chronology, records, and proxy caregiver administration.\n\n" +
                    "Please state your clinical query or symptoms to receive educational coordination and specialist routing.";
        } else {
            responseText = "Hello! I'm your CarePulse AI Health Companion. I'm here to support you with practical healthcare guidance.\n\n" +
                    "You can tell me about any symptoms you're feeling (like headaches, stomach ache, fever, cough, joint pain, or allergies), and I will help you identify the right medical department and prepare questions for your doctor.\n\n" +
                    "How can I help you today?";
        }

        List<String> suggestedQuestions = Arrays.asList(
                "I have a headache, what should I do?",
                "I have stomach discomfort",
                "What should I prepare before my doctor appointment?",
                "How do I ask my doctor via DoctorConnect?"
        );

        return new AiChatResponse(
                responseText,
                tone,
                suggestedQuestions,
                "General Medicine",
                false,
                true,
                "Health Guidance"
        );
    }
}
