import React, { useState, useEffect } from 'react';
import { FileText, Pill, PlusCircle, Calendar, User, CheckCircle2, AlertCircle } from 'lucide-react';
import api, { recordService, prescriptionService } from '../../services/api';
import Modal from '../../components/Modal';

const DoctorRecords = () => {
  const [records, setRecords] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('RECORDS'); // 'RECORDS' | 'PRESCRIPTIONS'

  // Record Modal
  const [isRecordModalOpen, setIsRecordModalOpen] = useState(false);
  const [recordData, setRecordData] = useState({
    patientId: '',
    recordDate: new Date().toISOString().split('T')[0],
    diagnosis: '',
    symptoms: '',
    treatment: '',
    consultationNotes: '',
  });

  // Prescription Modal
  const [isPrescriptionModalOpen, setIsPrescriptionModalOpen] = useState(false);
  const [presData, setPresData] = useState({
    patientId: '',
    medicineName: '',
    dosage: '500mg',
    frequency: 'Twice daily after meals',
    duration: '14 days',
    instructions: '',
  });

  const [formLoading, setFormLoading] = useState(false);
  const [formSuccess, setFormSuccess] = useState('');
  const [formError, setFormError] = useState('');

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [recRes, presRes, patRes] = await Promise.all([
        recordService.getRecordsByDoctor(),
        prescriptionService.getPrescriptionsByDoctor(),
        api.get('/patients'),
      ]);
      setRecords(recRes.data || []);
      setPrescriptions(presRes.data || []);
      setPatients(patRes.data || []);
      if (patRes.data?.length > 0) {
        setRecordData((prev) => ({ ...prev, patientId: patRes.data[0].id }));
        setPresData((prev) => ({ ...prev, patientId: patRes.data[0].id }));
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleRecordSubmit = async (e) => {
    e.preventDefault();
    setFormLoading(true);
    setFormError('');
    setFormSuccess('');

    try {
      await recordService.createRecord(recordData);
      setFormSuccess('Clinical record saved and patient notified.');
      fetchData();
      setTimeout(() => {
        setIsRecordModalOpen(false);
        setFormSuccess('');
      }, 1500);
    } catch (err) {
      setFormError(err.response?.data?.message || 'Failed to save medical record');
    } finally {
      setFormLoading(false);
    }
  };

  const handlePrescriptionSubmit = async (e) => {
    e.preventDefault();
    setFormLoading(true);
    setFormError('');
    setFormSuccess('');

    try {
      await prescriptionService.createPrescription(presData);
      setFormSuccess('Prescription issued and patient notified.');
      fetchData();
      setTimeout(() => {
        setIsPrescriptionModalOpen(false);
        setFormSuccess('');
      }, 1500);
    } catch (err) {
      setFormError(err.response?.data?.message || 'Failed to issue prescription');
    } finally {
      setFormLoading(false);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px', flexWrap: 'wrap', gap: '16px' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Consultation Records & Prescriptions</h2>
          <p style={{ color: 'var(--slate-600)' }}>
            Document medical diagnoses, write digital treatment plans, and author structured prescriptions.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => {
              setFormError('');
              setFormSuccess('');
              setIsPrescriptionModalOpen(true);
            }}
          >
            <Pill size={16} /> New Prescription
          </button>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              setFormError('');
              setFormSuccess('');
              setIsRecordModalOpen(true);
            }}
          >
            <PlusCircle size={16} /> New Medical Record
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div
        style={{
          display: 'flex',
          gap: '8px',
          borderBottom: '1px solid var(--slate-200)',
          paddingBottom: '12px',
          marginBottom: '24px',
        }}
      >
        <button
          className={`btn btn-sm ${activeTab === 'RECORDS' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('RECORDS')}
        >
          <FileText size={14} /> Medical Records ({records.length})
        </button>
        <button
          className={`btn btn-sm ${activeTab === 'PRESCRIPTIONS' ? 'btn-primary' : 'btn-secondary'}`}
          onClick={() => setActiveTab('PRESCRIPTIONS')}
        >
          <Pill size={14} /> Prescriptions Issued ({prescriptions.length})
        </button>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-500)' }}>
          Loading records...
        </div>
      ) : activeTab === 'RECORDS' ? (
        records.length === 0 ? (
          <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
            <FileText size={40} style={{ color: 'var(--slate-400)', margin: '0 auto 12px' }} />
            <p style={{ color: 'var(--slate-600)' }}>No medical records logged yet.</p>
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            {records.map((r) => (
              <div key={r.id} className="card" style={{ padding: '20px' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '10px' }}>
                  <div>
                    <h3 style={{ fontSize: '1.2rem', color: 'var(--slate-900)' }}>{r.diagnosis}</h3>
                    <div style={{ fontSize: '0.875rem', color: 'var(--primary-700)', fontWeight: 600 }}>
                      Patient: {r.patient.fullName} (Blood: {r.patient.bloodGroup || 'N/A'})
                    </div>
                  </div>
                  <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)' }}>
                    Date: {r.recordDate}
                  </div>
                </div>

                <div style={{ background: 'var(--slate-50)', padding: '12px', borderRadius: '6px', marginBottom: '10px', fontSize: '0.875rem' }}>
                  <strong>Symptoms:</strong> {r.symptoms || 'None recorded'}
                </div>
                <div style={{ background: 'var(--slate-50)', padding: '12px', borderRadius: '6px', marginBottom: '10px', fontSize: '0.875rem' }}>
                  <strong>Treatment Plan:</strong> {r.treatment}
                </div>
                {r.consultationNotes && (
                  <div style={{ fontSize: '0.875rem', color: 'var(--slate-600)' }}>
                    <strong>Clinical Notes:</strong> {r.consultationNotes}
                  </div>
                )}
              </div>
            ))}
          </div>
        )
      ) : prescriptions.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
          <Pill size={40} style={{ color: 'var(--slate-400)', margin: '0 auto 12px' }} />
          <p style={{ color: 'var(--slate-600)' }}>No prescriptions issued yet.</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '18px' }}>
          {prescriptions.map((p) => (
            <div key={p.id} className="prescription-card">
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
                <h3 style={{ fontSize: '1.15rem', color: 'var(--slate-900)' }}>{p.medicineName}</h3>
                <span style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>{p.issuedDate}</span>
              </div>
              <div style={{ fontWeight: 600, color: 'var(--primary-700)', fontSize: '0.875rem', marginBottom: '8px' }}>
                Patient: {p.patient?.fullName}
              </div>
              <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', marginBottom: '6px' }}>
                Dosage: <strong>{p.dosage}</strong> • Frequency: <strong>{p.frequency}</strong>
              </div>
              <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', marginBottom: '8px' }}>
                Duration: <strong>{p.duration}</strong>
              </div>
              {p.instructions && (
                <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', borderTop: '1px solid var(--slate-100)', paddingTop: '6px' }}>
                  Instructions: {p.instructions}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* New Medical Record Modal */}
      <Modal
        isOpen={isRecordModalOpen}
        onClose={() => setIsRecordModalOpen(false)}
        title="Create Medical Record"
        maxWidth="580px"
      >
        {formSuccess ? (
          <div style={{ textAlign: 'center', padding: '24px 0' }}>
            <CheckCircle2 size={44} style={{ color: 'var(--teal-600)', margin: '0 auto 12px' }} />
            <p style={{ fontWeight: 600 }}>{formSuccess}</p>
          </div>
        ) : (
          <form onSubmit={handleRecordSubmit}>
            {formError && (
              <div style={{ padding: '10px', background: 'var(--danger-bg)', color: 'var(--danger-text)', borderRadius: '6px', marginBottom: '14px', fontSize: '0.875rem' }}>
                {formError}
              </div>
            )}

            <div className="form-group">
              <label className="form-label" htmlFor="medrec-patient-select">Select Patient *</label>
              <select
                id="medrec-patient-select"
                className="form-control"
                value={recordData.patientId}
                onChange={(e) => setRecordData({ ...recordData, patientId: e.target.value })}
                required
              >
                {patients.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.fullName} (DOB: {p.dateOfBirth || 'N/A'})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="medrec-date-input">Record Date *</label>
              <input
                id="medrec-date-input"
                type="date"
                className="form-control"
                value={recordData.recordDate}
                onChange={(e) => setRecordData({ ...recordData, recordDate: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="medrec-diag-input">Primary Clinical Diagnosis *</label>
              <input
                id="medrec-diag-input"
                type="text"
                className="form-control"
                placeholder="e.g. Acute Bronchitis, Essential Hypertension"
                value={recordData.diagnosis}
                onChange={(e) => setRecordData({ ...recordData, diagnosis: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="medrec-sym-input">Symptoms</label>
              <textarea
                id="medrec-sym-input"
                className="form-control"
                rows="2"
                placeholder="Patient presented with cough, low-grade fever..."
                value={recordData.symptoms}
                onChange={(e) => setRecordData({ ...recordData, symptoms: e.target.value })}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="medrec-treat-input">Prescribed Treatment / Therapy *</label>
              <textarea
                id="medrec-treat-input"
                className="form-control"
                rows="2"
                placeholder="Medication therapy, dietary recommendations, rest..."
                value={recordData.treatment}
                onChange={(e) => setRecordData({ ...recordData, treatment: e.target.value })}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="medrec-notes-input">Consultation Notes (Optional)</label>
              <textarea
                id="medrec-notes-input"
                className="form-control"
                rows="2"
                placeholder="Follow-up planned in 2 weeks..."
                value={recordData.consultationNotes}
                onChange={(e) => setRecordData({ ...recordData, consultationNotes: e.target.value })}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsRecordModalOpen(false)}
              >
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={formLoading}>
                {formLoading ? 'Saving...' : 'Save Medical Record'}
              </button>
            </div>
          </form>
        )}
      </Modal>

      {/* New Prescription Modal */}
      <Modal
        isOpen={isPrescriptionModalOpen}
        onClose={() => setIsPrescriptionModalOpen(false)}
        title="Issue Prescription"
        maxWidth="540px"
      >
        {formSuccess ? (
          <div style={{ textAlign: 'center', padding: '24px 0' }}>
            <CheckCircle2 size={44} style={{ color: 'var(--teal-600)', margin: '0 auto 12px' }} />
            <p style={{ fontWeight: 600 }}>{formSuccess}</p>
          </div>
        ) : (
          <form onSubmit={handlePrescriptionSubmit}>
            {formError && (
              <div style={{ padding: '10px', background: 'var(--danger-bg)', color: 'var(--danger-text)', borderRadius: '6px', marginBottom: '14px', fontSize: '0.875rem' }}>
                {formError}
              </div>
            )}

            <div className="form-group">
              <label className="form-label" htmlFor="pres-patient-select">Patient *</label>
              <select
                id="pres-patient-select"
                className="form-control"
                value={presData.patientId}
                onChange={(e) => setPresData({ ...presData, patientId: e.target.value })}
                required
              >
                {patients.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.fullName}
                  </option>
                ))}
              </select>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: '14px' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="pres-med-name-input">Medicine Name *</label>
                <input
                  id="pres-med-name-input"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Amoxicillin"
                  value={presData.medicineName}
                  onChange={(e) => setPresData({ ...presData, medicineName: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="pres-dosage-input">Dosage *</label>
                <input
                  id="pres-dosage-input"
                  type="text"
                  className="form-control"
                  placeholder="e.g. 500mg, 10ml"
                  value={presData.dosage}
                  onChange={(e) => setPresData({ ...presData, dosage: e.target.value })}
                  required
                />
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '14px' }}>
              <div className="form-group">
                <label className="form-label" htmlFor="pres-freq-input">Frequency *</label>
                <input
                  id="pres-freq-input"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Once daily after breakfast"
                  value={presData.frequency}
                  onChange={(e) => setPresData({ ...presData, frequency: e.target.value })}
                  required
                />
              </div>
              <div className="form-group">
                <label className="form-label" htmlFor="pres-dur-input">Duration *</label>
                <input
                  id="pres-dur-input"
                  type="text"
                  className="form-control"
                  placeholder="e.g. 7 days, 30 days"
                  value={presData.duration}
                  onChange={(e) => setPresData({ ...presData, duration: e.target.value })}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="pres-notes-input">Instructions / Warnings</label>
              <textarea
                id="pres-notes-input"
                className="form-control"
                rows="2"
                placeholder="Take with a full glass of water. Do not skip doses."
                value={presData.instructions}
                onChange={(e) => setPresData({ ...presData, instructions: e.target.value })}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsPrescriptionModalOpen(false)}
              >
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={formLoading}>
                {formLoading ? 'Issuing...' : 'Issue Prescription'}
              </button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
};

export default DoctorRecords;
