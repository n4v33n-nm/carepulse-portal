import React, { useState, useRef, useEffect } from 'react';
import {
  Bot,
  Send,
  Sparkles,
  AlertTriangle,
  User,
  Info,
} from 'lucide-react';
import { aiService } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

const AiCompanionPage = () => {
  const { user } = useAuth();
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content:
        user?.communicationPreference === 'SIMPLE'
          ? "Hello! I am your CarePulse Health Companion. Ask me how to prepare for your doctor visits, what questions to ask, or how to organize your records."
          : user?.communicationPreference === 'PROFESSIONAL'
          ? "CarePulse Coordination Engine Active. I assist with clinical questionnaire formulation, health chronology auditing, and consultation preparation. How may I assist your coordination today?"
          : "Hello! I'm your CarePulse AI Health Companion. I'm here to support you with practical guidance—like getting ready for upcoming appointments, organizing your questions, and navigating your medical records. How can I help make your healthcare journey smoother today?",
      suggestedQuestions: [
        "What should I prepare before my doctor appointment?",
        "What information should I tell my doctor?",
        "How can I organize my medical records?",
        "Can a caregiver view my consultation notes?",
      ],
    },
  ]);
  const [inputText, setInputText] = useState('');
  const [loading, setLoading] = useState(false);
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
      };
      setMessages((prev) => [...prev, aiReply]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: 'Sorry, I encountered an issue processing your question. Please try again.',
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
    <div style={{ maxWidth: '900px', margin: '0 auto' }}>
      <div style={{ marginBottom: '20px' }}>
        <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>AI Health Companion</h2>
        <p style={{ color: 'var(--slate-600)' }}>
          Intelligent pre-consultation coordination, clinical questionnaire formulation, and health record organization.
        </p>
      </div>

      {/* Mandatory Safety Disclaimer Alert */}
      <div className="ai-disclaimer-box" style={{ padding: '12px 18px', marginBottom: '16px' }}>
        <AlertTriangle size={18} style={{ flexShrink: 0 }} />
        <span>
          <strong>Safety Protocol:</strong> This AI provides general informational support and does not provide medical diagnosis or replace professional medical advice.
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
                Empathy Mode: {user?.communicationPreference || 'Supportive'}
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
            <div key={idx} style={{ display: 'flex', flexDirection: 'column' }}>
              <div
                className={`chat-bubble ${
                  m.role === 'user' ? 'chat-bubble-user' : 'chat-bubble-ai'
                }`}
              >
                <div style={{ whiteSpace: 'pre-line' }}>{m.content}</div>
              </div>

              {/* Suggested Questions */}
              {m.suggestedQuestions && m.suggestedQuestions.length > 0 && (
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '10px', marginLeft: '6px' }}>
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
            <div className="chat-bubble chat-bubble-ai" style={{ width: '140px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--slate-500)', fontSize: '0.875rem' }}>
                <Sparkles size={16} className="text-primary" /> Thinking...
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
            placeholder="Ask about consultation prep, symptom logs, or medical questions..."
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            disabled={loading}
          />
          <button type="submit" className="btn btn-primary" disabled={loading || !inputText.trim()}>
            <Send size={16} /> Send
          </button>
        </form>
      </div>
    </div>
  );
};

export default AiCompanionPage;
