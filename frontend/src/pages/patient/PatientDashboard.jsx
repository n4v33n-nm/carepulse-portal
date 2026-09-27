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
  HeartPulse,
  Siren,
  Phone,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { appointmentService, recordService, prescriptionService, notificationService, emergencyService } from '../../services/api';
import EmergencyAssistanceModal from '../../components/EmergencyAssistanceModal';

const PatientDashboard = () => {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState([]);
  const [records, setRecords] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [emergencyRequests, setEmergencyRequests] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showEmergencyModal, setShowEmergencyModal] = useState(false);

  const fetchDashboardData = async () => {
    try {
      const [appRes, recRes, presRes, notifRes, emergRes] = await Promise.all([
        appointmentService.getMyAppointments(),
        recordService.getMyRecords(),
        prescriptionService.getMyPrescriptions(),
        notificationService.getUnreadCount(),
        emergencyService.getMyEmergencyRequests().catch(() => ({ data: [] })),
      ]);
      setAppointments(appRes.data || []);
      setRecords(recRes.data || []);
      setPrescriptions(presRes.data || []);
      setUnreadCount(notifRes.data?.unreadCount || 0);
      setEmergencyRequests(emergRes.data || []);
    } catch (err) {
      console.error('Failed to load dashboard data:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
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

      {/* Emergency Assistance Direct Dispatch Card */}
      <div className="emergency-hero-card" style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            <div
              style={{
                width: '48px',
                height: '48px',
                borderRadius: '50%',
                background: '#ffe4e6',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#e11d48',
                flexShrink: 0,
              }}
            >
              <Siren size={26} />
            </div>
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap', marginBottom: '4px' }}>
                <h3 style={{ fontSize: '1.25rem', color: '#be123c', fontWeight: 800, margin: 0 }}>
                  Emergency Medical Assistance
                </h3>
                <span className="emergency-pulse-badge">24/7 Active Duty Roster</span>
              </div>
              <p style={{ color: 'var(--slate-600)', fontSize: '0.875rem', maxWidth: '680px', margin: 0 }}>
                Need urgent care? Skip scheduling wait times. Our deterministic triage algorithm instantly verifies today's active duty roster and assigns an available physician.
              </p>
            </div>
          </div>
          <button
            type="button"
            className="btn btn-emergency"
            onClick={() => setShowEmergencyModal(true)}
            style={{ padding: '12px 22px', fontSize: '0.9375rem' }}
          >
            <HeartPulse size={18} />
            Emergency Assistance
          </button>
        </div>
      </div>

      {/* Active Emergency Assistance Live Status Banner & 4-Step Patient Timeline (Requirements 8 & 14) */}
      {(() => {
        const latestEmergency = emergencyRequests.length > 0 ? emergencyRequests[0] : null;
        if (!latestEmergency || latestEmergency.status === 'CANCELLED') {
          return null;
        }

        const isWaiting = latestEmergency.status === 'WAITING';
        const isAssigned = latestEmergency.status === 'ASSIGNED';
        const isInProgress = latestEmergency.status === 'IN_PROGRESS';
        const isCompleted = latestEmergency.status === 'COMPLETED';

        return (
          <div
            className="card"
            style={{
              marginBottom: '28px',
              borderLeft: isWaiting ? '5px solid #f59e0b' : isCompleted ? '5px solid #10b981' : '5px solid #e11d48',
              background: isWaiting ? '#fffbeb' : isCompleted ? '#f0fdf4' : '#fff1f2',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px', marginBottom: '16px' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <div
                  style={{
                    width: '42px',
                    height: '42px',
                    borderRadius: '50%',
                    background: isWaiting ? '#fef3c7' : isCompleted ? '#dcfce7' : '#ffe4e6',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    color: isWaiting ? '#b45309' : isCompleted ? '#16a34a' : '#e11d48',
                    flexShrink: 0,
                  }}
                >
                  <Siren size={22} />
                </div>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                    <h4 style={{ fontSize: '1.2rem', fontWeight: 800, color: isWaiting ? '#92400e' : isCompleted ? '#166534' : '#9f1239', margin: 0 }}>
                      Emergency Assistance Tracking #{latestEmergency.id}
                    </h4>
                    <span className={`badge badge-${latestEmergency.status.toLowerCase()}`}>
                      {latestEmergency.status}
                    </span>
                    {latestEmergency.priority && (
                      <span className="badge badge-emergency" style={{ fontSize: '0.75rem' }}>
                        PRIORITY: {latestEmergency.priority}
                      </span>
                    )}
                  </div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', marginTop: '2px' }}>
                    Category: <strong>{latestEmergency.category || 'General'}</strong> • Symptoms: {latestEmergency.description || 'Emergency care requested'}
                  </div>
                </div>
              </div>

              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={fetchDashboardData}
                title="Refresh Status"
              >
                <Clock size={14} /> Refresh Status
              </button>
            </div>

            {/* Waiting Queue Warning Banner (Requirement 8) */}
            {isWaiting && (
              <div style={{ background: '#fef2f2', border: '1px solid #fecaca', borderRadius: 'var(--radius-md)', padding: '12px 16px', marginBottom: '18px', color: '#991b1b', fontSize: '0.8125rem', lineHeight: 1.45 }}>
                <div style={{ fontWeight: 700, marginBottom: '2px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <AlertCircle size={16} /> All emergency-duty doctors are currently occupied.
                </div>
                <div>
                  Your request is queued with <strong>{latestEmergency.priority || 'NORMAL'}</strong> priority and will be assigned immediately when a physician finishes their consultation.
                </div>
                <div style={{ marginTop: '4px', fontWeight: 700 }}>
                  FOR LIFE-THREATENING EMERGENCIES: Please call local emergency services (911 / 112 / 108) immediately.
                </div>
              </div>
            )}

            {/* 4-Step Patient Status Timeline with Actual Backend Timestamps (Requirement 14) */}
            <div style={{ background: 'white', border: '1px solid var(--slate-200)', borderRadius: 'var(--radius-lg)', padding: '20px' }}>
              <div style={{ fontSize: '0.8125rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--slate-500)', marginBottom: '16px' }}>
                Real-Time Clinical Progression Timeline
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px', position: 'relative' }}>
                {/* Step 1: Request Created */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div style={{ width: '28px', height: '28px', borderRadius: '50%', background: '#10b981', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: 700, fontSize: '0.8125rem' }}>
                      ✓
                    </div>
                    <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--slate-900)' }}>
                      Request Created
                    </span>
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)', paddingLeft: '36px' }}>
                    {latestEmergency.requestTime ? new Date(latestEmergency.requestTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'N/A'}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', paddingLeft: '36px' }}>
                    Triage dispatched
                  </div>
                </div>

                {/* Step 2: Doctor Assigned */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div
                      style={{
                        width: '28px',
                        height: '28px',
                        borderRadius: '50%',
                        background: (isAssigned || isInProgress || isCompleted) ? '#10b981' : isWaiting ? '#f59e0b' : 'var(--slate-300)',
                        color: 'white',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: 700,
                        fontSize: '0.8125rem',
                      }}
                    >
                      {(isAssigned || isInProgress || isCompleted) ? '✓' : isWaiting ? '⏳' : '○'}
                    </div>
                    <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: (isAssigned || isInProgress || isCompleted) ? 'var(--slate-900)' : 'var(--slate-500)' }}>
                      Doctor Assigned
                    </span>
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)', paddingLeft: '36px' }}>
                    {latestEmergency.assignedTime
                      ? new Date(latestEmergency.assignedTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                      : isWaiting ? 'Waiting in Queue...' : 'Pending'}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: latestEmergency.doctorName ? 'var(--primary-700)' : 'var(--slate-500)', fontWeight: 600, paddingLeft: '36px' }}>
                    {latestEmergency.doctorName ? `${latestEmergency.doctorName} (${latestEmergency.doctorSpecialization})` : 'Awaiting physician'}
                  </div>
                </div>

                {/* Step 3: Consultation Started */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div
                      style={{
                        width: '28px',
                        height: '28px',
                        borderRadius: '50%',
                        background: (isInProgress || isCompleted) ? '#10b981' : isAssigned ? '#0284c7' : 'var(--slate-300)',
                        color: 'white',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: 700,
                        fontSize: '0.8125rem',
                      }}
                    >
                      {(isInProgress || isCompleted) ? '✓' : isAssigned ? '●' : '○'}
                    </div>
                    <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: (isInProgress || isCompleted) ? 'var(--slate-900)' : 'var(--slate-500)' }}>
                      Consultation Started
                    </span>
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)', paddingLeft: '36px' }}>
                    {latestEmergency.startedTime
                      ? new Date(latestEmergency.startedTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                      : isAssigned ? 'Doctor alerted' : 'Pending start'}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', paddingLeft: '36px' }}>
                    {isInProgress ? 'Consultation in progress' : isCompleted ? 'Started' : 'Awaiting clinical start'}
                  </div>
                </div>

                {/* Step 4: Emergency Completed */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <div
                      style={{
                        width: '28px',
                        height: '28px',
                        borderRadius: '50%',
                        background: isCompleted ? '#10b981' : 'var(--slate-300)',
                        color: 'white',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: 700,
                        fontSize: '0.8125rem',
                      }}
                    >
                      {isCompleted ? '✓' : '○'}
                    </div>
                    <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: isCompleted ? 'var(--slate-900)' : 'var(--slate-500)' }}>
                      Emergency Completed
                    </span>
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)', paddingLeft: '36px' }}>
                    {latestEmergency.completedTime
                      ? new Date(latestEmergency.completedTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                      : 'Pending'}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: isCompleted ? '#059669' : 'var(--slate-500)', fontWeight: isCompleted ? 600 : 400, paddingLeft: '36px' }}>
                    {isCompleted ? (latestEmergency.doctorNotes ? `Notes: ${latestEmergency.doctorNotes}` : 'Care delivered') : 'Under treatment'}
                  </div>
                </div>
              </div>
            </div>
          </div>
        );
      })()}

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

      {/* Emergency Requests History */}
      <div className="card" style={{ marginTop: '28px' }}>
        <div className="card-header">
          <span className="card-title">
            <Siren size={20} style={{ color: '#e11d48' }} /> Emergency Assistance History
          </span>
          <button
            type="button"
            className="btn btn-emergency btn-sm"
            onClick={() => setShowEmergencyModal(true)}
          >
            <HeartPulse size={14} /> New Request
          </button>
        </div>

        {emergencyRequests.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px', color: 'var(--slate-500)', fontSize: '0.875rem' }}>
            No emergency requests logged. Use the Emergency Assistance button above if immediate clinical triage is needed.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Request ID & Date</th>
                  <th>Category</th>
                  <th>Assigned Physician</th>
                  <th>Symptoms / Reason</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {emergencyRequests.map((req) => (
                  <tr key={req.id}>
                    <td>
                      <div style={{ fontWeight: 700, color: 'var(--slate-900)' }}>
                        #{req.id}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                        {new Date(req.requestTime).toLocaleDateString()} {new Date(req.requestTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </td>
                    <td>
                      <span className="badge badge-emergency">
                        {req.category || 'General'}
                      </span>
                    </td>
                    <td>
                      {req.doctorName ? (
                        <div>
                          <div style={{ fontWeight: 600, color: 'var(--slate-900)' }}>
                            {req.doctorName}
                          </div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--primary-700)', fontWeight: 500 }}>
                            {req.doctorSpecialization}
                          </div>
                        </div>
                      ) : (
                        <span style={{ color: 'var(--slate-400)', fontStyle: 'italic', fontSize: '0.8125rem' }}>
                          Not Allocated
                        </span>
                      )}
                    </td>
                    <td style={{ maxWidth: '240px', fontSize: '0.8125rem', color: 'var(--slate-600)' }}>
                      {req.description || 'Immediate emergency triage requested'}
                    </td>
                    <td>
                      <span className={`badge badge-${(req.status || 'waiting').toLowerCase()}`}>
                        {req.status === 'NO_DOCTOR_AVAILABLE' ? 'NO DOCTOR' : req.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Emergency Assistance Modal */}
      <EmergencyAssistanceModal
        isOpen={showEmergencyModal}
        onClose={() => setShowEmergencyModal(false)}
        onSuccess={() => fetchDashboardData()}
      />
    </div>
  );
};

export default PatientDashboard;
