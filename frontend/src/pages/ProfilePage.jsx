import React, { useState } from 'react';
import { User, Sparkles, CheckCircle2, Shield, HeartPulse } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

const ProfilePage = () => {
  const { user, updateEmpathyPreference } = useAuth();
  const [preference, setPreference] = useState(user?.communicationPreference || 'SUPPORTIVE');
  const [success, setSuccess] = useState('');
  const [saving, setSaving] = useState(false);

  const handlePreferenceSave = async () => {
    setSaving(true);
    setSuccess('');
    try {
      await updateEmpathyPreference(preference);
      setSuccess('Communication tone preference successfully updated!');
      setTimeout(() => setSuccess(''), 3000);
    } catch (err) {
      alert('Failed to update preference');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ maxWidth: '780px' }}>
      <div style={{ marginBottom: '28px' }}>
        <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Account Profile & Empathy Settings</h2>
        <p style={{ color: 'var(--slate-600)' }}>
          Manage your personal details, credentials, and healthcare communication tone.
        </p>
      </div>

      {success && (
        <div
          style={{
            padding: '12px 16px',
            background: 'var(--success-bg)',
            color: 'var(--success-text)',
            border: '1px solid var(--success-border)',
            borderRadius: 'var(--radius-md)',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <CheckCircle2 size={18} />
          <span>{success}</span>
        </div>
      )}

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

      {/* Empathy Engine Tone Card */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">
            <Sparkles size={20} className="text-teal" /> Empathy Engine – Communication Tone
          </span>
        </div>

        <p style={{ fontSize: '0.9375rem', color: 'var(--slate-600)', marginBottom: '18px', lineHeight: 1.5 }}>
          Select how you want notifications, reminders, and AI companion guidance formatted. The platform automatically customizes phrasing to your comfort level:
        </p>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', marginBottom: '24px' }}>
          {[
            {
              id: 'SIMPLE',
              title: 'Simple',
              desc: 'Direct, clear, concise instructions without clinical jargon or lengthy descriptions.',
              example: '"Your appointment is tomorrow at 10:00 AM."',
            },
            {
              id: 'SUPPORTIVE',
              title: 'Supportive',
              desc: 'Compassionate, reassuring, and encouraging tone designed to ease healthcare anxiety.',
              example: '"Great news! Your consultation is tomorrow at 10:00 AM. Please keep your previous documents ready."',
            },
            {
              id: 'PROFESSIONAL',
              title: 'Professional',
              desc: 'Formal, structured clinical phrasing prioritizing protocol and medical terminology.',
              example: '"Notice: Your scheduled consultation is slated for tomorrow at 10:00 AM."',
            },
          ].map((item) => (
            <label
              key={item.id}
              style={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: '14px',
                padding: '16px',
                border: preference === item.id ? '2px solid var(--primary-600)' : '1px solid var(--slate-200)',
                background: preference === item.id ? 'var(--primary-50)' : 'white',
                borderRadius: 'var(--radius-md)',
                cursor: 'pointer',
                transition: 'all 0.15s ease',
              }}
            >
              <input
                type="radio"
                name="empathyPreference"
                value={item.id}
                checked={preference === item.id}
                onChange={(e) => setPreference(e.target.value)}
                style={{ marginTop: '4px', accentColor: 'var(--primary-600)' }}
              />
              <div>
                <div style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--slate-900)' }}>
                  {item.title}
                </div>
                <div style={{ fontSize: '0.875rem', color: 'var(--slate-600)', margin: '4px 0 6px' }}>
                  {item.desc}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--primary-800)', fontStyle: 'italic', background: 'rgba(2, 132, 199, 0.08)', padding: '4px 8px', borderRadius: '4px' }}>
                  {item.example}
                </div>
              </div>
            </label>
          ))}
        </div>

        <button
          type="button"
          className="btn btn-primary"
          onClick={handlePreferenceSave}
          disabled={saving}
        >
          {saving ? 'Saving Preference...' : 'Save Communication Preference'}
        </button>
      </div>
    </div>
  );
};

export default ProfilePage;
