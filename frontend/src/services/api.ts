import axios from 'axios';

// Use the local Vite proxy in development and a relative path in production.
export const GATEWAY_URL = import.meta.env.VITE_API_BASE_URL ?? '/api';
export const SIMULATOR_URL = import.meta.env.VITE_SIMULATOR_BASE_URL ?? '/api/simulator';

const api = axios.create({
  baseURL: GATEWAY_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add JWT Interceptor
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Auth API
export const authApi = {
  login: (credentials: any) => api.post('/auth/login', credentials),
};

// Patients API
export const patientApi = {
  getAll: (page = 0, size = 20) => api.get(`/patients?page=${page}&size=${size}`),
  getPatientById: (id: string) => api.get(`/patients/${id}`),
  getPatientByIdentifier: (patientIdentifier: string) => api.get(`/patients/identifier/${patientIdentifier}`),
  getStats: () => api.get('/patients/statistics/total-active'),
  updateCondition: (id: string, condition: string) => api.patch(`/patients/${id}/condition`, { condition }),
};

// Staff API
export const staffApi = {
  getAll: (role?: string) => api.get(`/staff-members${role ? `?role=${role}` : ''}`),
  getDoctors: () => api.get('/staff-members/doctors'),
  getNurses: () => api.get('/staff-members/nurses'),
  create: (staff: any) => api.post('/staff-members', staff),
  update: (id: string | number, staff: any) => api.put(`/staff-members/${id}`, staff),
  remove: (id: string | number) => api.delete(`/staff-members/${id}`),
};

// Backwards-compatible alias
export const doctorApi = {
  getAllDoctors: () => staffApi.getDoctors(),
  addDoctor: (doctor: any) => staffApi.create({ ...doctor, role: 'doctor' }),
};

// Vitals API
export const vitalsApi = {
  createVital: (reading: any) => api.post('/vitals', reading),
  getPatientReadings: (patientId: string | number, params?: Record<string, string | number | undefined>) =>
    api.get(`/vitals/patient/${patientId}`, { params }),
  getLatestVitals: (patientId: string | number) => api.get(`/vitals/patient/${patientId}/latest`),
  getAbnormalVitals: (patientId: string | number) => api.get(`/vitals/patient/${patientId}/abnormal`),
  getCriticalVitals: (patientId: string | number) => api.get(`/vitals/patient/${patientId}/critical`),
  getSummary: (patientId: string | number) => api.get(`/vitals/patient/${patientId}/summary`),
};

// Report API
export const reportApi = {
  downloadPatientReport: (patientId: string) => api.get(`/reports/patient/${patientId}/pdf`, { responseType: 'blob' }),
  downloadDoctorShiftReport: () => api.get('/reports/doctor/shift/pdf', { responseType: 'blob' }),
  downloadNurseHandoverReport: () => api.get('/reports/nurse/handover/pdf', { responseType: 'blob' }),
};

// Alert API
export const alertApi = {
  getAll: (limit = 100) => api.get(`/alert-service/alerts?limit=${limit}`),
  getById: (alertId: string) => api.get(`/alert-service/alerts/${alertId}`),
  getByPatient: (patientId: string | number) => api.get(`/alert-service/alerts/patient/${patientId}`),
  updateStatus: (alertId: string, status: string) => api.patch(`/alert-service/alerts/${alertId}/status`, { status }),
  checkEscalation: (alertId: string) => api.post(`/alert-service/escalations/check/${alertId}`),
};

// Simulator API
export const simulatorApi = {
  getStatus: () => axios.get(`${SIMULATOR_URL}/status`),
  start: (patientCount = 10, intervalSeconds = 5) => axios.post(`${SIMULATOR_URL}/start`, { patientCount, intervalSeconds }),
  stop: () => axios.post(`${SIMULATOR_URL}/stop`),
  pause: () => axios.post(`${SIMULATOR_URL}/pause`),
  resume: () => axios.post(`${SIMULATOR_URL}/resume`),
  generateAnomaly: (patientId: string, vitalType: string, anomalyType: string) => 
    axios.post(`${SIMULATOR_URL}/generate/anomaly`, { patientId, vitalType, anomalyType }),
};

export default api;
