import React from 'react';
import { Link } from 'react-router-dom';
import {
  Activity,
  ShieldCheck,
  Bot,
  Calendar,
  FileText,
  Users,
  HeartPulse,
  Clock,
  Award,
  ChevronRight,
  ArrowRight,
  CheckCircle2,
  Lock,
  Pill,
} from 'lucide-react';

const LandingPage = () => {
  return (
    <div style={{ background: '#ffffff', minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <header
        style={{
          borderBottom: '1px solid var(--slate-200)',
          padding: '16px 40px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          position: 'sticky',
          top: 0,
          background: 'rgba(255, 255, 255, 0.92)',
          backdropFilter: 'blur(8px)',
          zIndex: 50,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <div className="brand-icon">
            <Activity size={22} strokeWidth={2.5} />
          </div>
          <div>
            <span style={{ fontSize: '1.35rem', fontWeight: 800, color: 'var(--slate-900)', fontFamily: 'var(--font-heading)' }}>
              CarePulse <span style={{ color: 'var(--primary-600)' }}>Portal</span>
            </span>
          </div>
        </div>

        <nav style={{ display: 'flex', alignItems: 'center', gap: '28px' }}>
          <a href="#about" style={{ color: 'var(--slate-600)', fontWeight: 500, fontSize: '0.9375rem' }}>About</a>
          <a href="#features" style={{ color: 'var(--slate-600)', fontWeight: 500, fontSize: '0.9375rem' }}>Features</a>
          <a href="#ai-companion" style={{ color: 'var(--slate-600)', fontWeight: 500, fontSize: '0.9375rem' }}>AI Companion</a>
          <a href="#security" style={{ color: 'var(--slate-600)', fontWeight: 500, fontSize: '0.9375rem' }}>Security</a>
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <Link to="/login" className="btn btn-secondary btn-sm" style={{ padding: '8px 18px' }}>
            Login
          </Link>
          <Link to="/register" className="btn btn-primary btn-sm" style={{ padding: '8px 18px' }}>
            Get Started <ArrowRight size={14} />
          </Link>
        </div>
      </header>

      {/* Hero Section */}
      <section
        style={{
          padding: '80px 40px 100px',
          background: 'linear-gradient(180deg, #f0f9ff 0%, #ffffff 100%)',
          textAlign: 'center',
          position: 'relative',
          overflow: 'hidden',
        }}
      >
        <div style={{ maxWidth: '900px', margin: '0 auto' }}>
          <div
            style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: '8px',
              background: 'white',
              border: '1px solid var(--primary-200)',
              padding: '6px 16px',
              borderRadius: 'var(--radius-full)',
              color: 'var(--primary-700)',
              fontSize: '0.875rem',
              fontWeight: 600,
              marginBottom: '24px',
              boxShadow: 'var(--shadow-sm)',
            }}
          >
            <HeartPulse size={16} /> Intelligent Healthcare Coordination Platform
          </div>

          <h1
            style={{
              fontSize: '3.5rem',
              fontWeight: 800,
              letterSpacing: '-0.03em',
              color: 'var(--slate-900)',
              marginBottom: '20px',
              lineHeight: 1.15,
            }}
          >
            CarePulse Portal
          </h1>

          <p
            style={{
              fontSize: '1.35rem',
              color: 'var(--slate-600)',
              marginBottom: '36px',
              fontWeight: 400,
              maxWidth: '720px',
              margin: '0 auto 36px',
            }}
          >
            Intelligent Healthcare Coordination, Connected Around You. Connecting patients, board-certified doctors, and caregivers with unified health timelines, smart prep, and digital prescriptions.
          </p>

          <div style={{ display: 'flex', justifyContent: 'center', gap: '16px', flexWrap: 'wrap' }}>
            <Link to="/register" className="btn btn-primary" style={{ padding: '14px 28px', fontSize: '1rem' }}>
              Create Free Account <ChevronRight size={18} />
            </Link>
            <Link to="/login" className="btn btn-secondary" style={{ padding: '14px 28px', fontSize: '1rem' }}>
              Explore Demo Logins
            </Link>
          </div>

          {/* Quick Demo Credential Pills */}
          <div
            style={{
              marginTop: '40px',
              padding: '16px 20px',
              background: 'white',
              border: '1px solid var(--slate-200)',
              borderRadius: 'var(--radius-lg)',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '16px',
              boxShadow: 'var(--shadow-md)',
              fontSize: '0.875rem',
            }}
          >
            <span style={{ fontWeight: 700, color: 'var(--slate-700)' }}>Demo Access:</span>
            <span style={{ background: 'var(--slate-100)', padding: '4px 10px', borderRadius: '6px' }}>
              <strong>Patient:</strong> john.doe@example.com
            </span>
            <span style={{ background: 'var(--slate-100)', padding: '4px 10px', borderRadius: '6px' }}>
              <strong>Doctor:</strong> dr.jenkins@carepulse.com
            </span>
            <span style={{ background: 'var(--slate-100)', padding: '4px 10px', borderRadius: '6px' }}>
              <strong>Admin:</strong> admin@carepulse.com
            </span>
          </div>
        </div>
      </section>

      {/* About Section */}
      <section id="about" style={{ padding: '80px 40px', background: '#ffffff', maxWidth: '1200px', margin: '0 auto' }}>
        <div style={{ textAlign: 'center', marginBottom: '60px' }}>
          <h2 style={{ fontSize: '2.25rem', marginBottom: '16px' }}>About CarePulse Portal</h2>
          <p style={{ color: 'var(--slate-600)', maxWidth: '700px', margin: '0 auto', fontSize: '1.1rem' }}>
            CarePulse is an enterprise-grade coordination system designed to remove friction from clinical workflows. By bridging patient records, doctor scheduling, and family caregiver access, medical consultations become collaborative, precise, and organized.
          </p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '30px' }}>
          <div className="card" style={{ padding: '32px' }}>
            <div className="stat-icon-wrapper stat-icon-primary" style={{ marginBottom: '20px' }}>
              <Users size={24} />
            </div>
            <h3 style={{ fontSize: '1.25rem', marginBottom: '12px' }}>Patient Empowerment</h3>
            <p style={{ color: 'var(--slate-600)', fontSize: '0.9375rem', lineHeight: 1.6 }}>
              Patients maintain sovereign control over their health timelines, active prescriptions, caregiver delegate permissions, and appointment schedule with zero ambiguity.
            </p>
          </div>

          <div className="card" style={{ padding: '32px' }}>
            <div className="stat-icon-wrapper stat-icon-teal" style={{ marginBottom: '20px' }}>
              <Clock size={24} />
            </div>
            <h3 style={{ fontSize: '1.25rem', marginBottom: '12px' }}>Physician Workflow</h3>
            <p style={{ color: 'var(--slate-600)', fontSize: '0.9375rem', lineHeight: 1.6 }}>
              Doctors configure their exact availability schedule, accept or reschedule consultations, write digital structured prescriptions, and log clinical diagnostic notes effortlessly.
            </p>
          </div>

          <div className="card" style={{ padding: '32px' }}>
            <div className="stat-icon-wrapper stat-icon-indigo" style={{ marginBottom: '20px' }}>
              <ShieldCheck size={24} />
            </div>
            <h3 style={{ fontSize: '1.25rem', marginBottom: '12px' }}>Security & Auditability</h3>
            <p style={{ color: 'var(--slate-600)', fontSize: '0.9375rem', lineHeight: 1.6 }}>
              Role-Based Access Control (RBAC), BCrypt password cryptography, and continuous immutable audit logs track logins, record inspections, and medication approvals.
            </p>
          </div>
        </div>
      </section>

      {/* Key Features Section */}
      <section id="features" style={{ padding: '80px 40px', background: 'var(--slate-50)' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '60px' }}>
            <h2 style={{ fontSize: '2.25rem', marginBottom: '16px' }}>Key Capabilities</h2>
            <p style={{ color: 'var(--slate-600)', maxWidth: '650px', margin: '0 auto' }}>
              Built from the ground up for reliable real-world clinical coordination.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(270px, 1fr))', gap: '24px' }}>
            {[
              { icon: <Calendar />, title: 'Doctor Discovery & Booking', desc: 'Filter specialists by Cardiology, Dermatology, or General Medicine. Book real-time available 30-min slots with conflict prevention.' },
              { icon: <FileText />, title: 'Unified Medical Records', desc: 'Chronological health timeline tracking patient symptoms, clinical assessments, treatment protocols, and consultation history.' },
              { icon: <Pill />, title: 'Digital Prescriptions', desc: 'Structured medical cards with dosage, frequency, course duration, and pharmacist/patient follow-up notes.' },
              { icon: <Bot />, title: 'AI Health Companion', desc: 'Guidance engine assisting patients in organizing symptoms, generating consultation questions, and understanding preparation.' },
              { icon: <Users />, title: 'Caregiver Delegations', desc: 'Secure proxy permissions allowing family members to view appointments or records while patient retains revoke controls.' },
              { icon: <ShieldCheck />, title: 'Empathy Engine', desc: 'Personalized communication preference (Simple, Supportive, Professional) adapting notification and assistance tone.' },
            ].map((f, i) => (
              <div key={i} className="card" style={{ padding: '24px' }}>
                <div style={{ color: 'var(--primary-600)', marginBottom: '16px' }}>{f.icon}</div>
                <h4 style={{ fontSize: '1.1rem', marginBottom: '8px' }}>{f.title}</h4>
                <p style={{ color: 'var(--slate-600)', fontSize: '0.875rem', lineHeight: 1.5 }}>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* AI Health Companion Section with Mandatory Safety Disclaimer */}
      <section id="ai-companion" style={{ padding: '80px 40px', background: '#ffffff', maxWidth: '1100px', margin: '0 auto' }}>
        <div
          style={{
            background: 'linear-gradient(135deg, var(--slate-900), var(--primary-900))',
            borderRadius: 'var(--radius-xl)',
            padding: '50px',
            color: 'white',
            display: 'grid',
            gridTemplateColumns: '1.2fr 1fr',
            gap: '40px',
            alignItems: 'center',
          }}
        >
          <div>
            <div
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                background: 'rgba(255, 255, 255, 0.1)',
                padding: '6px 14px',
                borderRadius: 'var(--radius-full)',
                fontSize: '0.8125rem',
                fontWeight: 600,
                marginBottom: '20px',
              }}
            >
              <Bot size={16} /> Clinical Prep Assistant
            </div>
            <h2 style={{ color: 'white', fontSize: '2.25rem', marginBottom: '18px' }}>
              Meet your AI Health Companion
            </h2>
            <p style={{ color: 'var(--slate-300)', fontSize: '1rem', lineHeight: 1.6, marginBottom: '24px' }}>
              Never feel unprepared for a medical visit again. Your companion helps you organize medical questions, build symptom timelines, and learn what information to tell your doctor.
            </p>
            <div
              style={{
                background: 'rgba(254, 240, 138, 0.15)',
                border: '1px solid rgba(254, 240, 138, 0.3)',
                padding: '12px 16px',
                borderRadius: 'var(--radius-md)',
                color: '#fef08a',
                fontSize: '0.8125rem',
                lineHeight: 1.4,
              }}
            >
              <strong>Safety Guardrail:</strong> This AI provides general informational support and does not provide medical diagnosis or replace professional medical advice.
            </div>
          </div>

          <div
            style={{
              background: 'white',
              borderRadius: 'var(--radius-lg)',
              padding: '24px',
              color: 'var(--slate-800)',
              boxShadow: 'var(--shadow-xl)',
            }}
          >
            <div style={{ fontWeight: 700, fontSize: '0.9375rem', marginBottom: '14px', color: 'var(--slate-900)' }}>
              Interactive Consultation Assistant Preview
            </div>
            <div
              style={{
                background: 'var(--primary-50)',
                padding: '12px 16px',
                borderRadius: '8px',
                fontSize: '0.875rem',
                color: 'var(--primary-900)',
                marginBottom: '12px',
              }}
            >
              "What should I prepare before my cardiology consultation?"
            </div>
            <div
              style={{
                background: 'var(--slate-100)',
                padding: '12px 16px',
                borderRadius: '8px',
                fontSize: '0.875rem',
                color: 'var(--slate-700)',
                lineHeight: 1.4,
              }}
            >
              • List your current blood pressure readings.<br />
              • Bring all active prescription bottles.<br />
              • Note what time palpitations or fatigue occur.<br />
              • Prioritize 2-3 key questions for Dr. Jenkins.
            </div>
          </div>
        </div>
      </section>

      {/* Security Section */}
      <section id="security" style={{ padding: '80px 40px', background: 'var(--slate-50)' }}>
        <div style={{ maxWidth: '900px', margin: '0 auto', textAlign: 'center' }}>
          <div className="brand-icon" style={{ margin: '0 auto 20px', width: '48px', height: '48px' }}>
            <Lock size={24} />
          </div>
          <h2 style={{ fontSize: '2.25rem', marginBottom: '16px' }}>Enterprise Healthcare Security</h2>
          <p style={{ color: 'var(--slate-600)', fontSize: '1.1rem', marginBottom: '40px' }}>
            CarePulse implements defense-in-depth architecture.
          </p>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '20px', textAlign: 'left' }}>
            <div className="card" style={{ padding: '20px' }}>
              <CheckCircle2 size={20} style={{ color: 'var(--teal-600)', marginBottom: '8px' }} />
              <h4 style={{ fontSize: '1rem', marginBottom: '4px' }}>BCrypt Hashing</h4>
              <p style={{ fontSize: '0.8125rem', color: 'var(--slate-600)' }}>Passwords salted and cryptographically hashed before persistent storage.</p>
            </div>
            <div className="card" style={{ padding: '20px' }}>
              <CheckCircle2 size={20} style={{ color: 'var(--teal-600)', marginBottom: '8px' }} />
              <h4 style={{ fontSize: '1rem', marginBottom: '4px' }}>JWT Bearer Auth</h4>
              <p style={{ fontSize: '0.8125rem', color: 'var(--slate-600)' }}>HMAC-SHA-256 tokens validate requests statelessly across Spring Security.</p>
            </div>
            <div className="card" style={{ padding: '20px' }}>
              <CheckCircle2 size={20} style={{ color: 'var(--teal-600)', marginBottom: '8px' }} />
              <h4 style={{ fontSize: '1rem', marginBottom: '4px' }}>Immutable Audit Logs</h4>
              <p style={{ fontSize: '0.8125rem', color: 'var(--slate-600)' }}>Access, appointment bookings, and record views are logged with timestamps.</p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer
        style={{
          borderTop: '1px solid var(--slate-200)',
          padding: '40px',
          background: 'white',
          textAlign: 'center',
          color: 'var(--slate-500)',
          fontSize: '0.875rem',
          marginTop: 'auto',
        }}
      >
        <p>© 2026 CarePulse Portal – Intelligent Healthcare Coordination Platform. All rights reserved.</p>
        <p style={{ marginTop: '8px', fontSize: '0.75rem', color: 'var(--slate-400)' }}>
          Engineering Prototype designed for Academic & Professional Demonstration.
        </p>
      </footer>
    </div>
  );
};

export default LandingPage;
