-- =============================================================================
-- CarePulse Portal - Relational Database Schema
-- Target Engine: PostgreSQL 14+ / 16+
-- Database: carepulse
-- =============================================================================

-- Create database if not exists
-- CREATE DATABASE carepulse;
-- \c carepulse;

-- 1. Users Table (Core Identity & RBAC)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('PATIENT', 'DOCTOR', 'ADMIN')),
    phone VARCHAR(30),
    address VARCHAR(255),
    communication_preference VARCHAR(30) DEFAULT 'SUPPORTIVE' CHECK (communication_preference IN ('SIMPLE', 'SUPPORTIVE', 'PROFESSIONAL')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- 2. Patients Table (Patient Demographics & Medical Profile)
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    date_of_birth DATE,
    gender VARCHAR(20),
    blood_group VARCHAR(10),
    emergency_contact VARCHAR(60),
    medical_history_summary TEXT,
    allergies TEXT,
    insurance_provider VARCHAR(100),
    insurance_policy_number VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_patients_user_id ON patients(user_id);

-- 3. Doctors Table (Physician Credentials & Clinical Specialization)
CREATE TABLE IF NOT EXISTS doctors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    specialization VARCHAR(100) NOT NULL,
    license_number VARCHAR(60) NOT NULL UNIQUE,
    qualification VARCHAR(120),
    years_of_experience INT DEFAULT 0,
    bio TEXT,
    consultation_fee NUMERIC(10, 2) DEFAULT 800.00,
    hospital_affiliation VARCHAR(150),
    is_verified BOOLEAN NOT NULL DEFAULT TRUE,
    rating DOUBLE PRECISION DEFAULT 4.9,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_doctors_specialization ON doctors(specialization);

-- 4. Doctor Availability (Weekly Consultation Shifts)
CREATE TABLE IF NOT EXISTS doctor_availability (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    day_of_week VARCHAR(15) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    slot_duration_minutes INT NOT NULL DEFAULT 30,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_doctor_day UNIQUE (doctor_id, day_of_week)
);

CREATE INDEX IF NOT EXISTS idx_availability_doctor ON doctor_availability(doctor_id);

-- 5. Appointments Table (Scheduling & Status Lifecycle)
CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    appointment_date DATE NOT NULL,
    appointment_time TIME WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    reason VARCHAR(255) NOT NULL,
    doctor_notes TEXT,
    cancellation_reason VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_date ON appointments(doctor_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status);

-- 6. Medical Records Table (Clinical History & Longitudinal Timeline)
CREATE TABLE IF NOT EXISTS medical_records (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    record_date DATE NOT NULL,
    diagnosis VARCHAR(200) NOT NULL,
    symptoms TEXT,
    treatment TEXT,
    consultation_notes TEXT,
    clinical_summary TEXT,
    ai_draft_summary TEXT,
    summary_status VARCHAR(30) DEFAULT 'APPROVED' CHECK (summary_status IN ('APPROVED', 'PENDING_REVIEW', 'REJECTED')),
    attachments VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_records_patient ON medical_records(patient_id);

-- 7. Prescriptions Table (Digital Medications & Instructions)
CREATE TABLE IF NOT EXISTS prescriptions (
    id BIGSERIAL PRIMARY KEY,
    medical_record_id BIGINT REFERENCES medical_records(id) ON DELETE SET NULL,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    medicine_name VARCHAR(120) NOT NULL,
    dosage VARCHAR(60) NOT NULL,
    frequency VARCHAR(60) NOT NULL,
    duration VARCHAR(60) NOT NULL,
    instructions TEXT,
    issued_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prescriptions_patient ON prescriptions(patient_id);

-- 8. Caregiver Access Table (Proxy Delegations)
CREATE TABLE IF NOT EXISTS caregiver_access (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    caregiver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    relationship VARCHAR(60) NOT NULL,
    permissions VARCHAR(50) NOT NULL DEFAULT 'READ_ONLY',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    granted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_caregiver_patient ON caregiver_access(patient_id);
CREATE INDEX IF NOT EXISTS idx_caregiver_user ON caregiver_access(caregiver_id);

-- 9. Notifications Table (System Alerts & Push Events)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'INFO',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    reference_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);

-- 10. Audit Logs Table (HIPAA-Ready Security Audit Trail)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    user_email VARCHAR(120),
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(60),
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_email ON audit_logs(user_email);

-- 11. Emergency Doctor Roster Table (Daily Dynamic Duty & Shift Allocation)
CREATE TABLE IF NOT EXISTS emergency_doctor_roster (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    roster_date DATE NOT NULL,
    shift_name VARCHAR(30) NOT NULL,
    shift_start TIME WITHOUT TIME ZONE NOT NULL,
    shift_end TIME WITHOUT TIME ZONE NOT NULL,
    duty_status VARCHAR(30) NOT NULL DEFAULT 'EMERGENCY_DUTY' CHECK (duty_status IN ('EMERGENCY_DUTY', 'NOT_ASSIGNED')),
    doctor_availability_status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE' CHECK (doctor_availability_status IN ('AVAILABLE', 'BUSY', 'IN_CONSULTATION', 'OFF_DUTY', 'ON_LEAVE')),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uq_emergency_roster_doc_date_shift UNIQUE (doctor_id, roster_date, shift_name)
);

CREATE INDEX IF NOT EXISTS idx_emergency_roster_date ON emergency_doctor_roster(roster_date);
CREATE INDEX IF NOT EXISTS idx_emergency_roster_doctor ON emergency_doctor_roster(doctor_id);
CREATE INDEX IF NOT EXISTS idx_emergency_roster_duty_status ON emergency_doctor_roster(duty_status);
CREATE INDEX IF NOT EXISTS idx_emergency_roster_avail_status ON emergency_doctor_roster(doctor_availability_status);
CREATE INDEX IF NOT EXISTS idx_emergency_roster_active_lookup ON emergency_doctor_roster(roster_date, duty_status, doctor_availability_status);

-- 12. Emergency Requests Table (Rapid Triage & Deterministic Physician Allocation)
CREATE TABLE IF NOT EXISTS emergency_requests (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    assigned_doctor_id BIGINT REFERENCES doctors(id) ON DELETE SET NULL,
    request_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_time TIMESTAMP WITHOUT TIME ZONE,
    started_time TIMESTAMP WITHOUT TIME ZONE,
    completed_time TIMESTAMP WITHOUT TIME ZONE,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_DOCTOR_AVAILABLE')),
    priority VARCHAR(30) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('NORMAL', 'URGENT', 'EMERGENCY')),
    category VARCHAR(50) DEFAULT 'General',
    description TEXT,
    doctor_notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_emergency_requests_patient ON emergency_requests(patient_id);
CREATE INDEX IF NOT EXISTS idx_emergency_requests_doctor ON emergency_requests(assigned_doctor_id);
CREATE INDEX IF NOT EXISTS idx_emergency_requests_status ON emergency_requests(status);
CREATE INDEX IF NOT EXISTS idx_emergency_requests_priority ON emergency_requests(priority);
CREATE INDEX IF NOT EXISTS idx_emergency_requests_time ON emergency_requests(request_time DESC);
CREATE INDEX IF NOT EXISTS idx_emergency_requests_created_at ON emergency_requests(created_at DESC);

-- 13. Appointment Waitlist (Intelligent Slot Reuse & Patient Notification)
CREATE TABLE IF NOT EXISTS appointment_waitlist (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id BIGINT REFERENCES doctors(id) ON DELETE CASCADE,
    specialization VARCHAR(100),
    preferred_date DATE NOT NULL,
    preferred_time TIME WITHOUT TIME ZONE,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING' CHECK (status IN ('WAITING', 'NOTIFIED', 'BOOKED', 'CANCELLED', 'EXPIRED')),
    notes TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_waitlist_patient ON appointment_waitlist(patient_id);
CREATE INDEX IF NOT EXISTS idx_waitlist_doctor_date ON appointment_waitlist(doctor_id, preferred_date);
CREATE INDEX IF NOT EXISTS idx_waitlist_status ON appointment_waitlist(status);

