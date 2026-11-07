import React, { useState, useEffect } from 'react';
import {
  Calendar,
  Clock,
  AlertCircle,
  CheckCircle2,
  XCircle,
  FileText,
  UserCheck,
  Send,
  CalendarRange,
  Info,
  ShieldCheck,
  Users,
} from 'lucide-react';
import { doctorLeaveService } from '../../services/api';

const DoctorLeavePage = () => {
  const [leaves, setLeaves] = useState([]);
  const [reassignments, setReassignments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [activeTab, setActiveTab] = useState('LEAVES'); // 'LEAVES' or 'REASSIGNMENTS'
  const [feedback, setFeedback] = useState(null);

  const [formData, setFormData] = useState({
    startDate: '',
    endDate: '',
    reason: '',
  });

  const fetchData = async () => {
    try {
      setLoading(true);
      const [leavesRes, reassignRes] = await Promise.all([
        doctorLeaveService.getMyLeaves(),
        doctorLeaveService.getDoctorReassignedHistory(),
      ]);
      setLeaves(leavesRes.data || []);
      setReassignments(reassignRes.data || []);
    } catch (err) {
      console.error('Failed to load leave data:', err);
      setFeedback({ type: 'error', message: 'Failed to load leave history. Please refresh.' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!formData.startDate || !formData.endDate || !formData.reason.trim()) {
      setFeedback({ type: 'error', message: 'Please provide start date, end date, and reason for leave.' });
      return;
    }

    if (new Date(formData.endDate) < new Date(formData.startDate)) {
      setFeedback({ type: 'error', message: 'End date cannot be earlier than start date.' });
      return;
    }

    try {
      setSubmitting(true);
      setFeedback(null);
      await doctorLeaveService.submitLeave({
        startDate: formData.startDate,
        endDate: formData.endDate,
        reason: formData.reason.trim(),
      });
      setFeedback({ type: 'success', message: 'Leave request submitted successfully. Awaiting administrative review.' });
      setFormData({ startDate: '', endDate: '', reason: '' });
      await fetchData();
    } catch (err) {
      console.error('Leave submission failed:', err);
      const msg = err.response?.data?.message || 'Failed to submit leave request.';
      setFeedback({ type: 'error', message: msg });
    } finally {
      setSubmitting(false);
    }
  };

  const getStatusBadge = (status) => {
    switch (status) {
      case 'APPROVED':
        return (
          <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <CheckCircle2 size={13} /> Approved
          </span>
        );
      case 'REJECTED':
        return (
          <span className="badge badge-danger" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <XCircle size={13} /> Rejected
          </span>
        );
      case 'PENDING':
      default:
        return (
          <span className="badge badge-warning" style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
            <Clock size={13} /> Pending Review
          </span>
        );
    }
  };

  const pendingCount = leaves.filter((l) => l.status === 'PENDING').length;
  const approvedCount = leaves.filter((l) => l.status === 'APPROVED').length;

  return (
    <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
      {/* Page Title */}
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Doctor Leave & Coverage</h2>
        <p style={{ color: 'var(--slate-600)' }}>
          Submit leave requests, monitor administrative approval, and view coverage for reassigned appointments.
        </p>
      </div>

      {feedback && (
        <div
          className={`alert ${feedback.type === 'error' ? 'alert-danger' : 'alert-success'}`}
          style={{ marginBottom: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}
        >
          <span>{feedback.message}</span>
          <button
            type="button"
            onClick={() => setFeedback(null)}
            style={{ background: 'none', border: 'none', cursor: 'pointer', fontWeight: 'bold' }}
          >
            ✕
          </button>
        </div>
      )}

      {/* KPI Cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '16px', marginBottom: '24px' }}>
        <div className="card" style={{ padding: '20px', borderLeft: '4px solid var(--primary-500)' }}>
          <div style={{ color: 'var(--slate-500)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase' }}>
            Total Requests
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--slate-900)', marginTop: '4px' }}>
            {leaves.length}
          </div>
        </div>

        <div className="card" style={{ padding: '20px', borderLeft: '4px solid var(--warning-500)' }}>
          <div style={{ color: 'var(--slate-500)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase' }}>
            Pending Review
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--warning-600)', marginTop: '4px' }}>
            {pendingCount}
          </div>
        </div>

        <div className="card" style={{ padding: '20px', borderLeft: '4px solid var(--success-500)' }}>
          <div style={{ color: 'var(--slate-500)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase' }}>
            Approved Leaves
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--success-600)', marginTop: '4px' }}>
            {approvedCount}
          </div>
        </div>

        <div className="card" style={{ padding: '20px', borderLeft: '4px solid var(--teal-500)' }}>
          <div style={{ color: 'var(--slate-500)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase' }}>
            Reassigned Cases
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--teal-600)', marginTop: '4px' }}>
            {reassignments.length}
          </div>
        </div>
      </div>

      {/* Layout Grid: Submission Form + Tabs */}
      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(320px, 1fr) 2fr', gap: '24px', alignItems: 'start' }}>
        {/* Leave Request Form */}
        <div className="card" style={{ padding: '24px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '18px' }}>
            <div
              style={{
                width: '36px',
                height: '36px',
                borderRadius: '8px',
                background: 'var(--primary-100)',
                color: 'var(--primary-700)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
              }}
            >
              <CalendarRange size={20} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.125rem', margin: 0, fontWeight: 700 }}>Request Leave</h3>
              <span style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>Submit planned absence for review</span>
            </div>
          </div>

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Leave Start Date *</label>
              <input
                type="date"
                className="form-control"
                min={new Date().toISOString().split('T')[0]}
                value={formData.startDate}
                onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Leave End Date *</label>
              <input
                type="date"
                className="form-control"
                min={formData.startDate || new Date().toISOString().split('T')[0]}
                value={formData.endDate}
                onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label">Reason for Absence *</label>
              <textarea
                className="form-control"
                rows="4"
                placeholder="E.g., Medical conference, personal leave, surgical rotation..."
                value={formData.reason}
                onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
                required
                maxLength={500}
              />
              <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', marginTop: '4px', textAlign: 'right' }}>
                {formData.reason.length}/500
              </div>
            </div>

            <div
              style={{
                background: 'var(--slate-50)',
                border: '1px solid var(--slate-200)',
                borderRadius: '8px',
                padding: '12px',
                marginBottom: '16px',
                fontSize: '0.8125rem',
                color: 'var(--slate-600)',
                display: 'flex',
                gap: '8px',
              }}
            >
              <Info size={16} style={{ flexShrink: 0, marginTop: '2px', color: 'var(--primary-600)' }} />
              <span>
                Approved leaves automatically flag conflicting patient appointments so administrators can assign qualified replacement physicians of the same specialization.
              </span>
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              disabled={submitting}
              style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
            >
              {submitting ? (
                <>
                  <div className="spinner" style={{ width: '16px', height: '16px' }} />
                  Submitting...
                </>
              ) : (
                <>
                  <Send size={16} />
                  Submit Leave Request
                </>
              )}
            </button>
          </form>
        </div>

        {/* Right Section: Tabs for Leaves & Reassignments */}
        <div className="card" style={{ padding: '0', overflow: 'hidden' }}>
          {/* Tab Header */}
          <div style={{ display: 'flex', borderBottom: '1px solid var(--slate-200)', background: 'var(--slate-50)' }}>
            <button
              type="button"
              onClick={() => setActiveTab('LEAVES')}
              style={{
                padding: '14px 24px',
                fontWeight: 600,
                fontSize: '0.875rem',
                border: 'none',
                background: activeTab === 'LEAVES' ? 'white' : 'transparent',
                color: activeTab === 'LEAVES' ? 'var(--primary-600)' : 'var(--slate-600)',
                borderBottom: activeTab === 'LEAVES' ? '2px solid var(--primary-600)' : 'none',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              <CalendarRange size={16} />
              Leave Requests History ({leaves.length})
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('REASSIGNMENTS')}
              style={{
                padding: '14px 24px',
                fontWeight: 600,
                fontSize: '0.875rem',
                border: 'none',
                background: activeTab === 'REASSIGNMENTS' ? 'white' : 'transparent',
                color: activeTab === 'REASSIGNMENTS' ? 'var(--primary-600)' : 'var(--slate-600)',
                borderBottom: activeTab === 'REASSIGNMENTS' ? '2px solid var(--primary-600)' : 'none',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
              }}
            >
              <UserCheck size={16} />
              Reassigned Consultations ({reassignments.length})
            </button>
          </div>

          <div style={{ padding: '20px' }}>
            {loading ? (
              <div style={{ textAlign: 'center', padding: '40px 20px' }}>
                <div className="spinner" style={{ width: '32px', height: '32px', margin: '0 auto 12px' }} />
                <p style={{ color: 'var(--slate-500)', fontSize: '0.875rem' }}>Loading leave records...</p>
              </div>
            ) : activeTab === 'LEAVES' ? (
              leaves.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--slate-500)' }}>
                  <CalendarRange size={36} style={{ margin: '0 auto 12px', opacity: 0.5 }} />
                  <p>You have not submitted any leave requests yet.</p>
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table className="table" style={{ width: '100%', fontSize: '0.875rem' }}>
                    <thead>
                      <tr>
                        <th>Leave Period</th>
                        <th>Reason</th>
                        <th>Status</th>
                        <th>Admin Feedback / Notes</th>
                        <th>Affected Visits</th>
                      </tr>
                    </thead>
                    <tbody>
                      {leaves.map((l) => (
                        <tr key={l.id}>
                          <td>
                            <div style={{ fontWeight: 600, color: 'var(--slate-900)' }}>
                              {l.startDate} to {l.endDate}
                            </div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                              Submitted: {l.createdAt?.substring(0, 10)}
                            </div>
                          </td>
                          <td style={{ maxWidth: '240px' }}>
                            <div style={{ color: 'var(--slate-700)' }}>{l.reason}</div>
                          </td>
                          <td>{getStatusBadge(l.status)}</td>
                          <td style={{ maxWidth: '220px', color: 'var(--slate-600)' }}>
                            {l.adminNotes || <span style={{ color: 'var(--slate-400)' }}>—</span>}
                            {l.reviewedByName && (
                              <div style={{ fontSize: '0.6875rem', color: 'var(--slate-400)', marginTop: '2px' }}>
                                by {l.reviewedByName}
                              </div>
                            )}
                          </td>
                          <td>
                            <span
                              className="badge"
                              style={{
                                background: l.affectedAppointmentsCount > 0 ? '#fee2e2' : 'var(--slate-100)',
                                color: l.affectedAppointmentsCount > 0 ? '#b91c1c' : 'var(--slate-600)',
                                fontWeight: 700,
                              }}
                            >
                              {l.affectedAppointmentsCount} affected
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )
            ) : reassignments.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 20px', color: 'var(--slate-500)' }}>
                <UserCheck size={36} style={{ margin: '0 auto 12px', opacity: 0.5 }} />
                <p>No consultations have been reassigned to or from your schedule.</p>
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table className="table" style={{ width: '100%', fontSize: '0.875rem' }}>
                  <thead>
                    <tr>
                      <th>Consultation Date</th>
                      <th>Patient</th>
                      <th>Original Doctor</th>
                      <th>Replacement Doctor</th>
                      <th>Patient Consent</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reassignments.map((r) => (
                      <tr key={r.id}>
                        <td>
                          <div style={{ fontWeight: 600, color: 'var(--slate-900)' }}>{r.appointmentDate}</div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>{r.appointmentTime}</div>
                        </td>
                        <td>
                          <div style={{ fontWeight: 600 }}>{r.patientName}</div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>{r.patientEmail}</div>
                        </td>
                        <td>
                          <span style={{ color: 'var(--slate-700)' }}>{r.originalDoctorName}</span>
                        </td>
                        <td>
                          <strong style={{ color: 'var(--primary-700)' }}>{r.replacementDoctorName}</strong>
                          <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                            {r.replacementDoctorSpecialization}
                          </div>
                        </td>
                        <td>
                          {r.patientConsentStatus === 'ACCEPTED' ? (
                            <span className="badge badge-success">Accepted by Patient</span>
                          ) : r.patientConsentStatus === 'REJECTED' ? (
                            <span className="badge badge-danger">Declined by Patient</span>
                          ) : (
                            <span className="badge badge-warning">Awaiting Patient Consent</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DoctorLeavePage;

