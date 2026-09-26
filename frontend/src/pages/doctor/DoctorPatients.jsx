import React, { useState, useEffect } from 'react';
import { Users, FileText, Pill, Calendar, Search } from 'lucide-react';
import api, { recordService, prescriptionService } from '../../services/api';
import Modal from '../../components/Modal';

const DoctorPatients = () => {
  const [patients, setPatients] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  // Selected patient history modal
  const [selectedPatient, setSelectedPatient] = useState(null);
  const [patientRecords, setPatientRecords] = useState([]);
  const [patientPrescriptions, setPatientPrescriptions] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  useEffect(() => {
    fetchPatients();
  }, []);

  const fetchPatients = async () => {
    try {
      const res = await api.get('/patients');
      setPatients(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const openPatientHistory = async (patient) => {
    setSelectedPatient(patient);
    setHistoryLoading(true);
    try {
      const [recRes, presRes] = await Promise.all([
        recordService.getRecordsByPatientId(patient.id),
        prescriptionService.getPrescriptionsByPatientId(patient.id),
      ]);
      setPatientRecords(recRes.data || []);
      setPatientPrescriptions(presRes.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setHistoryLoading(false);
    }
  };

  const filtered = patients.filter((p) =>
    p.fullName.toLowerCase().includes(search.toLowerCase()) ||
    (p.phone && p.phone.includes(search))
  );

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Assigned Patient Roster</h2>
          <p style={{ color: 'var(--slate-600)' }}>
            Access clinical profiles, past records, and active medication regimens for your patients.
          </p>
        </div>
      </div>

      <div style={{ maxWidth: '400px', marginBottom: '20px' }}>
        <input
          type="text"
          className="form-control"
          placeholder="Filter by patient name or phone..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <div className="card">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '36px', color: 'var(--slate-500)' }}>
            Loading patient profiles...
          </div>
        ) : filtered.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '36px 16px', color: 'var(--slate-500)' }}>
            No patients found matching your search.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Patient Name</th>
                  <th>Gender & Blood Group</th>
                  <th>Contact Phone</th>
                  <th>Emergency Contact</th>
                  <th>Home Address</th>
                  <th style={{ textAlign: 'right' }}>Clinical History</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((patient) => (
                  <tr key={patient.id}>
                    <td>
                      <div style={{ fontWeight: 700, color: 'var(--slate-900)' }}>
                        {patient.fullName}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                        DOB: {patient.dateOfBirth || 'N/A'}
                      </div>
                    </td>
                    <td>
                      <div>{patient.gender || 'N/A'}</div>
                      <span
                        style={{
                          fontSize: '0.75rem',
                          fontWeight: 700,
                          background: 'var(--slate-100)',
                          padding: '2px 6px',
                          borderRadius: '4px',
                        }}
                      >
                        {patient.bloodGroup || 'O+'}
                      </span>
                    </td>
                    <td>{patient.phone || 'N/A'}</td>
                    <td style={{ fontSize: '0.8125rem' }}>{patient.emergencyContact || 'N/A'}</td>
                    <td style={{ fontSize: '0.8125rem', maxWidth: '200px' }}>{patient.address || 'N/A'}</td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        type="button"
                        className="btn btn-secondary btn-sm"
                        onClick={() => openPatientHistory(patient)}
                      >
                        <FileText size={14} /> Full History
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Patient History Modal */}
      <Modal
        isOpen={!!selectedPatient}
        onClose={() => setSelectedPatient(null)}
        title={`Clinical History – ${selectedPatient?.fullName || ''}`}
        maxWidth="680px"
      >
        {historyLoading ? (
          <div style={{ textAlign: 'center', padding: '30px' }}>Loading records...</div>
        ) : (
          <div>
            <h4 style={{ fontSize: '1rem', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <FileText size={18} className="text-primary" /> Past Consultation Records ({patientRecords.length})
            </h4>

            {patientRecords.length === 0 ? (
              <p style={{ fontSize: '0.875rem', color: 'var(--slate-500)', marginBottom: '20px' }}>
                No past medical records logged for this patient.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '24px', maxHeight: '240px', overflowY: 'auto' }}>
                {patientRecords.map((r) => (
                  <div key={r.id} style={{ background: 'var(--slate-50)', padding: '12px 14px', borderRadius: '8px', border: '1px solid var(--slate-200)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontWeight: 600, fontSize: '0.875rem' }}>
                      <span>{r.diagnosis}</span>
                      <span style={{ color: 'var(--slate-500)', fontSize: '0.75rem' }}>{r.recordDate}</span>
                    </div>
                    <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', marginTop: '4px' }}>
                      <strong>Treatment:</strong> {r.treatment}
                    </div>
                  </div>
                ))}
              </div>
            )}

            <h4 style={{ fontSize: '1rem', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Pill size={18} className="text-teal" /> Prescribed Medications ({patientPrescriptions.length})
            </h4>

            {patientPrescriptions.length === 0 ? (
              <p style={{ fontSize: '0.875rem', color: 'var(--slate-500)' }}>
                No active prescriptions found.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', maxHeight: '200px', overflowY: 'auto' }}>
                {patientPrescriptions.map((p) => (
                  <div key={p.id} style={{ background: 'var(--slate-50)', padding: '12px 14px', borderRadius: '8px', border: '1px solid var(--slate-200)' }}>
                    <div style={{ fontWeight: 600, fontSize: '0.875rem' }}>
                      {p.medicineName} ({p.dosage})
                    </div>
                    <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)' }}>
                      {p.frequency} • Duration: {p.duration}
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '24px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setSelectedPatient(null)}
              >
                Close
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default DoctorPatients;
