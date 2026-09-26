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
} from 'lucide-react';
import { appointmentService, doctorService } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

const DoctorDashboard = () => {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusUpdating, setStatusUpdating] = useState(null);

  const todayStr = new Date().toISOString().split('T')[0];

  useEffect(() => {
    fetchAppointments();
  }, []);

  const fetchAppointments = async () => {
    try {
      const res = await appointmentService.getMyAppointments();
      setAppointments(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const todayAppointments = appointments.filter((a) => a.appointmentDate === todayStr);
  const pendingAppointments = appointments.filter((a) => a.status === 'PENDING');
  const confirmedAppointments = appointments.filter((a) => a.status === 'CONFIRMED');
  const completedAppointments = appointments.filter((a) => a.status === 'COMPLETED');

  // Unique patient count
  const patientIds = new Set(appointments.map((a) => a.patient.id));

  const handleQuickStatus = async (apptId, status) => {
    setStatusUpdating(apptId);
    try {
      await appointmentService.updateStatus(apptId, { status });
      fetchAppointments();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update appointment status');
    } finally {
      setStatusUpdating(null);
    }
  };

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
