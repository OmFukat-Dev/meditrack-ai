import axios from 'axios'

// API Configuration
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'
const PATIENT_SERVICE_URL = API_BASE_URL
const VITALS_SERVICE_URL = API_BASE_URL
const ALERT_SERVICE_URL = API_BASE_URL
const REPORT_SERVICE_URL = API_BASE_URL

type AuthHeaders = Record<string, string>

function readStoredAuth(): any | null {
  if (typeof window === 'undefined' || typeof window.localStorage === 'undefined') {
    return null
  }

  for (const key of ['meditrack_auth', 'currentUser']) {
    const value = localStorage.getItem(key)
    if (!value) {
      continue
    }

    try {
      return JSON.parse(value)
    } catch (error) {
      console.warn(`Failed to parse ${key}`, error)
    }
  }

  return null
}

function buildAuthHeaders(): AuthHeaders {
  const headers: AuthHeaders = {}
  const storedUser = readStoredAuth()
  const hasStorage = typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
  const token = storedUser?.token
    ?? storedUser?.sessionToken
    ?? (hasStorage ? localStorage.getItem('token') : null)
    ?? (hasStorage ? localStorage.getItem('sessionToken') : null)
    ?? (hasStorage ? localStorage.getItem('authToken') : null)

  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const role = storedUser?.role ?? storedUser?.roleName
  if (role) {
    headers['X-User-Role'] = String(role)
  }

  const email = storedUser?.email
  if (email) {
    headers['X-User-Email'] = String(email)
  }

  const department = storedUser?.department ?? storedUser?.departmentName
  if (department) {
    headers['X-User-Department'] = String(department)
  }

  const displayName = storedUser?.name
    ?? [storedUser?.firstName, storedUser?.lastName].filter(Boolean).join(' ').trim()
  if (displayName) {
    headers['X-User-Display-Name'] = String(displayName)
  }

  const id = storedUser?.id
  if (id) {
    headers['X-User-Id'] = String(id)
  }

  return headers
}

function mergeHeaders(existing?: unknown) {
  return {
    ...(existing as Record<string, string> | undefined),
    ...buildAuthHeaders(),
  }
}

// Create axios instances
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

const patientClient = axios.create({
  baseURL: PATIENT_SERVICE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

const vitalsClient = axios.create({
  baseURL: VITALS_SERVICE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to add auth token
apiClient.interceptors.request.use((config) => {
  config.headers = mergeHeaders(config.headers as any) as any
  return config
})

patientClient.interceptors.request.use((config) => {
  config.headers = mergeHeaders(config.headers as any) as any
  return config
})

vitalsClient.interceptors.request.use((config) => {
  config.headers = mergeHeaders(config.headers as any) as any
  return config
})

// Response interceptor to handle errors
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error)
    if (error.response?.status === 401) {
      // Handle unauthorized access
      localStorage.removeItem('authToken')
      localStorage.removeItem('token')
      localStorage.removeItem('sessionToken')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

// API Service Functions
export const apiService = {
  // Health checks
  async checkBackendHealth() {
    try {
      const response = await apiClient.get('/actuator/health')
      return response.data
    } catch (error) {
      console.error('Backend health check failed:', error)
      return null
    }
  },

  async checkServiceHealth(serviceUrl: string) {
    try {
      const response = await axios.get(`${serviceUrl}/actuator/health`)
      return response.data
    } catch (error) {
      console.error(`Service health check failed for ${serviceUrl}:`, error)
      return null
    }
  },

  // Patient Service
  async getPatients(page = 0, size = 20) {
    try {
      const response = await patientClient.get(`/api/patients?page=${page}&size=${size}`)
      return response.data
    } catch (error) {
      console.error('Failed to fetch patients:', error)
      throw error
    }
  },

  async getPatientById(id: string) {
    try {
      const response = await patientClient.get(`/api/patients/${id}`)
      return response.data
    } catch (error) {
      console.error('Failed to fetch patient:', error)
      throw error
    }
  },

  async createPatient(patientData: any) {
    try {
      const response = await patientClient.post('/api/patients', patientData)
      return response.data
    } catch (error) {
      console.error('Failed to create patient:', error)
      throw error
    }
  },

  // Vitals Service
  async getVitals(patientId: string, page = 0, size = 50) {
    try {
      const response = await vitalsClient.get(`/api/vitals/patient/${patientId}?page=${page}&size=${size}`)
      return response.data
    } catch (error) {
      console.error('Failed to fetch vitals:', error)
      throw error
    }
  },

  async createVital(vitalData: any) {
    try {
      const response = await vitalsClient.post('/api/vitals', vitalData)
      return response.data
    } catch (error) {
      console.error('Failed to create vital:', error)
      throw error
    }
  },

  async getVitalSummary(patientId: string) {
    try {
      const response = await vitalsClient.get(`/api/vitals/patient/${patientId}/summary`)
      return response.data
    } catch (error) {
      console.error('Failed to fetch vital summary:', error)
      throw error
    }
  },

  // Alert Service
  async getAlerts(page = 0, size = 20) {
    try {
      const response = await axios.get(`${ALERT_SERVICE_URL}/api/alert-service/alerts?page=${page}&size=${size}`, {
        headers: buildAuthHeaders(),
      })
      return response.data
    } catch (error) {
      console.error('Failed to fetch alerts:', error)
      throw error
    }
  },

  async createAlert(alertData: any) {
    try {
      const response = await axios.post(`${ALERT_SERVICE_URL}/api/alert-service/alerts`, alertData, {
        headers: buildAuthHeaders(),
      })
      return response.data
    } catch (error) {
      console.error('Failed to create alert:', error)
      throw error
    }
  },

  // Report Service
  async generatePatientReport(patientId: string) {
    try {
      const response = await axios.get(`${REPORT_SERVICE_URL}/api/reports/patient/${patientId}/pdf`, {
        responseType: 'blob',
        headers: buildAuthHeaders(),
      })
      return response.data
    } catch (error) {
      console.error('Failed to generate patient report:', error)
      throw error
    }
  },

  async generateDoctorShiftReport() {
    try {
      const response = await axios.get(`${REPORT_SERVICE_URL}/api/reports/doctor/shift/pdf`, {
        responseType: 'blob',
        headers: buildAuthHeaders(),
      })
      return response.data
    } catch (error) {
      console.error('Failed to generate doctor shift report:', error)
      throw error
    }
  },

  async generateNurseShiftReport() {
    try {
      const response = await axios.get(`${REPORT_SERVICE_URL}/api/reports/nurse/handover/pdf`, {
        responseType: 'blob',
        headers: buildAuthHeaders(),
      })
      return response.data
    } catch (error) {
      console.error('Failed to generate nurse shift report:', error)
      throw error
    }
  },
}

// Utility function to download blob data
export const downloadBlob = (blob: Blob, filename: string) => {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

// Backend connection status
export const checkBackendConnection = async () => {
  const services = [
    { name: 'API Gateway', url: API_BASE_URL },
    { name: 'Patient Service', url: PATIENT_SERVICE_URL },
    { name: 'Vitals Service', url: VITALS_SERVICE_URL },
    { name: 'Alert Service', url: ALERT_SERVICE_URL },
    { name: 'Report Service', url: REPORT_SERVICE_URL },
  ]

  const results = await Promise.allSettled(
    services.map(async (service) => {
      const health = await apiService.checkServiceHealth(service.url)
      return { name: service.name, status: health ? 'healthy' : 'unhealthy' }
    })
  )

  return results.map(result => 
    result.status === 'fulfilled' ? result.value : { name: 'Unknown', status: 'error' }
  )
}
