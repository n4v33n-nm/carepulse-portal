import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar,
  Clock,
  Search,
  CheckCircle,
  XCircle,
  FileText,
  AlertCircle,
  Bot,
} from 'lucide-react';
import { appointmentService } from '../../services/api';
import Modal from '../../components/Modal';

const PatientAppointments = () => {
  const [appointments, setAppointments] = useState([]);
  const [activeTab, setActiveTab] = useState('ALL');
  const [loading, setLoading] = useState(true);

  // Cancellation Modal
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [selectedAppt, setSelectedAppt] = useState(null);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelLoading, setCancelLoading] = useState(false);

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

  const filteredAppointments = appointments.filter((a) => {
    if (activeTab === 'ALL') return true;
    return a.status.toUpperCase() === activeTab;
  });

  const handleCancelClick = (appt) => {
    setSelectedAppt(appt);
    setCancelReason('');
    setCancelModalOpen(true);
  };

  const handleConfirmCancel = async () => {
    if (!selectedAppt) return;
    setCancelLoading(true);
    try {
      await appointmentService.cancelAppointment(selectedAppt.id, cancelReason || 'Cancelled by patient');
      setCancelModalOpen(false);
      fetchAppointments();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to cancel appointment');
    } finally {
      setCancelLoading(false);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>My Appointments</h2>
          <p style={{ color: 'var(--slate-600)' }}>
            Review past consultations and manage upcoming appointments.
          </p>
        </div>
        <Link to="/patient/doctors" className="btn btn-primary">
          <Calendar size={16} /> Book New Appointment
        </Link>
      </div>

      {/* Tabs */}
      <div
        style={{
          display: 'flex',
          gap: '8px',
          borderBottom: '1px solid var(--slate-200)',
          paddingBottom: '12px',
          marginBottom: '24px',
        }}
      >
        {['ALL', 'CONFIRMED', 'PENDING', 'COMPLETED', 'CANCELLED'].map((tab) => (
          <button
            key={tab}
            className={`btn btn-sm ${activeTab === tab ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab(tab)}
          >
            {tab.charAt(0) + tab.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {/* Appointment Cards / List */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-500)' }}>
          Loading your appointments...
        </div>
      ) : filteredAppointments.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
          <Calendar size={40} style={{ color: 'var(--slate-400)', margin: '0 auto 12px' }} />
          <h3 style={{ fontSize: '1.25rem', marginBottom: '6px' }}>No {activeTab.toLowerCase()} appointments</h3>
          <p style={{ color: 'var(--slate-500)', fontSize: '0.9375rem', marginBottom: '16px' }}>
            You do not have any appointments matching this category.
          </p>
          <Link to="/patient/doctors" className="btn btn-primary btn-sm">
            Find Doctor
          </Link>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {filteredAppointments.map((appt) => (
            <div key={appt.id} className="card" style={{ padding: '20px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px' }}>
                <div style={{ display: 'flex', gap: '16px', alignItems: 'center' }}>
                  <div
                    style={{
                      width: '48px',
                      height: '48px',
                      borderRadius: 'var(--radius-md)',
                      background: 'var(--primary-100)',
                      color: 'var(--primary-700)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 700,
                    }}
                  >
                    <Calendar size={22} />
                  </div>
                  <div>
                    <h3 style={{ fontSize: '1.15rem', color: 'var(--slate-900)' }}>
                      {appt.doctor.fullName}
                    </h3>
                    <div style={{ fontSize: '0.875rem', color: 'var(--slate-500)' }}>
                      {appt.doctor.specialization} • Fee: ₹{appt.doctor.consultationFee ? Number(appt.doctor.consultationFee).toLocaleString('en-IN') : '500'}
                    </div>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <span className={`badge badge-${appt.status.toLowerCase()}`}>
                    {appt.status}
                  </span>
                  {(appt.status === 'PENDING' || appt.status === 'CONFIRMED') && (
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      style={{ color: '#ef4444' }}
                      onClick={() => handleCancelClick(appt)}
                    >
                      Cancel
                    </button>
                  )}
                </div>
              </div>

              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
                  gap: '12px',
                  background: 'var(--slate-50)',
                  padding: '14px',
                  borderRadius: 'var(--radius-md)',
                  margin: '16px 0',
                }}
              >
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', fontWeight: 600 }}>CONSULTATION DATE</div>
                  <div style={{ fontWeight: 600, fontSize: '0.9375rem', color: 'var(--slate-800)' }}>
                    {appt.appointmentDate}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', fontWeight: 600 }}>SCHEDULED TIME</div>
                  <div style={{ fontWeight: 600, fontSize: '0.9375rem', color: 'var(--slate-800)' }}>
                    {appt.appointmentTime}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', fontWeight: 600 }}>CHIEF COMPLAINT / REASON</div>
                  <div style={{ fontSize: '0.875rem', color: 'var(--slate-700)' }}>
                    {appt.reason}
                  </div>
                </div>
              </div>

              {appt.consultationNotes && (
                <div style={{ fontSize: '0.875rem', background: '#f8fafc', borderLeft: '4px solid var(--primary-600)', padding: '10px 14px', borderRadius: '4px' }}>
                  <strong>Doctor's Notes:</strong> {appt.consultationNotes}
                </div>
              )}

              {appt.cancellationReason && (
                <div style={{ fontSize: '0.875rem', background: 'var(--danger-bg)', color: 'var(--danger-text)', padding: '10px 14px', borderRadius: '4px' }}>
                  <strong>Cancellation Note:</strong> {appt.cancellationReason}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Cancellation Confirmation Modal */}
      <Modal
        isOpen={cancelModalOpen}
        onClose={() => setCancelModalOpen(false)}
        title="Cancel Appointment"
        maxWidth="460px"
      >
        <div style={{ marginBottom: '16px' }}>
          <p style={{ color: 'var(--slate-700)', fontSize: '0.9375rem', marginBottom: '12px' }}>
            Are you sure you want to cancel your consultation with{' '}
            <strong>{selectedAppt?.doctor.fullName}</strong> scheduled for{' '}
            <strong>{selectedAppt?.appointmentDate} at {selectedAppt?.appointmentTime}</strong>?
          </p>
          <p style={{ fontSize: '0.8125rem', color: 'var(--slate-500)' }}>
            This time slot will immediately be released and made available for other patients.
          </p>
        </div>

        <div className="form-group">
          <label className="form-label" htmlFor="cancel-reason-input">Reason for Cancellation (optional)</label>
          <input
            id="cancel-reason-input"
            type="text"
            className="form-control"
            placeholder="e.g. Schedule conflict, feeling better"
            value={cancelReason}
            onChange={(e) => setCancelReason(e.target.value)}
          />
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => setCancelModalOpen(false)}
          >
            Keep Appointment
          </button>
          <button
            type="button"
            className="btn btn-danger"
            onClick={handleConfirmCancel}
            disabled={cancelLoading}
          >
            {cancelLoading ? 'Cancelling...' : 'Confirm Cancellation'}
          </button>
        </div>
      </Modal>
    </div>
  );
};

export default PatientAppointments;
