import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar,
  FileText,
  Pill,
  Bell,
  ArrowRight,
  Clock,
  CheckCircle2,
  AlertCircle,
  Bot,
  Users,
  Search,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { appointmentService, recordService, prescriptionService, notificationService } from '../../services/api';

const PatientDashboard = () => {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState([]);
  const [records, setRecords] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchDashboardData = async () => {
      try {
        const [appRes, recRes, presRes, notifRes] = await Promise.all([
          appointmentService.getMyAppointments(),
          recordService.getMyRecords(),
          prescriptionService.getMyPrescriptions(),
          notificationService.getUnreadCount(),
        ]);
        setAppointments(appRes.data || []);
        setRecords(recRes.data || []);
        setPrescriptions(presRes.data || []);
        setUnreadCount(notifRes.data?.unreadCount || 0);
      } catch (err) {
        console.error('Failed to load dashboard data:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchDashboardData();
  }, []);

  // Upcoming confirmed or pending appointment
  const upcomingAppointment = appointments.find(
    (a) => a.status === 'CONFIRMED' || a.status === 'PENDING'
  );

  // Empathy Engine guidance message based on user preference
  const getEmpathyMessage = () => {
    const tone = user?.communicationPreference || 'SUPPORTIVE';
    if (tone === 'SIMPLE') {
      return upcomingAppointment
        ? `Next visit: ${upcomingAppointment.appointmentDate} at ${upcomingAppointment.appointmentTime} with Dr. ${upcomingAppointment.doctor.fullName}.`
        : 'You have no scheduled visits today.';
    } else if (tone === 'PROFESSIONAL') {
      return upcomingAppointment
        ? `Scheduled Consultation: Dr. ${upcomingAppointment.doctor.fullName} (${upcomingAppointment.doctor.specialization}) on ${upcomingAppointment.appointmentDate} at ${upcomingAppointment.appointmentTime}. Status: ${upcomingAppointment.status}.`
        : 'Clinical Schedule: All consultation queues are currently clear.';
    } else {
      return upcomingAppointment
        ? `We're keeping you on track! You have an upcoming consultation with Dr. ${upcomingAppointment.doctor.fullName} on ${upcomingAppointment.appointmentDate} at ${upcomingAppointment.appointmentTime}. Remember to have any questions ready!`
        : "Welcome back! Your health timeline is completely up to date. You can consult with a specialist or chat with your AI companion anytime.";
    }
  };

  return (
    <div>
      {/* Welcome Banner */}
      <div
        style={{
          background: 'linear-gradient(135deg, #0284c7 0%, #0d9488 100%)',
          borderRadius: 'var(--radius-xl)',
          padding: '28px 32px',
          color: 'white',
          marginBottom: '28px',
          boxShadow: 'var(--shadow-md)',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <div style={{ position: 'relative', zIndex: 10 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', opacity: 0.9, fontSize: '0.875rem', fontWeight: 600, marginBottom: '6px' }}>
            <Sparkles size={16} /> CAREPULSE COORDINATION HUB
          </div>
          <h2 style={{ color: 'white', fontSize: '1.85rem', fontWeight: 800, marginBottom: '8px' }}>
            Welcome back, {user?.fullName || 'Patient'}!
          </h2>
          <p style={{ maxWidth: '720px', fontSize: '1rem', opacity: 0.95, lineHeight: 1.5 }}>
            {getEmpathyMessage()}
          </p>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-primary">
            <Calendar size={26} />
          </div>
          <div>
            <div className="stat-val">{appointments.length}</div>
            <div className="stat-label">Appointments</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-teal">
            <FileText size={26} />
          </div>
          <div>
            <div className="stat-val">{records.length}</div>
            <div className="stat-label">Medical Records</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-amber">
            <Pill size={26} />
          </div>
          <div>
            <div className="stat-val">{prescriptions.length}</div>
            <div className="stat-label">Prescriptions</div>
          </div>
        </div>

        <div className="stat-card">
          <div className="stat-icon-wrapper stat-icon-indigo">
            <Bell size={26} />
          </div>
          <div>
            <div className="stat-val">{unreadCount}</div>
            <div className="stat-label">Notifications</div>
          </div>
        </div>
      </div>

      {/* Main Grid: Upcoming Consultation & Quick Actions */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: '24px', marginBottom: '28px' }}>
        {/* Next Appointment Card */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">
              <Calendar size={20} className="text-primary" /> Upcoming Consultation
            </span>
            <Link to="/patient/appointments" className="btn btn-outline btn-sm">
              View All
            </Link>
          </div>

          {upcomingAppointment ? (
            <div
              style={{
                background: 'var(--slate-50)',
                border: '1px solid var(--slate-200)',
                borderRadius: 'var(--radius-lg)',
                padding: '20px',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '16px' }}>
                <div>
                  <h3 style={{ fontSize: '1.25rem', marginBottom: '4px' }}>
                    {upcomingAppointment.doctor.fullName}
                  </h3>
                  <div style={{ color: 'var(--primary-700)', fontWeight: 600, fontSize: '0.9375rem' }}>
                    {upcomingAppointment.doctor.specialization} • {upcomingAppointment.doctor.qualification}
                  </div>
                </div>
                <span className={`badge badge-${upcomingAppointment.status.toLowerCase()}`}>
                  {upcomingAppointment.status}
                </span>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '16px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--slate-700)', fontSize: '0.875rem' }}>
                  <Calendar size={16} style={{ color: 'var(--primary-600)' }} />
                  <span>{upcomingAppointment.appointmentDate}</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--slate-700)', fontSize: '0.875rem' }}>
                  <Clock size={16} style={{ color: 'var(--teal-600)' }} />
                  <span>{upcomingAppointment.appointmentTime}</span>
                </div>
              </div>

              <div style={{ fontSize: '0.875rem', color: 'var(--slate-600)', marginBottom: '16px' }}>
                <strong>Reason:</strong> {upcomingAppointment.reason}
              </div>

              <div style={{ display: 'flex', gap: '10px' }}>
                <Link to="/patient/ai-companion" className="btn btn-primary btn-sm">
                  <Bot size={14} /> Prepare for Visit
                </Link>
                <Link to="/patient/appointments" className="btn btn-secondary btn-sm">
                  Manage Booking
                </Link>
              </div>
            </div>
          ) : (
            <div style={{ textAlign: 'center', padding: '36px 16px' }}>
              <div style={{ color: 'var(--slate-400)', marginBottom: '12px' }}>
                <Calendar size={44} style={{ margin: '0 auto' }} />
              </div>
              <p style={{ color: 'var(--slate-600)', marginBottom: '16px' }}>
                You have no upcoming consultations scheduled.
              </p>
              <Link to="/patient/doctors" className="btn btn-primary">
                <Search size={16} /> Find a Doctor
              </Link>
            </div>
          )}
        </div>

        {/* Quick Coordination Actions */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">
              <Sparkles size={20} className="text-teal" /> Healthcare Actions
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            <Link
              to="/patient/doctors"
              className="card"
              style={{
                padding: '14px 18px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                background: 'var(--primary-50)',
                borderColor: 'var(--primary-200)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <Search size={20} style={{ color: 'var(--primary-600)' }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--slate-900)' }}>
                    Book Appointment
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)' }}>
                    Search doctors & available slots
                  </div>
                </div>
              </div>
              <ArrowRight size={16} style={{ color: 'var(--primary-600)' }} />
            </Link>

            <Link
              to="/patient/ai-companion"
              className="card"
              style={{
                padding: '14px 18px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                background: 'var(--teal-50)',
                borderColor: 'var(--teal-200)',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <Bot size={20} style={{ color: 'var(--teal-600)' }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--slate-900)' }}>
                    AI Health Companion
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)' }}>
                    Prep questions & organize reports
                  </div>
                </div>
              </div>
              <ArrowRight size={16} style={{ color: 'var(--teal-600)' }} />
            </Link>

            <Link
              to="/patient/caregivers"
              className="card"
              style={{
                padding: '14px 18px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                background: '#f5f3ff',
                borderColor: '#ddd6fe',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <Users size={20} style={{ color: '#7c3aed' }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--slate-900)' }}>
                    Caregiver Access
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)' }}>
                    Invite family or manage permissions
                  </div>
                </div>
              </div>
              <ArrowRight size={16} style={{ color: '#7c3aed' }} />
            </Link>
          </div>
        </div>
      </div>

      {/* Health Timeline Preview */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">
            <FileText size={20} className="text-primary" /> Recent Health Timeline
          </span>
          <Link to="/patient/records" className="btn btn-outline btn-sm">
            Full Health Timeline
          </Link>
        </div>

        {records.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px', color: 'var(--slate-500)', fontSize: '0.875rem' }}>
            No consultation notes or medical records added yet.
          </div>
        ) : (
          <div className="timeline">
            {records.slice(0, 3).map((rec) => (
              <div key={rec.id} className="timeline-item">
                <div className="timeline-dot" />
                <div className="timeline-card">
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                    <span style={{ fontWeight: 700, color: 'var(--slate-900)' }}>{rec.diagnosis}</span>
                    <span style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', fontWeight: 500 }}>
                      {rec.recordDate}
                    </span>
                  </div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--primary-700)', marginBottom: '8px', fontWeight: 600 }}>
                    Attending Physician: {rec.doctor.fullName} ({rec.doctor.specialization})
                  </div>
                  <div style={{ fontSize: '0.875rem', color: 'var(--slate-600)' }}>
                    <strong>Treatment Plan:</strong> {rec.treatment}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default PatientDashboard;
