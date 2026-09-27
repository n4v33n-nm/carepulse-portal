# CarePulse Portal — Phase 5: Smart Healthcare Coordination & Intelligent Scheduling

## 📌 Executive Summary

Phase 5 elevates **CarePulse Portal** from a foundational healthcare portal into an **Intelligent Healthcare Coordination Platform**. Building upon the robust security, emergency allocation, and role-based workflows established in Phases 1–4, Phase 5 introduces:
1. **Explainable Smart Doctor Matching Engine** driven by real database schedules and workloads.
2. **Dynamic Slot Generation & Cancelled Slot Reuse** with automatic schedule reconciliation.
3. **Automated Priority Waitlist Engine** notifying queued patients upon consultation cancellations.
4. **Data-Grounded Consultation Wait-Time Estimation**.
5. **Enhanced Emergency Integration & Caseload Guardrails** preventing physician overload.
6. **Doctor Workload Balancing Matrix** combining clinic consultations with emergency shifts.
7. **Enhanced AI Health Companion Guardrails & Physician-Verified Clinical Summaries**.
8. **Real-Time Admin Healthcare Coordination Analytics**.
9. **Reliable Spring Scheduler Background Workers** for waitlist expiration and clinical reminders.

All features are implemented strictly using existing architectural patterns (Spring Boot 3, Spring Data JPA, PostgreSQL, React 18, Vite) without adding unnecessary distributed infrastructure (no Kafka, Redis, or microservices).

---

## 1. Features Added & Upgraded

### 1.1 Smart Doctor Matching Engine (`SmartDoctorMatchingService`)
- **Endpoint:** `GET /api/doctors/match?specialization=...&date=...&time=...`
- **Matching Criteria:**
  - Strict specialization match.
  - Doctor active status (`user.active == true`).
  - Active clinic availability on the specified day of the week.
  - Active time window match (requested time falls between `startTime` and `endTime`).
  - Leave status check (`DoctorAvailability.onLeave == false`).
  - Existing appointment conflict detection (`status != CANCELLED` at the exact requested time).
  - Emergency duty status check on the requested date.
- **Scoring & Workload Ranking:**
  - Calculates active appointments for the requested date.
  - Calculates active emergency requests.
  - Sorts matching candidates by lowest total workload first.
- **Explainable Match Rationale:** Returns transparent badges/reasons for patient visibility (e.g., `["Cardiology Specialist", "Available at 10:30:00", "Zero Schedule Conflicts", "Current Workload: 1 appointment"]`).
- **Clinical Non-Diagnostic Disclaimer:** Every payload explicitly includes a standard medical safety disclaimer confirming that the matching algorithm assists clinical coordination and does not provide medical diagnosis.

### 1.2 Smart Dynamic Slot Generation & Cancelled Slot Reuse
- **Endpoints:** `GET /api/doctors/{id}/available-slots?date=...` and `GET /api/appointments/available-slots?doctorId=...&date=...`
- **Dynamic Slot Generation:**
  - Evaluates doctor's configured weekly schedule (`startTime`, `endTime`, `slotDurationMinutes`).
  - Automatically slices consultation hours into precise 30-minute intervals.
  - Filters out past slots if the requested date is today (`slotTime.isAfter(LocalTime.now())`).
  - Excludes booked appointments (`PENDING`, `CONFIRMED`, `IN_PROGRESS`).
- **Immediate Cancelled Slot Reuse:**
  - When an appointment is cancelled (`status = CANCELLED`), it is immediately omitted from slot collision filters.
  - The slot automatically re-appears in available slot listings without requiring manual database resets or recreation.

### 1.3 Priority Waitlist System (`AppointmentWaitlistService`)
- **Endpoints:**
  - `POST /api/appointments/waitlist`: Patient joins the waitlist when zero slots are available.
  - `GET /api/appointments/waitlist`: Patient reviews their active and historical waitlist entries.
  - `DELETE /api/appointments/waitlist/{id}`: Patient can withdraw from the waitlist (`CANCELLED`).
- **Automatic Notification Loop:**
  - When an appointment is cancelled via `AppointmentService.cancelAppointment()`, the system queries the `appointment_waitlist` table for patients waiting for that doctor (or specialization) on that date.
  - Automatically sends high-priority in-app notifications (`NOTIFICATION_WAITLIST_SLOT_OPEN`).
  - Transitions waitlist status from `WAITING` to `NOTIFIED`.

