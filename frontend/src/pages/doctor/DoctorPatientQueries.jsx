import React, { useState, useEffect, useRef } from 'react';
import {
  MessageSquare,
  Send,
  CheckCircle2,
  Clock,
  User,
  Stethoscope,
  Calendar,
  AlertCircle,
  Search,
  Filter,
  Phone,
  Droplet,
} from 'lucide-react';
import { doctorConnectService } from '../../services/api';

const DoctorPatientQueries = () => {
  const [conversations, setConversations] = useState([]);
  const [selectedConv, setSelectedConv] = useState(null);
  const [activeTab, setActiveTab] = useState('ALL'); // ALL, OPEN, ANSWERED, RESOLVED
  const [loading, setLoading] = useState(true);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [replyText, setReplyText] = useState('');
  const [replySending, setReplySending] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [stats, setStats] = useState({ total: 0, open: 0, answered: 0, resolved: 0 });

  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    fetchConversations();
    fetchStats();
  }, [activeTab]);

  useEffect(() => {
    scrollToBottom();
  }, [selectedConv?.messages]);

  const fetchStats = async () => {
    try {
      const res = await doctorConnectService.getStats();
      if (res.data) {
        setStats({
          total: res.data.totalConversations || 0,
          open: res.data.openConversations || 0,
          answered: res.data.answeredConversations || 0,
          resolved: res.data.resolvedConversations || 0,
        });
      }
    } catch (err) {
      console.error('Failed to load stats', err);
    }
  };

  const fetchConversations = async (autoSelectId = null) => {
    setLoading(true);
    try {
      const params = activeTab !== 'ALL' ? { status: activeTab } : {};
      const res = await doctorConnectService.getConversations(params);
      const list = res.data || [];
      setConversations(list);

      if (autoSelectId) {
        loadConversationDetails(autoSelectId);
      } else if (list.length > 0 && !selectedConv) {
        loadConversationDetails(list[0].id);
      }
    } catch (err) {
      console.error('Failed to load doctor inquiries', err);
    } finally {
      setLoading(false);
    }
  };

  const loadConversationDetails = async (id) => {
    setMessagesLoading(true);
    try {
      const res = await doctorConnectService.getConversationById(id);
      setSelectedConv(res.data);
      // Mark as read locally
      setConversations((prev) =>
        prev.map((c) => (c.id === id ? { ...c, unreadMessagesCount: 0 } : c))
      );
    } catch (err) {
      console.error('Failed to load conversation details', err);
    } finally {
      setMessagesLoading(false);
    }
  };

  const handleSendReply = async (e) => {
    e.preventDefault();
    if (!replyText.trim() || replySending || !selectedConv) return;

    setReplySending(true);
    try {
      const res = await doctorConnectService.sendMessage(selectedConv.id, {
        content: replyText.trim(),
      });
      setSelectedConv((prev) => ({
        ...prev,
        status: 'ANSWERED',
        messages: [...(prev.messages || []), res.data],
      }));
      setReplyText('');
      fetchConversations(selectedConv.id);
      fetchStats();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to send reply');
    } finally {
      setReplySending(false);
    }
  };

  const handleUpdateStatus = async (newStatus) => {
    if (!selectedConv) return;
    try {
      const res = await doctorConnectService.updateStatus(selectedConv.id, {
        status: newStatus,
      });
      setSelectedConv((prev) => ({ ...prev, status: res.data.status }));
      fetchConversations(selectedConv.id);
      fetchStats();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update status');
    }
  };

  const filteredConversations = conversations.filter((c) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      c.subject?.toLowerCase().includes(q) ||
      c.patientName?.toLowerCase().includes(q)
    );
  });

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ANSWERED':
        return (
          <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <CheckCircle2 size={12} /> Answered
          </span>
        );
      case 'RESOLVED':
        return (
          <span className="badge" style={{ background: 'var(--slate-200)', color: 'var(--slate-700)' }}>
            Resolved
          </span>
        );
      case 'OPEN':
      default:
        return (
          <span className="badge badge-warning" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Clock size={12} /> Needs Response
          </span>
        );
    }
  };

  return (
    <div>
      {/* Page Title */}
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Patient Consultation Inquiries</h2>
        <p style={{ color: 'var(--slate-600)' }}>
          Review and respond to clinical inquiries and follow-up questions from your assigned patients.
        </p>
      </div>

      {/* Stats Quick Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div className="card" style={{ padding: '16px', borderLeft: '4px solid var(--primary-500)' }}>
          <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', fontWeight: 600 }}>Total Inquiries</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--slate-900)' }}>{stats.total}</div>
        </div>
        <div className="card" style={{ padding: '16px', borderLeft: '4px solid var(--warning-500)' }}>
          <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', fontWeight: 600 }}>Awaiting Response</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--warning-700)' }}>{stats.open}</div>
        </div>
        <div className="card" style={{ padding: '16px', borderLeft: '4px solid var(--success-500)' }}>
          <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', fontWeight: 600 }}>Answered</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--success-700)' }}>{stats.answered}</div>
        </div>
        <div className="card" style={{ padding: '16px', borderLeft: '4px solid var(--slate-400)' }}>
          <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', fontWeight: 600 }}>Resolved Cases</div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--slate-700)' }}>{stats.resolved}</div>
        </div>
      </div>

      {/* Filter Tabs */}
      <div style={{ display: 'flex', gap: '8px', marginBottom: '20px', borderBottom: '1px solid var(--slate-200)', paddingBottom: '12px' }}>
        {['ALL', 'OPEN', 'ANSWERED', 'RESOLVED'].map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`btn ${activeTab === tab ? 'btn-primary' : 'btn-secondary'} btn-sm`}
            style={{ fontWeight: 600 }}
          >
            {tab === 'ALL' && 'All Inquiries'}
            {tab === 'OPEN' && `Awaiting Reply (${stats.open})`}
            {tab === 'ANSWERED' && 'Answered'}
            {tab === 'RESOLVED' && 'Resolved'}
          </button>
        ))}
      </div>

      {/* Main Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(320px, 380px) 1fr', gap: '24px', minHeight: '620px' }}>
        {/* Left Column: Inquiries List */}
        <div className="card" style={{ padding: '16px', display: 'flex', flexDirection: 'column' }}>
          <div style={{ position: 'relative', marginBottom: '16px' }}>
            <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--slate-400)' }} />
            <input
              type="text"
              placeholder="Search by patient or subject..."
              className="form-control"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{ paddingLeft: '36px' }}
            />
          </div>

          <div style={{ overflowY: 'auto', flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {loading ? (
              <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-400)' }}>
                Loading inquiries...
              </div>
            ) : filteredConversations.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 16px', color: 'var(--slate-500)' }}>
                <MessageSquare size={36} style={{ margin: '0 auto 12px', color: 'var(--slate-300)' }} />
                <p style={{ fontWeight: 600, fontSize: '0.9375rem', marginBottom: '4px' }}>No inquiries in this category</p>
                <p style={{ fontSize: '0.8125rem' }}>Patients will appear here when they submit health questions.</p>
              </div>
            ) : (
              filteredConversations.map((c) => {
                const isSelected = selectedConv?.id === c.id;
                return (
                  <div
                    key={c.id}
                    onClick={() => loadConversationDetails(c.id)}
                    style={{
                      padding: '14px',
                      borderRadius: 'var(--radius-md)',
                      background: isSelected ? 'var(--primary-50)' : 'var(--slate-50)',
                      border: isSelected ? '1.5px solid var(--primary-500)' : '1px solid var(--slate-200)',
                      cursor: 'pointer',
                      transition: 'all 0.15s ease',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '6px' }}>
                      <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: isSelected ? 'var(--primary-900)' : 'var(--slate-900)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '180px' }}>
                        {c.subject}
                      </span>
                      {getStatusBadge(c.status)}
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8125rem', color: 'var(--slate-700)', fontWeight: 600, marginBottom: '6px' }}>
                      <User size={14} style={{ color: 'var(--primary-600)' }} />
                      <span>{c.patientName}</span>
                    </div>

                    <p style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginBottom: '6px' }}>
                      {c.lastMessageSnippet || 'No messages'}
                    </p>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.75rem', color: 'var(--slate-400)' }}>
                      <span>{c.updatedAt ? new Date(c.updatedAt).toLocaleDateString() : ''}</span>
                      {c.unreadMessagesCount > 0 && (
                        <span style={{ background: 'var(--warning-500)', color: 'white', padding: '2px 6px', borderRadius: '10px', fontWeight: 700, fontSize: '0.6875rem' }}>
                          Needs Reply
                        </span>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Right Column: Selected Thread & Patient Context */}
        <div className="card" style={{ padding: '0', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          {selectedConv ? (
            <>
              {/* Header with Patient Context */}
              <div style={{ padding: '18px 24px', borderBottom: '1px solid var(--slate-200)', background: '#ffffff' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px', marginBottom: '12px' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px' }}>
                      <h3 style={{ fontSize: '1.25rem', color: 'var(--slate-900)' }}>{selectedConv.subject}</h3>
                      {getStatusBadge(selectedConv.status)}
                    </div>
                    <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)' }}>
                      Inquiry #{selectedConv.id} • Initiated on {selectedConv.createdAt ? new Date(selectedConv.createdAt).toLocaleString() : ''}
                    </div>
                  </div>

                  <div>
                    {selectedConv.status === 'RESOLVED' ? (
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => handleUpdateStatus('OPEN')}
                      >
                        Re-open Inquiry
                      </button>
                    ) : (
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => handleUpdateStatus('RESOLVED')}
                        style={{ color: 'var(--success-700)', borderColor: 'var(--success-300)' }}
                      >
                        <CheckCircle2 size={15} style={{ marginRight: '6px' }} /> Mark as Resolved
                      </button>
                    )}
                  </div>
                </div>

                {/* Patient Information Strip */}
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '16px', background: 'var(--slate-50)', padding: '10px 14px', borderRadius: '8px', border: '1px solid var(--slate-200)', fontSize: '0.8125rem', color: 'var(--slate-700)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <User size={14} style={{ color: 'var(--primary-600)' }} />
                    <strong>Patient:</strong> {selectedConv.patientName}
                  </div>
                  {selectedConv.patientGender && (
                    <div><strong>Gender:</strong> {selectedConv.patientGender}</div>
                  )}
                  {selectedConv.patientBloodGroup && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <Droplet size={13} style={{ color: '#ef4444' }} />
                      <strong>Blood Group:</strong> {selectedConv.patientBloodGroup}
                    </div>
                  )}
                  {selectedConv.patientPhone && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <Phone size={13} />
                      <strong>Phone:</strong> {selectedConv.patientPhone}
                    </div>
                  )}
                  {selectedConv.appointmentDate && (
                    <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                      <Calendar size={13} />
                      <strong>Related Appt:</strong> {selectedConv.appointmentDate}
                    </div>
                  )}
                </div>
              </div>

              {/* Message Thread */}
              <div style={{ flex: 1, padding: '24px', overflowY: 'auto', background: 'var(--slate-50)', display: 'flex', flexDirection: 'column', gap: '16px', maxHeight: '420px' }}>
                {messagesLoading ? (
                  <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-500)' }}>
                    Loading message history...
                  </div>
                ) : (
                  selectedConv.messages?.map((m) => {
                    const isDoctor = m.senderRole === 'DOCTOR';
                    return (
                      <div
                        key={m.id}
                        style={{
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: isDoctor ? 'flex-end' : 'flex-start',
                        }}
                      >
                        <div
                          style={{
                            maxWidth: '78%',
                            padding: '14px 18px',
                            borderRadius: 'var(--radius-lg)',
                            background: isDoctor ? 'var(--primary-600)' : '#ffffff',
                            color: isDoctor ? '#ffffff' : 'var(--slate-900)',
                            border: isDoctor ? 'none' : '1px solid var(--slate-200)',
                            boxShadow: 'var(--shadow-sm)',
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem', fontWeight: 700, marginBottom: '6px', color: isDoctor ? 'rgba(255, 255, 255, 0.9)' : 'var(--primary-700)' }}>
                            {isDoctor ? <Stethoscope size={13} /> : <User size={13} />}
                            <span>{m.senderName}</span>
                            {isDoctor && (
                              <span style={{ background: 'rgba(255, 255, 255, 0.2)', padding: '1px 6px', borderRadius: '4px', fontSize: '0.6875rem' }}>
                                Attending Physician
                              </span>
                            )}
                          </div>

                          <div style={{ fontSize: '0.9375rem', lineHeight: 1.5, whiteSpace: 'pre-wrap' }}>
                            {m.content}
                          </div>
                        </div>

                        <span style={{ fontSize: '0.6875rem', color: 'var(--slate-400)', marginTop: '4px', padding: '0 4px' }}>
                          {m.sentAt ? new Date(m.sentAt).toLocaleString([], { dateStyle: 'short', timeStyle: 'short' }) : ''}
                        </span>
                      </div>
                    );
                  })
                )}
                <div ref={messagesEndRef} />
              </div>

              {/* Bottom Clinical Response Form */}
              <div style={{ padding: '16px 20px', background: '#ffffff', borderTop: '1px solid var(--slate-200)' }}>
                <form onSubmit={handleSendReply} style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                  <label className="form-label" style={{ fontSize: '0.8125rem', color: 'var(--slate-700)', fontWeight: 600 }}>
                    Provide Clinical Guidance / Follow-up Instruction:
                  </label>
                  <textarea
                    rows={3}
                    className="form-control"
                    placeholder="Enter professional clinical response, triage recommendations, or medication instructions..."
                    value={replyText}
                    onChange={(e) => setReplyText(e.target.value)}
                    style={{ resize: 'none' }}
                  />
                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                    <button
                      type="submit"
                      className="btn btn-primary"
                      disabled={!replyText.trim() || replySending}
                      style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}
                    >
                      <Send size={16} />
                      <span>{replySending ? 'Sending Guidance...' : 'Send Clinical Response'}</span>
                    </button>
                  </div>
                </form>
              </div>
            </>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', padding: '40px', color: 'var(--slate-400)' }}>
              <MessageSquare size={54} strokeWidth={1.5} style={{ marginBottom: '16px', color: 'var(--slate-300)' }} />
              <h3 style={{ fontSize: '1.25rem', color: 'var(--slate-700)', marginBottom: '6px' }}>Select an Inquiry</h3>
              <p style={{ textAlign: 'center', maxWidth: '380px', fontSize: '0.9375rem', color: 'var(--slate-500)' }}>
                Select a patient inquiry from the left to view clinical details and provide physician response.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default DoctorPatientQueries;
