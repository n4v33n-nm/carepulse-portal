import React, { useState, useEffect } from 'react';
import {
  Calendar,
  Clock,
  CheckCircle2,
  XCircle,
  FileText,
  AlertCircle,
  Check,
  X,
  PlusCircle,
} from 'lucide-react';
import { appointmentService } from '../../services/api';
import Modal from '../../components/Modal';

const DoctorAppointments = () => {
  const [appointments, setAppointments] = useState([]);
  const [activeTab, setActiveTab] = useState('ALL');
  const [loading, setLoading] = useState(true);

  // Status Action Modal
  const [selectedAppt, setSelectedAppt] = useState(null);
  const [modalMode, setModalMode] = useState(null); // 'COMPLETE' | 'CANCEL'
  const [consultationNotes, setConsultationNotes] = useState('');
  const [cancellationReason, setCancellationReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  useEffect(() => {
    fetchAppointments();
  }, []);

  const fetchAppointments = async () => {
    setLoading(true);
    try {
      const res = await appointmentService.getMyAppointments();
      setAppointments(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleConfirm = async (apptId) => {
    try {
      await appointmentService.updateStatus(apptId, { status: 'CONFIRMED' });
      fetchAppointments();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to confirm appointment');
    }
  };

  const openCompleteModal = (appt) => {
    setSelectedAppt(appt);
    setConsultationNotes(appt.consultationNotes || '');
    setModalMode('COMPLETE');
  };

  const openCancelModal = (appt) => {
    setSelectedAppt(appt);
    setCancellationReason('');
    setModalMode('CANCEL');
  };

  const handleModalSubmit = async (e) => {
    e.preventDefault();
    if (!selectedAppt) return;
    setActionLoading(true);

    try {
      if (modalMode === 'COMPLETE') {
        await appointmentService.updateStatus(selectedAppt.id, {
          status: 'COMPLETED',
          consultationNotes: consultationNotes,
        });
      } else if (modalMode === 'CANCEL') {
        await appointmentService.updateStatus(selectedAppt.id, {
          status: 'CANCELLED',
          cancellationReason: cancellationReason || 'Cancelled by physician',
        });
      }
      setModalMode(null);
      fetchAppointments();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update appointment');
    } finally {
      setActionLoading(false);
    }
  };

  const filtered = appointments.filter((a) => {
    if (activeTab === 'ALL') return true;
    return a.status.toUpperCase() === activeTab;
  });

  return (
    <div>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Manage Clinical Appointments</h2>
        <p style={{ color: 'var(--slate-600)' }}>
          Review scheduled consultations, log clinical consultation notes, and confirm patient booking requests.
        </p>
      </div>

      {/* Status Filter Tabs */}
      <div
        style={{
          display: 'flex',
          gap: '8px',
          borderBottom: '1px solid var(--slate-200)',
          paddingBottom: '12px',
          marginBottom: '24px',
        }}
      >
        {['ALL', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].map((tab) => (
          <button
            key={tab}
            className={`btn btn-sm ${activeTab === tab ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab(tab)}
          >
            {tab.charAt(0) + tab.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {/* Appointment Table */}
      <div className="card">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '36px', color: 'var(--slate-500)' }}>
            Loading appointments...
          </div>
        ) : filtered.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 16px', color: 'var(--slate-500)' }}>
            No appointments found in this category.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Patient</th>
                  <th>Date & Time</th>
                  <th>Reason / Symptoms</th>
                  <th>Status</th>
                  <th>Notes</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((appt) => (
                  <tr key={appt.id}>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--slate-900)' }}>
                        {appt.patient.fullName}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                        {appt.patient.gender} • {appt.patient.bloodGroup || 'Blood: N/A'}
                      </div>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{appt.appointmentDate}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>{appt.appointmentTime}</div>
                    </td>
                    <td style={{ maxWidth: '220px', fontSize: '0.8125rem' }}>{appt.reason}</td>
                    <td>
                      <span className={`badge badge-${appt.status.toLowerCase()}`}>
                        {appt.status}
                      </span>
                    </td>
                    <td style={{ maxWidth: '200px', fontSize: '0.8125rem', color: 'var(--slate-600)' }}>
                      {appt.consultationNotes || appt.cancellationReason || '—'}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '6px' }}>
                        {appt.status === 'PENDING' && (
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            style={{ background: '#10b981' }}
                            onClick={() => handleConfirm(appt.id)}
                            title="Confirm Appointment"
                          >
                            <Check size={14} /> Accept
                          </button>
                        )}

                        {appt.status === 'CONFIRMED' && (
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            onClick={() => openCompleteModal(appt)}
                            title="Complete Consultation"
                          >
                            <CheckCircle2 size={14} /> Complete
                          </button>
                        )}

                        {(appt.status === 'PENDING' || appt.status === 'CONFIRMED') && (
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            style={{ color: '#ef4444' }}
                            onClick={() => openCancelModal(appt)}
                            title="Cancel Appointment"
                          >
                            <X size={14} /> Cancel
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Complete or Cancel Modal */}
      <Modal
        isOpen={!!modalMode}
        onClose={() => setModalMode(null)}
        title={
          modalMode === 'COMPLETE'
            ? `Complete Consultation – ${selectedAppt?.patient.fullName}`
            : `Cancel Consultation – ${selectedAppt?.patient.fullName}`
        }
      >
        <form onSubmit={handleModalSubmit}>
          {modalMode === 'COMPLETE' ? (
            <div>
              <p style={{ fontSize: '0.875rem', color: 'var(--slate-600)', marginBottom: '14px' }}>
                Conclude the appointment and enter consultation remarks. These notes will be saved and visible to the patient.
              </p>
              <div className="form-group">
                <label className="form-label" htmlFor="consult-notes-input">Consultation Notes & Remarks</label>
                <textarea
                  id="consult-notes-input"
                  className="form-control"
                  rows="4"
                  placeholder="Record summary of examination, vital signs, recommendations..."
                  value={consultationNotes}
                  onChange={(e) => setConsultationNotes(e.target.value)}
                  required
                />
              </div>
            </div>
          ) : (
            <div>
              <p style={{ fontSize: '0.875rem', color: 'var(--slate-600)', marginBottom: '14px' }}>
                Provide a reason for cancelling this consultation with {selectedAppt?.patient.fullName}.
              </p>
              <div className="form-group">
                <label className="form-label" htmlFor="cancel-reason-input">Reason for Cancellation</label>
                <input
                  id="cancel-reason-input"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Emergency clinical duty, schedule conflict"
                  value={cancellationReason}
                  onChange={(e) => setCancellationReason(e.target.value)}
                  required
                />
              </div>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setModalMode(null)}
            >
              Close
            </button>
            <button
              type="submit"
              className={`btn ${modalMode === 'COMPLETE' ? 'btn-primary' : 'btn-danger'}`}
              disabled={actionLoading}
            >
              {actionLoading ? 'Updating...' : modalMode === 'COMPLETE' ? 'Mark Completed' : 'Confirm Cancellation'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default DoctorAppointments;
