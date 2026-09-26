import React, { useState, useEffect } from 'react';
import { Pill, Calendar, Stethoscope, Clock, Printer, AlertCircle } from 'lucide-react';
import { prescriptionService } from '../../services/api';

const PatientPrescriptions = () => {
  const [prescriptions, setPrescriptions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchPrescriptions = async () => {
      try {
        const res = await prescriptionService.getMyPrescriptions();
        setPrescriptions(res.data || []);
      } catch (err) {
        console.error('Failed to load prescriptions:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchPrescriptions();
  }, []);

  const handlePrint = () => {
    window.print();
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '28px' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', marginBottom: '8px' }}>Active Prescriptions & Regimens</h2>
          <p style={{ color: 'var(--slate-600)' }}>
            Digital pharmaceutical instructions verified and authorized by your attending physicians.
          </p>
        </div>
        {prescriptions.length > 0 && (
          <button type="button" className="btn btn-secondary" onClick={handlePrint}>
            <Printer size={16} /> Print Prescription Cards
          </button>
        )}
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-500)' }}>
          Loading your prescriptions...
        </div>
      ) : prescriptions.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
          <Pill size={40} style={{ color: 'var(--slate-400)', margin: '0 auto 12px' }} />
          <h3 style={{ fontSize: '1.25rem', marginBottom: '6px' }}>No active prescriptions</h3>
          <p style={{ color: 'var(--slate-500)', fontSize: '0.9375rem' }}>
            When a physician prescribes medications during your consultation, they will appear here.
          </p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(360px, 1fr))', gap: '20px' }}>
          {prescriptions.map((pres) => (
            <div key={pres.id} className="prescription-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <div
                    style={{
                      width: '40px',
                      height: '40px',
                      borderRadius: '8px',
                      background: 'var(--teal-50)',
                      color: 'var(--teal-700)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                    }}
                  >
                    <Pill size={22} />
                  </div>
                  <div>
                    <h3 style={{ fontSize: '1.2rem', color: 'var(--slate-900)' }}>{pres.medicineName}</h3>
                    <span style={{ fontSize: '0.8125rem', fontWeight: 700, color: 'var(--primary-700)', background: 'var(--primary-50)', padding: '2px 8px', borderRadius: '4px' }}>
                      {pres.dosage}
                    </span>
                  </div>
                </div>

                <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <Calendar size={12} />
                  {pres.issuedDate}
                </div>
              </div>

              <div
                style={{
                  background: 'var(--slate-50)',
                  borderRadius: 'var(--radius-md)',
                  padding: '12px',
                  marginBottom: '14px',
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: '8px',
                  fontSize: '0.8125rem',
                }}
              >
                <div>
                  <span style={{ color: 'var(--slate-500)', display: 'block' }}>Frequency</span>
                  <strong style={{ color: 'var(--slate-800)' }}>{pres.frequency}</strong>
                </div>
                <div>
                  <span style={{ color: 'var(--slate-500)', display: 'block' }}>Duration</span>
                  <strong style={{ color: 'var(--slate-800)' }}>{pres.duration}</strong>
                </div>
              </div>

              {pres.instructions && (
                <div style={{ fontSize: '0.875rem', color: 'var(--slate-600)', marginBottom: '14px', lineHeight: 1.4 }}>
                  <strong>Instructions:</strong> {pres.instructions}
                </div>
              )}

              <div style={{ borderTop: '1px solid var(--slate-100)', paddingTop: '10px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.8125rem', color: 'var(--slate-500)' }}>
                <Stethoscope size={14} style={{ color: 'var(--teal-600)' }} />
                <span>Prescribed by {pres.doctor?.fullName || 'Physician'}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default PatientPrescriptions;