### 1.4 Explainable Wait-Time Estimation (`AppointmentWaitTimeService`)
- **Endpoint:** `GET /api/appointments/{id}/wait-time`
- **Data-Grounded Calculation:**
  - Queries active appointments scheduled earlier on the same day for that physician.
  - Multiplies patients ahead by average consultation duration (default: 20 minutes, or doctor's configured slot duration).
  - Adds doctor's current active emergency caseload offset (+30 minutes per active emergency case).
  - Categorizes doctor's load (`LIGHT`, `MODERATE`, `BUSY`, `OVERLOADED`).
- **Explainable Metrics:** Returns `patientsAhead`, `estimatedWaitMinutes`, `currentDoctorCaseload`, and human-readable explanation string.

### 1.5 Emergency Queue Concurrency & Workload Protection
- **Pessimistic Write Locking:** `EmergencyDoctorAllocationService` locks candidate emergency roster rows during assignment (`@Lock(LockModeType.PESSIMISTIC_WRITE)`), eliminating concurrent allocation races.
- **Cross-Shift Availability Safeguard:** Doctors actively engaged in an emergency (`IN_CONSULTATION`) or regular consultation are blocked from receiving concurrent emergency dispatches.
- **Auto-Recovery on Case Completion:** When an emergency case transitions to `COMPLETED`, if the doctor has zero remaining active emergency cases, their emergency status automatically resets to `AVAILABLE`, triggering auto-dispatch for pending `WAITING` emergency queue items.

### 1.6 Doctor Workload Balancing Matrix
- **Endpoints:**
  - `GET /api/doctors/workload`: Admin overview of all physicians' clinical metrics for a given date.
  - `GET /api/doctors/{id}/workload`: Individual doctor's daily capacity and caseload breakdown.
- **Metrics Tracked:**
  - Scheduled Appointments (Total, Pending, Confirmed, Completed, Cancelled).
  - Emergency Requests (Active, Completed).
  - Total Workload score (`activeAppointments + activeEmergencyCases`).
  - Emergency Duty status and Shift Window.

### 1.7 AI Safety Guardrails & Physician-Verified Clinical Summaries
- **AI Health Companion Safety Hardening (`AiHealthCompanionService`):**
  - Detects emergent keywords (`chest pain`, `stroke`, `unconscious`, `shortness of breath`, `overdose`).
  - Immediately diverts to emergency care (dial 911/112 or click "Emergency Assistance") and refuses medical diagnosis.
  - Detects prescription/dosing inquiries and disclaims medication prescribing authority.
- **Physician Verification Loop (`MedicalRecordService`):**
  - `POST /api/records/{id}/ai-summary-draft`: Generates a structured AI draft based on consultation notes, symptoms, and diagnosis. Marked as `DRAFT_PENDING_REVIEW`.
  - `POST /api/records/{id}/ai-summary-review`: Physician can `APPROVE`, `EDIT_AND_APPROVE`, or `REJECT`.
  - Only approved text is committed to `clinicalSummary` and marked `APPROVED`. Rejections are marked `REJECTED`. Unreviewed AI content is never treated as a finalized clinical record.

### 1.8 Admin Healthcare Coordination Analytics (`AdminController`)
- **Endpoint:** `GET /api/admin/analytics`
- **Real Database Aggregations:**
  - Operational KPIs: Total Patients, Total Doctors, Today's Appointments, Completed Consultations, Cancelled Consultations.
  - Emergency Metrics: Total Emergency Requests, Waiting, Assigned, In Progress, Completed, No Doctor Available.
  - Doctor Workload Balancing Table: Real-time appointment counts, emergency cases, and availability statuses.
  - Consultation & Emergency Daily Volume Trends.

### 1.9 Spring Boot Background Schedulers (`ScheduledTasksService`)
- `@Scheduled(cron = "0 0 2 * * *")`: Automatically marks past `WAITING` waitlist entries as `EXPIRED`.
- `@Scheduled(cron = "0 0 7 * * *")`: Sends appointment day-of reminders to patients for consultations scheduled today.
- `@Scheduled(fixedDelay = 600000)`: Cleans up transient stale notifications older than 30 days.

---

## 2. Database Changes

### 2.1 New Table: `appointment_waitlist`
```sql
CREATE TABLE IF NOT EXISTS appointment_waitlist (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    doctor_id BIGINT REFERENCES doctors(id) ON DELETE SET NULL,
    specialization VARCHAR(100),
    preferred_date DATE NOT NULL,
    preferred_time TIME,
    status VARCHAR(30) NOT NULL DEFAULT 'WAITING',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### 2.2 Table Alterations: `medical_records`
Added structured clinical review columns:
```sql
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS clinical_summary TEXT;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS ai_draft_summary TEXT;
ALTER TABLE medical_records ADD COLUMN IF NOT EXISTS summary_status VARCHAR(30) DEFAULT 'NONE';
```

### 2.3 Performance Indexing
```sql
CREATE INDEX IF NOT EXISTS idx_waitlist_patient ON appointment_waitlist(patient_id);
CREATE INDEX IF NOT EXISTS idx_waitlist_doc_date ON appointment_waitlist(doctor_id, preferred_date);
CREATE INDEX IF NOT EXISTS idx_waitlist_status ON appointment_waitlist(status);
CREATE INDEX IF NOT EXISTS idx_records_summary_status ON medical_records(summary_status);
```

---

## 3. REST API Specification (Phase 5 Endpoints)

| Method | Endpoint | Allowed Roles | Description |
|:---|:---|:---|:---|
| `GET` | `/api/doctors/match` | Any | Explainable doctor matching based on specialization, date, time, leave, and workload. |
| `GET` | `/api/doctors/workload` | `ADMIN` | Overview of all physicians' daily workload (appointments + emergency cases). |
| `GET` | `/api/doctors/{id}/workload` | `DOCTOR`, `ADMIN` | Detailed workload breakdown for a specific physician. |
| `GET` | `/api/doctors/{id}/available-slots` | Any | Dynamic slot generator filtering out booked and past slots. |
| `GET` | `/api/appointments/available-slots` | Any | Alias for slot generation using `doctorId` query parameter. |
| `POST` | `/api/appointments/waitlist` | `PATIENT`, `ADMIN` | Join priority waitlist for fully booked dates/specializations. |
| `GET` | `/api/appointments/waitlist` | `PATIENT`, `ADMIN` | Retrieve caller's waitlist entries with live status. |
| `DELETE` | `/api/appointments/waitlist/{id}`| `PATIENT`, `ADMIN` | Cancel/withdraw an active waitlist request. |
| `GET` | `/api/appointments/{id}/wait-time`| Any Authenticated | Data-grounded wait time estimation for a consultation. |
| `POST` | `/api/records/{id}/ai-summary-draft`| `DOCTOR` | Generate an AI draft clinical summary for physician review. |
| `POST` | `/api/records/{id}/ai-summary-review`| `DOCTOR` | Physician approves, edits, or rejects the AI clinical summary draft. |
| `GET` | `/api/admin/analytics` | `ADMIN` | Aggregated executive KPIs, emergency metrics, and workload balancing. |

---

## 4. Frontend Enhancements

1. **Patient Doctor Discovery (`DoctorDiscovery.jsx`):**
   - Added tabbed switching: **Browse All Doctors** vs. **Smart Doctor Matching**.
   - Input specialization, preferred date, and preferred time to query `/api/doctors/match`.
   - Renders explainable match reason badges (e.g., `Cardiology Match`, `Available at 10:30`, `Workload: 1`).
   - Displays clear clinical non-diagnostic disclaimer.
   - When slots are 0, dynamically renders **Join Priority Waitlist** button and triggers modal.

2. **Patient Appointment Timeline (`PatientAppointments.jsx`):**
   - Added **Priority Waitlist** tab alongside appointment history.
   - Waitlist card displays preferred doctor, specialization, target date, and current status (`WAITING`, `NOTIFIED`, `BOOKED`, `CANCELLED`).
   - If slot opens, shows a prominent green alert badge notifying patient to book.
   - Added **Wait Time Calculator** modal for confirmed appointments, displaying patients ahead, doctor caseload, and estimated wait minutes.

3. **Doctor Command Center (`DoctorDashboard.jsx`):**
   - Integrated **Today's Workload & Clinical Capacity** card: Displays total appointments, pending, confirmed, completed, emergency caseload, and total clinical load.
   - Integrated **Today's Emergency Duty** card: Highlights whether the doctor is rostered on emergency duty today, active shift hours, and emergency status (`AVAILABLE`, `IN_CONSULTATION`, `OFF_DUTY`).

4. **Doctor Clinical Records (`DoctorRecords.jsx`):**
   - Added **AI Clinical Summary** generation section on existing records.
   - Displays **AI Draft Summary (Pending Verification)** with amber warning banner.
   - Features physician verification modal:
     - **Approve As-Is** commits draft to permanent clinical summary.
     - **Save Edits & Approve** allows doctor to refine draft before committing.
     - **Reject Draft** discards inaccurate AI summary.

5. **Admin Coordination Analytics (`AdminDashboard.jsx`):**
   - Added **Healthcare Coordination Analytics** view:
     - Real-time KPI cards for consultations and emergency cases.
     - Emergency Dispatch Analytics (Assigned, In Progress, Completed, Waiting).
     - Doctor Workload Balancing Matrix detailing individual physician stress levels and emergency duties.
     - Daily consultation and emergency request volume charts.

---

## 5. Security & Safety Considerations

1. **HIPAA-Aligned Medical Record Access:**
   - Admins cannot arbitrarily view sensitive medical records or draft summaries without clinical necessity.
   - Patients can only view records for themselves or delegated dependents via verified `CaregiverService` relationships.
   - Doctors can only view records for their assigned patients.
2. **AI Clinical Verification Boundary:**
   - AI draft summaries are tagged with `summaryStatus = DRAFT_PENDING_REVIEW`.
   - Never treated as finalized medical records until a licensed physician explicitly executes `APPROVE` or `EDIT_AND_APPROVE`.
3. **Pessimistic Emergency Locking:**
   - Race conditions on emergency doctor assignment are prevented using `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
4. **Credential Isolation:**
   - System timezone alignment (`UTC`) eliminates historical JDBC sub-nanosecond integer underflows on `LocalTime` columns.

---

## 6. Verification & Test Results

- **Unit & Integration Tests (`mvn test`):**
  - Total Tests Run: **70**
  - Failures: **0**
  - Errors: **0**
  - Skipped: **0**
  - **Result: BUILD SUCCESS**
- **Frontend Production Build (`npm run build`):**
  - Modules transformed: **165**
  - Build Time: **10.85s**
  - Warnings/Errors: **0**
  - **Result: SUCCESS (`dist/` directory generated)**
