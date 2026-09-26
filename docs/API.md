# CarePulse Portal – REST API Specification

This document provides a comprehensive reference for the CarePulse Portal REST API. All endpoints (except public authentication and discovery routes) require a JWT Bearer token passed in the `Authorization` header:

```http
Authorization: Bearer <jwt-token>
```

---

## 📑 API Table of Contents
1. [Authentication Endpoints (`/api/auth`)](#1-authentication-endpoints)
2. [Doctor Discovery & Availability (`/api/doctors`)](#2-doctor-discovery--availability)
3. [Appointment Coordination (`/api/appointments`)](#3-appointment-coordination)
4. [Medical Records Timeline (`/api/records`)](#4-medical-records-timeline)
5. [Digital Prescriptions (`/api/prescriptions`)](#5-digital-prescriptions)
6. [Caregiver Proxy Access (`/api/caregivers`)](#6-caregiver-proxy-access)
7. [Notifications (`/api/notifications`)](#7-notifications)
8. [AI Health Companion (`/api/ai`)](#8-ai-health-companion)
9. [Platform Administration & Auditing (`/api/admin` & `/api/audit-logs`)](#9-platform-administration--auditing)

---

## 1. Authentication Endpoints

### `POST /api/auth/login`
Authenticates a user and issues a signed JWT token.
* **Auth Requirement:** None (Public)
* **Allowed Roles:** Any
* **Request Example:**
```json
{
  "email": "john.doe@example.com",
  "password": "Patient@123"
}
```
* **Response Example (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImlhdCI6MTc...",
  "type": "Bearer",
  "id": 1,
  "email": "john.doe@example.com",
  "fullName": "John Doe",
  "role": "PATIENT",
  "communicationPreference": "SUPPORTIVE"
}
```

---

### `POST /api/auth/register/patient`
Registers a new patient account and profile.
* **Auth Requirement:** None (Public)
* **Allowed Roles:** Any
* **Request Example:**
```json
{
  "email": "sarah.connor@example.com",
  "password": "SecurePassword@123",
  "fullName": "Sarah Connor",
  "phone": "+1-555-0199",
  "address": "742 Evergreen Terrace, Springfield",
  "dateOfBirth": "1994-05-12",
  "gender": "Female",
  "bloodGroup": "O+",
  "emergencyContact": "+1-555-0198",
  "allergies": "Penicillin",
  "medicalHistorySummary": "Mild seasonal asthma"
}
```
* **Response Example (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 8,
  "email": "sarah.connor@example.com",
  "fullName": "Sarah Connor",
  "role": "PATIENT",
  "communicationPreference": "SUPPORTIVE"
}
```

---

### `POST /api/auth/register/doctor`
Registers a new physician profile.
* **Auth Requirement:** None (Public)
* **Allowed Roles:** Any
* **Request Example:**
```json
{
  "email": "dr.smith@carepulse.com",
  "password": "DoctorPassword@123",
  "fullName": "Dr. Gregory Smith",
  "phone": "+1-555-0188",
  "specialization": "Dermatology",
  "licenseNumber": "MD-987654",
  "qualification": "MD, Dermatology (Hopkins)",
  "yearsOfExperience": 10,
  "consultationFee": 1200.00,
  "hospitalAffiliation": "Metropolitan Health",
  "bio": "Specializing in clinical dermatology and laser therapeutics."
}
```
* **Response Example (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "id": 9,
  "email": "dr.smith@carepulse.com",
  "fullName": "Dr. Gregory Smith",
  "role": "DOCTOR",
  "communicationPreference": "PROFESSIONAL"
}
```

---

### `PUT /api/auth/communication-preference`
Updates the patient's Empathy Engine tone preference.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`, `DOCTOR`, `ADMIN`
* **Request Example:**
```json
{
  "preference": "SIMPLE"
}
```
* **Response Example (200 OK):**
```json
{
  "message": "Communication preference updated successfully to SIMPLE"
}
```

---

## 2. Doctor Discovery & Availability

### `GET /api/doctors`
Search and filter verified physicians.
* **Auth Requirement:** None (Public or Authenticated)
* **Query Parameters:** `specialization` (optional), `query` (optional name search)
* **Response Example (200 OK):**
```json
[
  {
    "id": 1,
    "user": {
      "id": 2,
      "email": "dr.jenkins@carepulse.com",
      "fullName": "Dr. Sarah Jenkins",
      "phone": "+1-555-0111"
    },
    "specialization": "Cardiology",
    "licenseNumber": "MD-884920",
    "qualification": "MD, FACC (Harvard Medical)",
    "yearsOfExperience": 12,
    "consultationFee": 1500.00,
    "hospitalAffiliation": "St. Jude Medical Center",
    "isVerified": true,
    "rating": 4.9
  }
]
```

---

### `GET /api/doctors/{id}/slots`
Calculates dynamic, conflict-free consultation time slots for a given doctor and date.
* **Auth Requirement:** None (Public or Authenticated)
* **Query Parameters:** `date` (format: `YYYY-MM-DD`, required)
* **Response Example (200 OK):**
```json
[
  "09:00:00",
  "09:30:00",
  "10:00:00",
  "10:30:00",
  "11:00:00",
  "14:00:00",
  "14:30:00",
  "15:00:00"
]
```

---

## 3. Appointment Coordination

### `POST /api/appointments`
Books a new consultation slot with double-booking collision detection.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`, `ADMIN`
* **Request Example:**
```json
{
  "doctorId": 1,
  "appointmentDate": "2026-10-05",
  "appointmentTime": "10:30:00",
  "reason": "Routine hypertension checkup and prescription refill"
}
```
* **Response Example (200 OK):**
```json
{
  "id": 6,
  "patient": {
    "id": 1,
    "fullName": "John Doe"
  },
  "doctor": {
    "id": 1,
    "specialization": "Cardiology",
    "user": { "fullName": "Dr. Sarah Jenkins" }
  },
  "appointmentDate": "2026-10-05",
  "appointmentTime": "10:30:00",
  "status": "PENDING",
  "reason": "Routine hypertension checkup and prescription refill",
  "createdAt": "2026-09-26T19:38:32.482"
}
```

---

### `GET /api/appointments/my`
Retrieves appointments for the authenticated caller (patients see their bookings; doctors see their scheduled consultations).
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`, `DOCTOR`, `ADMIN`
* **Response Example (200 OK):**
```json
[
  {
    "id": 6,
    "appointmentDate": "2026-10-05",
    "appointmentTime": "10:30:00",
    "status": "PENDING",
    "reason": "Routine hypertension checkup and prescription refill"
  }
]
```

---

### `PUT /api/appointments/{id}/status`
Updates an appointment status (`CONFIRMED`, `COMPLETED`, `CANCELLED`).
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `DOCTOR`, `ADMIN`
* **Request Example:**
```json
{
  "status": "CONFIRMED",
  "doctorNotes": "Confirmed. Please bring recent blood pressure log."
}
```
* **Response Example (200 OK):**
```json
{
  "id": 6,
  "status": "CONFIRMED",
  "doctorNotes": "Confirmed. Please bring recent blood pressure log."
}
```

---

## 4. Medical Records Timeline

### `GET /api/records/my`
Retrieves the authenticated user's complete clinical timeline.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`, `DOCTOR`
* **Response Example (200 OK):**
```json
[
  {
    "id": 1,
    "recordDate": "2026-09-15",
    "diagnosis": "Essential (Primary) Hypertension",
    "symptoms": "Mild afternoon headaches, elevated resting blood pressure (145/92 mmHg)",
    "treatment": "Lifestyle modification (DASH diet) and daily oral Lisinopril",
    "consultationNotes": "Patient responded favorably to initial trial. Target BP < 130/80 mmHg.",
    "doctor": {
      "user": { "fullName": "Dr. Sarah Jenkins" }
    }
  }
]
```

---

### `POST /api/records`
Authors a new clinical record entry.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `DOCTOR`, `ADMIN`
* **Request Example:**
```json
{
  "patientId": 1,
  "recordDate": "2026-09-26",
  "diagnosis": "Follow-up: Stable Hypertension",
  "symptoms": "No acute complaints, BP normalized to 124/82 mmHg",
  "treatment": "Continue Lisinopril 10mg once daily",
  "consultationNotes": "Excellent compliance. Schedule next routine visit in 6 months."
}
```
* **Response Example (200 OK):**
```json
{
  "id": 4,
  "recordDate": "2026-09-26",
  "diagnosis": "Follow-up: Stable Hypertension",
  "treatment": "Continue Lisinopril 10mg once daily"
}
```

---

## 5. Digital Prescriptions

### `GET /api/prescriptions/my`
Retrieves all active prescriptions issued to the authenticated patient.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`, `DOCTOR`
* **Response Example (200 OK):**
```json
[
  {
    "id": 1,
    "medicineName": "Lisinopril",
    "dosage": "10mg",
    "frequency": "Once daily every morning",
    "duration": "90 days",
    "instructions": "Take with a full glass of water. Monitor blood pressure weekly.",
    "issuedDate": "2026-09-15",
    "doctor": {
      "user": { "fullName": "Dr. Sarah Jenkins" }
    }
  }
]
```

---

### `POST /api/prescriptions`
Prescribes a medication regimen to a patient.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `DOCTOR`, `ADMIN`
* **Request Example:**
```json
{
  "patientId": 1,
  "medicalRecordId": 1,
  "medicineName": "Atorvastatin",
  "dosage": "20mg",
  "frequency": "Once daily at bedtime",
  "duration": "90 days",
  "instructions": "Take at evening with or without food. Avoid grapefruit."
}
```
* **Response Example (200 OK):**
```json
{
  "id": 5,
  "medicineName": "Atorvastatin",
  "dosage": "20mg",
  "issuedDate": "2026-09-26"
}
```

---

## 6. Caregiver Proxy Access

### `POST /api/caregivers`
Delegates time-limited proxy access to a trusted relative or caregiver.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`
* **Request Example:**
```json
{
  "caregiverEmail": "emma.watson@example.com",
  "relationship": "Daughter",
  "permissions": "READ_RECORDS_APPOINTMENTS",
  "expiresAt": "2027-09-26T00:00:00"
}
```
* **Response Example (200 OK):**
```json
{
  "id": 2,
  "relationship": "Daughter",
  "permissions": "READ_RECORDS_APPOINTMENTS",
  "isActive": true,
  "grantedAt": "2026-09-26T19:40:00"
}
```

---

### `DELETE /api/caregivers/{id}`
Revokes an existing caregiver proxy access delegation.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`
* **Response Example (200 OK):**
```json
{
  "message": "Caregiver proxy access successfully revoked"
}
```

---

## 7. Notifications

### `GET /api/notifications`
Fetches user notifications.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** Any Authenticated User
* **Response Example (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Appointment Confirmed",
    "message": "Your appointment on Oct 05, 2026 at 10:30 is confirmed.",
    "type": "APPOINTMENT_CONFIRMED",
    "isRead": false,
    "createdAt": "2026-09-26T19:38:32"
  }
]
```

---

## 8. AI Health Companion

### `POST /api/ai/chat`
Interacts with the AI companion. Formats output according to user empathy preference (`SIMPLE`, `SUPPORTIVE`, `PROFESSIONAL`) and attaches medical disclaimers.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`, `DOCTOR`, `ADMIN`
* **Request Example:**
```json
{
  "message": "I have had a mild headache for 2 days. What should I do?"
}
```
* **Response Example (200 OK):**
```json
{
  "response": "I hear that you've been experiencing a mild headache. Staying well-hydrated, resting in a quiet dim room, and taking breaks from digital screens can often provide relief. If the headache worsens, is accompanied by nausea or visual changes, please consult your physician.",
  "tone": "SUPPORTIVE",
  "disclaimer": "This AI provides general informational support and does not provide medical diagnosis or replace professional medical advice. Always consult a certified healthcare professional."
}
```

---

## 9. Platform Administration & Auditing

### `GET /api/admin/dashboard`
Aggregates high-level system KPIs.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`
* **Response Example (200 OK):**
```json
{
  "totalPatients": 3,
  "totalDoctors": 3,
  "totalAppointments": 5,
  "activeUsers": 7,
  "appointmentsByStatus": {
    "PENDING": 1,
    "CONFIRMED": 3,
    "COMPLETED": 1,
    "CANCELLED": 0
  }
}
```

---

### `GET /api/admin/users`
Returns all registered system user accounts.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`
* **Response Example (200 OK):**
```json
[
  {
    "id": 1,
    "email": "admin@carepulse.com",
    "fullName": "System Administrator",
    "role": "ADMIN",
    "active": true
  }
]
```

---

### `GET /api/audit-logs`
Retrieves immutable system audit records.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`
* **Response Example (200 OK):**
```json
[
  {
    "id": 1,
    "userEmail": "john.doe@example.com",
    "action": "USER_LOGIN",
    "entityName": "User",
    "entityId": 4,
    "details": "User successfully logged in via credentials",
    "timestamp": "2026-09-26T19:33:51.291"
  }
]
```
