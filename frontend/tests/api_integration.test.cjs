/**
 * CarePulse Portal - Automated End-to-End API Integration Suite
 * Verifies Patient, Doctor, and Admin workflows through Vite Proxy.
 */
const http = require('http');

function post(path, data, token = null) {
  return new Promise((resolve, reject) => {
    const payload = JSON.stringify(data);
    const options = {
      hostname: 'localhost',
      port: 5173,
      path: path,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(payload),
      },
    };
    if (token) options.headers['Authorization'] = `Bearer ${token}`;

    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => (body += chunk));
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(body) });
        } catch (e) {
          resolve({ status: res.statusCode, data: body });
        }
      });
    });

    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

function get(path, token = null) {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: 'localhost',
      port: 5173,
      path: path,
      method: 'GET',
      headers: {},
    };
    if (token) options.headers['Authorization'] = `Bearer ${token}`;

    const req = http.request(options, (res) => {
      let body = '';
      res.on('data', (chunk) => (body += chunk));
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, data: JSON.parse(body) });
        } catch (e) {
          resolve({ status: res.statusCode, data: body });
        }
      });
    });

    req.on('error', reject);
    req.end();
  });
}

async function run() {
  console.log('Testing CarePulse Endpoints...');
  const patientLogin = await post('/api/auth/login', {
    email: 'john.doe@example.com',
    password: 'Patient@123',
  });
  console.log('Patient Login:', patientLogin.status === 200 ? 'PASS' : 'FAIL');
  const patientToken = patientLogin.data?.token;

  if (patientToken) {
    const appointments = await get('/api/appointments/my', patientToken);
    console.log('Patient Appointments:', appointments.status === 200 ? 'PASS' : 'FAIL');

    const doctors = await get('/api/doctors', patientToken);
    console.log('Doctor Discovery:', doctors.status === 200 ? 'PASS' : 'FAIL');

    const records = await get('/api/records/my', patientToken);
    console.log('Patient Records:', records.status === 200 ? 'PASS' : 'FAIL');

    const prescriptions = await get('/api/prescriptions/my', patientToken);
    console.log('Patient Prescriptions:', prescriptions.status === 200 ? 'PASS' : 'FAIL');

    const aiResp = await post('/api/ai/chat', { message: 'I have a mild headache.' }, patientToken);
    console.log('AI Companion & Disclaimer:', aiResp.status === 200 && aiResp.data?.disclaimer ? 'PASS' : 'FAIL');
  }

  const doctorLogin = await post('/api/auth/login', {
    email: 'dr.jenkins@carepulse.com',
    password: 'Doctor@123',
  });
  console.log('Doctor Login:', doctorLogin.status === 200 ? 'PASS' : 'FAIL');

  const adminLogin = await post('/api/auth/login', {
    email: 'admin@carepulse.com',
    password: 'Admin@123',
  });
  console.log('Admin Login:', adminLogin.status === 200 ? 'PASS' : 'FAIL');
}

run().catch(console.error);
