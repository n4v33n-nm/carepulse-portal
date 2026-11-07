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

// Response interceptor for token expiry and standardized error handling
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const status = error.response.status;
      const data = error.response.data;

      if (status === 401) {
        error.userMessage = data?.message || 'Your session has expired. Please sign in again.';
        if (!window.location.pathname.includes('/login') && !window.location.pathname.includes('/register') && window.location.pathname !== '/') {
          localStorage.removeItem('carepulse_token');
          localStorage.removeItem('carepulse_user');
          window.location.href = '/login?expired=true';
        }
      } else if (status === 403) {
        error.userMessage = data?.message || 'Access denied: You do not have permission to perform this action.';
      } else if (status === 404) {
        error.userMessage = data?.message || 'The requested resource was not found.';
      } else if (status === 409) {
        error.userMessage = data?.message || 'A data conflict occurred. Please refresh and try again.';
      } else if (status === 400 || status === 422) {
        error.userMessage = data?.message || 'Validation error: Please verify your input and try again.';
      } else if (status >= 500) {
        error.userMessage = data?.message || 'A server error occurred. Please try again later.';
      } else {
        error.userMessage = data?.message || 'An unexpected error occurred.';
      }
    } else if (error.request) {
      error.userMessage = 'Unable to connect to CarePulse server. Please check your network connection.';
    } else {
      error.userMessage = error.message || 'An unexpected error occurred.';
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
  getSmartAvailableSlots: (id, date) => api.get(`/doctors/${id}/available-slots`, { params: { date } }),
  matchDoctors: (params) => api.get('/doctors/match', { params }),
  getAllWorkloads: () => api.get('/doctors/workload'),
  getDoctorWorkload: (id) => api.get(`/doctors/${id}/workload`),
};

export const appointmentService = {
  bookAppointment: (data) => api.post('/appointments', data),
  getMyAppointments: () => api.get('/appointments/my'),
  getAppointmentById: (id) => api.get(`/appointments/${id}`),
  updateStatus: (id, data) => api.put(`/appointments/${id}/status`, data),
  cancelAppointment: (id, reason) => api.delete(`/appointments/${id}`, { params: { reason } }),
  rescheduleAppointment: (id, data) => api.put(`/appointments/${id}/reschedule`, data),
  getAvailableSlots: (doctorId, date) => api.get('/appointments/available-slots', { params: { doctorId, date } }),
  joinWaitlist: (data) => api.post('/appointments/waitlist', data),
  getMyWaitlist: () => api.get('/appointments/waitlist/my'),
  cancelWaitlist: (id) => api.delete(`/appointments/waitlist/${id}`),
  getWaitTime: (id) => api.get(`/appointments/${id}/wait-time`),
};

export const patientService = {
  getAllPatients: () => api.get('/patients'),
  getPatientById: (id) => api.get(`/patients/${id}`),
  updateProfile: (id, data) => api.put(`/patients/${id}/profile`, data),
};

