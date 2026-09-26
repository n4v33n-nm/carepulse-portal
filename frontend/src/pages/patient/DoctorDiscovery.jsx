import React, { useState, useEffect } from 'react';
import {
  Search,
  Calendar,
  Clock,
  Star,
  CheckCircle,
  AlertCircle,
  Stethoscope,
  IndianRupee,
  Briefcase,
} from 'lucide-react';
import { doctorService, appointmentService } from '../../services/api';
import Modal from '../../components/Modal';

const DoctorDiscovery = () => {
  const [doctors, setDoctors] = useState([]);
  const [specializations, setSpecializations] = useState([]);
  const [selectedSpec, setSelectedSpec] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);

  // Booking Modal State
  const [selectedDoctor, setSelectedDoctor] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [appointmentDate, setAppointmentDate] = useState(
    new Date(Date.now() + 86400000).toISOString().split('T')[0] // default tomorrow
  );
  const [availableSlots, setAvailableSlots] = useState([]);
  const [selectedSlot, setSelectedSlot] = useState('');
  const [reason, setReason] = useState('');
  const [bookingLoading, setBookingLoading] = useState(false);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [bookingError, setBookingError] = useState('');
  const [bookingSuccess, setBookingSuccess] = useState('');

  useEffect(() => {
    fetchSpecializations();
    fetchDoctors();
  }, []);

  const fetchSpecializations = async () => {
    try {
      const res = await doctorService.getSpecializations();
      setSpecializations(res.data || []);
    } catch (e) {
      console.error(e);
    }
  };

  const fetchDoctors = async (spec = selectedSpec, query = searchQuery) => {
    setLoading(true);
    try {
      const params = {};
      if (spec) params.specialization = spec;
      if (query) params.query = query;
      const res = await doctorService.getAllDoctors(params);
      setDoctors(res.data || []);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleSpecSelect = (spec) => {
    setSelectedSpec(spec);
    fetchDoctors(spec, searchQuery);
  };

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    fetchDoctors(selectedSpec, searchQuery);
  };

  const openBookingModal = (doc) => {
    setSelectedDoctor(doc);
    setSelectedSlot('');
    setReason('');
    setBookingError('');
    setBookingSuccess('');
    setIsModalOpen(true);
    loadSlots(doc.id, appointmentDate);
  };

  const loadSlots = async (docId, date) => {
    setSlotsLoading(true);
    try {
      const res = await doctorService.getAvailableSlots(docId, date);
      setAvailableSlots(res.data || []);
      if (res.data?.length > 0) {
        setSelectedSlot(res.data[0]);
      } else {
        setSelectedSlot('');
      }
    } catch (err) {
      console.error('Failed to load slots', err);
      setAvailableSlots([]);
    } finally {
      setSlotsLoading(false);
    }
  };

  const handleDateChange = (newDate) => {
    setAppointmentDate(newDate);
    if (selectedDoctor) {
      loadSlots(selectedDoctor.id, newDate);
    }
  };

  const handleBookSubmit = async (e) => {
    e.preventDefault();
    if (!selectedSlot) {
      setBookingError('Please select an available consultation time slot.');
      return;
    }
    setBookingLoading(true);
    setBookingError('');
    setBookingSuccess('');

    try {
      await appointmentService.bookAppointment({
        doctorId: selectedDoctor.id,
        appointmentDate: appointmentDate,
        appointmentTime: selectedSlot.length === 5 ? `${selectedSlot}:00` : selectedSlot,
        reason: reason || 'General Consultation',
      });
      setBookingSuccess('Appointment successfully booked! The doctor has been notified.');
      setTimeout(() => {
        setIsModalOpen(false);
      }, 1800);
    } catch (err) {
      setBookingError(err.response?.data?.message || 'Failed to book appointment. Please choose another time slot.');
    } finally {
      setBookingLoading(false);
    }
  };

  return (
    <div>
      {/* Header and Search */}
      <div style={{ marginBottom: '28px' }}>
        <h2 style={{ fontSize: '1.75rem', marginBottom: '8px' }}>Find Healthcare Specialists</h2>
        <p style={{ color: 'var(--slate-600)', marginBottom: '20px' }}>
          Connect with vetted physicians, compare clinical specializations, and schedule consultations.
        </p>

        {/* Search Bar */}
        <form onSubmit={handleSearchSubmit} style={{ display: 'flex', gap: '12px', maxWidth: '640px', marginBottom: '16px' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search
              size={18}
              style={{
                position: 'absolute',
                left: '14px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: 'var(--slate-400)',
              }}
            />
            <input
              type="text"
              className="form-control"
              style={{ paddingLeft: '42px' }}
              placeholder="Search by doctor name or keywords..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
          <button type="submit" className="btn btn-primary">
            Search
          </button>
        </form>

        {/* Specialization Filter Pills */}
        <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
          <button
            type="button"
            className={`btn btn-sm ${selectedSpec === '' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => handleSpecSelect('')}
          >
            All Specializations
          </button>
          {specializations.map((spec) => (
            <button
              key={spec}
              type="button"
              className={`btn btn-sm ${selectedSpec === spec ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => handleSpecSelect(spec)}
            >
              {spec}
            </button>
          ))}
        </div>
      </div>

      {/* Doctor Cards Grid */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-500)' }}>
          Loading available physicians...
        </div>
      ) : doctors.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
          <Stethoscope size={40} style={{ color: 'var(--slate-400)', margin: '0 auto 12px' }} />
          <h3 style={{ fontSize: '1.25rem', marginBottom: '6px' }}>No specialists found</h3>
          <p style={{ color: 'var(--slate-500)', fontSize: '0.9375rem' }}>
            Try adjusting your search criteria or selecting a different specialization.
          </p>
        </div>
      ) : (
        <div className="doctor-grid">
          {doctors.map((doc) => (
            <div key={doc.id} className="doctor-card">
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '12px' }}>
                  <div style={{ display: 'flex', gap: '14px', alignItems: 'center' }}>
                    <div
                      style={{
                        width: '52px',
                        height: '52px',
                        borderRadius: 'var(--radius-md)',
                        background: 'linear-gradient(135deg, var(--primary-600), var(--teal-600))',
                        color: 'white',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: 700,
                        fontSize: '1.2rem',
                      }}
                    >
                      {doc.fullName.replace('Dr. ', '').charAt(0)}
                    </div>
                    <div>
                      <h3 style={{ fontSize: '1.125rem', color: 'var(--slate-900)' }}>{doc.fullName}</h3>
                      <span
                        style={{
                          fontSize: '0.8125rem',
                          fontWeight: 600,
                          color: 'var(--primary-700)',
                          background: 'var(--primary-50)',
                          padding: '2px 8px',
                          borderRadius: '4px',
                        }}
                      >
                        {doc.specialization}
                      </span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px', color: '#f59e0b', fontWeight: 700, fontSize: '0.875rem' }}>
                    <Star size={16} fill="#f59e0b" />
                    <span>{doc.rating || 4.9}</span>
                  </div>
                </div>

                <div style={{ fontSize: '0.8125rem', color: 'var(--slate-600)', marginBottom: '8px' }}>
                  <strong>Qualification:</strong> {doc.qualification || 'Board Certified'}
                </div>

                <div style={{ display: 'flex', gap: '16px', fontSize: '0.8125rem', color: 'var(--slate-500)', marginBottom: '14px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <Briefcase size={14} /> {doc.experienceYears} Years Exp.
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '3px', fontWeight: 600, color: 'var(--primary-700)' }}>
                    <IndianRupee size={14} /> {doc.consultationFee ? Number(doc.consultationFee).toLocaleString('en-IN') : '500'}
                  </div>
                </div>

                <p style={{ fontSize: '0.875rem', color: 'var(--slate-600)', lineHeight: '1.45', marginBottom: '18px' }}>
                  {doc.bio || 'Dedicated to providing high-quality, patient-centered medical consultations.'}
                </p>
              </div>

              <button
                type="button"
                className="btn btn-primary"
                style={{ width: '100%' }}
                onClick={() => openBookingModal(doc)}
              >
                <Calendar size={16} /> Book Consultation
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Interactive Booking Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={`Book Appointment – ${selectedDoctor?.fullName || ''}`}
        maxWidth="560px"
      >
        {bookingSuccess ? (
          <div style={{ textAlign: 'center', padding: '24px 0' }}>
            <CheckCircle size={48} style={{ color: 'var(--teal-600)', margin: '0 auto 16px' }} />
            <h3 style={{ fontSize: '1.25rem', marginBottom: '8px' }}>Appointment Confirmed!</h3>
            <p style={{ color: 'var(--slate-600)', fontSize: '0.9375rem' }}>{bookingSuccess}</p>
          </div>
        ) : (
          <form onSubmit={handleBookSubmit}>
            {bookingError && (
              <div
                style={{
                  padding: '10px 14px',
                  background: 'var(--danger-bg)',
                  border: '1px solid var(--danger-border)',
                  borderRadius: 'var(--radius-md)',
                  color: 'var(--danger-text)',
                  fontSize: '0.875rem',
                  marginBottom: '16px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                <AlertCircle size={16} />
                <span>{bookingError}</span>
              </div>
            )}

            {/* Doctor Info & Fee Summary */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 14px', background: 'var(--slate-50)', borderRadius: 'var(--radius-md)', border: '1px solid var(--slate-200)', marginBottom: '16px', fontSize: '0.875rem' }}>
              <span style={{ color: 'var(--slate-600)' }}>Specialization: <strong>{selectedDoctor?.specialization}</strong></span>
              <span style={{ color: 'var(--primary-700)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '3px' }}>
                Consultation Fee: <IndianRupee size={14} />{selectedDoctor?.consultationFee ? Number(selectedDoctor.consultationFee).toLocaleString('en-IN') : '500'}
              </span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '16px', marginBottom: '16px' }}>
              <div className="form-group" style={{ marginBottom: 0 }}>
                <label className="form-label" htmlFor="appt-date-input">Select Date</label>
                <input
                  id="appt-date-input"
                  type="date"
                  className="form-control"
                  min={new Date().toISOString().split('T')[0]}
                  value={appointmentDate}
                  onChange={(e) => handleDateChange(e.target.value)}
                  required
                />
              </div>
            </div>

            {/* Time Slots */}
            <div style={{ marginBottom: '16px' }}>
              <label className="form-label">Available Time Slots</label>
              {slotsLoading ? (
                <div style={{ fontSize: '0.875rem', color: 'var(--slate-500)', padding: '12px 0' }}>
                  Loading real-time availability...
                </div>
              ) : availableSlots.length === 0 ? (
                <div
                  style={{
                    padding: '12px',
                    background: 'var(--slate-100)',
                    borderRadius: 'var(--radius-md)',
                    color: 'var(--slate-600)',
                    fontSize: '0.875rem',
                  }}
                >
                  No slots available on this date. Doctor may be off-duty or fully booked. Please choose another date.
                </div>
              ) : (
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(84px, 1fr))', gap: '8px' }}>
                  {availableSlots.map((slot) => {
                    const timeLabel = typeof slot === 'string' ? slot.substring(0, 5) : slot;
                    const isSelected = selectedSlot === slot || selectedSlot === timeLabel;
                    return (
                      <button
                        key={timeLabel}
                        type="button"
                        onClick={() => setSelectedSlot(timeLabel)}
                        style={{
                          padding: '8px 10px',
                          borderRadius: 'var(--radius-sm)',
                          border: isSelected ? '2px solid var(--primary-600)' : '1px solid var(--slate-300)',
                          background: isSelected ? 'var(--primary-50)' : 'white',
                          color: isSelected ? 'var(--primary-700)' : 'var(--slate-700)',
                          fontWeight: isSelected ? 700 : 500,
                          fontSize: '0.875rem',
                          cursor: 'pointer',
                          transition: 'all 0.15s ease',
                        }}
                      >
                        {timeLabel}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="consult-reason-input">Reason for Consultation</label>
              <textarea
                id="consult-reason-input"
                className="form-control"
                rows="3"
                placeholder="Briefly describe your symptoms or inquiry..."
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                required
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '10px', marginTop: '20px' }}>
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => setIsModalOpen(false)}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="btn btn-primary"
                disabled={bookingLoading || availableSlots.length === 0}
              >
                {bookingLoading ? 'Confirming...' : 'Confirm Appointment'}
              </button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
};

export default DoctorDiscovery;
