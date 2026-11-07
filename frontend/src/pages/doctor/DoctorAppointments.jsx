import React, { useState, useEffect } from 'react';
import {
  Calendar,
  Clock,
  CheckCircle2,
  XCircle,
  FileText,
  AlertCircle,
  Check,
  X,
  PlusCircle,
  Pill,
  History,
  Stethoscope,
  Activity,
} from 'lucide-react';
import { appointmentService, recordService, prescriptionService } from '../../services/api';
import Modal from '../../components/Modal';

const DoctorAppointments = () => {
  const [appointments, setAppointments] = useState([]);
  const [activeTab, setActiveTab] = useState('ALL');
  const [loading, setLoading] = useState(true);

  // Status Action Modal
  const [selectedAppt, setSelectedAppt] = useState(null);
  const [modalMode, setModalMode] = useState(null); // 'COMPLETE' | 'CANCEL'
  const [consultationNotes, setConsultationNotes] = useState('');
  const [cancellationReason, setCancellationReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  // Phase 4: Clinical History Modal for Pre-Consultation Review
  const [historyModalOpen, setHistoryModalOpen] = useState(false);
  const [historyPatient, setHistoryPatient] = useState(null);
  const [patientRecords, setPatientRecords] = useState([]);
  const [patientPrescriptions, setPatientPrescriptions] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  // Phase 4: Clinical Documentation Fields for Consultation Completion
  const [diagnosis, setDiagnosis] = useState('');
  const [symptoms, setSymptoms] = useState('');
  const [treatment, setTreatment] = useState('');
  const [includePrescription, setIncludePrescription] = useState(false);
  const [medicineName, setMedicineName] = useState('');
  const [dosage, setDosage] = useState('500mg');
  const [frequency, setFrequency] = useState('Twice daily after meals');
  const [duration, setDuration] = useState('7 days');
  const [instructions, setInstructions] = useState('');

  useEffect(() => {
    fetchAppointments();
  }, []);

  const fetchAppointments = async () => {
    setLoading(true);
    try {
      const res = await appointmentService.getMyAppointments();
      setAppointments(res.data || []);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const openPatientHistory = async (patient) => {
    setHistoryPatient(patient);
    setHistoryLoading(true);
    setHistoryModalOpen(true);
    try {
      const [recRes, presRes] = await Promise.all([
        recordService.getRecordsByPatientId(patient.id).catch(() => ({ data: [] })),
        prescriptionService.getPrescriptionsByPatientId(patient.id).catch(() => ({ data: [] })),
      ]);
      setPatientRecords(recRes.data || []);
      setPatientPrescriptions(presRes.data || []);
    } catch (err) {
      console.error('Failed to load patient history:', err);
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleConfirm = async (apptId) => {
    try {
      await appointmentService.updateStatus(apptId, { status: 'CONFIRMED' });
      fetchAppointments();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to confirm appointment');
    }
  };

  const openCompleteModal = (appt) => {
    setSelectedAppt(appt);
    setConsultationNotes(appt.consultationNotes || '');
    setDiagnosis(appt.reason || '');
    setSymptoms(appt.reason || '');
    setTreatment('');
    setIncludePrescription(false);
    setMedicineName('');
    setDosage('500mg');
    setFrequency('Twice daily after meals');
    setDuration('7 days');
    setInstructions('');
    setModalMode('COMPLETE');
  };

  const openCancelModal = (appt) => {
    setSelectedAppt(appt);
    setCancellationReason('');
    setModalMode('CANCEL');
  };

  const handleModalSubmit = async (e) => {
    e.preventDefault();
    if (!selectedAppt) return;
    setActionLoading(true);

    try {
      if (modalMode === 'COMPLETE') {
        const payload = {
          status: 'COMPLETED',
          consultationNotes: consultationNotes,
        };

        if (diagnosis && diagnosis.trim()) {
          payload.diagnosis = diagnosis.trim();
          payload.symptoms = symptoms;
          payload.treatment = treatment || 'Standard care regimen advised';
        }

        if (includePrescription && medicineName && medicineName.trim()) {
          payload.medicineName = medicineName.trim();
          payload.dosage = dosage;
          payload.frequency = frequency;
          payload.duration = duration;
          payload.instructions = instructions;
        }

        await appointmentService.updateStatus(selectedAppt.id, payload);
      } else if (modalMode === 'CANCEL') {
        await appointmentService.updateStatus(selectedAppt.id, {
          status: 'CANCELLED',
          cancellationReason: cancellationReason || 'Cancelled by physician',
        });
      }
      setModalMode(null);
      fetchAppointments();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update appointment');
    } finally {
      setActionLoading(false);
    }
  };

  const filtered = appointments.filter((a) => {
    if (activeTab === 'ALL') return true;
    return a.status.toUpperCase() === activeTab;
  });

  return (
    <div>
      <div style={{ marginBottom: '24px' }}>
        <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Manage Clinical Appointments</h2>
        <p style={{ color: 'var(--slate-600)' }}>
          Review scheduled consultations, log clinical consultation notes, and confirm patient booking requests.
        </p>
      </div>

      {/* Status Filter Tabs */}
      <div
        style={{
          display: 'flex',
          gap: '8px',
          borderBottom: '1px solid var(--slate-200)',
          paddingBottom: '12px',
          marginBottom: '24px',
        }}
      >
        {['ALL', 'PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].map((tab) => (
          <button
            key={tab}
            className={`btn btn-sm ${activeTab === tab ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab(tab)}
          >
            {tab.charAt(0) + tab.slice(1).toLowerCase()}
          </button>
        ))}
      </div>

      {/* Appointment Table */}
      <div className="card">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '36px', color: 'var(--slate-500)' }}>
            Loading appointments...
          </div>
        ) : filtered.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px 16px', color: 'var(--slate-500)' }}>
            No appointments found in this category.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Patient</th>
                  <th>Date & Time</th>
                  <th>Reason / Symptoms</th>
                  <th>Status</th>
                  <th>Notes</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((appt) => (
                  <tr key={appt.id}>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--slate-900)' }}>
                        {appt.patient.fullName}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                        {appt.patient.gender} • {appt.patient.bloodGroup || 'Blood: N/A'}
                      </div>
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{appt.appointmentDate}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>{appt.appointmentTime}</div>
                    </td>
                    <td style={{ maxWidth: '220px', fontSize: '0.8125rem' }}>{appt.reason}</td>
                    <td>
                      <span className={`badge badge-${appt.status.toLowerCase()}`}>
                        {appt.status}
                      </span>
                    </td>
                    <td style={{ maxWidth: '200px', fontSize: '0.8125rem', color: 'var(--slate-600)' }}>
                      {appt.consultationNotes || appt.cancellationReason || '—'}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '6px' }}>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          onClick={() => openPatientHistory(appt.patient)}
                          title="Review Patient Past Medical History"
                        >
                          <FileText size={14} /> History
                        </button>

                        {appt.status === 'PENDING' && (
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            style={{ background: '#10b981' }}
                            onClick={() => handleConfirm(appt.id)}
                            title="Confirm Appointment"
                          >
                            <Check size={14} /> Accept
                          </button>
                        )}

                        {appt.status === 'CONFIRMED' && (
                          <button
                            type="button"
                            className="btn btn-primary btn-sm"
                            onClick={() => openCompleteModal(appt)}
                            title="Complete Consultation"
                          >
                            <CheckCircle2 size={14} /> Complete
                          </button>
                        )}

                        {(appt.status === 'PENDING' || appt.status === 'CONFIRMED') && (
                          <button
                            type="button"
                            className="btn btn-secondary btn-sm"
                            style={{ color: '#ef4444' }}
                            onClick={() => openCancelModal(appt)}
                            title="Cancel Appointment"
                          >
                            <X size={14} /> Cancel
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Complete or Cancel Modal */}
      <Modal
        isOpen={!!modalMode}
        onClose={() => setModalMode(null)}
        title={
          modalMode === 'COMPLETE'
            ? `Complete Consultation – ${selectedAppt?.patient.fullName}`
            : `Cancel Consultation – ${selectedAppt?.patient.fullName}`
        }
        maxWidth={modalMode === 'COMPLETE' ? '640px' : '500px'}
      >
        <form onSubmit={handleModalSubmit}>
          {modalMode === 'COMPLETE' ? (
            <div>
              <p style={{ fontSize: '0.875rem', color: 'var(--slate-600)', marginBottom: '14px' }}>
                Conclude the consultation, document examination notes, and optionally log an official diagnosis and prescription directly into the patient's medical history.
              </p>
              
              <div className="form-group">
                <label className="form-label" htmlFor="consult-notes-input">
                  Consultation Notes & Clinical Examination Remarks *
                </label>
                <textarea
                  id="consult-notes-input"
                  className="form-control"
                  rows="3"
                  placeholder="Record summary of physical examination, vital signs, recommendations..."
                  value={consultationNotes}
                  onChange={(e) => setConsultationNotes(e.target.value)}
                  required
                />
              </div>

              {/* Diagnosis and Treatment Protocol Section */}
              <div style={{ background: 'var(--slate-50)', padding: '14px', borderRadius: 'var(--radius-md)', border: '1px solid var(--slate-200)', marginTop: '14px', marginBottom: '14px' }}>
                <div style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--slate-800)', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Stethoscope size={16} className="text-primary" /> Record Official Diagnosis into Medical History
                </div>
                
                <div className="form-group" style={{ marginBottom: '10px' }}>
                  <label className="form-label" style={{ fontSize: '0.8125rem' }}>Primary Clinical Diagnosis</label>
                  <input
                    type="text"
                    className="form-control"
                    placeholder="e.g. Acute Bacterial Pharyngitis, Essential Hypertension"
                    value={diagnosis}
                    onChange={(e) => setDiagnosis(e.target.value)}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" style={{ fontSize: '0.8125rem' }}>Reported Symptoms</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="e.g. Sore throat, fever (101°F)"
                      value={symptoms}
                      onChange={(e) => setSymptoms(e.target.value)}
                    />
                  </div>
                  <div className="form-group" style={{ marginBottom: 0 }}>
                    <label className="form-label" style={{ fontSize: '0.8125rem' }}>Treatment Protocol</label>
                    <input
                      type="text"
                      className="form-control"
                      placeholder="e.g. Antibiotic course & rest"
                      value={treatment}
                      onChange={(e) => setTreatment(e.target.value)}
                    />
                  </div>
                </div>
              </div>

              {/* Optional Digital Prescription Section */}
              <div style={{ background: '#f0fdf4', padding: '14px', borderRadius: 'var(--radius-md)', border: '1px solid #bbf7d0', marginBottom: '14px' }}>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: includePrescription ? '12px' : 0 }}>
                  <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', margin: 0, fontWeight: 700, fontSize: '0.875rem', color: '#166534' }}>
                    <input
                      type="checkbox"
                      checked={includePrescription}
                      onChange={(e) => setIncludePrescription(e.target.checked)}
                      style={{ width: '16px', height: '16px' }}
                    />
                    <Pill size={16} /> Issue Pharmaceutical Prescription
                  </label>
                  <span style={{ fontSize: '0.75rem', color: '#15803d' }}>
                    {includePrescription ? 'Prescription will be published' : 'Optional'}
                  </span>
                </div>

                {includePrescription && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginTop: '10px' }}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1.4fr 1fr', gap: '10px' }}>
                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="form-label" style={{ fontSize: '0.75rem', color: '#166534' }}>Medicine Name *</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="e.g. Amoxicillin Clavulanate"
                          value={medicineName}
                          onChange={(e) => setMedicineName(e.target.value)}
                          required={includePrescription}
                        />
                      </div>
                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="form-label" style={{ fontSize: '0.75rem', color: '#166534' }}>Dosage *</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="e.g. 625mg"
                          value={dosage}
                          onChange={(e) => setDosage(e.target.value)}
                          required={includePrescription}
                        />
                      </div>
                    </div>

                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="form-label" style={{ fontSize: '0.75rem', color: '#166534' }}>Frequency *</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="e.g. Twice daily after meals"
                          value={frequency}
                          onChange={(e) => setFrequency(e.target.value)}
                          required={includePrescription}
                        />
                      </div>
                      <div className="form-group" style={{ marginBottom: 0 }}>
                        <label className="form-label" style={{ fontSize: '0.75rem', color: '#166534' }}>Duration *</label>
                        <input
                          type="text"
                          className="form-control"
                          placeholder="e.g. 5 days"
                          value={duration}
                          onChange={(e) => setDuration(e.target.value)}
                          required={includePrescription}
                        />
                      </div>
                    </div>

                    <div className="form-group" style={{ marginBottom: 0 }}>
                      <label className="form-label" style={{ fontSize: '0.75rem', color: '#166534' }}>Patient Administration Instructions</label>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="e.g. Take with plenty of water. Finish full course."
                        value={instructions}
                        onChange={(e) => setInstructions(e.target.value)}
                      />
                    </div>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div>
              <p style={{ fontSize: '0.875rem', color: 'var(--slate-600)', marginBottom: '14px' }}>
                Provide a reason for cancelling this consultation with {selectedAppt?.patient.fullName}.
              </p>
              <div className="form-group">
                <label className="form-label" htmlFor="cancel-reason-input">Reason for Cancellation</label>
                <input
                  id="cancel-reason-input"
                  type="text"
                  className="form-control"
                  placeholder="e.g. Emergency clinical duty, schedule conflict"
                  value={cancellationReason}
                  onChange={(e) => setCancellationReason(e.target.value)}
                  required
                />
              </div>
            </div>
          )}

          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setModalMode(null)}
            >
              Close
            </button>
            <button
              type="submit"
              className={`btn ${modalMode === 'COMPLETE' ? 'btn-primary' : 'btn-danger'}`}
              disabled={actionLoading}
            >
              {actionLoading ? 'Saving...' : modalMode === 'COMPLETE' ? 'Save & Conclude Consultation' : 'Confirm Cancellation'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Patient Past Medical History Modal (Phase 4) */}
      <Modal
        isOpen={historyModalOpen}
        onClose={() => setHistoryModalOpen(false)}
        title={`Medical History Review – ${historyPatient?.fullName || 'Patient'}`}
        maxWidth="720px"
      >
        {historyLoading ? (
          <div style={{ textAlign: 'center', padding: '36px', color: 'var(--slate-500)' }}>
            Loading patient medical records...
          </div>
        ) : (
          <div>
            {/* Patient Header Badges */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap', padding: '12px 16px', background: 'var(--slate-50)', borderRadius: 'var(--radius-md)', marginBottom: '18px' }}>
              <div style={{ fontWeight: 700, color: 'var(--slate-900)' }}>{historyPatient?.fullName}</div>
              <span className="badge badge-primary">{historyPatient?.gender || 'Gender: N/A'}</span>
              <span className="badge badge-teal">Blood: {historyPatient?.bloodGroup || 'N/A'}</span>
              <span style={{ fontSize: '0.8125rem', color: 'var(--slate-600)' }}>DOB: {historyPatient?.dateOfBirth || 'N/A'}</span>
              {historyPatient?.emergencyContact && (
                <span style={{ fontSize: '0.8125rem', color: 'var(--slate-600)' }}>Emergency: {historyPatient.emergencyContact}</span>
              )}
            </div>

            {/* Past Consultations */}
            <div style={{ marginBottom: '22px' }}>
              <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--slate-900)', display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
                <FileText size={16} className="text-primary" /> Past Consultation Records ({patientRecords.length})
              </h4>
              {patientRecords.length === 0 ? (
                <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', background: 'var(--slate-50)', padding: '12px', borderRadius: '6px' }}>
                  No prior consultation records logged for this patient.
                </div>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', maxHeight: '240px', overflowY: 'auto' }}>
                  {patientRecords.map((rec) => (
                    <div key={rec.id} style={{ background: 'white', border: '1px solid var(--slate-200)', borderRadius: '8px', padding: '12px 14px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '6px' }}>
                        <div>
                          <strong style={{ color: 'var(--slate-900)', fontSize: '0.9375rem' }}>{rec.diagnosis}</strong>
                          <div style={{ fontSize: '0.75rem', color: 'var(--primary-700)', fontWeight: 600 }}>
                            Dr. {rec.doctor?.fullName} ({rec.doctor?.specialization})
                          </div>
                        </div>
                        <span style={{ fontSize: '0.75rem', color: 'var(--slate-500)', fontWeight: 500 }}>
                          {rec.recordDate}
                        </span>
                      </div>
                      {rec.symptoms && (
                        <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', marginBottom: '4px' }}>
                          <strong>Symptoms:</strong> {rec.symptoms}
                        </div>
                      )}
                      {rec.treatment && (
                        <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', marginBottom: '4px' }}>
                          <strong>Treatment:</strong> {rec.treatment}
                        </div>
                      )}
                      {rec.consultationNotes && (
                        <div style={{ fontSize: '0.8125rem', color: 'var(--slate-700)', background: 'var(--slate-50)', padding: '6px 10px', borderRadius: '4px', marginTop: '6px' }}>
                          <strong>Notes:</strong> {rec.consultationNotes}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Prescriptions */}
            <div>
              <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--slate-900)', display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
                <Pill size={16} className="text-teal" /> Prescribed Medication Regimens ({patientPrescriptions.length})
              </h4>
              {patientPrescriptions.length === 0 ? (
                <div style={{ fontSize: '0.8125rem', color: 'var(--slate-500)', background: 'var(--slate-50)', padding: '12px', borderRadius: '6px' }}>
                  No active or past prescriptions recorded.
                </div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '10px', maxHeight: '200px', overflowY: 'auto' }}>
                  {patientPrescriptions.map((pres) => (
                    <div key={pres.id} style={{ background: '#f8fafc', border: '1px solid var(--slate-200)', borderRadius: '8px', padding: '10px 12px' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                        <span style={{ fontWeight: 700, fontSize: '0.875rem', color: 'var(--slate-900)' }}>{pres.medicineName}</span>
                        <span style={{ fontSize: '0.75rem', fontWeight: 700, color: 'var(--primary-700)', background: 'var(--primary-50)', padding: '1px 6px', borderRadius: '4px' }}>{pres.dosage}</span>
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-600)', marginBottom: '4px' }}>
                        {pres.frequency} • {pres.duration}
                      </div>
                      {pres.instructions && (
                        <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)', fontStyle: 'italic' }}>
                          {pres.instructions}
                        </div>
                      )}
                      <div style={{ fontSize: '0.7rem', color: 'var(--slate-400)', marginTop: '4px' }}>
                        Issued: {pres.issuedDate} by Dr. {pres.doctor?.fullName || 'Physician'}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '20px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setHistoryModalOpen(false)}
              >
                Close History Review
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default DoctorAppointments;
