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
  const [waitlistEntries, setWaitlistEntries] = useState([]);
  const [activeTab, setActiveTab] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [waitTimes, setWaitTimes] = useState({}); // { [apptId]: { estimatedWaitMinutes, patientsAheadCount, explanation, loading } }

  // Cancellation Modal
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [selectedAppt, setSelectedAppt] = useState(null);
  const [cancelReason, setCancelReason] = useState('');
  const [cancelLoading, setCancelLoading] = useState(false);

  useEffect(() => {
    fetchAppointments();
    fetchWaitlist();
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

  const fetchWaitlist = async () => {
    try {
      const res = await appointmentService.getMyWaitlist();
      setWaitlistEntries(res.data || []);
    } catch (err) {
      console.error('Failed to load waitlist', err);
    }
  };

  const handleFetchWaitTime = async (apptId) => {
    setWaitTimes((prev) => ({ ...prev, [apptId]: { loading: true } }));
    try {
      const res = await appointmentService.getWaitTime(apptId);
      setWaitTimes((prev) => ({ ...prev, [apptId]: { ...res.data, loading: false } }));
    } catch (err) {
      console.error('Failed to fetch wait time', err);
      setWaitTimes((prev) => ({
        ...prev,
        [apptId]: { explanation: 'Wait time could not be calculated currently.', loading: false },
      }));
    }
  };

  const handleCancelWaitlist = async (waitlistId) => {
    if (!window.confirm('Are you sure you want to withdraw from this waitlist?')) return;
    try {
      await appointmentService.cancelWaitlist(waitlistId);
      fetchWaitlist();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to cancel waitlist request');
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
      fetchWaitlist();
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
          flexWrap: 'wrap',
        }}
      >
        {['ALL', 'CONFIRMED', 'PENDING', 'COMPLETED', 'CANCELLED', 'WAITLIST'].map((tab) => (
          <button
            key={tab}
            className={`btn btn-sm ${activeTab === tab ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab(tab)}
            style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
          >
            <span>{tab === 'WAITLIST' ? 'Priority Waitlist' : tab.charAt(0) + tab.slice(1).toLowerCase()}</span>
            {tab === 'WAITLIST' && waitlistEntries.length > 0 && (
              <span style={{ background: activeTab === 'WAITLIST' ? 'white' : 'var(--primary-600)', color: activeTab === 'WAITLIST' ? 'var(--primary-700)' : 'white', borderRadius: '10px', padding: '1px 6px', fontSize: '0.75rem', fontWeight: 700 }}>
                {waitlistEntries.length}
              </span>
            )}
          </button>
        ))}
      </div>

      {/* TAB: WAITLIST ENTRIES */}
      {activeTab === 'WAITLIST' ? (
        <div>
          {waitlistEntries.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
              <Clock size={40} style={{ color: 'var(--slate-400)', margin: '0 auto 12px' }} />
              <h3 style={{ fontSize: '1.25rem', marginBottom: '6px' }}>No Active Waitlist Requests</h3>
              <p style={{ color: 'var(--slate-500)', fontSize: '0.9375rem', marginBottom: '16px' }}>
                When your preferred doctor is fully booked, you can join the Priority Waitlist to get automated notifications the moment a cancelled slot opens.
              </p>
              <Link to="/patient/doctors" className="btn btn-primary btn-sm">
                Explore Specialists
              </Link>
            </div>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {waitlistEntries.map((w) => (
                <div key={w.id} className="card" style={{ padding: '20px', borderLeft: w.status === 'NOTIFIED' ? '4px solid var(--teal-500)' : '4px solid #f59e0b' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px', marginBottom: '12px' }}>
                    <div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '4px' }}>
                        <h3 style={{ fontSize: '1.15rem', color: 'var(--slate-900)' }}>
                          {w.doctorName || (w.specialization ? `Specialist in ${w.specialization}` : 'Any Available Specialist')}
                        </h3>
                        <span className={`badge ${w.status === 'NOTIFIED' ? 'badge-success' : w.status === 'WAITING' ? 'badge-warning' : 'badge-secondary'}`}>
                          {w.status === 'NOTIFIED' ? 'Slot Available • Action Required' : w.status}
                        </span>
                      </div>
                      <div style={{ fontSize: '0.875rem', color: 'var(--slate-500)' }}>
                        Preferred Date: <strong>{w.preferredDate}</strong> {w.preferredTime ? `at ${w.preferredTime}` : '(Flexible time)'}
                      </div>
                    </div>

                    <div style={{ display: 'flex', gap: '8px' }}>
                      {w.status === 'NOTIFIED' && (
                        <Link to="/patient/doctors" className="btn btn-primary btn-sm">
                          Book Open Slot
                        </Link>
                      )}
                      {(w.status === 'WAITING' || w.status === 'NOTIFIED') && (
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          style={{ color: '#ef4444' }}
                          onClick={() => handleCancelWaitlist(w.id)}
                        >
                          Withdraw
                        </button>
                      )}
                    </div>
                  </div>

                  {w.status === 'NOTIFIED' && (
                    <div style={{ background: 'var(--teal-50)', border: '1px solid var(--teal-200)', color: 'var(--teal-800)', padding: '10px 14px', borderRadius: 'var(--radius-md)', fontSize: '0.875rem', marginBottom: '10px' }}>
                      ⚡ <strong>A slot has opened up on {w.preferredDate}!</strong> A previous appointment was cancelled. Visit Doctor Discovery now to book your preferred consultation before others.
                    </div>
                  )}

                  {w.notes && (
                    <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', background: 'var(--slate-50)', padding: '8px 12px', borderRadius: '4px' }}>
                      <strong>Notes:</strong> {w.notes}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        /* APPOINTMENT CARDS / LIST */
        loading ? (
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
            {filteredAppointments.map((appt) => {
              const waitData = waitTimes[appt.id];
              return (
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

                  {/* Explainable Appointment Wait-Time Estimation */}
                  {(appt.status === 'CONFIRMED' || appt.status === 'PENDING') && (
                    <div style={{ background: '#f8fafc', border: '1px solid var(--slate-200)', padding: '10px 14px', borderRadius: 'var(--radius-md)', marginBottom: '12px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px' }}>
                      <div>
                        {waitData ? (
                          <div style={{ fontSize: '0.875rem' }}>
                            <span style={{ color: 'var(--primary-700)', fontWeight: 700 }}>
                              ⏱ Estimated Waiting Time: {waitData.estimatedWaitMinutes} minutes
                            </span>
                            <span style={{ color: 'var(--slate-500)', marginLeft: '8px', fontSize: '0.8125rem' }}>
                              ({waitData.patientsAheadCount} patients ahead • {waitData.explanation})
                            </span>
                          </div>
                        ) : (
                          <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)' }}>
                            Check live queue wait-time estimation based on real doctor consultation schedule.
                          </div>
                        )}
                      </div>

                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() => handleFetchWaitTime(appt.id)}
                        disabled={waitData?.loading}
                        style={{ fontSize: '0.8125rem' }}
                      >
                        <Clock size={13} /> {waitData?.loading ? 'Calculating...' : waitData ? 'Refresh Wait Time' : 'Estimate Wait Time'}
                      </button>
                    </div>
                  )}

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
              );
            })}
          </div>
        )
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
