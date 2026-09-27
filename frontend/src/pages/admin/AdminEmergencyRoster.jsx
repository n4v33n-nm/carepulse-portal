import React, { useState, useEffect } from 'react';
import {
  Siren,
  Calendar,
  Clock,
  Plus,
  Trash2,
  Edit2,
  CheckCircle2,
  AlertTriangle,
  HeartPulse,
  UserCheck,
  Activity,
  Filter,
  RefreshCw,
  X,
  Stethoscope,
} from 'lucide-react';
import { emergencyService, doctorService } from '../../services/api';

const AdminEmergencyRoster = () => {
  const [selectedDate, setSelectedDate] = useState(new Date().toISOString().split('T')[0]);
  const [rosterList, setRosterList] = useState([]);
  const [emergencyStats, setEmergencyStats] = useState(null);
  const [emergencyRequests, setEmergencyRequests] = useState([]);
  const [doctors, setDoctors] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modal State for Adding Doctor
  const [showAddModal, setShowAddModal] = useState(false);
  const [selectedDoctorId, setSelectedDoctorId] = useState('');
  const [rosterDate, setRosterDate] = useState(new Date().toISOString().split('T')[0]);
  const [shiftName, setShiftName] = useState('MORNING');
  const [shiftStart, setShiftStart] = useState('08:00');
  const [shiftEnd, setShiftEnd] = useState('14:00');
  const [modalLoading, setModalLoading] = useState(false);
  const [modalError, setModalError] = useState(null);

  // Modal State for Editing Doctor Roster
  const [editingRoster, setEditingRoster] = useState(null);

  useEffect(() => {
    fetchRosterAndStats();
    fetchDoctors();
  }, [selectedDate]);

  const fetchRosterAndStats = async () => {
    setLoading(true);
    try {
      const [rosterRes, statsRes, reqsRes] = await Promise.all([
        emergencyService.getRoster({ date: selectedDate }),
        emergencyService.getEmergencyStats(),
        emergencyService.getAllEmergencyRequests(),
      ]);
      setRosterList(rosterRes.data || []);
      setEmergencyStats(statsRes.data || {});
      setEmergencyRequests(reqsRes.data || []);
    } catch (err) {
      console.error('Failed to load emergency roster data:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchDoctors = async () => {
    try {
      const res = await doctorService.getAllDoctors();
      setDoctors(res.data || []);
      if (res.data?.length > 0 && !selectedDoctorId) {
        setSelectedDoctorId(res.data[0].id);
      }
    } catch (err) {
      console.error('Failed to load doctors list:', err);
    }
  };

  const handleShiftPresetChange = (preset) => {
    setShiftName(preset);
    if (preset === 'MORNING') {
      setShiftStart('08:00');
      setShiftEnd('14:00');
    } else if (preset === 'EVENING') {
      setShiftStart('14:00');
      setShiftEnd('20:00');
    } else if (preset === 'NIGHT') {
      setShiftStart('20:00');
      setShiftEnd('08:00');
    }
  };

  const handleAddDoctor = async (e) => {
    e.preventDefault();
    setModalLoading(true);
    setModalError(null);

    try {
      await emergencyService.createRoster({
        doctorId: Number(selectedDoctorId),
        rosterDate,
        shiftName,
        shiftStart,
        shiftEnd,
        dutyStatus: 'EMERGENCY_DUTY',
        doctorAvailabilityStatus: 'AVAILABLE',
      });
      setShowAddModal(false);
      fetchRosterAndStats();
    } catch (err) {
      setModalError(err.response?.data?.message || 'Failed to assign doctor to emergency duty.');
    } finally {
      setModalLoading(false);
    }
  };

  const handleDeleteRoster = async (id, docName) => {
    if (!window.confirm(`Are you sure you want to remove ${docName} from emergency duty?`)) {
      return;
    }
    try {
      await emergencyService.deleteRoster(id);
      fetchRosterAndStats();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to delete emergency roster entry');
    }
  };

  const handleUpdateRoster = async (e) => {
    e.preventDefault();
    if (!editingRoster) return;
    try {
      await emergencyService.updateRoster(editingRoster.id, {
        shiftName: editingRoster.shiftName,
        shiftStart: editingRoster.shiftStart,
        shiftEnd: editingRoster.shiftEnd,
        dutyStatus: editingRoster.dutyStatus,
        doctorAvailabilityStatus: editingRoster.doctorAvailabilityStatus,
      });
      setEditingRoster(null);
      fetchRosterAndStats();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update roster entry');
    }
  };

  return (
    <div>
      {/* Top Header & Triage Stats Overview */}
      <div style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '16px', marginBottom: '20px' }}>
          <div>
            <h3 style={{ fontSize: '1.45rem', fontWeight: 800, color: 'var(--slate-900)', display: 'flex', alignItems: 'center', gap: '8px' }}>
              <Siren size={24} style={{ color: '#e11d48' }} /> Daily Emergency Duty Roster & Workload
            </h3>
            <p style={{ color: 'var(--slate-600)', fontSize: '0.875rem' }}>
              Configure daily dynamic emergency shifts, inspect on-duty physician capacity, and monitor incoming triage requests.
            </p>
          </div>

          <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={fetchRosterAndStats}
              title="Refresh"
            >
              <RefreshCw size={14} /> Refresh
            </button>
            <button
              type="button"
              className="btn btn-emergency btn-sm"
              onClick={() => {
                setRosterDate(selectedDate);
                setShowAddModal(true);
              }}
            >
              <Plus size={16} /> Assign Doctor to Duty
            </button>
          </div>
        </div>

        {/* Emergency Triage Stats Grid */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '14px', marginBottom: '24px' }}>
          <div style={{ background: '#fffbeb', border: '1px solid #fde68a', borderRadius: 'var(--radius-md)', padding: '14px', textAlign: 'center' }}>
            <div style={{ fontSize: '1.4rem', fontWeight: 800, color: '#92400e' }}>
              {emergencyStats?.WAITING ?? 0}
            </div>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#b45309', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              Waiting
            </div>
          </div>

          <div style={{ background: '#e0f2fe', border: '1px solid #bae6fd', borderRadius: 'var(--radius-md)', padding: '14px', textAlign: 'center' }}>
            <div style={{ fontSize: '1.4rem', fontWeight: 800, color: '#0369a1' }}>
              {emergencyStats?.ASSIGNED ?? 0}
            </div>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#0284c7', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              Assigned
            </div>
          </div>

          <div style={{ background: '#ede9fe', border: '1px solid #ddd6fe', borderRadius: 'var(--radius-md)', padding: '14px', textAlign: 'center' }}>
            <div style={{ fontSize: '1.4rem', fontWeight: 800, color: '#6d28d9' }}>
              {emergencyStats?.IN_PROGRESS ?? 0}
            </div>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#7c3aed', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              In Progress
            </div>
          </div>

          <div style={{ background: '#ecfdf5', border: '1px solid #a7f3d0', borderRadius: 'var(--radius-md)', padding: '14px', textAlign: 'center' }}>
            <div style={{ fontSize: '1.4rem', fontWeight: 800, color: '#047857' }}>
              {emergencyStats?.COMPLETED ?? 0}
            </div>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#059669', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              Completed
            </div>
          </div>

          <div style={{ background: '#fee2e2', border: '1px solid #fecaca', borderRadius: 'var(--radius-md)', padding: '14px', textAlign: 'center' }}>
            <div style={{ fontSize: '1.4rem', fontWeight: 800, color: '#991b1b' }}>
              {emergencyStats?.NO_DOCTOR_AVAILABLE ?? 0}
            </div>
            <div style={{ fontSize: '0.75rem', fontWeight: 700, color: '#b91c1c', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
              No Doctor Avail.
            </div>
          </div>
        </div>
      </div>

      {/* Roster Controls: Date Selector */}
      <div className="card" style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '14px', paddingBottom: '16px', borderBottom: '1px solid var(--slate-200)', marginBottom: '16px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <Calendar size={18} style={{ color: 'var(--primary-600)' }} />
            <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--slate-800)' }}>
              Select Roster Date:
            </span>
            <input
              type="date"
              className="form-control form-control-sm"
              style={{ width: '170px' }}
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
            />
          </div>

          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              type="button"
              className={`btn btn-sm ${selectedDate === new Date().toISOString().split('T')[0] ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setSelectedDate(new Date().toISOString().split('T')[0])}
            >
              Today
            </button>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => {
                const d = new Date();
                d.setDate(d.getDate() + 1);
                setSelectedDate(d.toISOString().split('T')[0]);
              }}
            >
              Tomorrow
            </button>
          </div>
        </div>

        {/* Emergency Roster Table */}
        <div className="card-header" style={{ paddingTop: 0 }}>
          <span className="card-title" style={{ fontSize: '1.05rem' }}>
            Emergency Physician Roster for {selectedDate} ({rosterList.length} Scheduled)
          </span>
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: '32px', color: 'var(--slate-500)' }}>
            Loading emergency roster...
          </div>
        ) : rosterList.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '36px 16px', color: 'var(--slate-500)' }}>
            <AlertTriangle size={36} style={{ margin: '0 auto 12px', color: '#f59e0b' }} />
            <p style={{ fontWeight: 600, color: 'var(--slate-700)', marginBottom: '6px' }}>
              No emergency doctors scheduled for {selectedDate}.
            </p>
            <p style={{ fontSize: '0.8125rem', marginBottom: '16px' }}>
              Emergency requests on this date will trigger NO_DOCTOR_AVAILABLE if no physicians are assigned.
            </p>
            <button
              type="button"
              className="btn btn-emergency btn-sm"
              onClick={() => {
                setRosterDate(selectedDate);
                setShowAddModal(true);
              }}
            >
              <Plus size={14} /> Add Doctor to Roster
            </button>
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Doctor</th>
                  <th>Specialization</th>
                  <th>Shift</th>
                  <th>Duty Status</th>
                  <th>Current Availability</th>
                  <th>Active Workload</th>
                  <th style={{ textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rosterList.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <div style={{ fontWeight: 700, color: 'var(--slate-900)' }}>
                        {item.doctorName}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                        Phone: {item.doctorPhone || 'N/A'} • Rating: {item.doctorRating || 4.9}
                      </div>
                    </td>
                    <td>
                      <span style={{ fontWeight: 600, color: 'var(--primary-700)', fontSize: '0.875rem' }}>
                        {item.doctorSpecialization}
                      </span>
                    </td>
                    <td>
                      <div style={{ fontWeight: 700, color: 'var(--slate-800)', fontSize: '0.875rem' }}>
                        {item.shiftName}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                        {item.shiftStart?.slice(0, 5)} - {item.shiftEnd?.slice(0, 5)}
                      </div>
                    </td>
                    <td>
                      <span className={`badge ${item.dutyStatus === 'EMERGENCY_DUTY' ? 'badge-emergency' : 'badge-off-duty'}`}>
                        {item.dutyStatus}
                      </span>
                    </td>
                    <td>
                      <span className={`badge badge-${(item.doctorAvailabilityStatus || 'available').toLowerCase()}`}>
                        {item.doctorAvailabilityStatus}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <HeartPulse size={14} style={{ color: item.activeEmergencyCasesCount > 0 ? '#e11d48' : 'var(--slate-400)' }} />
                        <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: item.activeEmergencyCasesCount > 0 ? '#e11d48' : 'var(--slate-700)' }}>
                          {item.activeEmergencyCasesCount} Active
                        </span>
                      </div>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <div style={{ display: 'inline-flex', gap: '8px' }}>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          title="Edit Roster Entry"
                          onClick={() => setEditingRoster({ ...item })}
                        >
                          <Edit2 size={13} />
                        </button>
                        <button
                          type="button"
                          className="btn btn-secondary btn-sm"
                          style={{ color: '#ef4444' }}
                          title="Remove from Emergency Duty"
                          onClick={() => handleDeleteRoster(item.id, item.doctorName)}
                        >
                          <Trash2 size={13} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Today's Emergency Requests Log for Admin */}
      <div className="card">
        <div className="card-header">
          <span className="card-title" style={{ fontSize: '1.05rem' }}>
            <Activity size={18} className="text-primary" /> Recent Emergency Requests Dispatch Log
          </span>
        </div>

        {emergencyRequests.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px', color: 'var(--slate-500)', fontSize: '0.875rem' }}>
            No emergency requests recorded.
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Patient</th>
                  <th>Category</th>
                  <th>Assigned Physician</th>
                  <th>Request Time</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {emergencyRequests.slice(0, 10).map((req) => (
                  <tr key={req.id}>
                    <td>#{req.id}</td>
                    <td>
                      <div style={{ fontWeight: 600, color: 'var(--slate-900)' }}>
                        {req.patientName}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>
                        {req.patientPhone || 'No phone'}
                      </div>
                    </td>
                    <td>
                      <span className="badge badge-emergency">{req.category || 'General'}</span>
                    </td>
                    <td>
                      {req.doctorName ? (
                        <div>
                          <div style={{ fontWeight: 600 }}>{req.doctorName}</div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--primary-700)' }}>{req.doctorSpecialization}</div>
                        </div>
                      ) : (
                        <span style={{ color: 'var(--slate-400)', fontStyle: 'italic', fontSize: '0.8125rem' }}>
                          None
                        </span>
                      )}
                    </td>
                    <td style={{ fontSize: '0.8125rem' }}>
                      {new Date(req.requestTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
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

      {/* Modal: Assign Doctor to Emergency Duty */}
      {showAddModal && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '520px' }}>
            <div className="modal-header">
              <h3 style={{ fontSize: '1.25rem', fontWeight: 700 }}>
                Assign Doctor to Emergency Duty
              </h3>
              <button className="modal-close-btn" onClick={() => setShowAddModal(false)}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleAddDoctor}>
              {modalError && (
                <div style={{ background: '#fee2e2', color: '#991b1b', padding: '10px 14px', borderRadius: 'var(--radius-md)', fontSize: '0.8125rem', marginBottom: '16px' }}>
                  {modalError}
                </div>
              )}

              <div className="form-group">
                <label className="form-label">Select Licensed Physician</label>
                <select
                  className="form-control"
                  value={selectedDoctorId}
                  onChange={(e) => setSelectedDoctorId(e.target.value)}
                  required
                >
                  {doctors.map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.fullName} — {d.specialization} ({d.qualification || 'Physician'})
                    </option>
                  ))}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Roster Date</label>
                <input
                  type="date"
                  className="form-control"
                  value={rosterDate}
                  onChange={(e) => setRosterDate(e.target.value)}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Shift Preset</label>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px', marginBottom: '10px' }}>
                  {['MORNING', 'EVENING', 'NIGHT'].map((preset) => (
                    <button
                      key={preset}
                      type="button"
                      className={`btn btn-sm ${shiftName === preset ? 'btn-primary' : 'btn-secondary'}`}
                      onClick={() => handleShiftPresetChange(preset)}
                    >
                      {preset}
                    </button>
                  ))}
                </div>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div className="form-group">
                  <label className="form-label">Shift Start</label>
                  <input
                    type="time"
                    className="form-control"
                    value={shiftStart}
                    onChange={(e) => {
                      setShiftStart(e.target.value);
                      setShiftName('CUSTOM');
                    }}
                    required
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Shift End</label>
                  <input
                    type="time"
                    className="form-control"
                    value={shiftEnd}
                    onChange={(e) => {
                      setShiftEnd(e.target.value);
                      setShiftName('CUSTOM');
                    }}
                    required
                  />
                </div>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowAddModal(false)}
                  disabled={modalLoading}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-emergency"
                  disabled={modalLoading}
                >
                  {modalLoading ? 'Assigning...' : 'Confirm Duty Assignment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Edit Shift & Status */}
      {editingRoster && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '480px' }}>
            <div className="modal-header">
              <h3 style={{ fontSize: '1.2rem', fontWeight: 700 }}>
                Edit Emergency Duty: {editingRoster.doctorName}
              </h3>
              <button className="modal-close-btn" onClick={() => setEditingRoster(null)}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleUpdateRoster}>
              <div className="form-group">
                <label className="form-label">Shift Name</label>
                <select
                  className="form-control"
                  value={editingRoster.shiftName}
                  onChange={(e) => setEditingRoster({ ...editingRoster, shiftName: e.target.value })}
                >
                  <option value="MORNING">MORNING</option>
                  <option value="EVENING">EVENING</option>
                  <option value="NIGHT">NIGHT</option>
                  <option value="CUSTOM">CUSTOM</option>
                </select>
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
                <div className="form-group">
                  <label className="form-label">Start Time</label>
                  <input
                    type="time"
                    className="form-control"
                    value={editingRoster.shiftStart?.slice(0, 5) || ''}
                    onChange={(e) => setEditingRoster({ ...editingRoster, shiftStart: e.target.value })}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">End Time</label>
                  <input
                    type="time"
                    className="form-control"
                    value={editingRoster.shiftEnd?.slice(0, 5) || ''}
                    onChange={(e) => setEditingRoster({ ...editingRoster, shiftEnd: e.target.value })}
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Duty Assignment Status</label>
                <select
                  className="form-control"
                  value={editingRoster.dutyStatus}
                  onChange={(e) => setEditingRoster({ ...editingRoster, dutyStatus: e.target.value })}
                >
                  <option value="EMERGENCY_DUTY">EMERGENCY_DUTY</option>
                  <option value="NOT_ASSIGNED">NOT_ASSIGNED</option>
                </select>
              </div>

              <div className="form-group">
                <label className="form-label">Physician Availability</label>
                <select
                  className="form-control"
                  value={editingRoster.doctorAvailabilityStatus}
                  onChange={(e) => setEditingRoster({ ...editingRoster, doctorAvailabilityStatus: e.target.value })}
                >
                  <option value="AVAILABLE">AVAILABLE</option>
                  <option value="BUSY">BUSY</option>
                  <option value="IN_CONSULTATION">IN_CONSULTATION</option>
                  <option value="OFF_DUTY">OFF_DUTY</option>
                  <option value="ON_LEAVE">ON_LEAVE</option>
                </select>
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setEditingRoster(null)}
                >
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary">
                  Save Changes
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminEmergencyRoster;
