import React, { useState, useEffect } from 'react';
import { Clock, CheckCircle2, Save, Calendar, AlertCircle } from 'lucide-react';
import { doctorService } from '../../services/api';
import { useAuth } from '../../context/AuthContext';

const DAYS_OF_WEEK = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

const DoctorAvailabilityPage = () => {
  const { user } = useAuth();
  const [schedule, setSchedule] = useState(
    DAYS_OF_WEEK.map((day) => ({
      dayOfWeek: day,
      startTime: '09:00',
      endTime: '17:00',
      breakStartTime: '13:00',
      breakEndTime: '14:00',
      slotDurationMinutes: 30,
      available: day !== 'SUNDAY',
    }))
  );
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (user?.doctorId) {
      loadAvailability(user.doctorId);
    } else {
      setLoading(false);
    }
  }, [user]);

  const loadAvailability = async (docId) => {
    try {
      const res = await doctorService.getDoctorAvailability(docId);
      if (res.data && res.data.length > 0) {
        // Merge with full week
        const merged = DAYS_OF_WEEK.map((day) => {
          const found = res.data.find((d) => d.dayOfWeek === day);
          if (found) {
            return {
              dayOfWeek: day,
              startTime: found.startTime ? found.startTime.substring(0, 5) : '09:00',
              endTime: found.endTime ? found.endTime.substring(0, 5) : '17:00',
              breakStartTime: found.breakStartTime ? found.breakStartTime.substring(0, 5) : '13:00',
              breakEndTime: found.breakEndTime ? found.breakEndTime.substring(0, 5) : '14:00',
              slotDurationMinutes: found.slotDurationMinutes || 30,
              available: found.available,
            };
          }
          return {
            dayOfWeek: day,
            startTime: '09:00',
            endTime: '17:00',
            breakStartTime: '13:00',
            breakEndTime: '14:00',
            slotDurationMinutes: 30,
            available: day !== 'SUNDAY',
          };
        });
        setSchedule(merged);
      }
    } catch (err) {
      console.error('Failed to load doctor availability:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDayChange = (index, field, value) => {
    setSchedule((prev) => {
      const updated = [...prev];
      updated[index] = { ...updated[index], [field]: value };
      return updated;
    });
  };

  const handleSave = async () => {
    if (!user?.doctorId) {
      setError('Doctor ID missing. Please re-login.');
      return;
    }
    setSaving(true);
    setError('');
    setSaveSuccess(false);

    try {
      const payload = schedule.map((s) => ({
        dayOfWeek: s.dayOfWeek,
        startTime: `${s.startTime}:00`,
        endTime: `${s.endTime}:00`,
        breakStartTime: s.breakStartTime ? `${s.breakStartTime}:00` : null,
        breakEndTime: s.breakEndTime ? `${s.breakEndTime}:00` : null,
        slotDurationMinutes: parseInt(s.slotDurationMinutes) || 30,
        available: s.available,
      }));

      await doctorService.saveDoctorAvailability(user.doctorId, payload);
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save availability schedule');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '28px' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Weekly Availability & Office Hours</h2>
          <p style={{ color: 'var(--slate-600)' }}>
            Configure working hours, break times, and consultation slot duration. Patients will only see bookable slots within this window.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-primary"
          onClick={handleSave}
          disabled={saving || loading}
        >
          <Save size={16} /> {saving ? 'Saving...' : 'Save Availability Schedule'}
        </button>
      </div>

      {saveSuccess && (
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
          <span>Weekly schedule successfully updated! Patient booking slots have been refreshed.</span>
        </div>
      )}

      {error && (
        <div
          style={{
            padding: '12px 16px',
            background: 'var(--danger-bg)',
            color: 'var(--danger-text)',
            border: '1px solid var(--danger-border)',
            borderRadius: 'var(--radius-md)',
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

      <div className="card">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '36px', color: 'var(--slate-500)' }}>
            Loading consultation schedule...
          </div>
        ) : (
          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Day of Week</th>
                  <th>Status</th>
                  <th>Shift Start</th>
                  <th>Shift End</th>
                  <th>Break Start</th>
                  <th>Break End</th>
                  <th>Slot Duration</th>
                </tr>
              </thead>
              <tbody>
                {schedule.map((item, idx) => (
                  <tr key={item.dayOfWeek} style={{ opacity: item.available ? 1 : 0.6 }}>
                    <td style={{ fontWeight: 700, color: 'var(--slate-900)' }}>
                      {item.dayOfWeek}
                    </td>
                    <td>
                      <label style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                        <input
                          type="checkbox"
                          checked={item.available}
                          onChange={(e) => handleDayChange(idx, 'available', e.target.checked)}
                          style={{ width: '18px', height: '18px', accentColor: 'var(--primary-600)' }}
                        />
                        <span style={{ fontSize: '0.8125rem', fontWeight: 600, color: item.available ? 'var(--teal-700)' : 'var(--slate-400)' }}>
                          {item.available ? 'Active' : 'Off-duty'}
                        </span>
                      </label>
                    </td>
                    <td>
                      <input
                        type="time"
                        className="form-control form-control-sm"
                        value={item.startTime}
                        onChange={(e) => handleDayChange(idx, 'startTime', e.target.value)}
                        disabled={!item.available}
                      />
                    </td>
                    <td>
                      <input
                        type="time"
                        className="form-control form-control-sm"
                        value={item.endTime}
                        onChange={(e) => handleDayChange(idx, 'endTime', e.target.value)}
                        disabled={!item.available}
                      />
                    </td>
                    <td>
                      <input
                        type="time"
                        className="form-control form-control-sm"
                        value={item.breakStartTime}
                        onChange={(e) => handleDayChange(idx, 'breakStartTime', e.target.value)}
                        disabled={!item.available}
                      />
                    </td>
                    <td>
                      <input
                        type="time"
                        className="form-control form-control-sm"
                        value={item.breakEndTime}
                        onChange={(e) => handleDayChange(idx, 'breakEndTime', e.target.value)}
                        disabled={!item.available}
                      />
                    </td>
                    <td>
                      <select
                        className="form-control form-control-sm"
                        value={item.slotDurationMinutes}
                        onChange={(e) => handleDayChange(idx, 'slotDurationMinutes', parseInt(e.target.value))}
                        disabled={!item.available}
                      >
                        <option value={15}>15 mins</option>
                        <option value={20}>20 mins</option>
                        <option value={30}>30 mins</option>
                        <option value={45}>45 mins</option>
                        <option value={60}>60 mins</option>
                      </select>
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

export default DoctorAvailabilityPage;
