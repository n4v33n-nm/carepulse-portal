const http = require('http');
const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer-core');

const DIST_DIR = path.join(__dirname, 'frontend', 'dist');
const SCREENSHOT_DIR = path.join(__dirname, 'screenshots');
const ARTIFACT_DIR = path.join('C:', 'Users', 'navee', '.gemini', 'antigravity', 'brain', 'b55f5570-1746-4f4a-9acb-8ab3437e76ca', 'screenshots');

if (!fs.existsSync(SCREENSHOT_DIR)) fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
if (!fs.existsSync(ARTIFACT_DIR)) fs.mkdirSync(ARTIFACT_DIR, { recursive: true });

const MIME_TYPES = {
  '.html': 'text/html',
  '.js': 'application/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.woff2': 'font/woff2',
};

const server = http.createServer((req, res) => {
  let reqPath = req.url.split('?')[0];
  let filePath = path.join(DIST_DIR, reqPath);

  if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    const ext = path.extname(filePath).toLowerCase();
    res.writeHead(200, { 'Content-Type': MIME_TYPES[ext] || 'application/octet-stream' });
    fs.createReadStream(filePath).pipe(res);
  } else {
    const indexPath = path.join(DIST_DIR, 'index.html');
    res.writeHead(200, { 'Content-Type': 'text/html' });
    fs.createReadStream(indexPath).pipe(res);
  }
});

