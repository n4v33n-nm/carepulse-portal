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
10. [System Health & Liveness (`/api/health`)](#10-system-health--liveness)
11. [Error Handling & Security Architecture](#11-error-handling--security-architecture)

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

### `GET /api/doctors/{id}/available-slots`
Calculates dynamic, real-time consultation slots excluding booked appointments and past time blocks for today.
* **Auth Requirement:** None (Public or Authenticated)
* **Query Parameters:** `date` (format: `YYYY-MM-DD`, required)
* **Response Example (200 OK):**
```json
[
  "09:30:00",
  "10:00:00",
  "11:00:00",
  "14:30:00"
]
```

---

### `GET /api/doctors/match`
Intelligent, explainable doctor matching engine. Evaluates physician specialization, daily schedule, time-window availability, absence/leave records, existing appointment conflicts, and emergency duty commitments. Ranks candidates by lowest workload first.
* **Auth Requirement:** None (Public or Authenticated)
* **Query Parameters:**
  - `specialization` (required, e.g., `Cardiology`)
  - `date` (required, `YYYY-MM-DD`)
  - `time` (required, `HH:mm` or `HH:mm:ss`)
* **Response Example (200 OK):**
```json
[
  {
    "doctorId": 1,
    "fullName": "Dr. Sarah Jenkins",
    "email": "dr.jenkins@carepulse.com",
    "specialization": "Cardiology",
    "hospitalAffiliation": "St. Jude Medical Center",
    "consultationFee": 1500.0,
    "rating": 4.9,
    "available": true,
    "hasAppointmentConflict": false,
    "onEmergencyDuty": false,
    "currentAppointmentCount": 1,
    "currentEmergencyWorkload": 0,
    "totalWorkload": 1,
    "nextAvailableSlot": "11:00:00",
    "matchReasons": [
      "Cardiology Specialist",
      "Available at 10:30:00",
      "Zero Schedule Conflicts",
      "Current Workload: 1 appointment"
    ],
    "disclaimer": "This matching engine provides administrative scheduling recommendations based on doctor availability, specialization, and clinical workload. It does not provide medical diagnosis, clinical triage, or treatment recommendations."
  }
]
```

---

### `GET /api/doctors/workload`
Retrieves daily clinical workload metrics for all active physicians across scheduled appointments and emergency requests.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`
* **Query Parameters:** `date` (format: `YYYY-MM-DD`, optional, defaults to today)
* **Response Example (200 OK):**
```json
[
  {
    "doctorId": 1,
    "doctorName": "Dr. Sarah Jenkins",
    "specialization": "Cardiology",
    "date": "2026-09-27",
    "totalAppointments": 4,
    "pendingAppointments": 1,
    "confirmedAppointments": 2,
    "completedAppointments": 1,
    "cancelledAppointments": 0,
    "emergencyRequests": 1,
    "completedEmergencyRequests": 1,
    "activeEmergencyRequests": 0,
    "totalWorkload": 3,
    "currentAvailability": "AVAILABLE",
    "emergencyDutyToday": true,
    "emergencyShiftName": "MORNING",
    "emergencyShiftHours": "08:00 - 14:00"
  }
]
```

---

### `GET /api/doctors/{id}/workload`
Retrieves individual doctor clinical capacity and caseload distribution for a given date.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `DOCTOR`, `ADMIN`
* **Query Parameters:** `date` (format: `YYYY-MM-DD`, optional, defaults to today)

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

### `GET /api/appointments/available-slots`
Dynamic slot retrieval endpoint filtering doctor's schedule, existing appointments, and past slots for today.
* **Auth Requirement:** None (Public or Authenticated)
* **Query Parameters:**
  - `doctorId` (required, Long)
  - `date` (required, `YYYY-MM-DD`)
* **Response Example (200 OK):**
```json
[
  "09:30:00",
  "10:00:00",
  "11:00:00",
  "14:30:00"
]
```

---

### `POST /api/appointments/waitlist`
Places a patient on the priority waitlist when preferred consultation slots are fully booked.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`, `ADMIN`
* **Request Example:**
```json
{
  "doctorId": 1,
  "specialization": "Cardiology",
  "preferredDate": "2026-10-05",
  "preferredTime": "10:30:00"
}
```
* **Response Example (200 OK):**
```json
{
  "id": 1,
  "patientId": 1,
  "patientName": "John Doe",
  "doctorId": 1,
  "doctorName": "Dr. Sarah Jenkins",
  "specialization": "Cardiology",
  "preferredDate": "2026-10-05",
  "preferredTime": "10:30:00",
  "status": "WAITING",
  "createdAt": "2026-09-27T10:15:00"
}
```

---

### `GET /api/appointments/waitlist`
Retrieves all priority waitlist entries for the authenticated patient.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`, `ADMIN`
* **Response Example (200 OK):**
```json
[
  {
    "id": 1,
    "patientId": 1,
    "patientName": "John Doe",
    "doctorId": 1,
    "doctorName": "Dr. Sarah Jenkins",
    "specialization": "Cardiology",
    "preferredDate": "2026-10-05",
    "preferredTime": "10:30:00",
    "status": "NOTIFIED",
    "createdAt": "2026-09-27T10:15:00"
  }
]
```

---

### `DELETE /api/appointments/waitlist/{id}`
Cancels and withdraws an active priority waitlist entry (`status = CANCELLED`).
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`, `ADMIN`
* **Response Example (200 OK):**
```json
{
  "message": "Waitlist entry cancelled successfully"
}
```

---

### `GET /api/appointments/{id}/wait-time`
Calculates an explainable, data-grounded consultation wait time estimate based on real database records.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`, `DOCTOR`, `ADMIN`
* **Response Example (200 OK):**
```json
{
  "appointmentId": 6,
  "doctorId": 1,
  "doctorName": "Dr. Sarah Jenkins",
  "appointmentDate": "2026-10-05",
  "scheduledTime": "10:30:00",
  "patientsAhead": 2,
  "estimatedWaitMinutes": 40,
  "doctorCurrentEmergencyCaseload": 0,
  "caseloadLevel": "MODERATE",
  "explanation": "2 patients scheduled ahead of you (~40 mins estimated wait based on 20 min avg consultation). Physician has 0 active emergency cases."
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

### `POST /api/records/{id}/ai-summary-draft`
Generates a structured, concise AI draft summary for a clinical medical record based on diagnosis, presenting symptoms, treatment plan, and consultation notes. The draft is saved with `summaryStatus = DRAFT_PENDING_REVIEW` and is NOT treated as a finalized clinical document until verified by a physician.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `DOCTOR`
* **Response Example (200 OK):**
```json
{
  "id": 1,
  "recordDate": "2026-09-15",
  "diagnosis": "Essential (Primary) Hypertension",
  "aiDraftSummary": "CLINICAL SUMMARY DRAFT (PENDING VERIFICATION)\n- Diagnosis: Essential (Primary) Hypertension\n- Presenting Symptoms: Mild afternoon headaches, elevated resting blood pressure (145/92 mmHg)\n- Treatment Plan: Lifestyle modification (DASH diet) and daily oral Lisinopril\n- Physician Notes: Patient responded favorably to initial trial. Target BP < 130/80 mmHg.",
  "summaryStatus": "DRAFT_PENDING_REVIEW"
}
```

---

### `POST /api/records/{id}/ai-summary-review`
Physician review action for an AI-generated draft summary. Doctors can `APPROVE`, `EDIT_AND_APPROVE` (submitting customized text), or `REJECT`.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `DOCTOR`
* **Request Example (Approve with Edits):**
```json
{
  "action": "EDIT_AND_APPROVE",
  "editedSummary": "CLINICAL SUMMARY (VERIFIED)\n- Patient stable on Lisinopril 10mg once daily.\n- Recommended home blood pressure monitoring twice weekly.\n- Follow up in 6 months."
}
```
* **Response Example (200 OK):**
```json
{
  "id": 1,
  "clinicalSummary": "CLINICAL SUMMARY (VERIFIED)\n- Patient stable on Lisinopril 10mg once daily.\n- Recommended home blood pressure monitoring twice weekly.\n- Follow up in 6 months.",
  "summaryStatus": "APPROVED"
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

---

## 10. Emergency Doctor Allocation & Daily Duty Roster

### `POST /api/emergency-requests`
Creates an immediate emergency request and triggers the deterministic allocation algorithm with pessimistic locking.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`
* **Request Example:**
```json
{
  "category": "Cardiology",
  "reason": "Severe acute chest pain and shortness of breath"
}
```
* **Response Example (200 OK - Doctor Assigned):**
```json
{
  "id": 1,
  "patientId": 1,
  "patientName": "John Doe",
  "patientPhone": "+1 (555) 234-5678",
  "doctorId": 1,
  "doctorName": "Dr. Sarah Jenkins",
  "doctorSpecialization": "Cardiology",
  "doctorPhone": "+1 (555) 010-2030",
  "requestTime": "2026-09-27T10:15:00",
  "assignedTime": "2026-09-27T10:15:01",
  "status": "ASSIGNED",
  "priority": "EMERGENCY",
  "category": "Cardiology",
  "reason": "Severe acute chest pain and shortness of breath",
  "doctorNotes": null,
  "emergencyWarning": "Emergency request assigned to Dr. Sarah Jenkins. If this is an immediate life-threatening emergency, call local emergency services (e.g. 911 / 112) immediately."
}
```
* **Response Example (200 OK - No Doctor Available):**
```json
{
  "id": 2,
  "status": "NO_DOCTOR_AVAILABLE",
  "priority": "EMERGENCY",
  "category": "General",
  "emergencyWarning": "CRITICAL NOTICE: No emergency-duty doctor is currently available. If you or the patient are facing an immediate life-threatening situation, please call your local emergency phone number (e.g. 911 / 112) or proceed to the nearest emergency room immediately."
}
```

---

### `GET /api/emergency-requests/my`
Retrieves all emergency requests submitted by the currently authenticated patient.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `PATIENT`
* **Response Example (200 OK):**
```json
[
  {
    "id": 1,
    "doctorName": "Dr. Sarah Jenkins",
    "doctorSpecialization": "Cardiology",
    "status": "COMPLETED",
    "priority": "EMERGENCY",
    "category": "Cardiology",
    "requestTime": "2026-09-27T10:15:00"
  }
]
```

---

### `GET /api/emergency-requests/assigned`
Retrieves active and assigned emergency cases for the currently authenticated physician.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `DOCTOR`
* **Response Example (200 OK):**
```json
[
  {
    "id": 1,
    "patientName": "John Doe",
    "patientPhone": "+1 (555) 234-5678",
    "category": "Cardiology",
    "reason": "Severe acute chest pain and shortness of breath",
    "status": "IN_PROGRESS",
    "requestTime": "2026-09-27T10:15:00"
  }
]
```

---

### `PATCH /api/emergency-requests/{id}/status`
Updates the status of an assigned emergency case (e.g. to `IN_PROGRESS` or `COMPLETED`).
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `DOCTOR`, `ADMIN`
* **Request Example:**
```json
{
  "status": "COMPLETED",
  "doctorNotes": "Patient assessed in ER Bay 2, vitals stabilized, sublingual nitroglycerin administered."
}
```

---

### `GET /api/emergency-roster/my-duty`
Checks the emergency roster duty and shift status for the authenticated doctor.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `DOCTOR`
* **Response Example (200 OK):**
```json
[
  {
    "id": 1,
    "rosterDate": "2026-09-27",
    "shiftName": "MORNING",
    "shiftStart": "08:00:00",
    "shiftEnd": "14:00:00",
    "dutyStatus": "EMERGENCY_DUTY",
    "doctorAvailabilityStatus": "AVAILABLE",
    "activeEmergencyCasesCount": 0
  }
]
```

---

### `PUT /api/emergency-roster/my-status`
Allows an authenticated physician to update their real-time availability status (`AVAILABLE`, `BUSY`, `IN_CONSULTATION`, `OFF_DUTY`, `ON_LEAVE`).
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `DOCTOR`
* **Request Example:**
```json
{
  "availabilityStatus": "IN_CONSULTATION"
}
```

---

### `GET /api/admin/emergency-roster`
Retrieves daily emergency duty roster with doctor availability and active workload. Supports `?date=YYYY-MM-DD` query parameter.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`
* **Response Example (200 OK):**
```json
[
  {
    "id": 1,
    "doctorId": 1,
    "doctorName": "Dr. Sarah Jenkins",
    "doctorSpecialization": "Cardiology",
    "rosterDate": "2026-09-27",
    "shiftName": "MORNING",
    "shiftStart": "08:00:00",
    "shiftEnd": "14:00:00",
    "dutyStatus": "EMERGENCY_DUTY",
    "doctorAvailabilityStatus": "AVAILABLE",
    "activeEmergencyCasesCount": 0
  }
]
```

---

### `POST /api/admin/emergency-roster`
Assigns a licensed physician to emergency duty for a specific date and shift. Enforces collision prevention against duplicate overlapping shifts.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`
* **Request Example:**
```json
{
  "doctorId": 2,
  "rosterDate": "2026-09-27",
  "shiftName": "EVENING",
  "shiftStart": "14:00",
  "shiftEnd": "20:00",
  "dutyStatus": "EMERGENCY_DUTY",
  "doctorAvailabilityStatus": "AVAILABLE"
}
```

---

### `PUT /api/admin/emergency-roster/{id}`
Updates an emergency duty roster assignment.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`

---

### `DELETE /api/admin/emergency-roster/{id}`
Removes a doctor from emergency duty for that roster date.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`

---

### `GET /api/admin/emergency-stats`
Retrieves daily emergency triage request status counts (`WAITING`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `NO_DOCTOR_AVAILABLE`).
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`

---

### `POST /api/admin/emergency-roster/generate`
Auto-generates a balanced, deterministic daily emergency physician roster for a selected date.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`
* **Request Example:**
```json
{
  "rosterDate": "2026-09-28",
  "doctorsPerShift": 1
}
```
* **Algorithm Highlights:**
  - Evaluates past 7-day emergency shift counts (fair rotation).
  - Evaluates scheduled appointment load on target date.
  - Enforces mandatory rest rule (no morning shift following a night shift).
  - Skips physicians marked `ON_LEAVE`.
  - Preserves existing assignments without duplicates.
* **Response Example (200 OK):**
```json
[
  {
    "id": 105,
    "doctorId": 2,
    "doctorName": "Dr. Priya Sharma",
    "doctorSpecialization": "Cardiology",
    "rosterDate": "2026-09-28",
    "shiftName": "MORNING",
    "shiftStart": "08:00:00",
    "shiftEnd": "14:00:00",
    "dutyStatus": "EMERGENCY_DUTY",
    "doctorAvailabilityStatus": "AVAILABLE",
    "activeEmergencyCasesCount": 0
  },
  {
    "id": 106,
    "doctorId": 3,
    "doctorName": "Dr. Kumar Sangakara",
    "doctorSpecialization": "Neurology",
    "rosterDate": "2026-09-28",
    "shiftName": "EVENING",
    "shiftStart": "14:00:00",
    "shiftEnd": "20:00:00",
    "dutyStatus": "EMERGENCY_DUTY",
    "doctorAvailabilityStatus": "AVAILABLE",
    "activeEmergencyCasesCount": 0
  },
  {
    "id": 107,
    "doctorId": 1,
    "doctorName": "Dr. Arun Kumar",
    "doctorSpecialization": "General Medicine",
    "rosterDate": "2026-09-28",
    "shiftName": "NIGHT",
    "shiftStart": "20:00:00",
    "shiftEnd": "08:00:00",
    "dutyStatus": "EMERGENCY_DUTY",
    "doctorAvailabilityStatus": "AVAILABLE",
    "activeEmergencyCasesCount": 0
  }
]
```

---

### `GET /api/admin/emergency-analytics`
Retrieves comprehensive emergency department statistics and individual doctor caseload balancing data from PostgreSQL.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`
* **Query Parameters:** `?date=YYYY-MM-DD` (optional, defaults to current date)
* **Response Example (200 OK):**
```json
{
  "date": "2026-09-27",
  "totalRequests": 12,
  "waitingRequests": 1,
  "assignedRequests": 3,
  "inProgressRequests": 2,
  "completedRequests": 6,
  "cancelledRequests": 0,
  "noDoctorAvailableRequests": 0,
  "doctorWorkloads": [
    {
      "doctorId": 1,
      "doctorName": "Dr. Sarah Jenkins",
      "specialization": "Cardiology",
      "currentStatus": "AVAILABLE",
      "emergencyDutyToday": true,
      "shiftName": "MORNING",
      "shiftHours": "08:00 - 14:00",
      "todayTotalCases": 3,
      "todayCompletedCases": 2,
      "todayActiveCases": 1,
      "totalPast7DaysShifts": 2
    }
  ]
}
```

---

### `GET /api/admin/analytics`
Unified healthcare coordination analytics aggregating high-level clinical metrics, emergency volume, doctor workload balancing distribution, and multi-day trends.
* **Auth Requirement:** JWT Bearer
* **Allowed Roles:** `ADMIN`
* **Response Example (200 OK):**
```json
{
  "totalPatients": 48,
  "totalDoctors": 12,
  "todayAppointments": 15,
  "completedAppointments": 8,
  "cancelledAppointments": 2,
  "emergencyRequests": 5,
  "completedEmergencyRequests": 3,
  "waitingEmergencyRequests": 1,
  "doctorWorkloads": [
    {
      "doctorId": 1,
      "doctorName": "Dr. Sarah Jenkins",
      "specialization": "Cardiology",
      "totalAppointments": 4,
      "pendingAppointments": 1,
      "confirmedAppointments": 2,
      "completedAppointments": 1,
      "emergencyRequests": 1,
      "totalWorkload": 3,
      "currentAvailability": "AVAILABLE"
    }
  ],
  "dailyAppointmentTrends": {
    "2026-09-25": 10,
    "2026-09-26": 14,
    "2026-09-27": 15
  },
  "dailyEmergencyTrends": {
    "2026-09-25": 3,
    "2026-09-26": 6,
    "2026-09-27": 5
  }
}
```

---

## 10. System Health & Liveness

### `GET /api/health`
Performs an active liveness and readiness probe, verifying application run-state and PostgreSQL database connectivity via standard connection validation without exposing internal credentials.
* **Auth Requirement:** None (Public)
* **Allowed Roles:** Any (Unauthenticated)
* **Response Example (200 OK - Healthy):**
```json
{
  "status": "UP",
  "database": "UP",
  "service": "CarePulse Portal",
  "timestamp": "2026-09-27T19:30:00Z"
}
```
* **Response Example (503 Service Unavailable - Degraded):**
```json
{
  "status": "DOWN",
  "database": "DOWN",
  "service": "CarePulse Portal",
  "timestamp": "2026-09-27T19:30:00Z"
}
```

---

## 11. Error Handling & Security Architecture

### Standardized Error Response Format
All errors returned by the CarePulse REST API follow a uniform, sanitized JSON schema managed centrally via `GlobalExceptionHandler` (`@RestControllerAdvice`). Stack traces, SQL commands, and internal class names are strictly suppressed from all client payloads.

```json
{
  "timestamp": "2026-09-27T19:30:00Z",
  "status": 400,
  "error": "Validation Error",
  "message": "Validation failed for one or more fields",
  "path": "/api/appointments",
  "fieldErrors": {
    "doctorId": "Doctor ID is required",
    "appointmentDate": "Appointment date must be today or in the future"
  }
}
```

### Standard HTTP Status Codes

| Status Code | Error Classification | Scenario |
|:---|:---|:---|
| **`200 OK`** | Success | Request succeeded. |
| **`400 Bad Request`** | `Validation Error` / `Bad Request` | Malformed JSON, missing required fields, or validation constraint violation. |
| **`401 Unauthorized`** | `Unauthorized` | Missing, malformed, or expired JWT bearer token. |
| **`403 Forbidden`** | `Access Denied` / `Forbidden` | User lacks role authority (`JwtAccessDeniedHandler`) or fails resource-level ownership checks. |
| **`404 Not Found`** | `Not Found` | Requested patient, doctor, appointment, record, or roster not found. |
| **`409 Conflict`** | `Conflict` / `Data Integrity Error` | Doctor double-booking, patient schedule collision, or duplicate unique DB constraints. |
| **`500 Internal Error`** | `Internal Server Error` | Unexpected server failure. Details are logged securely via SLF4J, masked from client. |
| **`503 Unavailable`** | `Service Unavailable` | Health check probe failure or degraded infrastructure. |

### Resource-Level Ownership Rules
1. **Patient Profiles (`/api/patients/{id}`)**: Patients may only view and edit their own profile. Caregivers may view authorized dependent patients. Doctors and Admins may view patient records according to role permissions.
2. **Doctor Profiles & Availability (`/api/doctors/{id}`)**: Doctors may only edit their own profile bio and availability slots. Admins have platform-wide override privileges.
3. **Medical Records (`/api/records/**`)**: Patients may only access their own records. Caregivers with active authorization can access assigned dependent records. Doctors can access records they authored or consultation records for authorized patients.
4. **Prescriptions (`/api/prescriptions/**`)**: Only licensed DOCTOR users may issue prescriptions. Patients and authorized caregivers can view their own prescriptions.
5. **Appointments (`/api/appointments/**`)**: Patients can view and cancel their own appointments. Doctors can view and manage appointments assigned to them. Double-booking conflicts return HTTP 409.
6. **Notifications (`/api/notifications/{id}/read`)**: Only the recipient user owning the notification can mark it as read.
7. **Emergency Requests (`/api/emergency-requests/**`)**: Patients can view their own emergency cases; assigned doctors can view and transition cases assigned directly to them.


