import React, { useState, useEffect } from 'react';
import { FileText, Calendar, User, Stethoscope, Activity, Pill, ShieldCheck, Search, Printer } from 'lucide-react';
import { recordService } from '../../services/api';

const PatientRecords = () => {
  const [records, setRecords] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    const fetchRecords = async () => {
      try {
        const res = await recordService.getMyRecords();
        setRecords(res.data || []);
      } catch (err) {
        console.error('Failed to fetch records:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchRecords();
  }, []);

  const filtered = records.filter((rec) => {
    if (!search) return true;
    const term = search.toLowerCase();
    return (
      rec.diagnosis?.toLowerCase().includes(term) ||
      rec.doctor?.fullName?.toLowerCase().includes(term) ||
      rec.doctor?.specialization?.toLowerCase().includes(term) ||
      rec.symptoms?.toLowerCase().includes(term) ||
      rec.treatment?.toLowerCase().includes(term) ||
      rec.consultationNotes?.toLowerCase().includes(term)
    );
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', marginBottom: '8px' }}>Medical Records & Health Timeline</h2>
          <p style={{ color: 'var(--slate-600)' }}>
            Secure, comprehensive chronological repository of your clinical consultations, diagnostic evaluations, and therapeutic plans.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button type="button" className="btn btn-secondary" onClick={() => window.print()}>
            <Printer size={16} /> Print Official Records
          </button>
        </div>
      </div>

      {/* Security and Integrity Banner */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '10px', background: '#f8fafc', border: '1px solid var(--slate-200)', borderRadius: 'var(--radius-md)', padding: '10px 14px', marginBottom: '20px', fontSize: '0.8125rem', color: 'var(--slate-600)' }}>
        <ShieldCheck size={18} style={{ color: 'var(--primary-600)', flexShrink: 0 }} />
        <span>
          <strong>Tamper-Proof Healthcare Records:</strong> All diagnoses, prescriptions, and clinical notes are legally signed by licensed medical practitioners and cannot be altered by patients.
        </span>
      </div>

      {/* Search Input */}
      <div style={{ maxWidth: '380px', marginBottom: '20px' }}>
        <input
          type="text"
          className="form-control"
          placeholder="Search diagnoses, doctors, or symptoms..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-500)' }}>
          Loading your health timeline...
        </div>
      ) : filtered.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
          <FileText size={40} style={{ color: 'var(--slate-400)', margin: '0 auto 12px' }} />
          <h3 style={{ fontSize: '1.25rem', marginBottom: '6px' }}>No medical records found</h3>
          <p style={{ color: 'var(--slate-500)', fontSize: '0.9375rem' }}>
            {search ? 'No records match your search criteria.' : 'Medical records will automatically populate here after your doctor concludes a consultation.'}
          </p>
        </div>
      ) : (
        <div className="timeline">
          {filtered.map((rec) => (
            <div key={rec.id} className="timeline-item">
              <div className="timeline-dot" />
              <div className="timeline-card">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '10px', marginBottom: '12px' }}>
                  <div>
                    <div style={{ display: 'inline-block', background: 'var(--primary-100)', color: 'var(--primary-800)', fontSize: '0.75rem', fontWeight: 700, padding: '3px 8px', borderRadius: '4px', marginBottom: '6px' }}>
                      CONSULTATION RECORD #{rec.id}
                    </div>
                    <h3 style={{ fontSize: '1.25rem', color: 'var(--slate-900)' }}>{rec.diagnosis}</h3>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--slate-500)', fontSize: '0.875rem', fontWeight: 600 }}>
                    <Calendar size={16} />
                    <span>{rec.recordDate}</span>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px', color: 'var(--slate-700)', fontSize: '0.9375rem' }}>
                  <Stethoscope size={18} style={{ color: 'var(--primary-600)' }} />
                  <span>
                    <strong>Attending Physician:</strong> Dr. {rec.doctor.fullName} ({rec.doctor.specialization})
                  </span>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '16px', background: 'var(--slate-50)', padding: '16px', borderRadius: 'var(--radius-md)', marginBottom: '16px' }}>
                  <div>
                    <h4 style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', textTransform: 'uppercase', marginBottom: '4px' }}>
                      Reported Symptoms
                    </h4>
                    <p style={{ fontSize: '0.875rem', color: 'var(--slate-800)' }}>
                      {rec.symptoms || 'None specified'}
                    </p>
                  </div>
                  <div>
                    <h4 style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', textTransform: 'uppercase', marginBottom: '4px' }}>
                      Prescribed Treatment Protocol
                    </h4>
                    <p style={{ fontSize: '0.875rem', color: 'var(--slate-800)' }}>
                      {rec.treatment || 'Observation and monitoring'}
                    </p>
                  </div>
                </div>

                {rec.consultationNotes && (
                  <div style={{ fontSize: '0.875rem', color: 'var(--slate-600)', background: '#ffffff', border: '1px solid var(--slate-200)', padding: '12px 14px', borderRadius: '6px' }}>
                    <strong>Clinical Consultation Notes:</strong> {rec.consultationNotes}
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default PatientRecords;
