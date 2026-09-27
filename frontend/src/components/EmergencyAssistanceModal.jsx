import React, { useState } from 'react';
import {
  AlertTriangle,
  Siren,
  CheckCircle2,
  X,
  Stethoscope,
  Phone,
  Clock,
  ShieldAlert,
  Loader2,
  HeartPulse,
} from 'lucide-react';
import { emergencyService } from '../services/api';

const EmergencyAssistanceModal = ({ isOpen, onClose, onSuccess }) => {
  const [category, setCategory] = useState('General');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [allocatedResult, setAllocatedResult] = useState(null);

  if (!isOpen) return null;

  const handleRequestEmergency = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const res = await emergencyService.createEmergencyRequest({
        category,
        description: description.trim(),
      });
      setAllocatedResult(res.data);
      if (onSuccess) onSuccess(res.data);
    } catch (err) {
      console.error('Emergency request failed:', err);
      setError(err.response?.data?.message || 'Failed to dispatch emergency request. Please seek immediate local emergency care.');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setAllocatedResult(null);
    setCategory('General');
    setDescription('');
    setError(null);
    onClose();
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ maxWidth: '580px', borderTop: '5px solid #e11d48' }}>
        <div className="modal-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div
              style={{
                width: '38px',
                height: '38px',
                borderRadius: '50%',
                background: '#ffe4e6',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#e11d48',
              }}
            >
              <Siren size={22} />
            </div>
            <div>
              <h3 style={{ fontSize: '1.25rem', color: '#be123c', fontWeight: 800 }}>
                Emergency Medical Assistance
              </h3>
              <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', fontWeight: 600 }}>
                RAPID TRIAGE & REAL-TIME ALLOCATION
              </div>
            </div>
          </div>
          <button className="modal-close-btn" onClick={handleClose}>
            <X size={20} />
          </button>
        </div>

        {/* Life-threatening Warning Notice */}
        <div
          style={{
            background: '#fff1f2',
            border: '1px solid #fecdd3',
            borderRadius: 'var(--radius-md)',
            padding: '12px 16px',
            marginBottom: '20px',
            display: 'flex',
            alignItems: 'flex-start',
            gap: '12px',
          }}
        >
          <ShieldAlert size={20} style={{ color: '#e11d48', flexShrink: 0, marginTop: '2px' }} />
          <div style={{ fontSize: '0.8125rem', color: '#9f1239', lineHeight: 1.45 }}>
            <strong>CRITICAL NOTICE:</strong> Emergency requests are for situations requiring immediate medical attention. If this is a life-threatening emergency, <strong>contact your local emergency service (911 / 112 / 108) immediately</strong>.
          </div>
        </div>

        {/* State 1: Allocated Success or No Doctor Available */}
        {allocatedResult ? (
          <div>
            {allocatedResult.status === 'ASSIGNED' ? (
              <div
                style={{
                  background: '#f0fdf4',
                  border: '1px solid #bbf7d0',
                  borderRadius: 'var(--radius-lg)',
                  padding: '20px',
                  marginBottom: '20px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#15803d', fontWeight: 700, marginBottom: '12px' }}>
                  <CheckCircle2 size={20} />
                  <span>Emergency Physician Successfully Allocated</span>
                </div>

                <div
                  style={{
                    background: 'white',
                    border: '1px solid #dcfce7',
                    borderRadius: 'var(--radius-md)',
                    padding: '16px',
                    marginBottom: '14px',
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '8px' }}>
                    <div>
                      <h4 style={{ fontSize: '1.15rem', color: 'var(--slate-900)' }}>
                        {allocatedResult.doctorName}
                      </h4>
                      <div style={{ color: 'var(--primary-700)', fontWeight: 600, fontSize: '0.875rem' }}>
                        {allocatedResult.doctorSpecialization} • {allocatedResult.doctorQualification}
                      </div>
                    </div>
                    <span className="badge badge-assigned">
                      {allocatedResult.status}
                    </span>
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', fontSize: '0.8125rem', color: 'var(--slate-700)', marginTop: '12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Phone size={14} style={{ color: '#16a34a' }} />
                      <span>{allocatedResult.doctorPhone || 'Hospital Direct Line'}</span>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <Clock size={14} style={{ color: 'var(--primary-600)' }} />
                      <span>{new Date(allocatedResult.assignedTime || allocatedResult.requestTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    </div>
                  </div>
                </div>

                <p style={{ fontSize: '0.8125rem', color: '#166534', margin: 0 }}>
                  Dr. {allocatedResult.doctorName} has been immediately alerted to your emergency case (#{allocatedResult.id}). Please keep your phone reachable.
                </p>
              </div>
            ) : (
              <div
                style={{
                  background: '#fef2f2',
                  border: '1px solid #fecaca',
                  borderRadius: 'var(--radius-lg)',
                  padding: '20px',
                  marginBottom: '20px',
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#b91c1c', fontWeight: 700, marginBottom: '8px' }}>
                  <AlertTriangle size={20} />
                  <span>No Emergency-Duty Doctor is Currently Available</span>
                </div>
                <p style={{ fontSize: '0.875rem', color: '#991b1b', marginBottom: '14px', lineHeight: 1.5 }}>
                  All scheduled emergency doctors for today are currently in active consultation or off-duty.
                </p>
                <div style={{ background: 'white', border: '1px solid #fee2e2', borderRadius: 'var(--radius-md)', padding: '14px', fontSize: '0.8125rem', color: '#7f1d1d' }}>
                  <strong>Next Action:</strong> Please call your local emergency medical hotline immediately or visit the nearest hospital emergency department.
                </div>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px' }}>
              <button type="button" className="btn btn-secondary" onClick={handleClose}>
                Close
              </button>
            </div>
          </div>
        ) : (
          /* State 2: Request Submission Form */
          <form onSubmit={handleRequestEmergency}>
            {error && (
              <div style={{ background: '#fee2e2', color: '#991b1b', padding: '10px 14px', borderRadius: 'var(--radius-md)', fontSize: '0.8125rem', marginBottom: '16px' }}>
                {error}
              </div>
            )}

            <div className="form-group">
              <label className="form-label">
                Emergency Category <span style={{ color: '#e11d48' }}>*</span>
              </label>
              <select
                className="form-control"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                disabled={loading}
              >
                <option value="General">General Emergency (Acute illness, high fever, abdominal pain)</option>
                <option value="Cardiology">Cardiology (Severe chest pain, palpitations, shortness of breath)</option>
                <option value="Dermatology">Dermatology (Severe acute allergic reaction, acute rashes)</option>
                <option value="Neurology">Neurology (Sudden dizziness, severe migraine, numbness)</option>
                <option value="Orthopedics">Orthopedics (Severe injury, suspected fracture, acute dislocation)</option>
                <option value="Pediatrics">Pediatrics (Child sudden high fever, acute distress)</option>
                <option value="Other">Other Urgent Condition</option>
              </select>
              <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', marginTop: '4px' }}>
                The allocation algorithm automatically prioritizes on-duty specialists matching this category.
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Brief Description of Symptoms</label>
              <textarea
                className="form-control"
                rows={3}
                placeholder="Describe current symptoms or emergency situation (e.g. sharp radiating chest pain for 30 minutes, dizziness)..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                disabled={loading}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '24px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={handleClose}
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-emergency"
                disabled={loading}
              >
                {loading ? (
                  <>
                    <Loader2 size={16} className="spinner" />
                    Allocating On-Duty Physician...
                  </>
                ) : (
                  <>
                    <HeartPulse size={18} />
                    Request Emergency Assistance
                  </>
                )}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
};

export default EmergencyAssistanceModal;
