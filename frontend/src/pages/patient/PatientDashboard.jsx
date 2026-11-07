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
  Users,
  Search,
  Sparkles,
  HeartPulse,
  Siren,
  Phone,
  MessageSquare,
  UserCheck,
  Printer,
  Stethoscope,
  Activity,
  ClipboardList,
  ShieldCheck,
  Eye,
  FileUp,
  Upload,
  Download,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { appointmentService, recordService, prescriptionService, notificationService, emergencyService, doctorLeaveService, reportService } from '../../services/api';
import EmergencyAssistanceModal from '../../components/EmergencyAssistanceModal';
import Modal from '../../components/Modal';

const PatientDashboard = () => {
  const { user } = useAuth();
  const [appointments, setAppointments] = useState([]);
  const [records, setRecords] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [reports, setReports] = useState([]);
  const [emergencyRequests, setEmergencyRequests] = useState([]);
  const [pendingReassignments, setPendingReassignments] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showEmergencyModal, setShowEmergencyModal] = useState(false);

  // Phase 4 & 5: Patient Medical History States
  const [historyTab, setHistoryTab] = useState('ALL'); // 'ALL' | 'CONSULTATIONS' | 'PRESCRIPTIONS' | 'APPOINTMENTS' | 'REPORTS'
  const [historySearch, setHistorySearch] = useState('');
  const [selectedRecordDetail, setSelectedRecordDetail] = useState(null);

  const fetchDashboardData = async () => {
    try {
      const [appRes, recRes, presRes, notifRes, emergRes, reassignRes, repRes] = await Promise.all([
        appointmentService.getMyAppointments(),
        recordService.getMyRecords(),
        prescriptionService.getMyPrescriptions(),
        notificationService.getUnreadCount(),
        emergencyService.getMyEmergencyRequests().catch(() => ({ data: [] })),
        doctorLeaveService.getPatientPendingReassignments().catch(() => ({ data: [] })),
        reportService.getMyReports().catch(() => ({ data: [] })),
      ]);
      setAppointments(appRes.data || []);
      setRecords(recRes.data || []);
      setPrescriptions(presRes.data || []);
      setReports(repRes.data || []);
      setUnreadCount(notifRes.data?.unreadCount || 0);
      setEmergencyRequests(emergRes.data || []);
      setPendingReassignments(reassignRes.data || []);
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
        : "Welcome back! Your health timeline is completely up to date. You can consult with a specialist or message your doctor via DoctorConnect anytime.";
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

      {/* Urgent Doctor Reassignment Consent Notice (Phase 3) */}
      {pendingReassignments.length > 0 && (
        <div
          className="card"
          style={{
            marginBottom: '24px',
            padding: '18px 24px',
            background: '#fffbeb',
            border: '2px solid #f59e0b',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: '16px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
            <div style={{ width: '42px', height: '42px', borderRadius: '50%', background: '#fef3c7', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#d97706', flexShrink: 0 }}>
              <UserCheck size={22} />
            </div>
            <div>
              <div style={{ fontWeight: 800, fontSize: '1rem', color: '#92400e' }}>
                Physician Absence Notice: {pendingReassignments.length} Consultation Reassignment Awaiting Your Consent
              </div>
              <div style={{ fontSize: '0.8125rem', color: '#b45309' }}>
                Your consulting doctor is on approved leave. Qualified replacement specialists have been proposed for your review.
              </div>
            </div>
          </div>
          <Link to="/patient/appointments" className="btn btn-primary" style={{ background: '#d97706', borderColor: '#b45309' }}>
            Review Reassignment Details →
          </Link>
        </div>
      )}

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
                <Link to="/patient/doctor-connect" className="btn btn-primary btn-sm">
                  <MessageSquare size={14} /> Message Doctor
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
              to="/patient/doctor-connect"
              className="card"
              style={{
                padding: '14px 18px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                background: '#fffbeb',
                borderColor: '#fde68a',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <MessageSquare size={20} style={{ color: '#d97706' }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--slate-900)' }}>
                    DoctorConnect Inquiries
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)' }}>
                    Ask questions & receive doctor guidance
                  </div>
                </div>
              </div>
              <ArrowRight size={16} style={{ color: '#d97706' }} />
            </Link>

            <Link
              to="/patient/records"
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
                <FileText size={20} style={{ color: 'var(--teal-600)' }} />
                <div>
                  <div style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--slate-900)' }}>
                    Medical Records
                  </div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)' }}>
                    View clinical notes & diagnostic history
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

      {/* Patient Medical History & Clinical Records Section (Phase 4) */}
      <div className="card" style={{ marginBottom: '28px' }}>
        <div className="card-header" style={{ flexWrap: 'wrap', gap: '12px' }}>
          <div>
            <span className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <ClipboardList size={22} className="text-primary" /> Patient Medical History & Clinical Documentation
            </span>
            <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', marginTop: '2px' }}>
              Comprehensive longitudinal health timeline of physician diagnoses, clinical notes, digital prescriptions, and previous consultations.
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => window.print()}
              title="Print Medical Summary"
            >
              <Printer size={14} /> Print Summary
            </button>
            <Link to="/patient/records" className="btn btn-outline btn-sm">
              All Records Repository →
            </Link>
          </div>
        </div>

        {/* Filter Tabs and Instant Search */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '14px', marginBottom: '20px' }}>
          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
            <button
              type="button"
              className={`btn btn-sm ${historyTab === 'ALL' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setHistoryTab('ALL')}
            >
              <Activity size={14} /> Unified Journey ({records.length + prescriptions.length + appointments.length + reports.length})
            </button>
            <button
              type="button"
              className={`btn btn-sm ${historyTab === 'CONSULTATIONS' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setHistoryTab('CONSULTATIONS')}
            >
              <FileText size={14} /> Diagnoses & Notes ({records.length})
            </button>
            <button
              type="button"
              className={`btn btn-sm ${historyTab === 'PRESCRIPTIONS' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setHistoryTab('PRESCRIPTIONS')}
            >
              <Pill size={14} /> Prescriptions ({prescriptions.length})
            </button>
            <button
              type="button"
              className={`btn btn-sm ${historyTab === 'REPORTS' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setHistoryTab('REPORTS')}
            >
              <FileUp size={14} /> Reports & Scans ({reports.length})
            </button>
            <button
              type="button"
              className={`btn btn-sm ${historyTab === 'APPOINTMENTS' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setHistoryTab('APPOINTMENTS')}
            >
              <Calendar size={14} /> Past Appointments ({appointments.length})
            </button>
          </div>

          <div style={{ minWidth: '220px', maxWidth: '320px', flex: 1 }}>
            <input
              type="text"
              className="form-control"
              style={{ fontSize: '0.8125rem', padding: '6px 12px' }}
              placeholder="Search diagnoses, doctors, medicines, reports..."
              value={historySearch}
              onChange={(e) => setHistorySearch(e.target.value)}
            />
          </div>
        </div>

        {/* Content Display */}
        {(() => {
          const s = historySearch.toLowerCase();

          const filteredRecords = records.filter((r) =>
            !s ||
            r.diagnosis?.toLowerCase().includes(s) ||
            r.doctor?.fullName?.toLowerCase().includes(s) ||
            r.doctor?.specialization?.toLowerCase().includes(s) ||
            r.symptoms?.toLowerCase().includes(s) ||
            r.treatment?.toLowerCase().includes(s) ||
            r.consultationNotes?.toLowerCase().includes(s)
          );

          const filteredPrescriptions = prescriptions.filter((p) =>
            !s ||
            p.medicineName?.toLowerCase().includes(s) ||
            p.doctor?.fullName?.toLowerCase().includes(s) ||
            p.dosage?.toLowerCase().includes(s) ||
            p.instructions?.toLowerCase().includes(s)
          );

          const filteredAppointments = appointments.filter((a) =>
            !s ||
            a.doctor?.fullName?.toLowerCase().includes(s) ||
            a.doctor?.specialization?.toLowerCase().includes(s) ||
            a.reason?.toLowerCase().includes(s) ||
            a.status?.toLowerCase().includes(s) ||
            a.consultationNotes?.toLowerCase().includes(s)
          );

          const filteredReports = reports.filter((rep) =>
            !s ||
            rep.title?.toLowerCase().includes(s) ||
            rep.reportType?.toLowerCase().includes(s) ||
            rep.originalFilename?.toLowerCase().includes(s) ||
            rep.clinicalNotes?.toLowerCase().includes(s)
          );

          const totalItems =
            (historyTab === 'ALL' ? (filteredRecords.length + filteredPrescriptions.length + filteredAppointments.length + filteredReports.length) :
             historyTab === 'CONSULTATIONS' ? filteredRecords.length :
             historyTab === 'PRESCRIPTIONS' ? filteredPrescriptions.length :
             historyTab === 'REPORTS' ? filteredReports.length :
             filteredAppointments.length);

          if (totalItems === 0) {
            return (
              <div style={{ textAlign: 'center', padding: '36px 16px', color: 'var(--slate-500)', fontSize: '0.875rem' }}>
                <ClipboardList size={36} style={{ color: 'var(--slate-400)', margin: '0 auto 10px' }} />
                <p style={{ fontWeight: 600, color: 'var(--slate-700)', marginBottom: '4px' }}>
                  No medical history found matching your filters.
                </p>
                <p style={{ fontSize: '0.8125rem' }}>
                  Medical records, doctor consultation notes, and prescriptions will appear here as your care journey progresses.
                </p>
              </div>
            );
          }

          return (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              {/* Consultations & Diagnoses Section */}
              {(historyTab === 'ALL' || historyTab === 'CONSULTATIONS') && filteredRecords.length > 0 && (
                <div>
                  <div style={{ fontSize: '0.8125rem', fontWeight: 700, color: 'var(--slate-500)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <FileText size={15} className="text-primary" /> Physician Diagnoses & Clinical Notes ({filteredRecords.length})
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                    {filteredRecords.map((rec) => (
                      <div
                        key={`rec-${rec.id}`}
                        style={{
                          background: 'white',
                          border: '1px solid var(--slate-200)',
                          borderRadius: 'var(--radius-lg)',
                          padding: '16px',
                          boxShadow: 'var(--shadow-sm)',
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '8px', marginBottom: '8px' }}>
                          <div>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                              <h4 style={{ fontSize: '1.1rem', fontWeight: 800, color: 'var(--slate-900)', margin: 0 }}>
                                {rec.diagnosis}
                              </h4>
                              <span style={{ fontSize: '0.75rem', fontWeight: 700, background: 'var(--primary-50)', color: 'var(--primary-700)', padding: '2px 8px', borderRadius: '4px' }}>
                                Clinical Record #{rec.id}
                              </span>
                            </div>
                            <div style={{ fontSize: '0.8125rem', color: 'var(--primary-700)', fontWeight: 600, marginTop: '2px' }}>
                              Attending Physician: Dr. {rec.doctor?.fullName} ({rec.doctor?.specialization})
                            </div>
                          </div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8125rem', color: 'var(--slate-500)', fontWeight: 500 }}>
                            <Calendar size={14} /> {rec.recordDate}
                          </div>
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '10px', background: 'var(--slate-50)', padding: '10px 14px', borderRadius: 'var(--radius-md)', marginBottom: '10px', fontSize: '0.8125rem' }}>
                          <div>
                            <span style={{ color: 'var(--slate-500)', display: 'block', fontWeight: 600 }}>Reported Symptoms</span>
                            <span style={{ color: 'var(--slate-800)' }}>{rec.symptoms || 'None recorded'}</span>
                          </div>
                          <div>
                            <span style={{ color: 'var(--slate-500)', display: 'block', fontWeight: 600 }}>Prescribed Treatment Plan</span>
                            <span style={{ color: 'var(--slate-800)' }}>{rec.treatment || 'Standard clinical observation'}</span>
                          </div>
                        </div>

                        {rec.consultationNotes && (
                          <div style={{ background: '#f8fafc', borderLeft: '3px solid var(--primary-600)', padding: '8px 12px', borderRadius: '0 6px 6px 0', fontSize: '0.8125rem', color: 'var(--slate-700)', marginBottom: '8px' }}>
                            <strong>Doctor Consultation Notes:</strong> {rec.consultationNotes}
                          </div>
                        )}

                        {rec.clinicalSummary && (
                          <div style={{ background: '#f0fdf4', border: '1px solid #bbf7d0', padding: '8px 12px', borderRadius: '6px', fontSize: '0.8125rem', color: '#166534', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '6px' }}>
                            <span><strong>Physician-Verified Summary:</strong> {rec.clinicalSummary}</span>
                            <span style={{ fontSize: '0.7rem', fontWeight: 700, color: '#15803d', display: 'flex', alignItems: 'center', gap: '4px' }}>
                              <ShieldCheck size={13} /> Verified
                            </span>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Prescriptions Section */}
              {(historyTab === 'ALL' || historyTab === 'PRESCRIPTIONS') && filteredPrescriptions.length > 0 && (
                <div style={{ marginTop: historyTab === 'ALL' ? '12px' : 0 }}>
                  <div style={{ fontSize: '0.8125rem', fontWeight: 700, color: 'var(--slate-500)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <Pill size={15} className="text-teal" /> Prescribed Pharmaceutical Regimens ({filteredPrescriptions.length})
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '12px' }}>
                    {filteredPrescriptions.map((pres) => (
                      <div
                        key={`pres-${pres.id}`}
                        style={{
                          background: 'white',
                          border: '1px solid var(--teal-200)',
                          borderRadius: 'var(--radius-lg)',
                          padding: '14px',
                          boxShadow: 'var(--shadow-sm)',
                        }}
                      >
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                          <div>
                            <strong style={{ fontSize: '1rem', color: 'var(--slate-900)' }}>{pres.medicineName}</strong>
                            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--teal-700)', background: 'var(--teal-50)', display: 'inline-block', padding: '1px 6px', borderRadius: '4px', marginTop: '2px' }}>
                              {pres.dosage}
                            </div>
                          </div>
                          <span style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>{pres.issuedDate}</span>
                        </div>
                        <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', marginBottom: '4px' }}>
                          <strong>Regimen:</strong> {pres.frequency} • {pres.duration}
                        </div>
                        {pres.instructions && (
                          <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)', fontStyle: 'italic', marginBottom: '6px' }}>
                            {pres.instructions}
                          </div>
                        )}
                        <div style={{ borderTop: '1px solid var(--slate-100)', paddingTop: '6px', fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                          Prescribed by Dr. {pres.doctor?.fullName || 'Physician'}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Medical Reports & Diagnostic Scans Section (Phase 5) */}
              {(historyTab === 'ALL' || historyTab === 'REPORTS') && filteredReports.length > 0 && (
                <div style={{ marginTop: historyTab === 'ALL' ? '12px' : 0 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                    <div style={{ fontSize: '0.8125rem', fontWeight: 700, color: 'var(--slate-500)', textTransform: 'uppercase', letterSpacing: '0.04em', display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <FileUp size={15} className="text-primary" /> Verified Medical Reports & Diagnostic Scans ({filteredReports.length})
                    </div>
                    <Link to="/patient/records" style={{ fontSize: '0.75rem', color: 'var(--primary-600)', fontWeight: 600 }}>
                      + Upload New Report
                    </Link>
                  </div>
                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '12px' }}>
                    {filteredReports.map((rep) => (
                      <div
                        key={`rep-${rep.id}`}
                        style={{
                          background: 'white',
                          border: '1px solid var(--primary-100)',
                          borderRadius: 'var(--radius-lg)',
                          padding: '14px',
                          boxShadow: 'var(--shadow-sm)',
                          display: 'flex',
                          flexDirection: 'column',
                          justifyContent: 'space-between',
                        }}
                      >
                        <div>
                          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                            <div style={{ fontWeight: 700, color: 'var(--slate-900)', fontSize: '0.9375rem', lineHeight: 1.3 }}>
                              {rep.title}
                            </div>
                            <span style={{ fontSize: '0.7rem', fontWeight: 700, background: 'var(--primary-50)', color: 'var(--primary-700)', padding: '2px 8px', borderRadius: '4px', textTransform: 'uppercase' }}>
                              {rep.reportType?.replace('_', ' ')}
                            </span>
                          </div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '6px' }}>
                            <Calendar size={13} /> {rep.reportDate} • <span style={{ textTransform: 'uppercase', fontWeight: 600 }}>{rep.fileFormat}</span> ({rep.formattedFileSize})
                          </div>
                          {rep.clinicalNotes && (
                            <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)', background: 'var(--slate-50)', padding: '6px 8px', borderRadius: '4px', marginBottom: '8px' }}>
                              {rep.clinicalNotes}
                            </div>
                          )}
                        </div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--slate-100)', paddingTop: '8px', marginTop: '6px' }}>
                          <span style={{ fontSize: '0.7rem', color: 'var(--slate-400)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '140px' }} title={rep.originalFilename}>
                            {rep.originalFilename}
                          </span>
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            style={{ padding: '3px 8px', fontSize: '0.75rem' }}
                            onClick={() => reportService.downloadReportBlob(rep.id, rep.originalFilename)}
                            title="Download Report"
                          >
                            <Download size={12} /> Download
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Past Appointments Section */}
              {(historyTab === 'ALL' || historyTab === 'APPOINTMENTS') && filteredAppointments.length > 0 && (
                <div style={{ marginTop: historyTab === 'ALL' ? '12px' : 0 }}>
                  <div style={{ fontSize: '0.8125rem', fontWeight: 700, color: 'var(--slate-500)', textTransform: 'uppercase', letterSpacing: '0.04em', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                    <Calendar size={15} className="text-indigo" /> Consultation Visit History ({filteredAppointments.length})
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {filteredAppointments.map((appt) => (
                      <div
                        key={`appt-${appt.id}`}
                        style={{
                          background: 'white',
                          border: '1px solid var(--slate-200)',
                          borderRadius: 'var(--radius-md)',
                          padding: '12px 16px',
                          display: 'flex',
                          justifyContent: 'space-between',
                          alignItems: 'center',
                          flexWrap: 'wrap',
                          gap: '12px',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                          <div style={{ width: '38px', height: '38px', borderRadius: '50%', background: appt.status === 'COMPLETED' ? '#ecfdf5' : appt.status === 'CANCELLED' ? '#fef2f2' : 'var(--primary-50)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: appt.status === 'COMPLETED' ? '#059669' : appt.status === 'CANCELLED' ? '#dc2626' : 'var(--primary-600)', flexShrink: 0 }}>
                            <Calendar size={18} />
                          </div>
                          <div>
                            <div style={{ fontWeight: 700, color: 'var(--slate-900)', fontSize: '0.9375rem' }}>
                              Dr. {appt.doctor?.fullName} ({appt.doctor?.specialization})
                            </div>
                            <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)' }}>
                              Reason: {appt.reason}
                            </div>
                            {(appt.consultationNotes || appt.cancellationReason) && (
                              <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', marginTop: '2px' }}>
                                {appt.consultationNotes ? `Remarks: ${appt.consultationNotes}` : `Cancellation: ${appt.cancellationReason}`}
                              </div>
                            )}
                          </div>
                        </div>

                        <div style={{ textAlign: 'right' }}>
                          <span className={`badge badge-${appt.status.toLowerCase()}`}>
                            {appt.status}
                          </span>
                          <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', marginTop: '4px' }}>
                            {appt.appointmentDate} at {appt.appointmentTime}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Data Integrity Assurance */}
              <div style={{ borderTop: '1px solid var(--slate-100)', paddingTop: '12px', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                <ShieldCheck size={16} style={{ color: 'var(--primary-600)' }} />
                <span>
                  Official Physician Documentation: All diagnoses, consultation notes, and prescriptions are immutable by patients and cryptographically audited by CarePulse Healthcare Systems.
                </span>
              </div>
            </div>
          );
        })()}
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
