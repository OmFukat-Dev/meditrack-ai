// Minimal mock dataset for local development and fallback auth
// Contains 1 doctor, 1 nurse, 1 patient, and a few vitals

export type Role = 'admin' | 'doctor' | 'nurse' | 'viewer'

export interface User {
  id: string
  email: string
  password: string
  name: string
  role: Role
  department?: string
  isActive: boolean
  createdAt?: string
}

export interface BloodPressure {
  systolic: number
  diastolic: number
}

export interface VitalSign {
  id: string
  patientId: string
  timestamp: string
  readingTimestamp: string
  heartRate: number
  bloodPressure: BloodPressure
  systolic?: number
  diastolic?: number
  temperature: number
  oxygenSaturation: number
  respiratoryRate?: number
  recordedBy: string
}

export interface MedicalReport {
  id: string
  patientId: string
  doctorId?: string
  title?: string
  content?: string
  createdAt?: string
}

export interface Patient {
  id: string
  patientIdentifier: string
  email: string
  password: string
  name: string
  age: number
  gender: 'Male' | 'Female' | 'Other'
  roomNumber: string
  condition: 'STABLE' | 'CRITICAL' | 'EMERGENCY' | string
  admittedDate: string
  doctorId: string
  nurseId: string
  mobileNumber: string
  guardianName: string
  guardianMobile: string
  vitals: VitalSign[]
  reports: MedicalReport[]
}

// Minimal but compatible mock data
export const mockUsers: User[] = [
  { id: 'admin-om', email: 'om@meditrackadmin.ai', password: 'password123', name: 'Om Admin', role: 'admin', department: 'Administration', isActive: true, createdAt: '2024-01-01' },
  { id: 'doc-dipanshu', email: 'dipanshu@meditrack.ai', password: 'password123', name: 'Dipanshu Sharma', role: 'doctor', department: 'Cardiology', isActive: true, createdAt: '2024-01-01' },
  { id: 'nurse-sarah', email: 'sarah@meditrack.ai', password: 'password123', name: 'Sarah Johnson', role: 'nurse', department: 'Cardiology', isActive: true, createdAt: '2024-01-01' }
]

export const mockVitalSigns: VitalSign[] = [
  { id: 'vs-1', patientId: 'patient-1', timestamp: new Date(Date.now() - 1000 * 60 * 60).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 60).toISOString(), heartRate: 72, bloodPressure: { systolic: 120, diastolic: 80 }, temperature: 98.6, oxygenSaturation: 98, respiratoryRate: 16, recordedBy: 'nurse-sarah' },
  { id: 'vs-2', patientId: 'patient-1', timestamp: new Date(Date.now() - 1000 * 60 * 10).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 10).toISOString(), heartRate: 75, bloodPressure: { systolic: 118, diastolic: 78 }, temperature: 98.4, oxygenSaturation: 97, respiratoryRate: 17, recordedBy: 'nurse-sarah' },
  { id: 'vs-3', patientId: 'patient-2', timestamp: new Date(Date.now() - 1000 * 60 * 120).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 120).toISOString(), heartRate: 82, bloodPressure: { systolic: 128, diastolic: 84 }, temperature: 99.1, oxygenSaturation: 97, respiratoryRate: 18, recordedBy: 'nurse-sarah' },
  { id: 'vs-4', patientId: 'patient-2', timestamp: new Date(Date.now() - 1000 * 60 * 40).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 40).toISOString(), heartRate: 88, bloodPressure: { systolic: 132, diastolic: 86 }, temperature: 99.4, oxygenSaturation: 95, respiratoryRate: 19, recordedBy: 'nurse-sarah' },
  { id: 'vs-5', patientId: 'patient-3', timestamp: new Date(Date.now() - 1000 * 60 * 150).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 150).toISOString(), heartRate: 68, bloodPressure: { systolic: 114, diastolic: 74 }, temperature: 98.3, oxygenSaturation: 99, respiratoryRate: 16, recordedBy: 'nurse-sarah' },
  { id: 'vs-6', patientId: 'patient-3', timestamp: new Date(Date.now() - 1000 * 60 * 55).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 55).toISOString(), heartRate: 71, bloodPressure: { systolic: 116, diastolic: 76 }, temperature: 98.5, oxygenSaturation: 98, respiratoryRate: 16, recordedBy: 'nurse-sarah' },
  { id: 'vs-7', patientId: 'patient-4', timestamp: new Date(Date.now() - 1000 * 60 * 180).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 180).toISOString(), heartRate: 103, bloodPressure: { systolic: 138, diastolic: 92 }, temperature: 100.2, oxygenSaturation: 93, respiratoryRate: 22, recordedBy: 'nurse-sarah' },
  { id: 'vs-8', patientId: 'patient-4', timestamp: new Date(Date.now() - 1000 * 60 * 22).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 22).toISOString(), heartRate: 108, bloodPressure: { systolic: 142, diastolic: 94 }, temperature: 100.4, oxygenSaturation: 91, respiratoryRate: 23, recordedBy: 'nurse-sarah' },
  { id: 'vs-9', patientId: 'patient-5', timestamp: new Date(Date.now() - 1000 * 60 * 210).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 210).toISOString(), heartRate: 118, bloodPressure: { systolic: 155, diastolic: 100 }, temperature: 101.2, oxygenSaturation: 89, respiratoryRate: 24, recordedBy: 'nurse-sarah' },
  { id: 'vs-10', patientId: 'patient-5', timestamp: new Date(Date.now() - 1000 * 60 * 30).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 30).toISOString(), heartRate: 121, bloodPressure: { systolic: 160, diastolic: 102 }, temperature: 101.4, oxygenSaturation: 88, respiratoryRate: 25, recordedBy: 'nurse-sarah' },
  { id: 'vs-11', patientId: 'patient-6', timestamp: new Date(Date.now() - 1000 * 60 * 240).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 240).toISOString(), heartRate: 76, bloodPressure: { systolic: 122, diastolic: 81 }, temperature: 98.9, oxygenSaturation: 97, respiratoryRate: 17, recordedBy: 'nurse-sarah' },
  { id: 'vs-12', patientId: 'patient-6', timestamp: new Date(Date.now() - 1000 * 60 * 14).toISOString(), readingTimestamp: new Date(Date.now() - 1000 * 60 * 14).toISOString(), heartRate: 74, bloodPressure: { systolic: 120, diastolic: 80 }, temperature: 98.7, oxygenSaturation: 98, respiratoryRate: 16, recordedBy: 'nurse-sarah' }
]

