import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  Activity,
  LayoutDashboard,
  Search,
  Calendar,
  FileText,
  Pill,
  Bot,
  Users,
  Clock,
  Shield,
  User,
  LogOut,
  Stethoscope,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const Sidebar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const role = user?.role || 'PATIENT';

  return (
    <aside className="sidebar">
      <div className="sidebar-brand">
        <div className="brand-icon">
          <Activity size={22} strokeWidth={2.5} />
        </div>
        <div>
          <div className="brand-name">CarePulse</div>
          <div style={{ fontSize: '0.6875rem', color: 'var(--slate-500)', fontWeight: 600, letterSpacing: '0.04em' }}>
            COORDINATION PLATFORM
          </div>
        </div>
      </div>

      <nav className="sidebar-nav">
        {/* PATIENT NAV */}
        {role === 'PATIENT' && (
          <>
            <NavLink to="/patient/dashboard" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <LayoutDashboard size={18} />
              <span>Dashboard</span>
            </NavLink>
            <NavLink to="/patient/doctors" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Search size={18} />
              <span>Find Doctor</span>
            </NavLink>
            <NavLink to="/patient/appointments" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Calendar size={18} />
              <span>Appointments</span>
            </NavLink>
            <NavLink to="/patient/records" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <FileText size={18} />
              <span>Medical Records</span>
            </NavLink>
            <NavLink to="/patient/prescriptions" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Pill size={18} />
              <span>Prescriptions</span>
            </NavLink>
            <NavLink to="/patient/ai-companion" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Bot size={18} />
              <span>AI Companion</span>
            </NavLink>
            <NavLink to="/patient/caregivers" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Users size={18} />
              <span>Caregiver Access</span>
            </NavLink>
          </>
        )}

        {/* DOCTOR NAV */}
        {role === 'DOCTOR' && (
          <>
            <NavLink to="/doctor/dashboard" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <LayoutDashboard size={18} />
              <span>Dashboard</span>
            </NavLink>
            <NavLink to="/doctor/appointments" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Calendar size={18} />
              <span>Appointments</span>
            </NavLink>
            <NavLink to="/doctor/availability" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Clock size={18} />
              <span>Availability</span>
            </NavLink>
            <NavLink to="/doctor/patients" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Users size={18} />
              <span>My Patients</span>
            </NavLink>
            <NavLink to="/doctor/records" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <FileText size={18} />
              <span>Consultation Records</span>
            </NavLink>
          </>
        )}

        {/* ADMIN NAV */}
        {role === 'ADMIN' && (
          <>
            <NavLink to="/admin/dashboard" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <LayoutDashboard size={18} />
              <span>Platform Dashboard</span>
            </NavLink>
            <NavLink to="/admin/audit-logs" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
              <Shield size={18} />
              <span>Security & Audit</span>
            </NavLink>
          </>
        )}

        <div style={{ height: '1px', background: 'var(--slate-200)', margin: '12px 4px' }} />

        <NavLink to="/profile" className={({ isActive }) => `nav-item ${isActive ? 'active' : ''}`}>
          <User size={18} />
          <span>My Profile</span>
        </NavLink>
      </nav>

      <div className="sidebar-footer">
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ overflow: 'hidden' }}>
            <div style={{ fontWeight: 600, fontSize: '0.875rem', color: 'var(--slate-900)', whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden' }}>
              {user?.fullName || user?.email}
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', textTransform: 'capitalize' }}>
              {role.toLowerCase()}
            </div>
          </div>
          <button
            onClick={handleLogout}
            title="Log out"
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--slate-400)',
              padding: '6px',
              borderRadius: '6px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              transition: 'color 0.15s ease',
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = '#ef4444')}
            onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--slate-400)')}
          >
            <LogOut size={18} />
          </button>
        </div>
      </div>
    </aside>
  );
};

export default Sidebar;
