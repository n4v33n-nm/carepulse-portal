import React from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from '../components/Sidebar';
import NotificationDropdown from '../components/NotificationDropdown';
import { useAuth } from '../context/AuthContext';
import { Sparkles } from 'lucide-react';

const DashboardLayout = () => {
  const { user, updateEmpathyPreference } = useAuth();
  const location = useLocation();

  const getPageTitle = () => {
    const path = location.pathname;
    if (path.includes('dashboard')) return 'Overview Dashboard';
    if (path.includes('doctors')) return 'Find Healthcare Specialists';
    if (path.includes('appointments')) return 'Appointment Management';
    if (path.includes('records')) return 'Medical Records & Timeline';
    if (path.includes('prescriptions')) return 'Digital Prescriptions';
    if (path.includes('ai-companion')) return 'AI Health Companion';
    if (path.includes('caregivers')) return 'Caregiver Access & Proxies';
    if (path.includes('availability')) return 'Consultation Schedule';
    if (path.includes('patients')) return 'Assigned Patients';
    if (path.includes('audit-logs')) return 'Security Audit Trail';
    if (path.includes('profile')) return 'My Profile & Preferences';
    return 'CarePulse Portal';
  };

  const handlePreferenceChange = (e) => {
    updateEmpathyPreference(e.target.value);
  };

  return (
    <div className="app-layout">
      <Sidebar />
      <div className="main-content">
        <header className="top-navbar">
          <h1 className="page-header-title">{getPageTitle()}</h1>

          <div className="navbar-actions">
            {/* Empathy Engine Tone Selector */}
            <div className="empathy-selector" title="Empathy Engine: Adjust communication tone">
              <Sparkles size={14} style={{ color: 'var(--primary-600)' }} />
              <span style={{ color: 'var(--slate-500)' }}>Tone:</span>
              <select
                value={user?.communicationPreference || 'SUPPORTIVE'}
                onChange={handlePreferenceChange}
              >
                <option value="SIMPLE">Simple</option>
                <option value="SUPPORTIVE">Supportive</option>
                <option value="PROFESSIONAL">Professional</option>
              </select>
            </div>

            {/* Notification Bell */}
            <NotificationDropdown />

            {/* User Profile Badge */}
            <div className="user-profile-badge">
              <div className="user-avatar">
                {user?.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', lineHeight: 1.2 }}>
                <span style={{ fontSize: '0.8125rem', fontWeight: 700, color: 'var(--slate-900)' }}>
                  {user?.fullName?.split(' ')[0] || 'User'}
                </span>
                <span style={{ fontSize: '0.6875rem', color: 'var(--slate-500)', textTransform: 'capitalize' }}>
                  {user?.role?.toLowerCase()}
                </span>
              </div>
            </div>
          </div>
        </header>

        <main className="content-body">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default DashboardLayout;
