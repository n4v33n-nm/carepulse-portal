import React, { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Bot,
  Send,
  Sparkles,
  AlertTriangle,
  User,
  Info,
  Stethoscope,
  MessageSquare,
  Siren,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
} from 'lucide-react';
import { aiService } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import EmergencyAssistanceModal from '../../components/EmergencyAssistanceModal';

const AiCompanionPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content:
        user?.communicationPreference === 'SIMPLE'
          ? "Hello! I am your CarePulse Health Companion. Tell me your symptoms (like headache, stomach ache, fever, cough), and I will help you identify the right medical department and guide you on what to ask your doctor."
          : user?.communicationPreference === 'PROFESSIONAL'
          ? "CarePulse Clinical Coordination Engine Active. I assist with clinical symptom profiling, medical department triage mapping, and pre-consultation formulation. Please state your symptoms or inquiry."
          : "Hello! I'm your CarePulse AI Health Companion. I'm here to support you with practical healthcare guidance—like understanding your symptoms, identifying the right medical specialty, and organizing questions for your doctor.\n\nTell me how you're feeling today (for example: 'I have a headache' or 'I have stomach pain').",
      suggestedQuestions: [
        "I have a headache, what should I do?",
        "I have stomach discomfort and acidity",
        "I have a fever and dry cough",
        "What should I prepare before my doctor appointment?",
      ],
      suggestedDepartment: 'General Medicine',
      canContactDoctor: true,
    },
  ]);
  const [inputText, setInputText] = useState('');
  const [loading, setLoading] = useState(false);
  const [showEmergencyModal, setShowEmergencyModal] = useState(false);
  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, loading]);

  const handleSend = async (messageToSend) => {
    const text = messageToSend || inputText;
    if (!text.trim() || loading) return;

    const userMsg = { role: 'user', content: text };
    setMessages((prev) => [...prev, userMsg]);
    setInputText('');
    setLoading(true);

    try {
      const res = await aiService.chat({ message: text });
      const aiReply = {
        role: 'assistant',
        content: res.data.response,
        suggestedQuestions: res.data.suggestedQuestions,
        suggestedDepartment: res.data.suggestedDepartment,
        isEmergency: res.data.emergency,
        canContactDoctor: res.data.canContactDoctor,
        symptomDetected: res.data.symptomDetected,
      };
      setMessages((prev) => [...prev, aiReply]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: 'Sorry, I encountered an issue processing your question. Please try again or consult your doctor on CarePulse.',
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handlePromptClick = (question) => {
    handleSend(question);
  };

  return (
    <div style={{ maxWidth: '960px', margin: '0 auto' }}>
      <div style={{ marginBottom: '20px' }}>
        <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>AI Health Companion</h2>
        <p style={{ color: 'var(--slate-600)' }}>
          Intelligent symptom assessment, medical department identification, and clinical consultation preparation.
        </p>
      </div>

      {/* Mandatory Safety Disclaimer Alert */}
      <div className="ai-disclaimer-box" style={{ padding: '12px 18px', marginBottom: '16px' }}>
        <AlertTriangle size={18} style={{ flexShrink: 0 }} />
        <span>
          <strong>Clinical Boundary Notice:</strong> This AI assistant provides general informational guidance and department routing. It does not provide medical diagnosis, prescribe drugs, or replace a licensed physician.
        </span>
      </div>

      {/* Chat Container */}
      <div className="chat-container">
        {/* Chat Header */}
        <div className="chat-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: 'var(--radius-full)',
                background: 'rgba(255, 255, 255, 0.2)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <Bot size={20} />
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: '1rem' }}>CarePulse Clinical Companion</div>
              <div style={{ fontSize: '0.75rem', opacity: 0.85 }}>
                Empathy Mode: {user?.communicationPreference || 'Supportive'} • Integrated with DoctorConnect
              </div>
            </div>
          </div>
          <div
            style={{
              fontSize: '0.75rem',
              background: 'rgba(255, 255, 255, 0.15)',
              padding: '4px 10px',
              borderRadius: 'var(--radius-full)',
            }}
          >
            Always Active
          </div>
        </div>

        {/* Message Stream */}
        <div className="chat-messages">
          {messages.map((m, idx) => (
            <div key={idx} style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
              <div
                className={`chat-bubble ${
                  m.role === 'user' ? 'chat-bubble-user' : 'chat-bubble-ai'
                }`}
                style={
                  m.isEmergency
                    ? {
                        border: '2px solid #ef4444',
                        background: '#fff1f2',
                        color: '#991b1b',
                      }
                    : {}
                }
              >
                <div style={{ whiteSpace: 'pre-line', lineHeight: 1.6 }}>{m.content}</div>

                {/* Emergency Direct Action Banner */}
                {m.isEmergency && (
                  <div
                    style={{
                      marginTop: '16px',
                      padding: '12px 16px',
                      background: '#fee2e2',
                      borderRadius: '8px',
                      border: '1px solid #f87171',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      flexWrap: 'wrap',
                      gap: '10px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#b91c1c', fontWeight: 700, fontSize: '0.875rem' }}>
                      <Siren size={18} /> Emergency Physician Triage Required
                    </div>
                    <button
                      type="button"
                      className="btn btn-emergency btn-sm"
                      onClick={() => setShowEmergencyModal(true)}
                      style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                    >
                      <Siren size={14} /> Request Emergency Doctor
                    </button>
                  </div>
                )}

                {/* Clinical Department Recommendation & DoctorConnect Integration Card */}
                {m.role === 'assistant' && m.suggestedDepartment && !m.isEmergency && (
                  <div
                    style={{
                      marginTop: '16px',
                      padding: '14px 16px',
                      background: '#f0fdfa',
                      borderRadius: '8px',
                      border: '1px solid #99f6e4',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '10px',
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '8px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8125rem', fontWeight: 700, color: '#0f766e' }}>
                        <Stethoscope size={16} />
                        <span>Recommended Department: <strong>{m.suggestedDepartment}</strong></span>
                      </div>
                      {m.symptomDetected && (
                        <span style={{ fontSize: '0.75rem', color: '#115e59', background: '#ccfbf1', padding: '2px 8px', borderRadius: '4px', fontWeight: 600 }}>
                          {m.symptomDetected}
                        </span>
                      )}
                    </div>

                    <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                      <Link
                        to="/patient/doctor-connect"
                        className="btn btn-primary btn-sm"
                        style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '0.8125rem' }}
                      >
                        <MessageSquare size={14} /> Ask Doctor in DoctorConnect
                      </Link>
                      <Link
                        to="/patient/doctors"
                        className="btn btn-outline btn-sm"
                        style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '0.8125rem', background: 'white' }}
                      >
                        <Stethoscope size={14} /> Find {m.suggestedDepartment} Doctor
                      </Link>
                    </div>
                  </div>
                )}
              </div>

              {/* Suggested Questions Chips */}
              {m.suggestedQuestions && m.suggestedQuestions.length > 0 && (
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '4px', marginLeft: '6px' }}>
                  {m.suggestedQuestions.map((q, qIdx) => (
                    <button
                      key={qIdx}
                      type="button"
                      className="prompt-chip"
                      onClick={() => handlePromptClick(q)}
                    >
                      {q}
                    </button>
                  ))}
                </div>
              )}
            </div>
          ))}

          {loading && (
            <div className="chat-bubble chat-bubble-ai" style={{ width: '160px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--slate-500)', fontSize: '0.875rem' }}>
                <Sparkles size={16} className="text-primary" /> Analyzing inquiry...
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* Chat Input Bar */}
        <form
          className="chat-input-bar"
          onSubmit={(e) => {
            e.preventDefault();
            handleSend();
          }}
        >
          <input
            type="text"
            className="form-control"
            placeholder="Type your symptoms (e.g. 'I have a headache', 'fever', 'stomach pain') or health question..."
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            disabled={loading}
          />
          <button type="submit" className="btn btn-primary" disabled={loading || !inputText.trim()}>
            <Send size={16} /> Send
          </button>
        </form>
      </div>

      {/* Emergency Assistance Modal when triggered from AI */}
      <EmergencyAssistanceModal
        isOpen={showEmergencyModal}
        onClose={() => setShowEmergencyModal(false)}
        onSuccess={() => {
          setShowEmergencyModal(false);
          navigate('/patient/dashboard');
        }}
      />
    </div>
  );
};

export default AiCompanionPage;
