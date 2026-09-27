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
  Sparkles,
  Info,
  ListPlus,
  ShieldCheck,
} from 'lucide-react';
import { doctorService, appointmentService } from '../../services/api';
import Modal from '../../components/Modal';

const DoctorDiscovery = () => {
  const [activeTab, setActiveTab] = useState('BROWSE'); // 'BROWSE' | 'SMART_MATCH'
  const [doctors, setDoctors] = useState([]);
  const [specializations, setSpecializations] = useState([]);
  const [selectedSpec, setSelectedSpec] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);

  // Smart Matching State
  const [matchSpec, setMatchSpec] = useState('');
  const [matchDate, setMatchDate] = useState(
    new Date(Date.now() + 86400000).toISOString().split('T')[0]
  );
  const [matchTime, setMatchTime] = useState('');
  const [matchedDoctors, setMatchedDoctors] = useState([]);
  const [matchLoading, setMatchLoading] = useState(false);
  const [matchSearched, setMatchSearched] = useState(false);

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

  // Waitlist State inside Modal
  const [waitlistLoading, setWaitlistLoading] = useState(false);
  const [waitlistSuccess, setWaitlistSuccess] = useState('');
  const [waitlistError, setWaitlistError] = useState('');

  useEffect(() => {
    fetchSpecializations();
    fetchDoctors();
  }, []);

  const fetchSpecializations = async () => {
    try {
      const res = await doctorService.getSpecializations();
      setSpecializations(res.data || []);
      if (res.data?.length > 0 && !matchSpec) {
        setMatchSpec(res.data[0]);
      }
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

  const handleSmartMatch = async (e) => {
    if (e) e.preventDefault();
    setMatchLoading(true);
    setMatchSearched(true);
    try {
      const params = {};
      if (matchSpec) params.specialization = matchSpec;
      if (matchDate) params.date = matchDate;
      if (matchTime) params.time = matchTime.length === 5 ? `${matchTime}:00` : matchTime;

      const res = await doctorService.matchDoctors(params);
      setMatchedDoctors(res.data || []);
    } catch (err) {
      console.error('Smart doctor matching failed:', err);
      setMatchedDoctors([]);
    } finally {
      setMatchLoading(false);
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

  const openBookingModal = (doc, initialDate = null, initialSlot = null) => {
    setSelectedDoctor(doc);
    const targetDate = initialDate || appointmentDate;
    setAppointmentDate(targetDate);
    setSelectedSlot(initialSlot || '');
    setReason('');
    setBookingError('');
    setBookingSuccess('');
    setWaitlistSuccess('');
    setWaitlistError('');
    setIsModalOpen(true);
    loadSlots(doc.id, targetDate);
  };

  const loadSlots = async (docId, date) => {
    setSlotsLoading(true);
    try {
      const res = await doctorService.getSmartAvailableSlots(docId, date);
      setAvailableSlots(res.data || []);
      if (res.data?.length > 0 && !selectedSlot) {
        setSelectedSlot(res.data[0]);
      }
    } catch (err) {
      console.error('Failed to load slots', err);
      // Fallback to legacy endpoint if needed
      try {
        const fallback = await doctorService.getAvailableSlots(docId, date);
        setAvailableSlots(fallback.data || []);
      } catch (fErr) {
        setAvailableSlots([]);
      }
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

  const handleJoinWaitlist = async () => {
    if (!selectedDoctor || !appointmentDate) return;
    setWaitlistLoading(true);
    setWaitlistError('');
    setWaitlistSuccess('');

    try {
      const payload = {
        doctorId: selectedDoctor.id,
        specialization: selectedDoctor.specialization,
        preferredDate: appointmentDate,
        preferredTime: selectedSlot ? (selectedSlot.length === 5 ? `${selectedSlot}:00` : selectedSlot) : null,
        notes: reason || `Patient waiting for open slot on ${appointmentDate}`,
      };
      await appointmentService.joinWaitlist(payload);
      setWaitlistSuccess('Successfully added to the waitlist! You will receive an instant notification when a cancelled slot opens.');
    } catch (err) {
      setWaitlistError(err.response?.data?.message || 'Failed to join waitlist. Please try again.');
    } finally {
      setWaitlistLoading(false);
    }
  };

  return (
    <div>
      {/* Header and Mode Selector */}
      <div style={{ marginBottom: '24px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '16px', marginBottom: '16px' }}>
          <div>
            <h2 style={{ fontSize: '1.75rem', marginBottom: '6px' }}>Find Healthcare Specialists</h2>
            <p style={{ color: 'var(--slate-600)' }}>
              Intelligent scheduling coordination: browse verified physicians or run explainable clinical matching.
            </p>
          </div>

          {/* Tab Switcher */}
          <div style={{ display: 'inline-flex', background: 'var(--slate-100)', padding: '4px', borderRadius: 'var(--radius-md)' }}>
            <button
              type="button"
              className={`btn btn-sm ${activeTab === 'BROWSE' ? 'btn-primary' : 'btn-ghost'}`}
              onClick={() => setActiveTab('BROWSE')}
              style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <Search size={15} /> Browse All Doctors
            </button>
            <button
              type="button"
              className={`btn btn-sm ${activeTab === 'SMART_MATCH' ? 'btn-primary' : 'btn-ghost'}`}
              onClick={() => setActiveTab('SMART_MATCH')}
              style={{ display: 'flex', alignItems: 'center', gap: '6px' }}
            >
              <Sparkles size={15} /> Smart Doctor Matching
            </button>
          </div>
        </div>

        {/* Clinical Disclaimer Banner */}
        <div
          style={{
            padding: '10px 14px',
            background: 'var(--primary-50)',
            border: '1px solid var(--primary-200)',
            borderRadius: 'var(--radius-md)',
            color: 'var(--primary-800)',
            fontSize: '0.8125rem',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            marginBottom: '20px',
          }}
        >
          <Info size={16} style={{ flexShrink: 0 }} />
          <span>
            <strong>Scheduling Advisory:</strong> The Smart Doctor Matching Engine balances specialization, clinic availability, appointment collision detection, emergency duty status, and physician workload. It provides administrative scheduling optimization, not a medical diagnosis.
          </span>
        </div>

        {/* TAB 1: BROWSE ALL DOCTORS */}
        {activeTab === 'BROWSE' && (
          <div>
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
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '20px' }}>
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
          </div>
        )}

        {/* TAB 2: SMART DOCTOR MATCHING ENGINE */}
        {activeTab === 'SMART_MATCH' && (
          <div>
            <div className="card" style={{ marginBottom: '24px', background: 'linear-gradient(180deg, #ffffff, var(--slate-50))' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
                <div style={{ padding: '8px', background: 'var(--primary-100)', color: 'var(--primary-700)', borderRadius: 'var(--radius-sm)' }}>
                  <Sparkles size={20} />
                </div>
                <div>
                  <h3 style={{ fontSize: '1.2rem', marginBottom: '2px' }}>Smart Doctor Matching Engine</h3>
                  <p style={{ fontSize: '0.875rem', color: 'var(--slate-600)' }}>
                    Match with available specialists based on specialty alignment, working calendar, conflict-free slots, and real-time workload balancing.
                  </p>
                </div>
              </div>

              <form onSubmit={handleSmartMatch} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '14px', alignItems: 'end' }}>
                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label">Specialization</label>
                  <select
                    className="form-control"
                    value={matchSpec}
                    onChange={(e) => setMatchSpec(e.target.value)}
                  >
                    <option value="">Any Specialization</option>
                    {specializations.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label">Preferred Date</label>
                  <input
                    type="date"
                    className="form-control"
                    min={new Date().toISOString().split('T')[0]}
                    value={matchDate}
                    onChange={(e) => setMatchDate(e.target.value)}
                  />
                </div>

                <div className="form-group" style={{ marginBottom: 0 }}>
                  <label className="form-label">Preferred Time (Optional)</label>
                  <input
                    type="time"
                    className="form-control"
                    value={matchTime}
                    onChange={(e) => setMatchTime(e.target.value)}
                  />
                </div>

                <div>
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={matchLoading}
                    style={{ width: '100%', height: '42px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}
                  >
                    <Sparkles size={16} />
                    {matchLoading ? 'Matching...' : 'Find Best Matches'}
                  </button>
                </div>
              </form>
            </div>

            {/* Smart Match Results */}
            {matchLoading ? (
              <div style={{ textAlign: 'center', padding: '40px', color: 'var(--slate-500)' }}>
                Running conflict detection & workload balancing calculations...
              </div>
            ) : matchSearched && matchedDoctors.length === 0 ? (
              <div className="card" style={{ textAlign: 'center', padding: '48px 20px' }}>
                <AlertCircle size={40} style={{ color: 'var(--warning-text, #f59e0b)', margin: '0 auto 12px' }} />
                <h3 style={{ fontSize: '1.25rem', marginBottom: '6px' }}>No Exact Matches Found</h3>
                <p style={{ color: 'var(--slate-600)', fontSize: '0.9375rem', maxWidth: '520px', margin: '0 auto 16px' }}>
                  No doctors in {matchSpec || 'selected specialty'} are available on {matchDate} at {matchTime || 'the selected time'} without conflicts.
                </p>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => openBookingModal(doctors[0] || { id: 1, fullName: 'Specialist', specialization: matchSpec }, matchDate, matchTime)}
                >
                  <ListPlus size={16} /> Join Priority Waitlist Instead
                </button>
              </div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {matchedDoctors.map((item) => (
                  <div key={item.doctorId} className="card" style={{ borderLeft: '4px solid var(--primary-600)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '14px', marginBottom: '14px' }}>
                      <div style={{ display: 'flex', gap: '14px', alignItems: 'center' }}>
                        <div
                          style={{
                            width: '50px',
                            height: '50px',
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
                          {item.fullName.replace('Dr. ', '').charAt(0)}
                        </div>
                        <div>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <h3 style={{ fontSize: '1.2rem', color: 'var(--slate-900)' }}>{item.fullName}</h3>
                            <span className="badge badge-success" style={{ fontSize: '0.75rem' }}>
                              {item.availabilityStatus}
                            </span>
                          </div>
                          <span style={{ fontSize: '0.875rem', color: 'var(--primary-700)', fontWeight: 600 }}>
                            {item.specialization}
                          </span>
                        </div>
                      </div>

                      <div style={{ textAlign: 'right' }}>
                        <div style={{ fontSize: '1.1rem', fontWeight: 700, color: 'var(--primary-700)' }}>
                          ₹{item.consultationFee ? Number(item.consultationFee).toLocaleString('en-IN') : '500'}
                        </div>
                        <span style={{ fontSize: '0.8125rem', color: 'var(--slate-500)' }}>
                          Workload Today: <strong>{item.currentWorkloadToday} cases</strong>
                        </span>
                      </div>
                    </div>

                    {/* Explainable Match Reasons */}
                    <div style={{ background: 'var(--slate-50)', padding: '12px 14px', borderRadius: 'var(--radius-md)', marginBottom: '14px' }}>
                      <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--slate-700)', marginBottom: '8px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                        <ShieldCheck size={14} style={{ color: 'var(--teal-600)' }} /> Match Validation & Explainability:
                      </div>
                      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                        {item.matchReasons?.map((reason, rIdx) => (
                          <span
                            key={rIdx}
                            style={{
                              fontSize: '0.8125rem',
                              padding: '3px 10px',
                              borderRadius: '12px',
                              background: 'var(--teal-50)',
                              color: 'var(--teal-800)',
                              border: '1px solid var(--teal-200)',
                            }}
                          >
                            ✓ {reason}
                          </span>
                        ))}
                      </div>
                    </div>

                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '10px' }}>
                      <div style={{ fontSize: '0.875rem', color: 'var(--slate-600)' }}>
                        {item.emergencyDutyToday ? (
                          <span style={{ color: '#d97706', fontWeight: 500 }}>
                            ⚡ Assigned to Emergency Duty Today ({item.emergencyShift || 'Active'})
                          </span>
                        ) : (
                          <span style={{ color: 'var(--slate-500)' }}>
                            Clinic Schedule Confirmed • No conflicting bookings
                          </span>
                        )}
                      </div>

                      <button
                        type="button"
                        className="btn btn-primary"
                        onClick={() => openBookingModal(
                          { id: item.doctorId, fullName: item.fullName, specialization: item.specialization, consultationFee: item.consultationFee },
                          matchDate,
                          matchTime
                        )}
                      >
                        <Calendar size={16} /> Book This Specialist
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>

      {/* Interactive Booking Modal with Cancelled Slot Reuse & Waitlist Option */}
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

            {waitlistSuccess && (
              <div
                style={{
                  padding: '10px 14px',
                  background: 'var(--teal-50)',
                  border: '1px solid var(--teal-200)',
                  borderRadius: 'var(--radius-md)',
                  color: 'var(--teal-800)',
                  fontSize: '0.875rem',
                  marginBottom: '16px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                }}
              >
                <CheckCircle size={16} />
                <span>{waitlistSuccess}</span>
              </div>
            )}

            {waitlistError && (
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
                <span>{waitlistError}</span>
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

            {/* Time Slots (Includes dynamically reclaimed cancelled slots) */}
            <div style={{ marginBottom: '16px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <label className="form-label" style={{ marginBottom: 0 }}>Available Consultation Slots</label>
                <span style={{ fontSize: '0.75rem', color: 'var(--slate-500)' }}>Reclaims cancelled slots in real-time</span>
              </div>

              {slotsLoading ? (
                <div style={{ fontSize: '0.875rem', color: 'var(--slate-500)', padding: '12px 0' }}>
                  Loading real-time availability...
                </div>
              ) : availableSlots.length === 0 ? (
                <div
                  style={{
                    padding: '16px',
                    background: 'var(--slate-50)',
                    border: '1px dashed var(--slate-300)',
                    borderRadius: 'var(--radius-md)',
                    textAlign: 'center',
                  }}
                >
                  <p style={{ color: 'var(--slate-600)', fontSize: '0.875rem', marginBottom: '12px' }}>
                    No slots are currently available on <strong>{appointmentDate}</strong>. All slots may be filled or the doctor is off-duty.
                  </p>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={handleJoinWaitlist}
                    disabled={waitlistLoading || waitlistSuccess}
                    style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                  >
                    <ListPlus size={15} />
                    {waitlistLoading ? 'Joining...' : waitlistSuccess ? 'Added to Waitlist' : 'Join Priority Waitlist'}
                  </button>
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
