import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('carepulse_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for token expiry handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // If unauthorized and not already on login
      if (!window.location.pathname.includes('/login') && !window.location.pathname.includes('/register') && window.location.pathname !== '/') {
        localStorage.removeItem('carepulse_token');
        localStorage.removeItem('carepulse_user');
        window.location.href = '/login?expired=true';
      }
    }
    return Promise.reject(error);
  }
);

export const authService = {
  login: (credentials) => api.post('/auth/login', credentials),
  registerPatient: (data) => api.post('/auth/register/patient', data),
  registerDoctor: (data) => api.post('/auth/register/doctor', data),
  getCurrentUser: () => api.get('/auth/me'),
  updateCommunicationPreference: (preference) => api.put('/auth/communication-preference', { preference }),
  logout: () => api.post('/auth/logout'),
};

export const doctorService = {
  getAllDoctors: (params) => api.get('/doctors', { params }),
  getSpecializations: () => api.get('/doctors/specializations'),
  getDoctorById: (id) => api.get(`/doctors/${id}`),
  updateDoctorProfile: (id, data) => api.put(`/doctors/${id}/profile`, data),
  getDoctorAvailability: (id) => api.get(`/doctors/${id}/availability`),
  saveDoctorAvailability: (id, data) => api.post(`/doctors/${id}/availability`, data),
  getAvailableSlots: (id, date) => api.get(`/doctors/${id}/slots`, { params: { date } }),
};

export const appointmentService = {
  bookAppointment: (data) => api.post('/appointments', data),
  getMyAppointments: () => api.get('/appointments/my'),
  getAppointmentById: (id) => api.get(`/appointments/${id}`),
  updateStatus: (id, data) => api.put(`/appointments/${id}/status`, data),
  cancelAppointment: (id, reason) => api.delete(`/appointments/${id}`, { params: { reason } }),
};

export const recordService = {
  getMyRecords: () => api.get('/records/my'),
  getRecordsByPatientId: (patientId) => api.get(`/records/patient/${patientId}`),
  getRecordsByDoctor: () => api.get('/records/doctor/my'),
  getRecordById: (id) => api.get(`/records/${id}`),
  createRecord: (data) => api.post('/records', data),
};

export const prescriptionService = {
  getMyPrescriptions: () => api.get('/prescriptions/my'),
  getPrescriptionsByPatientId: (patientId) => api.get(`/prescriptions/patient/${patientId}`),
  getPrescriptionsByDoctor: () => api.get('/prescriptions/doctor/my'),
  getPrescriptionById: (id) => api.get(`/prescriptions/${id}`),
  createPrescription: (data) => api.post('/prescriptions', data),
};

export const caregiverService = {
  grantAccess: (data) => api.post('/caregivers', data),
  getMyCaregivers: () => api.get('/caregivers/my'),
  getAccessiblePatients: () => api.get('/caregivers/accessible-patients'),
  revokeAccess: (id) => api.delete(`/caregivers/${id}`),
};

export const notificationService = {
  getMyNotifications: () => api.get('/notifications'),
  getUnreadCount: () => api.get('/notifications/unread-count'),
  markRead: (id) => api.put(`/notifications/${id}/read`),
  markAllRead: () => api.put('/notifications/read-all'),
};

export const aiService = {
  chat: (data) => api.post('/ai/chat', data),
  getDisclaimer: () => api.get('/ai/disclaimer'),
};

export const adminService = {
  getDashboardStats: () => api.get('/admin/dashboard'),
  getAllUsers: () => api.get('/admin/users'),
  toggleUserStatus: (id) => api.put(`/admin/users/${id}/toggle-status`),
  getAuditLogs: () => api.get('/audit-logs'),
};

export default api;
