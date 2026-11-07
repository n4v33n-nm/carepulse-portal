-- =============================================================================
-- CarePulse Portal - Relational Database Schema
-- Target Engine: PostgreSQL 14+ / 16+ / 18+
-- Database: carepulse
-- =============================================================================

-- 1. Users Table (Core Identity & RBAC)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL CHECK (role IN ('PATIENT', 'DOCTOR', 'ADMIN')),
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
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30),
    date_of_birth DATE,
    gender VARCHAR(20),
    blood_group VARCHAR(10),
    address VARCHAR(255),
    emergency_contact VARCHAR(100),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_patients_user_id ON patients(user_id);

-- 3. Doctors Table (Physician Credentials & Clinical Specialization)
CREATE TABLE IF NOT EXISTS doctors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(120) NOT NULL,
    phone VARCHAR(30),
    specialization VARCHAR(100) NOT NULL,
    qualification VARCHAR(100),
    experience_years INT DEFAULT 0,
    bio TEXT,
    consultation_fee DOUBLE PRECISION DEFAULT 800.00,
    hospital_affiliation VARCHAR(150) DEFAULT 'CarePulse Medical Center',
    rating DOUBLE PRECISION DEFAULT 4.8,
    availability_status VARCHAR(30) DEFAULT 'AVAILABLE',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_doctors_user_id ON doctors(user_id);
CREATE INDEX IF NOT EXISTS idx_doctors_specialization ON doctors(specialization);

-- 4. Doctor Availability (Weekly Consultation Shifts)
CREATE TABLE IF NOT EXISTS doctor_availability (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    day_of_week VARCHAR(20) NOT NULL CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time TIME WITHOUT TIME ZONE NOT NULL,
    end_time TIME WITHOUT TIME ZONE NOT NULL,
    break_start_time TIME WITHOUT TIME ZONE,
    break_end_time TIME WITHOUT TIME ZONE,
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
    consultation_notes TEXT,
    cancellation_reason VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_doctor_date ON appointments(doctor_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON appointments(status);

-- Database-level protection preventing concurrent double-booking of doctor slot:
CREATE UNIQUE INDEX IF NOT EXISTS uq_doctor_date_time_active 
    ON appointments (doctor_id, appointment_date, appointment_time) 
    WHERE status != 'CANCELLED';

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
CREATE INDEX IF NOT EXISTS idx_records_doctor ON medical_records(doctor_id);

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
CREATE INDEX IF NOT EXISTS idx_prescriptions_doctor ON prescriptions(doctor_id);

-- 8. Caregiver Access Table (Proxy Delegations)
CREATE TABLE IF NOT EXISTS caregiver_access (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    caregiver_email VARCHAR(120) NOT NULL,
    caregiver_name VARCHAR(120),
    relationship VARCHAR(60),
    permission VARCHAR(50) NOT NULL DEFAULT 'VIEW_ALL',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    granted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_caregiver_patient ON caregiver_access(patient_id);
CREATE INDEX IF NOT EXISTS idx_caregiver_email ON caregiver_access(caregiver_email);

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
    user_email VARCHAR(120),
    action VARCHAR(100) NOT NULL,
    target_resource VARCHAR(120),
    details TEXT,
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

CREATE INDEX IF NOT EXISTS idx_edr_doctor_id ON emergency_doctor_roster(doctor_id);
CREATE INDEX IF NOT EXISTS idx_edr_roster_date ON emergency_doctor_roster(roster_date);
CREATE INDEX IF NOT EXISTS idx_edr_duty_status ON emergency_doctor_roster(duty_status);
CREATE INDEX IF NOT EXISTS idx_edr_avail_status ON emergency_doctor_roster(doctor_availability_status);

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

CREATE INDEX IF NOT EXISTS idx_er_patient_id ON emergency_requests(patient_id);
CREATE INDEX IF NOT EXISTS idx_er_assigned_doctor_id ON emergency_requests(assigned_doctor_id);
CREATE INDEX IF NOT EXISTS idx_er_status ON emergency_requests(status);
CREATE INDEX IF NOT EXISTS idx_er_priority ON emergency_requests(priority);
CREATE INDEX IF NOT EXISTS idx_er_request_time ON emergency_requests(request_time DESC);
CREATE INDEX IF NOT EXISTS idx_er_created_at ON emergency_requests(created_at DESC);

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

-- 14. DoctorConnect Conversations (Patient-Physician Asynchronous Consultation Inquiries)
CREATE TABLE IF NOT EXISTS conversations (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE CASCADE,
    appointment_id BIGINT REFERENCES appointments(id) ON DELETE SET NULL,
    subject VARCHAR(200) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ANSWERED', 'RESOLVED')),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conv_patient ON conversations(patient_id);
CREATE INDEX IF NOT EXISTS idx_conv_doctor ON conversations(doctor_id);
CREATE INDEX IF NOT EXISTS idx_conv_status ON conversations(status);
CREATE INDEX IF NOT EXISTS idx_conv_updated_at ON conversations(updated_at DESC);

-- 15. DoctorConnect Conversation Messages (Threaded Messaging & Clinical Responses)
CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_role VARCHAR(30) NOT NULL CHECK (sender_role IN ('PATIENT', 'DOCTOR', 'ADMIN')),
    sender_name VARCHAR(120) NOT NULL,
    content TEXT NOT NULL,
    sent_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_msg_conv ON conversation_messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_msg_sent_at ON conversation_messages(sent_at ASC);