export const mockPatients: Patient[] = [
  { id: 'patient-1', patientIdentifier: 'PT-001', email: 'john.doe@example.com', password: 'password123', name: 'John Doe', age: 45, gender: 'Male', roomNumber: 'A-101', condition: 'STABLE', admittedDate: '2024-04-15', doctorId: 'doc-dipanshu', nurseId: 'nurse-sarah', mobileNumber: '+1-555-0101', guardianName: 'Jane Doe', guardianMobile: '+1-555-0102', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-1'), reports: [] },
  { id: 'patient-2', patientIdentifier: 'PT-002', email: 'jane.smith@example.com', password: 'password123', name: 'Jane Smith', age: 32, gender: 'Female', roomNumber: 'B-205', condition: 'CARDIAC_MONITORING', admittedDate: '2024-04-18', doctorId: 'doc-dipanshu', nurseId: 'nurse-sarah', mobileNumber: '+1-555-0103', guardianName: 'Robert Smith', guardianMobile: '+1-555-0104', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-2'), reports: [] },
  { id: 'patient-3', patientIdentifier: 'PT-003', email: 'aarav.kumar@example.com', password: 'password123', name: 'Aarav Kumar', age: 58, gender: 'Male', roomNumber: 'C-312', condition: 'STABLE', admittedDate: '2024-04-16', doctorId: 'doc-dipanshu', nurseId: 'nurse-sarah', mobileNumber: '+1-555-0105', guardianName: 'Priya Kumar', guardianMobile: '+1-555-0106', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-3'), reports: [] },
  { id: 'patient-4', patientIdentifier: 'PT-004', email: 'sophia.lee@example.com', password: 'password123', name: 'Sophia Lee', age: 39, gender: 'Female', roomNumber: 'D-120', condition: 'CRITICAL', admittedDate: '2024-04-19', doctorId: 'doc-dipanshu', nurseId: 'nurse-sarah', mobileNumber: '+1-555-0107', guardianName: 'Daniel Lee', guardianMobile: '+1-555-0108', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-4'), reports: [] },
  { id: 'patient-5', patientIdentifier: 'PT-005', email: 'emma.patel@example.com', password: 'password123', name: 'Emma Patel', age: 67, gender: 'Female', roomNumber: 'E-205', condition: 'EMERGENCY', admittedDate: '2024-04-20', doctorId: 'doc-dipanshu', nurseId: 'nurse-sarah', mobileNumber: '+1-555-0109', guardianName: 'Harsh Patel', guardianMobile: '+1-555-0110', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-5'), reports: [] },
  { id: 'patient-6', patientIdentifier: 'PT-006', email: 'liam.wilson@example.com', password: 'password123', name: 'Liam Wilson', age: 52, gender: 'Male', roomNumber: 'F-410', condition: 'POST_OPERATIVE', admittedDate: '2024-04-17', doctorId: 'doc-dipanshu', nurseId: 'nurse-sarah', mobileNumber: '+1-555-0111', guardianName: 'Olivia Wilson', guardianMobile: '+1-555-0112', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-6'), reports: [] }
]

export const mockMedicalReports: MedicalReport[] = []

// Authentication fallback
export const authenticateUser = (email: string, password: string): User | null => {
  const user = mockUsers.find(u => u.email === email && u.password === password)
  return user || null
}

// CRUD helpers expected by frontend
export const addNewUser = (userData: Partial<User>) => {
  const newUser: User = {
    id: `user-${Date.now()}`,
    email: userData.email || '',
    password: userData.password || 'password123',
    name: userData.name || '',
    role: userData.role || 'viewer',
    department: userData.department || '',
    isActive: true,
    createdAt: new Date().toISOString().split('T')[0]
  }
  mockUsers.push(newUser)
  return newUser
}

export const updateUser = (userId: string, updates: Partial<User>) => {
  const idx = mockUsers.findIndex(u => u.id === userId)
  if (idx === -1) throw new Error('User not found')
  mockUsers[idx] = { ...mockUsers[idx], ...updates }
  return mockUsers[idx]
}

export const addNewPatient = (patientData: Partial<Patient>) => {
  const newPatient: Patient = {
    id: `patient-${Date.now()}`,
    patientIdentifier: patientData.patientIdentifier || `PT-${String(mockPatients.length + 1).padStart(3, '0')}`,
    email: patientData.email || '',
    password: patientData.password || 'password123',
    name: patientData.name || 'Unnamed',
    age: patientData.age || 0,
    gender: patientData.gender || 'Male',
    roomNumber: patientData.roomNumber || '',
    condition: patientData.condition || 'STABLE',
    admittedDate: patientData.admittedDate || new Date().toISOString().split('T')[0],
    doctorId: patientData.doctorId || '',
    nurseId: patientData.nurseId || '',
    mobileNumber: patientData.mobileNumber || '',
    guardianName: patientData.guardianName || '',
    guardianMobile: patientData.guardianMobile || '',
    vitals: [],
    reports: []
  }
  mockPatients.push(newPatient)
  return newPatient
}

export const updatePatient = (patientId: string, updates: Partial<Patient>) => {
  const idx = mockPatients.findIndex(p => p.id === patientId)
  if (idx === -1) throw new Error('Patient not found')
  mockPatients[idx] = { ...mockPatients[idx], ...updates }
  return mockPatients[idx]
}

export const getPatientsByDoctor = (doctorId: string) => mockPatients.filter(p => p.doctorId === doctorId)
export const getPatientsByNurse = (nurseId: string) => mockPatients.filter(p => p.nurseId === nurseId)
export const getVitalSignsByPatient = (patientId: string) => mockVitalSigns.filter(v => v.patientId === patientId)

export const addVitalSign = (vitalData: Partial<VitalSign>) => {
  const newVital: VitalSign = {
    id: `v-${Date.now()}`,
    patientId: vitalData.patientId || '',
    timestamp: vitalData.timestamp || vitalData.readingTimestamp || new Date().toISOString(),
    readingTimestamp: vitalData.readingTimestamp || vitalData.timestamp || new Date().toISOString(),
    heartRate: vitalData.heartRate ?? 0,
    bloodPressure: vitalData.bloodPressure ?? { systolic: vitalData.systolic ?? 0, diastolic: vitalData.diastolic ?? 0 },
    systolic: vitalData.systolic,
    diastolic: vitalData.diastolic,
    temperature: vitalData.temperature ?? 0,
    oxygenSaturation: vitalData.oxygenSaturation ?? 0,
    respiratoryRate: vitalData.respiratoryRate ?? 0,
    recordedBy: vitalData.recordedBy || 'nurse-sarah'
  }
  mockVitalSigns.unshift(newVital)
  const patient = mockPatients.find(p => p.id === newVital.patientId)
  if (patient) {
    patient.vitals = [newVital, ...(patient.vitals || [])]
  }
  return newVital
}

export const generateMedicalReport = (patientId: string) => {
  const patient = mockPatients.find(p => p.id === patientId)
  if (!patient) return ''
  const vitals = mockVitalSigns.filter(v => v.patientId === patientId)
  return `Report for ${patient.name} - ${vitals.length} vitals recorded.`
}

export default {
  mockUsers,
  mockPatients,
  mockVitalSigns,
  mockMedicalReports,
  authenticateUser,
  addNewUser,
  updateUser,
  addNewPatient,
  updatePatient,
  getPatientsByDoctor,
  getPatientsByNurse,
  getVitalSignsByPatient,
  addVitalSign,
  generateMedicalReport
}
