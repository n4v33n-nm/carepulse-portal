import React, { useState, useEffect, useRef } from 'react';
import {
  MessageSquare,
  Send,
  PlusCircle,
  Clock,
  CheckCircle2,
  Stethoscope,
  User,
  AlertCircle,
  Calendar,
  Search,
  Filter,
} from 'lucide-react';
import { doctorConnectService, doctorService, appointmentService } from '../../services/api';
import Modal from '../../components/Modal';

const DoctorConnectPage = () => {
  const [conversations, setConversations] = useState([]);
  const [selectedConv, setSelectedConv] = useState(null);
  const [loading, setLoading] = useState(true);
  const [messagesLoading, setMessagesLoading] = useState(false);
  const [replyText, setReplyText] = useState('');
  const [replySending, setReplySending] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // New Inquiry Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [doctors, setDoctors] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [selectedDoctorId, setSelectedDoctorId] = useState('');
  const [selectedApptId, setSelectedApptId] = useState('');
  const [subject, setSubject] = useState('');
  const [message, setMessage] = useState('');
  const [modalLoading, setModalLoading] = useState(false);
  const [modalError, setModalError] = useState('');

  const messagesEndRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    fetchConversations();
    loadPrerequisites();
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [selectedConv?.messages]);

  const loadPrerequisites = async () => {
    try {
      const [docsRes, apptsRes] = await Promise.all([
        doctorService.getAllDoctors().catch(() => ({ data: [] })),
        appointmentService.getMyAppointments().catch(() => ({ data: [] })),
      ]);
      setDoctors(docsRes.data || []);
      setAppointments(apptsRes.data || []);
      if (docsRes.data?.length > 0) {
        setSelectedDoctorId(docsRes.data[0].id);
      }
    } catch (err) {
      console.error('Failed to load doctors/appointments', err);
    }
  };

  const fetchConversations = async (autoSelectId = null) => {
    setLoading(true);
    try {
      const res = await doctorConnectService.getConversations();
      const list = res.data || [];
      setConversations(list);

      if (autoSelectId) {
        loadConversationDetails(autoSelectId);
      } else if (list.length > 0 && !selectedConv) {
        loadConversationDetails(list[0].id);
      }
    } catch (err) {
      console.error('Failed to load conversations', err);
    } finally {
      setLoading(false);
    }
  };

  const loadConversationDetails = async (id) => {
    setMessagesLoading(true);
    try {
      const res = await doctorConnectService.getConversationById(id);
      setSelectedConv(res.data);
      // Update unread count locally in the list
      setConversations((prev) =>
        prev.map((c) => (c.id === id ? { ...c, unreadMessagesCount: 0 } : c))
      );
    } catch (err) {
      console.error('Failed to load conversation details', err);
    } finally {
      setMessagesLoading(false);
    }
  };

  const handleCreateInquiry = async (e) => {
    e.preventDefault();
    if (!selectedDoctorId || !subject.trim() || !message.trim()) {
      setModalError('Please fill in all mandatory fields');
      return;
    }

    setModalLoading(true);
    setModalError('');
    try {
      const payload = {
        doctorId: Number(selectedDoctorId),
        appointmentId: selectedApptId ? Number(selectedApptId) : null,
        subject: subject.trim(),
        message: message.trim(),
      };
      const res = await doctorConnectService.createConversation(payload);
      setIsModalOpen(false);
      setSubject('');
      setMessage('');
      setSelectedApptId('');
      fetchConversations(res.data.id);
    } catch (err) {
      setModalError(err.response?.data?.message || 'Failed to submit inquiry. Please try again.');
    } finally {
      setModalLoading(false);
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
        status: 'OPEN',
        messages: [...(prev.messages || []), res.data],
      }));
      setReplyText('');
      // Refresh list to update status and snippet
      fetchConversations(selectedConv.id);
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to send message');
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
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update inquiry status');
    }
  };

  const filteredConversations = conversations.filter((c) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      c.subject?.toLowerCase().includes(q) ||
      c.doctorName?.toLowerCase().includes(q) ||
      c.doctorSpecialization?.toLowerCase().includes(q)
    );
  });

  const getStatusBadge = (status) => {
    switch (status) {
      case 'ANSWERED':
        return (
          <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <CheckCircle2 size={12} /> Doctor Replied
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
            <Clock size={12} /> Awaiting Response
          </span>
        );
    }
  };

  return (
    <div>
      {/* Page Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>DoctorConnect</h2>
          <p style={{ color: 'var(--slate-600)' }}>
            Direct asynchronous clinical communication channel with your appointed healthcare specialists.
          </p>
        </div>

        <button
          className="btn btn-primary"
          onClick={() => {
            setModalError('');
            setIsModalOpen(true);
          }}
          style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}
        >
          <PlusCircle size={18} /> Ask a Doctor
        </button>
      </div>

      {/* Main Communication Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(300px, 360px) 1fr', gap: '24px', minHeight: '620px' }}>
        {/* Left: Conversation Thread List */}
        <div className="card" style={{ padding: '16px', display: 'flex', flexDirection: 'column' }}>
          <div style={{ position: 'relative', marginBottom: '16px' }}>
            <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: 'var(--slate-400)' }} />
            <input
              type="text"
              placeholder="Search inquiries or doctors..."
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
                <p style={{ fontWeight: 600, fontSize: '0.9375rem', marginBottom: '4px' }}>No inquiries yet</p>
                <p style={{ fontSize: '0.8125rem' }}>Click "Ask a Doctor" above to submit your first clinical question.</p>
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

                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8125rem', color: 'var(--slate-600)', marginBottom: '8px' }}>
                      <Stethoscope size={14} style={{ color: 'var(--primary-600)' }} />
                      <span>{c.doctorName}</span>
                    </div>

                    <p style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginBottom: '6px' }}>
                      {c.lastMessageSnippet || 'No messages'}
                    </p>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '0.75rem', color: 'var(--slate-400)' }}>
                      <span>{c.updatedAt ? new Date(c.updatedAt).toLocaleDateString() : ''}</span>
                      {c.unreadMessagesCount > 0 && (
                        <span style={{ background: 'var(--primary-600)', color: 'white', padding: '2px 6px', borderRadius: '10px', fontWeight: 700, fontSize: '0.6875rem' }}>
                          {c.unreadMessagesCount} new
                        </span>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Right: Active Conversation Thread */}
        <div className="card" style={{ padding: '0', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          {selectedConv ? (
            <>
              {/* Header */}
              <div style={{ padding: '20px', borderBottom: '1px solid var(--slate-200)', background: '#ffffff', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '12px' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '4px' }}>
                    <h3 style={{ fontSize: '1.25rem', color: 'var(--slate-900)' }}>{selectedConv.subject}</h3>
                    {getStatusBadge(selectedConv.status)}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '0.875rem', color: 'var(--slate-600)' }}>
                    <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                      <Stethoscope size={15} style={{ color: 'var(--primary-600)' }} />
                      <strong>Physician:</strong> {selectedConv.doctorName} ({selectedConv.doctorSpecialization})
                    </span>
                    {selectedConv.appointmentDate && (
                      <span style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                        <Calendar size={15} /> Appointment: {selectedConv.appointmentDate}
                      </span>
                    )}
                  </div>
                </div>

                <div>
                  {selectedConv.status === 'RESOLVED' ? (
                    <button
                      className="btn btn-secondary btn-sm"
                      onClick={() => handleUpdateStatus('OPEN')}
                      title="Re-open this consultation inquiry"
                    >
                      Re-open Inquiry
                    </button>
                  ) : (
                    <button
                      className="btn btn-secondary btn-sm"
                      onClick={() => handleUpdateStatus('RESOLVED')}
                      style={{ color: 'var(--success-700)', borderColor: 'var(--success-300)' }}
                      title="Mark as resolved when you have received satisfactory answers"
                    >
                      <CheckCircle2 size={15} style={{ marginRight: '6px' }} /> Mark as Resolved
                    </button>
                  )}
                </div>
              </div>

              {/* Message History Stream */}
              <div style={{ flex: 1, padding: '24px', overflowY: 'auto', background: 'var(--slate-50)', display: 'flex', flexDirection: 'column', gap: '16px', maxHeight: '460px' }}>
                {messagesLoading ? (
                  <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-500)' }}>
                    Loading message history...
                  </div>
                ) : selectedConv.messages?.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-500)' }}>
                    No messages yet.
                  </div>
                ) : (
                  selectedConv.messages.map((m) => {
                    const isDoctor = m.senderRole === 'DOCTOR';
                    return (
                      <div
                        key={m.id}
                        style={{
                          display: 'flex',
                          flexDirection: 'column',
                          alignItems: isDoctor ? 'flex-start' : 'flex-end',
                        }}
                      >
                        <div
                          style={{
                            maxWidth: '75%',
                            padding: '14px 18px',
                            borderRadius: 'var(--radius-lg)',
                            background: isDoctor ? '#ffffff' : 'var(--primary-600)',
                            color: isDoctor ? 'var(--slate-900)' : '#ffffff',
                            border: isDoctor ? '1px solid var(--slate-200)' : 'none',
                            boxShadow: 'var(--shadow-sm)',
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.75rem', fontWeight: 700, marginBottom: '6px', color: isDoctor ? 'var(--primary-700)' : 'rgba(255, 255, 255, 0.9)' }}>
                            {isDoctor ? <Stethoscope size={13} /> : <User size={13} />}
                            <span>{m.senderName}</span>
                            {isDoctor && (
                              <span style={{ background: 'var(--primary-100)', color: 'var(--primary-800)', padding: '1px 6px', borderRadius: '4px', fontSize: '0.6875rem' }}>
                                Verified Physician
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

              {/* Bottom Reply Form */}
              <div style={{ padding: '16px 20px', background: '#ffffff', borderTop: '1px solid var(--slate-200)' }}>
                {selectedConv.status === 'RESOLVED' && (
                  <div style={{ background: 'var(--slate-100)', color: 'var(--slate-600)', padding: '8px 12px', borderRadius: '6px', fontSize: '0.8125rem', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <AlertCircle size={14} />
                    <span>This consultation inquiry is marked as resolved. Sending a follow-up question will reopen it.</span>
                  </div>
                )}
                <form onSubmit={handleSendReply} style={{ display: 'flex', gap: '12px' }}>
                  <textarea
                    rows={2}
                    className="form-control"
                    placeholder="Type your follow-up medical question..."
                    value={replyText}
                    onChange={(e) => setReplyText(e.target.value)}
                    style={{ resize: 'none' }}
                  />
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={!replyText.trim() || replySending}
                    style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '0 20px', gap: '8px' }}
                  >
                    <Send size={18} />
                    <span>{replySending ? 'Sending...' : 'Send'}</span>
                  </button>
                </form>
              </div>
            </>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', height: '100%', padding: '40px', color: 'var(--slate-400)' }}>
              <MessageSquare size={54} strokeWidth={1.5} style={{ marginBottom: '16px', color: 'var(--slate-300)' }} />
              <h3 style={{ fontSize: '1.25rem', color: 'var(--slate-700)', marginBottom: '6px' }}>Select an Inquiry</h3>
              <p style={{ textAlign: 'center', maxWidth: '380px', fontSize: '0.9375rem', color: 'var(--slate-500)' }}>
                Select an existing consultation inquiry from the left, or click "Ask a Doctor" to begin a new discussion.
              </p>
            </div>
          )}
        </div>
      </div>

      {/* Modal: New Inquiry */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Ask a Health Question (DoctorConnect)"
      >
        <form onSubmit={handleCreateInquiry}>
          {modalError && (
            <div className="alert alert-danger" style={{ marginBottom: '16px' }}>
              <AlertCircle size={18} />
              <span>{modalError}</span>
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Select Consulting Physician *</label>
            <select
              className="form-control"
              value={selectedDoctorId}
              onChange={(e) => setSelectedDoctorId(e.target.value)}
              required
            >
              {doctors.map((d) => (
                <option key={d.id} value={d.id}>
                  Dr. {d.fullName} ({d.specialization}) - {d.hospitalAffiliation || 'CarePulse'}
                </option>
              ))}
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Related Appointment (Optional)</label>
            <select
              className="form-control"
              value={selectedApptId}
              onChange={(e) => setSelectedApptId(e.target.value)}
            >
              <option value="">-- General Health Inquiry (No Specific Appointment) --</option>
              {appointments
                .filter((a) => !selectedDoctorId || a.doctor?.id === Number(selectedDoctorId))
                .map((a) => (
                  <option key={a.id} value={a.id}>
                    Appointment #{a.id} on {a.appointmentDate} at {a.appointmentTime} (Status: {a.status})
                  </option>
                ))}
            </select>
          </div>

          <div className="form-group">
            <label className="form-label">Subject / Primary Complaint *</label>
            <input
              type="text"
              className="form-control"
              placeholder="e.g. Follow-up regarding blood pressure medication"
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              maxLength={200}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Your Question / Clinical Context *</label>
            <textarea
              className="form-control"
              rows={4}
              placeholder="Describe your symptoms, questions, medication questions or observations in detail..."
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              maxLength={4000}
              required
            />
            <small style={{ color: 'var(--slate-500)', fontSize: '0.75rem' }}>
              Your physician will review your clinical history alongside this inquiry and reply in DoctorConnect.
            </small>
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setIsModalOpen(false)}
              disabled={modalLoading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={modalLoading}
            >
              {modalLoading ? 'Submitting Inquiry...' : 'Submit to Doctor'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default DoctorConnectPage;