server.listen(5173, async () => {
  console.log('Static SPA server listening at http://localhost:5173');

  try {
    const browser = await puppeteer.launch({
      executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
      headless: 'new',
      args: ['--no-sandbox', '--disable-setuid-sandbox', '--window-size=1440,900'],
      defaultViewport: { width: 1440, height: 900, deviceScaleFactor: 1.5 }
    });

    const mockPatient = {
      id: 1,
      fullName: 'Naveen Kumar',
      gender: 'Male',
      bloodGroup: 'O+',
      dateOfBirth: '1998-06-15',
      phone: '+91 98765 43210',
      address: '42 Healthway Boulevard, Chennai',
      emergencyContact: '+91 98765 00000 (Spouse)'
    };

    const mockDoctor = {
      id: 1,
      fullName: 'Dr. Sarah Jenkins',
      specialization: 'Cardiology',
      qualification: 'MD, FACC, Harvard Medical',
      rating: 4.9,
      experienceYears: 12,
      consultationFee: 1200.0,
      availabilityStatus: 'AVAILABLE'
    };

    const mockDoctor2 = {
      id: 2,
      fullName: 'Dr. Marcus Vance',
      specialization: 'Cardiology',
      qualification: 'MBBS, MD Cardiology (Stanford)',
      rating: 4.8,
      experienceYears: 10,
      consultationFee: 1100.0,
      availabilityStatus: 'AVAILABLE'
    };

    const mockAppointments = [
      {
        id: 101,
        patient: mockPatient,
        doctor: mockDoctor,
        appointmentDate: '2026-11-20',
        appointmentTime: '10:30:00',
        status: 'CONFIRMED',
        reason: 'Quarterly cardiovascular checkup & blood pressure management',
        consultationNotes: 'Patient advised to bring previous ambulatory BP monitoring logs.'
      },
      {
        id: 102,
        patient: mockPatient,
        doctor: mockDoctor2,
        appointmentDate: '2026-10-15',
        appointmentTime: '14:00:00',
        status: 'COMPLETED',
        reason: 'Follow-up consultation for exertional palpitations',
        consultationNotes: '12-lead ECG normal. Continue prescribed Lisinopril therapy.'
      }
    ];

    const mockRecords = [
      {
        id: 201,
        recordDate: '2026-10-15',
        diagnosis: 'Stage 1 Essential Hypertension',
        symptoms: 'Occasional morning occipital headache, mild dizziness',
        treatment: 'Initiated Lisinopril 10mg daily. Low-sodium diet prescribed.',
        consultationNotes: 'Patient exhibits good compliance. Resting BP 132/84 mmHg.',
        clinicalSummary: 'Well-controlled hypertension without secondary end-organ damage.',
        doctor: mockDoctor
      },
      {
        id: 202,
        recordDate: '2026-08-10',
        diagnosis: 'Dyslipidemia & Cardiovascular Risk Mitigation',
        symptoms: 'Asymptomatic; detected during executive health check',
        treatment: 'Prescribed Atorvastatin 20mg at bedtime. Regular aerobic conditioning.',
        consultationNotes: 'Total cholesterol 235 mg/dL, LDL 152 mg/dL. Re-check in 3 months.',
        clinicalSummary: 'Cardiovascular risk factor modified with lipid-lowering therapy.',
        doctor: mockDoctor2
      }
    ];

    const mockPrescriptions = [
      {
        id: 301,
        medicineName: 'Lisinopril 10mg',
        dosage: '10mg Tablet',
        frequency: 'Once Daily in Morning',
        duration: '90 Days',
        instructions: 'Take with or without food. Monitor blood pressure weekly.',
        issuedDate: '2026-10-15',
        doctor: mockDoctor
      },
      {
        id: 302,
        medicineName: 'Atorvastatin 20mg',
        dosage: '20mg Tablet',
        frequency: 'Once Daily at Bedtime',
        duration: '90 Days',
        instructions: 'Avoid grapefruit consumption. Report muscle tenderness.',
        issuedDate: '2026-08-10',
        doctor: mockDoctor2
      }
    ];

    const mockReports = [
      {
        id: 401,
        title: 'Comprehensive Lipid & Cardiac Risk Profile',
        reportType: 'BLOOD_TEST',
        originalFilename: 'Lipid_Panel_Nov2026.pdf',
        fileFormat: 'pdf',
        formattedFileSize: '1.4 MB',
        reportDate: '2026-11-05',
        clinicalNotes: 'Total Cholesterol: 198 mg/dL, HDL: 52 mg/dL, Triglycerides: 145 mg/dL. Normal values achieved.'
      },
      {
        id: 402,
        title: '12-Lead Electrocardiogram (ECG) Report',
        reportType: 'SCAN_REPORT',
        originalFilename: '12_Lead_ECG_Telemetry.png',
        fileFormat: 'png',
        formattedFileSize: '3.8 MB',
        reportDate: '2026-10-28',
        clinicalNotes: 'Normal sinus rhythm at 68 bpm. No acute ST-T wave abnormalities.'
      },
      {
        id: 403,
        title: 'High-Resolution Cardiac MRI Scan',
        reportType: 'MRI_SCAN',
        originalFilename: 'Cardiac_MRI_Contrast.pdf',
        fileFormat: 'pdf',
        formattedFileSize: '8.2 MB',
        reportDate: '2026-09-14',
        clinicalNotes: 'Left ventricular ejection fraction calculated at 62%. Normal cardiac wall motion.'
      }
    ];

    const mockConversations = [
      {
        id: 501,
        subject: 'Clarification regarding blood pressure spike after morning jog',
        status: 'ANSWERED',
        createdAt: '2026-11-06T09:30:00',
        updatedAt: '2026-11-06T11:45:00',
        patient: mockPatient,
        doctor: mockDoctor,
        messages: [
          {
            id: 1,
            senderRole: 'PATIENT',
            senderName: 'Naveen Kumar',
            content: 'Good morning Dr. Jenkins. I noticed my BP was 138/88 right after my 30-minute morning jog. Should I take an additional dose or wait 20 minutes before taking readings?',
            sentAt: '2026-11-06T09:30:00'
          },
          {
            id: 2,
            senderRole: 'DOCTOR',
            senderName: 'Dr. Sarah Jenkins',
            content: 'Hello Naveen. Post-exercise elevation is completely physiological! Please rest in a seated position for 20-30 minutes before logging your reading. Do NOT take an extra dose.',
            sentAt: '2026-11-06T11:45:00'
          }
        ]
      }
    ];

    const mockDoctorLeaves = [
      {
        id: 601,
        doctor: mockDoctor,
        startDate: '2026-11-25',
        endDate: '2026-11-28',
        reason: 'Attending International Cardiology Symposium & CME Training',
        status: 'APPROVED',
        adminNotes: 'Leave approved. Outpatient appointments covered by Dr. Marcus Vance with patient consent.',
        reviewedAt: '2026-11-05T14:20:00',
        createdAt: '2026-11-04T10:00:00'
      }
    ];

    const mockEmergency = [
      {
        id: 38,
        status: 'IN_PROGRESS',
        priority: 'URGENT',
        category: 'Cardiology',
        description: 'Sudden onset chest tightness and palpitations following physical exertion',
        assignedDoctor: mockDoctor,
        requestTime: '2026-11-13T08:15:00',
        assignedTime: '2026-11-13T08:18:00',
        startedTime: '2026-11-13T08:22:00'
      }
    ];

    // Helper to configure page with authentication and mock endpoints
    async function createConfiguredPage(userRole, email, fullName, patientId, doctorId) {
      const p = await browser.newPage();
      await p.setViewport({ width: 1440, height: 900, deviceScaleFactor: 1.5 });

      if (userRole) {
        await p.evaluateOnNewDocument((r, e, n, pid, did) => {
          localStorage.setItem('carepulse_token', 'mock-jwt-token-xyz');
          localStorage.setItem('carepulse_user', JSON.stringify({
            token: 'mock-jwt-token-xyz',
            userId: 1,
            email: e,
            role: r,
            fullName: n,
            patientId: pid,
            doctorId: did,
            communicationPreference: 'SUPPORTIVE'
          }));
        }, userRole, email, fullName, patientId, doctorId);
      }

      await p.setRequestInterception(true);
      p.on('request', (req) => {
        const u = req.url();

        if (u.includes('/api/auth/login')) {
          return req.respond({
            status: 200,
            contentType: 'application/json',
            body: JSON.stringify({
              token: 'mock-jwt-token-xyz',
              userId: 1,
              email: email || 'john.doe@example.com',
              role: userRole || 'PATIENT',
              fullName: fullName || 'Naveen Kumar',
              patientId: 1,
              doctorId: null,
              communicationPreference: 'SUPPORTIVE'
            })
          });
        }

        if (u.includes('/api/appointments')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify(mockAppointments) });
        }
        if (u.includes('/api/records')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify(mockRecords) });
        }
        if (u.includes('/api/prescriptions')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify(mockPrescriptions) });
        }
        if (u.includes('/api/reports')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify(mockReports) });
        }
        if (u.includes('/api/doctor-connect/conversations/501')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify(mockConversations[0]) });
        }
        if (u.includes('/api/doctor-connect/conversations')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify(mockConversations) });
        }
        if (u.includes('/api/doctor-connect/stats')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify({ totalConversations: 18, pendingReplies: 2, resolvedInquiries: 16 }) });
        }
        if (u.includes('/api/doctor-leaves')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify(mockDoctorLeaves) });
        }
        if (u.includes('/api/emergency')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify(mockEmergency) });
        }
        if (u.includes('/api/notifications/unread-count')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify(3) });
        }
        if (u.includes('/api/doctors')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify([mockDoctor, mockDoctor2]) });
        }
        if (u.includes('/api/admin/stats')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify({ totalPatients: 1420, activeDoctors: 34, completedConsultations: 3820, activeEmergencyCases: 2 }) });
        }
        if (u.includes('/api/')) {
          return req.respond({ status: 200, contentType: 'application/json', body: JSON.stringify([]) });
        }

        req.continue();
      });

      return p;
    }

    // --- SCREENSHOT 1: Landing Page ---
    console.log('Capturing Screenshot 1: Landing Page...');
    const pagePublic = await createConfiguredPage(null, null, null, null, null);
    await pagePublic.goto('http://localhost:5173/', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 1000));
    await pagePublic.screenshot({ path: path.join(SCREENSHOT_DIR, '01_landing_page.png') });
    await pagePublic.close();

    // --- SCREENSHOT 2: Login Portal ---
    console.log('Capturing Screenshot 2: Login Portal...');
    const pageLogin = await createConfiguredPage(null, null, null, null, null);
    await pageLogin.goto('http://localhost:5173/login', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 1000));
    await pageLogin.screenshot({ path: path.join(SCREENSHOT_DIR, '02_login_portal.png') });
    await pageLogin.close();

    // --- SCREENSHOT 3: Patient Overview Dashboard ---
    console.log('Capturing Screenshot 3: Patient Overview Dashboard...');
    const pagePatient = await createConfiguredPage('PATIENT', 'john.doe@example.com', 'Naveen Kumar', 1, null);
    await pagePatient.goto('http://localhost:5173/patient/dashboard', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 1500));
    await pagePatient.screenshot({ path: path.join(SCREENSHOT_DIR, '03_patient_overview_dashboard.png') });

    // --- SCREENSHOT 4: Patient Longitudinal Medical History ---
    console.log('Capturing Screenshot 4: Patient Medical History & Regimens...');
    await pagePatient.evaluate(() => {
      const el = document.querySelector('.card:nth-of-type(3)') || document.body;
      window.scrollTo(0, 520);
    });
    await new Promise(r => setTimeout(r, 1000));
    await pagePatient.screenshot({ path: path.join(SCREENSHOT_DIR, '04_patient_medical_history.png') });
    await pagePatient.close();

    // --- SCREENSHOT 5: Patient Uploaded Medical Reports & Scans ---
    console.log('Capturing Screenshot 5: Patient Secure Reports & Scans...');
    const pageReports = await createConfiguredPage('PATIENT', 'john.doe@example.com', 'Naveen Kumar', 1, null);
    await pageReports.goto('http://localhost:5173/patient/records', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 1000));
    // Click Uploaded Reports tab
    await pageReports.evaluate(() => {
      const btns = Array.from(document.querySelectorAll('button'));
      const target = btns.find(b => b.textContent && b.textContent.includes('Reports & Scans'));
      if (target) target.click();
    });
    await new Promise(r => setTimeout(r, 1000));
    await pageReports.screenshot({ path: path.join(SCREENSHOT_DIR, '05_patient_uploaded_reports.png') });
    await pageReports.close();

    // --- SCREENSHOT 6: DoctorConnect Patient Chat ---
    console.log('Capturing Screenshot 6: DoctorConnect Patient Chat...');
    const pageDocConnect = await createConfiguredPage('PATIENT', 'john.doe@example.com', 'Naveen Kumar', 1, null);
    await pageDocConnect.goto('http://localhost:5173/patient/doctor-connect', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 1500));
    await pageDocConnect.screenshot({ path: path.join(SCREENSHOT_DIR, '06_doctorconnect_patient_chat.png') });
    await pageDocConnect.close();

    // --- SCREENSHOT 7: Doctor Overview Dashboard ---
    console.log('Capturing Screenshot 7: Doctor Overview Dashboard...');
    const pageDoctor = await createConfiguredPage('DOCTOR', 'dr.jenkins@carepulse.com', 'Dr. Sarah Jenkins', null, 1);
    await pageDoctor.goto('http://localhost:5173/doctor/dashboard', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 1500));
    await pageDoctor.screenshot({ path: path.join(SCREENSHOT_DIR, '07_doctor_overview_dashboard.png') });
    await pageDoctor.close();

    // --- SCREENSHOT 8: Doctor Consultation & Pre-Review ---
    console.log('Capturing Screenshot 8: Doctor Appointments & Consultation Management...');
    const pageDocAppts = await createConfiguredPage('DOCTOR', 'dr.jenkins@carepulse.com', 'Dr. Sarah Jenkins', null, 1);
    await pageDocAppts.goto('http://localhost:5173/doctor/appointments', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 1500));
    await pageDocAppts.screenshot({ path: path.join(SCREENSHOT_DIR, '08_doctor_consultation_review.png') });
    await pageDocAppts.close();

    // --- SCREENSHOT 9: Doctor Leave Management ---
    console.log('Capturing Screenshot 9: Doctor Leave Management & Tracking...');
    const pageDocLeave = await createConfiguredPage('DOCTOR', 'dr.jenkins@carepulse.com', 'Dr. Sarah Jenkins', null, 1);
    await pageDocLeave.goto('http://localhost:5173/doctor/leaves', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 1500));
    await pageDocLeave.screenshot({ path: path.join(SCREENSHOT_DIR, '09_doctor_leave_management.png') });
    await pageDocLeave.close();

    // --- SCREENSHOT 10: Admin Leave Coverage Dashboard ---
    console.log('Capturing Screenshot 10: Admin Leave Coverage & Hospital Operations...');
    const pageAdmin = await createConfiguredPage('ADMIN', 'admin@carepulse.com', 'CarePulse Medical Administrator', null, null);
    await pageAdmin.goto('http://localhost:5173/admin/doctor-leaves', { waitUntil: 'networkidle0' });
    await new Promise(r => setTimeout(r, 1500));
    await pageAdmin.screenshot({ path: path.join(SCREENSHOT_DIR, '10_admin_leave_coverage_dashboard.png') });
    await pageAdmin.close();

    await browser.close();

    // Copy to artifact directory
    const files = fs.readdirSync(SCREENSHOT_DIR);
    for (const f of files) {
      if (f.endsWith('.png')) {
        fs.copyFileSync(path.join(SCREENSHOT_DIR, f), path.join(ARTIFACT_DIR, f));
      }
    }

    console.log('SUCCESS_ALL_CAPTURED: All 10 distinct screenshots captured successfully!');
  } catch (err) {
    console.error('Error during screenshot generation:', err);
  } finally {
    server.close();
    process.exit(0);
  }
});
