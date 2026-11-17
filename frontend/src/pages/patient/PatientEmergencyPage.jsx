import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import {
  Siren,
  HeartPulse,
  AlertTriangle,
  Clock,
  CheckCircle2,
  Stethoscope,
  Phone,
  ArrowLeft,
  RefreshCw,
  Activity,
  ShieldAlert,
} from 'lucide-react';
import { emergencyService } from '../../services/api';

const PatientEmergencyPage = () => {
  const [category, setCategory] = useState('General');
  const [priority, setPriority] = useState('NORMAL');
  const [description, setDescription] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [emergencyRequests, setEmergencyRequests] = useState([]);
  const [allocatedResult, setAllocatedResult] = useState(null);

  useEffect(() => {
    fetchEmergencyRequests();
  }, []);

  const fetchEmergencyRequests = async () => {
    setLoading(true);
    try {
      const res = await emergencyService.getMyEmergencyRequests();
      const list = res.data || [];
      setEmergencyRequests(list);
      if (list.length > 0 && (list[0].status === 'WAITING' || list[0].status === 'ASSIGNED' || list[0].status === 'IN_PROGRESS')) {
        setAllocatedResult(list[0]);
      }
    } catch (err) {
      console.error('Failed to fetch emergency requests:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleRequestEmergency = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    setSuccessMessage(null);

    try {
      const res = await emergencyService.createEmergencyRequest({
        category,
        priority,
        description: description.trim(),
      });
      setAllocatedResult(res.data);
      setSuccessMessage('Emergency triage request dispatched successfully! An available physician is being assigned.');
      setDescription('');
      fetchEmergencyRequests();
    } catch (err) {
      console.error('Emergency request failed:', err);
      setError(err.response?.data?.message || 'Failed to dispatch emergency request. If life-threatening, please call 911 / 112 / 108 immediately.');
    } finally {
      setSubmitting(false);
    }
  };

  const activeRequest = allocatedResult || (emergencyRequests.length > 0 && emergencyRequests[0].status !== 'CANCELLED' && emergencyRequests[0].status !== 'COMPLETED' ? emergencyRequests[0] : null);

  return (
    <div className="container" style={{ maxWidth: '1100px', padding: '24px 16px' }}>
      {/* Top Header & Navigation */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '14px', marginBottom: '20px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div
            style={{
              width: '46px',
              height: '46px',
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
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flexWrap: 'wrap' }}>
              <h1 style={{ fontSize: '1.6rem', fontWeight: 800, color: '#be123c', margin: 0 }}>
                Emergency Medical Assistance
              </h1>
              <span className="emergency-pulse-badge">24/7 Active Duty Roster</span>
            </div>
            <p style={{ color: 'var(--slate-600)', fontSize: '0.875rem', margin: '4px 0 0 0' }}>
              Immediate deterministic clinical triage. Bypass regular booking to connect directly with an active on-duty physician.
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={fetchEmergencyRequests}
            title="Refresh Status"
          >
            <RefreshCw size={14} /> Refresh
          </button>
          <Link to="/patient/dashboard" className="btn btn-outline btn-sm">
            <ArrowLeft size={14} /> Back to Dashboard
          </Link>
        </div>
      </div>

      {/* Critical Life-Threatening Hotline Notice */}
      <div
        style={{
          background: '#fff1f2',
          border: '1px solid #fecdd3',
          borderRadius: 'var(--radius-lg)',
          padding: '14px 18px',
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'flex-start',
          gap: '14px',
        }}
      >
        <ShieldAlert size={22} style={{ color: '#e11d48', flexShrink: 0, marginTop: '2px' }} />
        <div style={{ fontSize: '0.875rem', color: '#9f1239', lineHeight: 1.5 }}>
          <strong>CRITICAL SAFETY NOTICE:</strong> Emergency requests are for urgent situations requiring rapid medical attention. If this is a life-threatening medical emergency (such as severe chest pain, loss of consciousness, or uncontrollable bleeding), please immediately contact your local emergency service: <strong>911 / 112 / 108</strong>.
        </div>
      </div>

      {/* Live Active Emergency Tracker (If an ongoing emergency request exists) */}
      {activeRequest && (
        <div
          className="card"
          style={{
            marginBottom: '28px',
            borderLeft: activeRequest.status === 'WAITING' ? '5px solid #f59e0b' : activeRequest.status === 'COMPLETED' ? '5px solid #10b981' : '5px solid #e11d48',
            background: activeRequest.status === 'WAITING' ? '#fffbeb' : '#fff1f2',
          }}
        >
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '14px', marginBottom: '16px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <Siren size={22} style={{ color: activeRequest.status === 'WAITING' ? '#b45309' : '#e11d48' }} />
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                  <h3 style={{ fontSize: '1.2rem', fontWeight: 800, margin: 0, color: activeRequest.status === 'WAITING' ? '#92400e' : '#9f1239' }}>
                    Active Emergency Tracking #{activeRequest.id}
                  </h3>
                  <span className={`badge badge-${activeRequest.status.toLowerCase()}`}>
                    {activeRequest.status}
                  </span>
                  {activeRequest.priority && (
                    <span className="badge badge-emergency" style={{ fontSize: '0.75rem' }}>
                      PRIORITY: {activeRequest.priority}
                    </span>
                  )}
                </div>
                <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', marginTop: '2px' }}>
                  Category: <strong>{activeRequest.category || 'General'}</strong> • Symptoms: {activeRequest.description || 'Emergency care requested'}
                </div>
              </div>
            </div>

            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={fetchEmergencyRequests}
            >
              <RefreshCw size={14} /> Update Status
            </button>
          </div>

          {/* 4-Step Patient Clinical Progression Timeline */}
          <div style={{ background: 'white', border: '1px solid var(--slate-200)', borderRadius: 'var(--radius-lg)', padding: '20px' }}>
            <div style={{ fontSize: '0.8125rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em', color: 'var(--slate-500)', marginBottom: '16px' }}>
              Real-Time Clinical Progression Timeline
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '16px' }}>
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
                  {activeRequest.requestTime ? new Date(activeRequest.requestTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Logged'}
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
                      background: (activeRequest.status === 'ASSIGNED' || activeRequest.status === 'IN_PROGRESS' || activeRequest.status === 'COMPLETED') ? '#10b981' : '#f59e0b',
                      color: 'white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 700,
                      fontSize: '0.8125rem',
                    }}
                  >
                    {(activeRequest.status === 'ASSIGNED' || activeRequest.status === 'IN_PROGRESS' || activeRequest.status === 'COMPLETED') ? '✓' : '●'}
                  </div>
                  <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--slate-900)' }}>
                    Doctor Assigned
                  </span>
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)', paddingLeft: '36px' }}>
                  {activeRequest.doctorName || (activeRequest.assignedDoctor ? activeRequest.assignedDoctor.fullName : 'Awaiting physician')}
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', paddingLeft: '36px' }}>
                  {activeRequest.doctorSpecialization || (activeRequest.assignedDoctor ? activeRequest.assignedDoctor.specialization : 'Specialist matching')}
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
                      background: (activeRequest.status === 'IN_PROGRESS' || activeRequest.status === 'COMPLETED') ? '#10b981' : activeRequest.status === 'ASSIGNED' ? '#0284c7' : 'var(--slate-300)',
                      color: 'white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 700,
                      fontSize: '0.8125rem',
                    }}
                  >
                    {(activeRequest.status === 'IN_PROGRESS' || activeRequest.status === 'COMPLETED') ? '✓' : activeRequest.status === 'ASSIGNED' ? '●' : '○'}
                  </div>
                  <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--slate-900)' }}>
                    Consultation Started
                  </span>
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)', paddingLeft: '36px' }}>
                  {activeRequest.status === 'IN_PROGRESS' ? 'Active clinical review' : activeRequest.status === 'COMPLETED' ? 'Concluded' : 'Awaiting doctor connect'}
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
                      background: activeRequest.status === 'COMPLETED' ? '#10b981' : 'var(--slate-300)',
                      color: 'white',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: 700,
                      fontSize: '0.8125rem',
                    }}
                  >
                    {activeRequest.status === 'COMPLETED' ? '✓' : '○'}
                  </div>
                  <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--slate-900)' }}>
                    Emergency Completed
                  </span>
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)', paddingLeft: '36px' }}>
                  {activeRequest.status === 'COMPLETED' ? 'Treatment delivered' : 'Under active care'}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* EMERGENCY BOOKING FORM (RIGHT AT THE START / TOP OF THE PAGE) */}
      <div className="card" style={{ marginBottom: '28px', borderTop: '5px solid #e11d48' }}>
        <div className="card-header" style={{ borderBottom: '1px solid var(--slate-100)', paddingBottom: '14px' }}>
          <div>
            <span className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '10px', color: '#be123c' }}>
              <HeartPulse size={22} /> Request Immediate Emergency Assistance
            </span>
            <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', marginTop: '2px' }}>
              Fill in your emergency symptoms below to instantly trigger the deterministic physician allocation algorithm.
            </div>
          </div>
        </div>

        {error && (
          <div style={{ background: '#fee2e2', color: '#991b1b', padding: '12px 16px', borderRadius: 'var(--radius-md)', fontSize: '0.875rem', margin: '16px 20px 0 20px', border: '1px solid #fecaca' }}>
            {error}
          </div>
        )}

        {successMessage && (
          <div style={{ background: '#dcfce7', color: '#166534', padding: '12px 16px', borderRadius: 'var(--radius-md)', fontSize: '0.875rem', margin: '16px 20px 0 20px', border: '1px solid #bbf7d0', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <CheckCircle2 size={18} /> {successMessage}
          </div>
        )}

        <form onSubmit={handleRequestEmergency} style={{ padding: '20px' }}>
          <div className="form-group" style={{ marginBottom: '20px' }}>
            <label className="form-label" style={{ fontWeight: 700, color: 'var(--slate-800)' }}>
              Emergency Category <span style={{ color: '#e11d48' }}>*</span>
            </label>
            <select
              className="form-control"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              disabled={submitting}
              style={{ fontSize: '0.9375rem', padding: '10px 14px' }}
            >
              <option value="General">General Emergency (Acute illness, high fever, severe abdominal pain)</option>
              <option value="Cardiology">Cardiology (Severe chest pain, palpitations, shortness of breath)</option>
              <option value="Dermatology">Dermatology (Severe acute allergic reaction, spreading rashes)</option>
              <option value="Neurology">Neurology (Sudden dizziness, severe migraine, numbness)</option>
              <option value="Orthopedics">Orthopedics (Severe injury, suspected fracture, acute dislocation)</option>
              <option value="Pediatrics">Pediatrics (Child sudden high fever, acute distress)</option>
              <option value="Other">Other Urgent Medical Condition</option>
            </select>
            <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', marginTop: '4px' }}>
              The allocation algorithm automatically prioritizes on-duty specialists matching this category.
            </div>
          </div>

          <div className="form-group" style={{ marginBottom: '20px' }}>
            <label className="form-label" style={{ fontWeight: 700, color: 'var(--slate-800)' }}>
              Priority Level <span style={{ color: '#e11d48' }}>*</span>
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
              <label
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  padding: '12px 16px',
                  borderRadius: 'var(--radius-md)',
                  border: priority === 'NORMAL' ? '2px solid #0284c7' : '1px solid var(--slate-200)',
                  background: priority === 'NORMAL' ? '#f0f9ff' : 'white',
                  cursor: 'pointer',
                  fontWeight: 600,
                  fontSize: '0.875rem',
                }}
              >
                <input
                  type="radio"
                  name="priority"
                  value="NORMAL"
                  checked={priority === 'NORMAL'}
                  onChange={() => setPriority('NORMAL')}
                />
                <span>Normal (Standard Urgent)</span>
              </label>

              <label
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px',
                  padding: '12px 16px',
                  borderRadius: 'var(--radius-md)',
                  border: priority === 'URGENT' ? '2px solid #e11d48' : '1px solid var(--slate-200)',
                  background: priority === 'URGENT' ? '#fff1f2' : 'white',
                  cursor: 'pointer',
                  fontWeight: 600,
                  fontSize: '0.875rem',
                  color: priority === 'URGENT' ? '#be123c' : 'inherit',
                }}
              >
                <input
                  type="radio"
                  name="priority"
                  value="URGENT"
                  checked={priority === 'URGENT'}
                  onChange={() => setPriority('URGENT')}
                />
                <span>Urgent (High Priority Queue)</span>
              </label>
            </div>
            <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', marginTop: '4px' }}>
              Urgent cases are placed at the front of the queue when physicians become available.
            </div>
          </div>

          <div className="form-group" style={{ marginBottom: '24px' }}>
            <label className="form-label" style={{ fontWeight: 700, color: 'var(--slate-800)' }}>
              Brief Description of Symptoms
            </label>
            <textarea
              className="form-control"
              rows={4}
              placeholder="Describe current symptoms or emergency situation (e.g. sharp radiating chest pain for 30 minutes, dizziness, high fever with chills)..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={submitting}
              style={{ fontSize: '0.9375rem', lineHeight: 1.5 }}
            />
          </div>

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
            <button
              type="submit"
              className="btn btn-emergency"
              disabled={submitting}
              style={{ padding: '12px 28px', fontSize: '1rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '8px' }}
            >
              <HeartPulse size={20} />
              {submitting ? 'Dispatching Emergency Request...' : 'Request Emergency Assistance'}
            </button>
          </div>
        </form>
      </div>

      {/* Emergency Requests History Table */}
      <div className="card">
        <div className="card-header">
          <span className="card-title" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <Clock size={18} className="text-primary" /> Emergency Assistance History ({emergencyRequests.length})
          </span>
        </div>

        {emergencyRequests.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '36px', color: 'var(--slate-500)', fontSize: '0.875rem' }}>
            No prior emergency requests logged.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Request ID</th>
                  <th>Date & Time</th>
                  <th>Category</th>
                  <th>Priority</th>
                  <th>Assigned Physician</th>
                  <th>Symptoms / Reason</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {emergencyRequests.map((req) => (
                  <tr key={req.id}>
                    <td>
                      <strong style={{ color: 'var(--slate-900)' }}>#{req.id}</strong>
                    </td>
                    <td style={{ fontSize: '0.8125rem', color: 'var(--slate-600)' }}>
                      {new Date(req.requestTime).toLocaleDateString()} at {new Date(req.requestTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </td>
                    <td>
                      <span className="badge badge-emergency" style={{ fontSize: '0.75rem' }}>
                        {req.category || 'General'}
                      </span>
                    </td>
                    <td>
                      <span style={{ fontSize: '0.75rem', fontWeight: 700, color: req.priority === 'URGENT' ? '#be123c' : 'var(--slate-600)' }}>
                        {req.priority || 'NORMAL'}
                      </span>
                    </td>
                    <td>
                      {req.doctorName || (req.assignedDoctor ? req.assignedDoctor.fullName : null) ? (
                        <div>
                          <div style={{ fontWeight: 600, color: 'var(--slate-900)', fontSize: '0.875rem' }}>
                            {req.doctorName || req.assignedDoctor.fullName}
                          </div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--primary-700)' }}>
                            {req.doctorSpecialization || req.assignedDoctor.specialization}
                          </div>
                        </div>
                      ) : (
                        <span style={{ color: 'var(--slate-400)', fontStyle: 'italic', fontSize: '0.8125rem' }}>
                          Awaiting Physician
                        </span>
                      )}
                    </td>
                    <td style={{ maxWidth: '220px', fontSize: '0.8125rem', color: 'var(--slate-600)' }}>
                      {req.description || 'Emergency triage requested'}
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
    </div>
  );
};

export default PatientEmergencyPage;
