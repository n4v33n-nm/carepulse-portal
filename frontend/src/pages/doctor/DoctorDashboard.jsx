import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar,
  Users,
  Clock,
  CheckCircle2,
  FileText,
  Pill,
  AlertCircle,
  ArrowRight,
  Activity,
  Check,
  X,
  Siren,
  ShieldCheck,
  HeartPulse,
  Stethoscope,
  Phone,
} from 'lucide-react';
import { appointmentService, doctorService, emergencyService } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

const DoctorDashboard = () => {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState([]);
  const [emergencyDuties, setEmergencyDuties] = useState([]);
  const [assignedEmergencies, setAssignedEmergencies] = useState([]);
  const [myAvailabilityStatus, setMyAvailabilityStatus] = useState('AVAILABLE');
  const [loading, setLoading] = useState(true);
  const [statusUpdating, setStatusUpdating] = useState(null);
  const [emergencyActionLoading, setEmergencyActionLoading] = useState(null);
  const [completionNotes, setCompletionNotes] = useState({});
  const [completingId, setCompletingId] = useState(null);

  const todayStr = new Date().toISOString().split('T')[0];

  useEffect(() => {
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    try {
      const [apptRes, dutyRes, emergRes] = await Promise.all([
        appointmentService.getMyAppointments(),
        emergencyService.getMyEmergencyDuty().catch(() => ({ data: [] })),
        emergencyService.getAssignedEmergencyRequests().catch(() => ({ data: [] })),
      ]);
      setAppointments(apptRes.data || []);
      const duties = dutyRes.data || [];
      setEmergencyDuties(duties);
      if (duties.length > 0 && duties[0].doctorAvailabilityStatus) {
        setMyAvailabilityStatus(duties[0].doctorAvailabilityStatus);
      }
      setAssignedEmergencies(emergRes.data || []);
    } catch (err) {
      console.error('Failed to load doctor dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleQuickStatus = async (apptId, status) => {
    setStatusUpdating(apptId);
    try {
      await appointmentService.updateStatus(apptId, { status });
      fetchDashboardData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update appointment status');
    } finally {
      setStatusUpdating(null);
    }
  };

  const handleUpdateAvailability = async (newStatus) => {
    try {
      await emergencyService.updateMyDoctorStatus({ availabilityStatus: newStatus });
      setMyAvailabilityStatus(newStatus);
      fetchDashboardData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update status');
    }
  };

  const handleUpdateEmergencyStatus = async (reqId, newStatus) => {
    setEmergencyActionLoading(reqId);
    try {
      const notes = completionNotes[reqId] || '';
      await emergencyService.updateEmergencyStatus(reqId, { status: newStatus, doctorNotes: notes });
      setCompletingId(null);
      fetchDashboardData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update emergency case status');
    } finally {
      setEmergencyActionLoading(null);
    }
  };

  const todayAppointments = appointments.filter((a) => a.appointmentDate === todayStr);
  const pendingAppointments = appointments.filter((a) => a.status === 'PENDING');
  const confirmedAppointments = appointments.filter((a) => a.status === 'CONFIRMED');
  const completedAppointments = appointments.filter((a) => a.status === 'COMPLETED');

  // Unique patient count
  const patientIds = new Set(appointments.map((a) => a.patient.id));

  return (
    <div>
      {/* Welcome Banner */}
      <div
        style={{
          background: 'linear-gradient(135deg, #075985 0%, #0e7490 100%)',
          borderRadius: 'var(--radius-xl)',
          padding: '28px 32px',
          color: 'white',
          marginBottom: '28px',
          boxShadow: 'var(--shadow-md)',
        }}
      >
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
          <div>
            <div style={{ fontSize: '0.875rem', fontWeight: 600, opacity: 0.9, marginBottom: '6px' }}>
              PHYSICIAN CLINICAL PORTAL
            </div>
            <h2 style={{ color: 'white', fontSize: '1.85rem', fontWeight: 800, marginBottom: '6px' }}>
              Welcome, {user?.fullName || 'Doctor'}
            </h2>
            <p style={{ opacity: 0.9, fontSize: '0.9375rem' }}>
              You have {pendingAppointments.length} pending appointment requests and {todayAppointments.length} consultations scheduled for today.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px' }}>
            <Link to="/doctor/availability" className="btn btn-secondary btn-sm" style={{ background: 'white', color: 'var(--slate-800)' }}>
              <Clock size={16} /> Update Availability
            </Link>
            <Link to="/doctor/records" className="btn btn-primary btn-sm">
              <FileText size={16} /> New Clinical Record
            </Link>
          </div>
        </div>
      </div>

      {/* Emergency Duty Roster & Real-time Physician Availability */}
      <div className="card" style={{ marginBottom: '28px', borderLeft: emergencyDuties.length > 0 ? '5px solid #e11d48' : '5px solid var(--slate-300)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div
              style={{
                width: '44px',
                height: '44px',
                borderRadius: '50%',
                background: emergencyDuties.length > 0 ? '#ffe4e6' : 'var(--slate-100)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: emergencyDuties.length > 0 ? '#e11d48' : 'var(--slate-500)',
              }}
            >
              <Siren size={22} />
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
                <h3 style={{ fontSize: '1.2rem', margin: 0 }}>
                  Today's Emergency Duty
                </h3>
                {emergencyDuties.length > 0 ? (
                  <span className="badge badge-emergency">EMERGENCY DUTY: YES</span>
                ) : (
                  <span className="badge badge-off-duty">EMERGENCY DUTY: NO</span>
                )}
              </div>
              <div style={{ fontSize: '0.875rem', color: 'var(--slate-600)', marginTop: '4px' }}>
                {emergencyDuties.length > 0 ? (
                  <span>
                    <strong>Assigned Shift:</strong> {emergencyDuties.map(d => `${d.shiftName} (${d.shiftStart?.slice(0, 5)} - ${d.shiftEnd?.slice(0, 5)})`).join(', ')}
                  </span>
                ) : (
                  <span>No emergency shift rostered for today by Administration.</span>
                )}
              </div>
            </div>
          </div>

          {/* Doctor Current Availability Status Selector */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--slate-700)' }}>
              Current Status:
            </span>
            <select
              className="form-control form-control-sm"
              style={{ width: '170px', fontWeight: 600 }}
              value={myAvailabilityStatus}
              onChange={(e) => handleUpdateAvailability(e.target.value)}
            >
              <option value="AVAILABLE">AVAILABLE</option>
              <option value="BUSY">BUSY</option>
              <option value="IN_CONSULTATION">IN_CONSULTATION</option>
              <option value="OFF_DUTY">OFF_DUTY</option>
              <option value="ON_LEAVE">ON_LEAVE</option>
            </select>
            <span className={`badge badge-${myAvailabilityStatus.toLowerCase()}`}>
              {myAvailabilityStatus}
            </span>
          </div>
        </div>
      </div>

      {/* Assigned Emergency Cases Queue */}
      {assignedEmergencies.length > 0 && (
        <div className="card" style={{ marginBottom: '28px', borderLeft: '5px solid #e11d48' }}>
          <div className="card-header">
            <span className="card-title">
              <Siren size={20} style={{ color: '#e11d48' }} /> Assigned Emergency Cases ({assignedEmergencies.filter(e => e.status !== 'COMPLETED' && e.status !== 'CANCELLED').length} Active)
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
            {assignedEmergencies.map((req) => (
              <div
                key={req.id}
                style={{
                  background: req.status === 'ASSIGNED' ? '#fff1f2' : 'var(--slate-50)',
                  border: req.status === 'ASSIGNED' ? '1px solid #fecdd3' : '1px solid var(--slate-200)',
                  borderRadius: 'var(--radius-md)',
                  padding: '16px 20px',
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '12px', marginBottom: '10px' }}>
                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontWeight: 700, fontSize: '1.05rem', color: 'var(--slate-900)' }}>
                        {req.patientName}
                      </span>
                      <span className="badge badge-emergency">{req.category || 'General'}</span>
                      <span className={`badge badge-${req.status.toLowerCase()}`}>{req.status}</span>
                    </div>
                    <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', marginTop: '4px' }}>
                      <strong>Contact:</strong> {req.patientPhone || 'N/A'} • <strong>Blood:</strong> {req.patientBloodGroup || 'N/A'} • <strong>Emergency Contact:</strong> {req.emergencyContact || 'N/A'}
                    </div>
                    <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', marginTop: '2px' }}>
                      <strong>Time:</strong> {new Date(req.requestTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} • <strong>Symptoms:</strong> {req.description || 'Emergency assistance requested'}
                    </div>
                  </div>

                  {/* Actions for Assigned Emergency Case */}
                  <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    {req.status === 'ASSIGNED' && (
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() => handleUpdateEmergencyStatus(req.id, 'IN_PROGRESS')}
                        disabled={emergencyActionLoading === req.id}
                      >
                        <HeartPulse size={14} /> Start Consultation
                      </button>
                    )}

                    {req.status === 'IN_PROGRESS' && completingId !== req.id && (
                      <button
                        type="button"
                        className="btn btn-sm"
                        style={{ background: '#10b981', color: 'white' }}
                        onClick={() => setCompletingId(req.id)}
                      >
                        <Check size={14} /> Complete Case
                      </button>
                    )}

                    {completingId === req.id && (
                      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', minWidth: '260px' }}>
                        <input
                          type="text"
                          className="form-control form-control-sm"
                          placeholder="Clinical summary / notes..."
                          value={completionNotes[req.id] || ''}
                          onChange={(e) => setCompletionNotes({ ...completionNotes, [req.id]: e.target.value })}
                        />
                        <div style={{ display: 'flex', gap: '6px' }}>
                          <button
                            type="button"
                            className="btn btn-sm btn-primary"
                            style={{ background: '#10b981' }}
                            onClick={() => handleUpdateEmergencyStatus(req.id, 'COMPLETED')}
                            disabled={emergencyActionLoading === req.id}
                          >
                            Confirm Complete
                          </button>
                          <button
                            type="button"
                            className="btn btn-sm btn-secondary"
                            onClick={() => setCompletingId(null)}
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Stats Grid */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-amber">
            <Clock size={24} />
          </div>
          <div>
            <div className="stat-val">{todayAppointments.length}</div>
            <div className="stat-label">Today's Consultations</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-primary">
            <Calendar size={24} />
          </div>
          <div>
            <div className="stat-val">{pendingAppointments.length}</div>
            <div className="stat-label">Pending Approval</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-teal">
            <CheckCircle2 size={24} />
          </div>
          <div>
            <div className="stat-val">{confirmedAppointments.length}</div>
            <div className="stat-label">Confirmed Upcoming</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-indigo">
            <Users size={24} />
          </div>
          <div>
            <div className="stat-val">{patientIds.size}</div>
            <div className="stat-label">Total Unique Patients</div>
          </div>
        </div>
      </div>

      {/* Pending Consultation Queue */}
      {pendingAppointments.length > 0 && (
        <div className="card" style={{ marginBottom: '28px', borderLeft: '5px solid #f59e0b' }}>
          <div className="card-header">
            <span className="card-title">
              <AlertCircle size={20} style={{ color: '#f59e0b' }} /> Action Required: Pending Appointment Requests
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {pendingAppointments.map((appt) => (
              <div
                key={appt.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  background: 'var(--slate-50)',
                  border: '1px solid var(--slate-200)',
                  borderRadius: 'var(--radius-md)',
                  padding: '14px 18px',
                  flexWrap: 'wrap',
                  gap: '12px',
                }}
              >
                <div>
                  <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--slate-900)' }}>
                    {appt.patient.fullName}
                  </div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)' }}>
                    <strong>Date & Time:</strong> {appt.appointmentDate} at {appt.appointmentTime}
                  </div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', marginTop: '2px' }}>
                    <strong>Reason:</strong> {appt.reason}
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    style={{ background: '#10b981' }}
                    onClick={() => handleQuickStatus(appt.id, 'CONFIRMED')}
                    disabled={statusUpdating === appt.id}
                  >
                    <Check size={14} /> Accept & Confirm
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    style={{ color: '#ef4444' }}
                    onClick={() => handleQuickStatus(appt.id, 'CANCELLED')}
                    disabled={statusUpdating === appt.id}
                  >
                    <X size={14} /> Decline
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Today's Schedule & Appointments Table */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">
            <Calendar size={20} className="text-primary" /> Upcoming Clinical Schedule
          </span>
          <Link to="/doctor/appointments" className="btn btn-outline btn-sm">
            View All Schedule
          </Link>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '32px', color: 'var(--slate-500)' }}>
            Loading consultation queue...
          </div>
        ) : appointments.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '36px 16px', color: 'var(--slate-500)' }}>
            No appointments booked yet.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Patient</th>
                  <th>Date & Time</th>
                  <th>Reason</th>
                  <th>Status</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {appointments.slice(0, 6).map((appt) => (
                  <tr key={appt.id}>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--slate-900)' }}>
                        {appt.patient.fullName}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                        Phone: {appt.patient.phone || 'N/A'} • Blood: {appt.patient.bloodGroup || 'N/A'}
                      </div>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{appt.appointmentDate}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>{appt.appointmentTime}</div>
                    </td>
                    <td style={{ maxWidth: '240px', fontSize: '0.8125rem' }}>{appt.reason}</td>
                    <td>
                      <span className={`badge badge-${appt.status.toLowerCase()}`}>
                        {appt.status}
                      </span>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <Link to="/doctor/appointments" className="btn btn-secondary btn-sm">
                        Manage
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default DoctorDashboard;
