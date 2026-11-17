import React from 'react';
import { User, Shield, CheckCircle2 } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const ProfilePage = () => {
  const { user } = useAuth();

  return (
    <div style={{ maxWidth: '780px' }}>
      <div style={{ marginBottom: '28px' }}>
        <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Account Profile & Credentials</h2>
        <p style={{ color: 'var(--slate-600)' }}>
          Manage your personal details, clinical credentials, and account authentication status.
        </p>
      </div>

      {/* Account Info Card */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <div className="card-header">
          <span className="card-title">
            <User size={20} className="text-primary" /> Profile Information
          </span>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
          <div>
            <label className="form-label">Full Name</label>
            <div style={{ fontWeight: 600, fontSize: '1.05rem', color: 'var(--slate-900)' }}>
              {user?.fullName || 'User'}
            </div>
          </div>

          <div>
            <label className="form-label">Email Address</label>
            <div style={{ fontWeight: 600, fontSize: '1.05rem', color: 'var(--slate-900)' }}>
              {user?.email}
            </div>
          </div>

          <div>
            <label className="form-label">Assigned Role</label>
            <span className="badge badge-role">{user?.role}</span>
          </div>

          <div>
            <label className="form-label">Security Protocol</label>
            <span style={{ fontSize: '0.875rem', color: 'var(--teal-700)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '4px' }}>
              <Shield size={16} /> BCrypt Protected & JWT Authenticated
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