export const recordService = {
  getMyRecords: () => api.get('/records/my'),
  getRecordsByPatientId: (patientId) => api.get(`/records/patient/${patientId}`),
  getRecordsByDoctor: () => api.get('/records/doctor/my'),
  getRecordById: (id) => api.get(`/records/${id}`),
  createRecord: (data) => api.post('/records', data),
  generateAiDraftSummary: (id) => api.post(`/records/${id}/ai-summary-draft`),
  reviewAiSummary: (id, data) => api.post(`/records/${id}/ai-summary-review`, data),
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

export const adminService = {
  getDashboardStats: () => api.get('/admin/dashboard'),
  getAllUsers: () => api.get('/admin/users'),
  toggleUserStatus: (id) => api.put(`/admin/users/${id}/toggle-status`),
  getAuditLogs: () => api.get('/audit-logs'),
  getAnalytics: (params) => api.get('/admin/analytics', { params }),
};

export const emergencyService = {
  // Patient endpoints
  createEmergencyRequest: (data) => api.post('/emergency-requests', data),
  getMyEmergencyRequests: () => api.get('/emergency-requests/my'),
  getEmergencyRequestById: (id) => api.get(`/emergency-requests/${id}`),
  cancelEmergencyRequest: (id) => api.put(`/emergency-requests/${id}/cancel`),

  // Doctor endpoints
  getAssignedEmergencyRequests: () => api.get('/emergency-requests/assigned'),
  updateEmergencyStatus: (id, data) => api.patch(`/emergency-requests/${id}/status`, data),
  getMyEmergencyDuty: () => api.get('/emergency-roster/my-duty'),
  getDoctorTodayDutySummary: () => api.get('/doctor/emergency-duty/today'),
  updateMyDoctorStatus: (data) => api.put('/emergency-roster/my-status', data),

  // Admin endpoints
  getAvailableDoctorsForRoster: () => api.get('/admin/emergency-roster/doctors'),
  getRoster: (params) => api.get('/admin/emergency-roster', { params }),
  getRosterRange: (params) => api.get('/admin/emergency-roster/range', { params }),
  createRoster: (data) => api.post('/admin/emergency-roster', data),
  updateRoster: (id, data) => api.put(`/admin/emergency-roster/${id}`, data),
  deleteRoster: (id) => api.delete(`/admin/emergency-roster/${id}`),
  getAllEmergencyRequests: () => api.get('/admin/emergency-requests'),
  getEmergencyStats: () => api.get('/admin/emergency-stats'),
  generateRoster: (data) => api.post('/admin/emergency-roster/generate', data),
  getEmergencyAnalytics: (params) => api.get('/admin/emergency-analytics', { params }),
};

export const doctorConnectService = {
  createConversation: (data) => api.post('/doctor-connect/conversations', data),
  getConversations: (params) => api.get('/doctor-connect/conversations', { params }),
  getConversationById: (id) => api.get(`/doctor-connect/conversations/${id}`),
  sendMessage: (id, data) => api.post(`/doctor-connect/conversations/${id}/messages`, data),
  updateStatus: (id, data) => api.patch(`/doctor-connect/conversations/${id}/status`, data),
  getStats: () => api.get('/doctor-connect/stats'),
};

export const doctorLeaveService = {
  submitLeave: (data) => api.post('/doctor-leave/requests', data),
  getMyLeaves: () => api.get('/doctor-leave/my'),
  getAllLeaves: (status) => api.get('/doctor-leave', { params: status ? { status } : {} }),
  getLeaveById: (id) => api.get(`/doctor-leave/${id}`),
  reviewLeave: (id, data) => api.put(`/doctor-leave/${id}/review`, data),
  getAffectedAppointments: (id) => api.get(`/doctor-leave/${id}/affected-appointments`),
  getReplacementCandidates: (appointmentId) => api.get(`/doctor-leave/appointments/${appointmentId}/candidates`),
  reassignAppointment: (appointmentId, data) => api.post(`/doctor-leave/appointments/${appointmentId}/reassign`, data),
  getPatientPendingReassignments: () => api.get('/doctor-leave/reassignments/my-pending'),
  respondToReassignment: (id, data) => api.post(`/doctor-leave/reassignments/${id}/consent`, data),
  getDoctorReassignedHistory: () => api.get('/doctor-leave/reassignments/doctor-history'),
};

export const healthService = {
  checkHealth: () => api.get('/health'),
};

export const reportService = {
  uploadReport: (formData) => api.post('/reports/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  getMyReports: () => api.get('/reports/my'),
  getReportsByPatientId: (patientId) => api.get(`/reports/patient/${patientId}`),
  getReportById: (id) => api.get(`/reports/${id}`),
  deleteReport: (id) => api.delete(`/reports/${id}`),
  downloadReportBlob: (id) => api.get(`/reports/${id}/download`, { responseType: 'blob' }),
  viewReportBlob: (id) => api.get(`/reports/${id}/view`, { responseType: 'blob' }),
};

export default api;

