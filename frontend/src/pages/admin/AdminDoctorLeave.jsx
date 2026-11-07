import React, { useState, useEffect } from 'react';
import {
  CalendarRange,
  CheckCircle2,
  XCircle,
  Clock,
  UserCheck,
  AlertTriangle,
  Search,
  Filter,
  User,
  Stethoscope,
  Info,
  Send,
  Eye,
  RefreshCw,
} from 'lucide-react';
import { doctorLeaveService } from '../../services/api';

const AdminDoctorLeave = () => {
  const [leaves, setLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [feedback, setFeedback] = useState(null);

  // Review Modal State
  const [selectedLeaveForReview, setSelectedLeaveForReview] = useState(null);
  const [reviewAction, setReviewAction] = useState('APPROVED');
  const [adminNotes, setAdminNotes] = useState('');
  const [reviewing, setReviewing] = useState(false);

  // Affected Appointments Drawer/Modal State
  const [selectedLeaveForAppointments, setSelectedLeaveForAppointments] = useState(null);
  const [affectedAppointments, setAffectedAppointments] = useState([]);
  const [loadingAffected, setLoadingAffected] = useState(false);

  // Replacement Candidate Selector State
  const [selectedAppointmentForReassignment, setSelectedAppointmentForReassignment] = useState(null);
  const [candidates, setCandidates] = useState([]);
  const [loadingCandidates, setLoadingCandidates] = useState(false);
  const [selectedReplacementDoctorId, setSelectedReplacementDoctorId] = useState('');
  const [reassignAdminNotes, setReassignAdminNotes] = useState('');
  const [reassigning, setReassigning] = useState(false);

  const fetchLeaves = async () => {
    try {
      setLoading(true);
      const res = await doctorLeaveService.getAllLeaves(statusFilter);
      setLeaves(res.data || []);
    } catch (err) {
      console.error('Failed to load leaves:', err);
      setFeedback({ type: 'error', message: 'Failed to fetch doctor leave requests.' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLeaves();
  }, [statusFilter]);

  const handleReviewSubmit = async (e) => {
    e.preventDefault();
    if (!selectedLeaveForReview) return;

    try {
      setReviewing(true);
      await doctorLeaveService.reviewLeave(selectedLeaveForReview.id, {
        status: reviewAction,
        adminNotes: adminNotes.trim(),
      });
      setFeedback({
        type: 'success',
        message: `Leave request for ${selectedLeaveForReview.doctorName} was ${reviewAction.toLowerCase()} successfully.`,
      });
      setSelectedLeaveForReview(null);
      setAdminNotes('');
      await fetchLeaves();
    } catch (err) {
      console.error('Failed to review leave:', err);
      setFeedback({ type: 'error', message: err.response?.data?.message || 'Review failed.' });
    } finally {
      setReviewing(false);
    }
  };

  const handleOpenAffected = async (leave) => {
    setSelectedLeaveForAppointments(leave);
    try {
      setLoadingAffected(true);
      const res = await doctorLeaveService.getAffectedAppointments(leave.id);
      setAffectedAppointments(res.data || []);
    } catch (err) {
      console.error('Failed to load affected appointments:', err);
      setFeedback({ type: 'error', message: 'Failed to load affected appointments.' });
    } finally {
      setLoadingAffected(false);
    }
  };

  const handleOpenCandidates = async (appt) => {
    setSelectedAppointmentForReassignment(appt);
    setSelectedReplacementDoctorId('');
    setReassignAdminNotes('');
    try {
      setLoadingCandidates(true);
      const res = await doctorLeaveService.getReplacementCandidates(appt.appointmentId);
      setCandidates(res.data || []);
      // Auto-select first recommended candidate
      const recommended = res.data?.find((c) => c.recommended);
      if (recommended) {
        setSelectedReplacementDoctorId(recommended.doctorId);
      }
    } catch (err) {
      console.error('Failed to load candidates:', err);
      setFeedback({ type: 'error', message: 'Failed to load replacement doctor candidates.' });
    } finally {
      setLoadingCandidates(false);
    }
  };

  const handleReassignSubmit = async (e) => {
    e.preventDefault();
    if (!selectedAppointmentForReassignment || !selectedReplacementDoctorId) {
      setFeedback({ type: 'error', message: 'Please select a replacement doctor.' });
      return;
    }

    try {
      setReassigning(true);
      await doctorLeaveService.reassignAppointment(selectedAppointmentForReassignment.appointmentId, {
        replacementDoctorId: selectedReplacementDoctorId,
        adminNotes: reassignAdminNotes.trim(),
      });
      setFeedback({
        type: 'success',
        message: 'Replacement doctor proposed successfully. Patient notified for consent.',
      });
      setSelectedAppointmentForReassignment(null);
      // Refresh affected list
      if (selectedLeaveForAppointments) {
        const res = await doctorLeaveService.getAffectedAppointments(selectedLeaveForAppointments.id);
        setAffectedAppointments(res.data || []);
      }
    } catch (err) {
      console.error('Reassignment failed:', err);
      setFeedback({ type: 'error', message: err.response?.data?.message || 'Reassignment failed.' });
    } finally {
      setReassigning(false);
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
  const totalAffected = leaves.reduce((acc, curr) => acc + (curr.affectedAppointmentsCount || 0), 0);

  return (
    <div style={{ maxWidth: '1280px', margin: '0 auto' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Doctor Leave & Reassignment Governance</h2>
          <p style={{ color: 'var(--slate-600)' }}>
            Review doctor leave requests, evaluate affected patient appointments, and assign qualified replacement physicians.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-secondary"
          onClick={fetchLeaves}
          style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}
        >
          <RefreshCw size={16} /> Refresh
        </button>
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
            Pending Approval
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

        <div className="card" style={{ padding: '20px', borderLeft: '4px solid #ef4444' }}>
          <div style={{ color: 'var(--slate-500)', fontSize: '0.8125rem', fontWeight: 600, textTransform: 'uppercase' }}>
            Affected Appointments
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#b91c1c', marginTop: '4px' }}>
            {totalAffected}
          </div>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="card" style={{ padding: '16px', marginBottom: '20px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Filter size={18} style={{ color: 'var(--slate-500)' }} />
          <span style={{ fontWeight: 600, fontSize: '0.875rem', color: 'var(--slate-700)' }}>Filter by Status:</span>
          {['ALL', 'PENDING', 'APPROVED', 'REJECTED'].map((st) => (
            <button
              key={st}
              type="button"
              className={`btn ${statusFilter === st ? 'btn-primary' : 'btn-secondary'} btn-sm`}
              onClick={() => setStatusFilter(st)}
            >
              {st}
            </button>
          ))}
        </div>
      </div>

      {/* Leave Requests Table */}
      <div className="card" style={{ padding: '0', overflow: 'hidden' }}>
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px 20px' }}>
            <div className="spinner" style={{ width: '32px', height: '32px', margin: '0 auto 12px' }} />
            <p style={{ color: 'var(--slate-500)', fontSize: '0.875rem' }}>Loading leave requests...</p>
          </div>
        ) : leaves.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '50px 20px', color: 'var(--slate-500)' }}>
            <CalendarRange size={40} style={{ margin: '0 auto 12px', opacity: 0.5 }} />
            <p>No doctor leave requests found matching filter.</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="table" style={{ width: '100%', fontSize: '0.875rem' }}>
              <thead>
                <tr>
                  <th>Physician</th>
                  <th>Leave Period</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th>Admin Notes</th>
                  <th>Affected Visits</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {leaves.map((l) => (
                  <tr key={l.id}>
                    <td>
                      <div style={{ fontWeight: 700, color: 'var(--slate-900)' }}>{l.doctorName}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>{l.doctorSpecialization}</div>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{l.startDate} to {l.endDate}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                        Requested: {l.createdAt?.substring(0, 10)}
                      </div>
                    </td>
                    <td style={{ maxWidth: '240px' }}>
                      <div style={{ color: 'var(--slate-700)' }}>{l.reason}</div>
                    </td>
                    <td>{getStatusBadge(l.status)}</td>
                    <td style={{ maxWidth: '200px', color: 'var(--slate-600)' }}>
                      {l.adminNotes || <span style={{ color: 'var(--slate-400)' }}>—</span>}
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
                        {l.affectedAppointmentsCount} consultations
                      </span>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '8px' }}>
                        {l.status === 'PENDING' && (
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            onClick={() => {
                              setSelectedLeaveForReview(l);
                              setReviewAction('APPROVED');
                              setAdminNotes('');
                            }}
                          >
                            Review
                          </button>
                        )}
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => handleOpenAffected(l)}
                          style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}
                        >
                          <Eye size={13} /> Affected ({l.affectedAppointmentsCount})
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Review Modal */}
      {selectedLeaveForReview && (
        <div className="modal-backdrop">
          <div className="modal-content" style={{ maxWidth: '500px' }}>
            <div className="modal-header">
              <h3 className="modal-title">Review Doctor Leave</h3>
              <button
                type="button"
                className="modal-close"
                onClick={() => setSelectedLeaveForReview(null)}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleReviewSubmit}>
              <div style={{ marginBottom: '16px' }}>
                <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--slate-900)' }}>
                  {selectedLeaveForReview.doctorName} ({selectedLeaveForReview.doctorSpecialization})
                </div>
                <div style={{ fontSize: '0.875rem', color: 'var(--slate-600)', marginTop: '4px' }}>
                  Period: <strong>{selectedLeaveForReview.startDate}</strong> to <strong>{selectedLeaveForReview.endDate}</strong>
                </div>
                <div style={{ fontSize: '0.875rem', color: 'var(--slate-700)', marginTop: '4px' }}>
                  Reason: "{selectedLeaveForReview.reason}"
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Administrative Decision *</label>
                <div style={{ display: 'flex', gap: '16px' }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}>
                    <input
                      type="radio"
                      name="decision"
                      value="APPROVED"
                      checked={reviewAction === 'APPROVED'}
                      onChange={() => setReviewAction('APPROVED')}
                    />
                    <span style={{ color: 'var(--success-700)', fontWeight: 600 }}>Approve Leave</span>
                  </label>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer' }}>
                    <input
                      type="radio"
                      name="decision"
                      value="REJECTED"
                      checked={reviewAction === 'REJECTED'}
                      onChange={() => setReviewAction('REJECTED')}
                    />
                    <span style={{ color: 'var(--danger-700)', fontWeight: 600 }}>Reject Leave</span>
                  </label>
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Administrative Notes / Comments</label>
                <textarea
                  className="form-control"
                  rows="3"
                  placeholder="Provide feedback or justification..."
                  value={adminNotes}
                  onChange={(e) => setAdminNotes(e.target.value)}
                  maxLength={1000}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setSelectedLeaveForReview(null)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className={`btn ${reviewAction === 'APPROVED' ? 'btn-primary' : 'btn-danger'}`}
                  disabled={reviewing}
                >
                  {reviewing ? 'Processing...' : `Confirm ${reviewAction}`}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Affected Appointments Modal */}
      {selectedLeaveForAppointments && (
        <div className="modal-backdrop">
          <div className="modal-content" style={{ maxWidth: '900px' }}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">
                  Affected Consultations: Dr. {selectedLeaveForAppointments.doctorName}
                </h3>
                <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)' }}>
                  Leave: {selectedLeaveForAppointments.startDate} to {selectedLeaveForAppointments.endDate} • {selectedLeaveForAppointments.doctorSpecialization}
                </div>
              </div>
              <button
                type="button"
                className="modal-close"
                onClick={() => setSelectedLeaveForAppointments(null)}
              >
                ✕
              </button>
            </div>

            <div style={{ padding: '16px 0' }}>
              {loadingAffected ? (
                <div style={{ textAlign: 'center', padding: '30px' }}>
                  <div className="spinner" style={{ width: '28px', height: '28px', margin: '0 auto 8px' }} />
                  <p style={{ color: 'var(--slate-500)', fontSize: '0.875rem' }}>Analyzing appointments...</p>
                </div>
              ) : affectedAppointments.length === 0 ? (
                <div style={{ textAlign: 'center', padding: '30px', color: 'var(--slate-500)' }}>
                  <CheckCircle2 size={36} style={{ color: 'var(--success-500)', margin: '0 auto 8px' }} />
                  <p>No booked patient appointments fall within this leave window.</p>
                </div>
              ) : (
                <div style={{ overflowX: 'auto' }}>
                  <table className="table" style={{ width: '100%', fontSize: '0.875rem' }}>
                    <thead>
                      <tr>
                        <th>Date & Time</th>
                        <th>Patient Name</th>
                        <th>Reason</th>
                        <th>Reassignment Status</th>
                        <th>Replacement Doctor</th>
                        <th style={{ textAlign: 'right' }}>Action</th>
                      </tr>
                    </thead>
                    <tbody>
                      {affectedAppointments.map((appt) => (
                        <tr key={appt.appointmentId}>
                          <td>
                            <div style={{ fontWeight: 600 }}>{appt.appointmentDate}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>{appt.appointmentTime}</div>
                          </td>
                          <td>
                            <div style={{ fontWeight: 600 }}>{appt.patientName}</div>
                            <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>{appt.patientEmail}</div>
                          </td>
                          <td>{appt.appointmentReason}</td>
                          <td>
                            {appt.patientConsentStatus === 'ACCEPTED' ? (
                              <span className="badge badge-success">Accepted</span>
                            ) : appt.patientConsentStatus === 'PROPOSED' ? (
                              <span className="badge badge-warning">Proposed</span>
                            ) : appt.patientConsentStatus === 'REJECTED' ? (
                              <span className="badge badge-danger">Declined</span>
                            ) : (
                              <span className="badge" style={{ background: '#fee2e2', color: '#b91c1c' }}>
                                Unassigned
                              </span>
                            )}
                          </td>
                          <td>
                            {appt.replacementDoctorName ? (
                              <div>
                                <div style={{ fontWeight: 600 }}>{appt.replacementDoctorName}</div>
                                <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                                  {appt.replacementDoctorSpecialization}
                                </div>
                              </div>
                            ) : (
                              <span style={{ color: 'var(--slate-400)' }}>—</span>
                            )}
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <button
                              type="button"
                              className="btn btn-primary btn-sm"
                              onClick={() => handleOpenCandidates(appt)}
                              disabled={appt.patientConsentStatus === 'ACCEPTED'}
                            >
                              {appt.replacementDoctorName ? 'Change Replacement' : 'Find Replacement'}
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '16px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setSelectedLeaveForAppointments(null)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Replacement Candidate Selection Modal */}
      {selectedAppointmentForReassignment && (
        <div className="modal-backdrop">
          <div className="modal-content" style={{ maxWidth: '720px' }}>
            <div className="modal-header">
              <div>
                <h3 className="modal-title">Recommend Replacement Physician</h3>
                <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)' }}>
                  Appointment #{selectedAppointmentForReassignment.appointmentId} • Patient: {selectedAppointmentForReassignment.patientName} • {selectedAppointmentForReassignment.appointmentDate} at {selectedAppointmentForReassignment.appointmentTime}
                </div>
              </div>
              <button
                type="button"
                className="modal-close"
                onClick={() => setSelectedAppointmentForReassignment(null)}
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleReassignSubmit}>
              <div style={{ padding: '16px 0' }}>
                <div style={{ fontSize: '0.875rem', color: 'var(--slate-700)', marginBottom: '14px' }}>
                  Evaluating qualified physicians in <strong>{selectedAppointmentForReassignment.originalDoctorSpecialization}</strong> with schedule availability and zero collision:
                </div>

                {loadingCandidates ? (
                  <div style={{ textAlign: 'center', padding: '30px' }}>
                    <div className="spinner" style={{ width: '28px', height: '28px', margin: '0 auto 8px' }} />
                    <p style={{ color: 'var(--slate-500)', fontSize: '0.875rem' }}>Scanning doctor availability...</p>
                  </div>
                ) : candidates.length === 0 ? (
                  <div style={{ textAlign: 'center', padding: '30px', color: 'var(--slate-500)' }}>
                    <AlertTriangle size={36} style={{ color: '#d97706', margin: '0 auto 8px' }} />
                    <p>No available replacement doctors found in {selectedAppointmentForReassignment.originalDoctorSpecialization}.</p>
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', maxHeight: '340px', overflowY: 'auto' }}>
                    {candidates.map((c) => (
                      <label
                        key={c.doctorId}
                        style={{
                          display: 'flex',
                          alignItems: 'flex-start',
                          gap: '12px',
                          padding: '12px 16px',
                          borderRadius: '8px',
                          border: `1px solid ${selectedReplacementDoctorId === c.doctorId ? 'var(--primary-500)' : 'var(--slate-200)'}`,
                          background: selectedReplacementDoctorId === c.doctorId ? 'var(--primary-50)' : c.recommended ? '#f0fdf4' : 'var(--slate-50)',
                          cursor: c.recommended ? 'pointer' : 'not-allowed',
                          opacity: c.recommended ? 1 : 0.6,
                        }}
                      >
                        <input
                          type="radio"
                          name="replacementDoctor"
                          value={c.doctorId}
                          checked={selectedReplacementDoctorId === c.doctorId}
                          onChange={() => setSelectedReplacementDoctorId(c.doctorId)}
                          disabled={!c.recommended}
                          style={{ marginTop: '4px' }}
                        />
                        <div style={{ flex: 1 }}>
                          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '8px' }}>
                            <div style={{ fontWeight: 700, color: 'var(--slate-900)' }}>{c.doctorName}</div>
                            {c.recommended ? (
                              <span className="badge badge-success" style={{ fontSize: '0.6875rem' }}>
                                ✓ Eligible Replacement
                              </span>
                            ) : (
                              <span className="badge badge-danger" style={{ fontSize: '0.6875rem' }}>
                                Unavailable
                              </span>
                            )}
                          </div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)', marginTop: '2px' }}>
                            {c.specialization} • Experience: {c.experienceYears} yrs • Fee: ${c.consultationFee}
                          </div>
                          <div style={{ fontSize: '0.75rem', color: c.recommended ? 'var(--success-700)' : '#b91c1c', marginTop: '4px' }}>
                            {c.matchExplanation}
                          </div>
                        </div>
                      </label>
                    ))}
                  </div>
                )}

                <div className="form-group" style={{ marginTop: '16px' }}>
                  <label className="form-label">Administrative Guidance Notes for Patient</label>
                  <textarea
                    className="form-control"
                    rows="2"
                    placeholder="E.g., Board-certified specialist covering Dr. House's clinical follow-ups during leave..."
                    value={reassignAdminNotes}
                    onChange={(e) => setReassignAdminNotes(e.target.value)}
                    maxLength={500}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '16px' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setSelectedAppointmentForReassignment(null)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={!selectedReplacementDoctorId || reassigning}
                  style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                >
                  <Send size={15} />
                  {reassigning ? 'Dispatching...' : 'Propose Replacement & Notify Patient'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDoctorLeave;

