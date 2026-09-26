import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Activity, User, Stethoscope, AlertCircle, ArrowRight } from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const RegisterPage = () => {
  const [role, setRole] = useState('PATIENT'); // 'PATIENT' | 'DOCTOR'
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  // Patient Fields
  const [patientData, setPatientData] = useState({
    fullName: '',
    email: '',
    phone: '',
    dateOfBirth: '',
    gender: 'Male',
    bloodGroup: 'O+',
    address: '',
    emergencyContact: '',
    password: '',
    communicationPreference: 'SUPPORTIVE',
  });

  // Doctor Fields
  const [doctorData, setDoctorData] = useState({
    fullName: '',
    email: '',
    phone: '',
    specialization: 'General Medicine',
    qualification: 'MD, Internal Medicine',
    experienceYears: 5,
    consultationFee: 800,
    bio: '',
    password: '',
    communicationPreference: 'PROFESSIONAL',
  });

  const { registerPatient, registerDoctor } = useAuth();
  const navigate = useNavigate();

  const handlePatientSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await registerPatient(patientData);
      navigate('/patient/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please check your details.');
    } finally {
      setLoading(false);
    }
  };

  const handleDoctorSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await registerDoctor(doctorData);
      navigate('/doctor/dashboard');
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please check your details.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '32px 16px',
      }}
    >
      <div
        style={{
          width: '100%',
          maxWidth: '620px',
          background: 'white',
          borderRadius: 'var(--radius-xl)',
          padding: '40px',
          boxShadow: 'var(--shadow-xl)',
          border: '1px solid var(--slate-200)',
        }}
      >
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <Link to="/" style={{ display: 'inline-flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
            <div className="brand-icon">
              <Activity size={22} strokeWidth={2.5} />
            </div>
            <span style={{ fontSize: '1.4rem', fontWeight: 800, color: 'var(--slate-900)' }}>
              CarePulse
            </span>
          </Link>
          <h2 style={{ fontSize: '1.5rem', marginBottom: '6px' }}>Create an Account</h2>
          <p style={{ color: 'var(--slate-500)', fontSize: '0.9375rem' }}>
            Join our unified healthcare coordination network
          </p>
        </div>

        {/* Role Toggle Tabs */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            background: 'var(--slate-100)',
            padding: '4px',
            borderRadius: 'var(--radius-md)',
            marginBottom: '24px',
          }}
        >
          <button
            type="button"
            onClick={() => { setRole('PATIENT'); setError(''); }}
            style={{
              padding: '10px',
              borderRadius: 'var(--radius-sm)',
              background: role === 'PATIENT' ? 'white' : 'transparent',
              color: role === 'PATIENT' ? 'var(--primary-700)' : 'var(--slate-600)',
              fontWeight: 700,
              fontSize: '0.9375rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              boxShadow: role === 'PATIENT' ? 'var(--shadow-sm)' : 'none',
              transition: 'all 0.15s ease',
            }}
          >
            <User size={18} /> I am a Patient
          </button>

          <button
            type="button"
            onClick={() => { setRole('DOCTOR'); setError(''); }}
            style={{
              padding: '10px',
              borderRadius: 'var(--radius-sm)',
              background: role === 'DOCTOR' ? 'white' : 'transparent',
              color: role === 'DOCTOR' ? 'var(--primary-700)' : 'var(--slate-600)',
              fontWeight: 700,
              fontSize: '0.9375rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              boxShadow: role === 'DOCTOR' ? 'var(--shadow-sm)' : 'none',
              transition: 'all 0.15s ease',
            }}
          >
            <Stethoscope size={18} /> I am a Doctor
          </button>
        </div>

        {error && (
          <div
            style={{
              padding: '12px 14px',
              background: 'var(--danger-bg)',
              border: '1px solid var(--danger-border)',
              borderRadius: 'var(--radius-md)',
              color: 'var(--danger-text)',
              fontSize: '0.875rem',
              marginBottom: '20px',
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}
          >
            <AlertCircle size={18} />
            <span>{error}</span>
          </div>
        )}

        {/* Patient Registration Form */}
        {role === 'PATIENT' && (
          <form onSubmit={handlePatientSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="pat-name">Full Name *</label>
                <input
                  id="pat-name"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Jane Doe"
                  value={patientData.fullName}
                  onChange={(e) => setPatientData({ ...patientData, fullName: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="pat-email">Email Address *</label>
                <input
                  id="pat-email"
                  type="email"
                  className="form-control"
                  placeholder="jane@example.com"
                  value={patientData.email}
                  onChange={(e) => setPatientData({ ...patientData, email: e.target.value })}
                  required
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="pat-phone">Phone Number</label>
                <input
                  id="pat-phone"
                  type="tel"
                  className="form-control"
                  placeholder="+1 (555) 000-0000"
                  value={patientData.phone}
                  onChange={(e) => setPatientData({ ...patientData, phone: e.target.value })}
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="pat-dob">Date of Birth</label>
                <input
                  id="pat-dob"
                  type="date"
                  className="form-control"
                  value={patientData.dateOfBirth}
                  onChange={(e) => setPatientData({ ...patientData, dateOfBirth: e.target.value })}
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="pat-gender">Gender</label>
                <select
                  id="pat-gender"
                  className="form-control"
                  value={patientData.gender}
                  onChange={(e) => setPatientData({ ...patientData, gender: e.target.value })}
                >
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Other">Other</option>
                  <option value="Prefer not to say">Prefer not to say</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="pat-blood">Blood Group</label>
                <select
                  id="pat-blood"
                  className="form-control"
                  value={patientData.bloodGroup}
                  onChange={(e) => setPatientData({ ...patientData, bloodGroup: e.target.value })}
                >
                  <option value="O+">O+</option>
                  <option value="O-">O-</option>
                  <option value="A+">A+</option>
                  <option value="A-">A-</option>
                  <option value="B+">B+</option>
                  <option value="B-">B-</option>
                  <option value="AB+">AB+</option>
                  <option value="AB-">AB-</option>
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="pat-address">Home Address</label>
              <input
                id="pat-address"
                type="text"
                className="form-control"
                placeholder="Street address, City, State"
                value={patientData.address}
                onChange={(e) => setPatientData({ ...patientData, address: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="pat-emg">Emergency Contact</label>
              <input
                id="pat-emg"
                type="text"
                className="form-control"
                placeholder="e.g. John Doe, Spouse: +1 (555) 999-1111"
                value={patientData.emergencyContact}
                onChange={(e) => setPatientData({ ...patientData, emergencyContact: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="pat-pass">Password (min 6 characters) *</label>
              <input
                id="pat-pass"
                type="password"
                className="form-control"
                placeholder="••••••••"
                value={patientData.password}
                onChange={(e) => setPatientData({ ...patientData, password: e.target.value })}
                minLength={6}
                required
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', marginTop: '12px' }}
              disabled={loading}
            >
              {loading ? 'Creating Account...' : 'Complete Patient Registration'} <ArrowRight size={16} />
            </button>
          </form>
        )}

        {/* Doctor Registration Form */}
        {role === 'DOCTOR' && (
          <form onSubmit={handleDoctorSubmit}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="doc-name">Doctor's Full Name *</label>
                <input
                  id="doc-name"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Dr. Arthur Conan"
                  value={doctorData.fullName}
                  onChange={(e) => setDoctorData({ ...doctorData, fullName: e.target.value })}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="doc-email">Email Address *</label>
                <input
                  id="doc-email"
                  type="email"
                  className="form-control"
                  placeholder="doctor@hospital.org"
                  value={doctorData.email}
                  onChange={(e) => setDoctorData({ ...doctorData, email: e.target.value })}
                  required
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="doc-spec">Specialization *</label>
                <select
                  id="doc-spec"
                  className="form-control"
                  value={doctorData.specialization}
                  onChange={(e) => setDoctorData({ ...doctorData, specialization: e.target.value })}
                >
                  <option value="Cardiology">Cardiology</option>
                  <option value="Dermatology">Dermatology</option>
                  <option value="General Medicine">General Medicine</option>
                  <option value="Neurology">Neurology</option>
                  <option value="Pediatrics">Pediatrics</option>
                  <option value="Orthopedics">Orthopedics</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="doc-qual">Qualification *</label>
                <input
                  id="doc-qual"
                  type="text"
                  className="form-control"
                  placeholder="e.g. MD, FACC, MBBS"
                  value={doctorData.qualification}
                  onChange={(e) => setDoctorData({ ...doctorData, qualification: e.target.value })}
                  required
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="doc-exp">Years of Experience</label>
                <input
                  id="doc-exp"
                  type="number"
                  className="form-control"
                  min="0"
                  value={doctorData.experienceYears}
                  onChange={(e) => setDoctorData({ ...doctorData, experienceYears: parseInt(e.target.value) || 0 })}
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="doc-fee">Consultation Fee (₹)</label>
                <input
                  id="doc-fee"
                  type="number"
                  className="form-control"
                  min="0"
                  step="50"
                  placeholder="800"
                  value={doctorData.consultationFee}
                  onChange={(e) => setDoctorData({ ...doctorData, consultationFee: parseFloat(e.target.value) || 0 })}
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="doc-bio">Professional Bio</label>
              <textarea
                id="doc-bio"
                className="form-control"
                rows="3"
                placeholder="Summary of clinical interests and care approach..."
                value={doctorData.bio}
                onChange={(e) => setDoctorData({ ...doctorData, bio: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="doc-pass">Password (min 6 characters) *</label>
              <input
                id="doc-pass"
                type="password"
                className="form-control"
                placeholder="••••••••"
                value={doctorData.password}
                onChange={(e) => setDoctorData({ ...doctorData, password: e.target.value })}
                minLength={6}
                required
              />
            </div>

            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: '100%', padding: '12px', marginTop: '12px' }}
              disabled={loading}
            >
              {loading ? 'Registering Physician...' : 'Complete Doctor Registration'} <ArrowRight size={16} />
            </button>
          </form>
        )}

        <div style={{ textAlign: 'center', marginTop: '24px', fontSize: '0.875rem', color: 'var(--slate-500)' }}>
          Already have an account?{' '}
          <Link to="/login" style={{ fontWeight: 600, color: 'var(--primary-600)' }}>
            Sign in
          </Link>
        </div>
      </div>
    </div>
  );
};

export default RegisterPage;
