// Mock Database for MediTrack AI
export interface User {
  id: string
  email: string
  password: string
  name: string
  role: 'admin' | 'doctor' | 'nurse' | 'patient'
  department?: string
  specialization?: string
  isActive: boolean
  createdAt: string
}

export interface Patient {
  id: string
  patientIdentifier: string
  name: string
  email: string
  password: string
  age: number
  gender: string
  roomNumber: string
  condition: string
  admittedDate: string
  doctorId: string
  nurseId: string
  mobileNumber: string
  guardianName: string
  guardianMobile: string
  vitals: VitalSign[]
  reports: Report[]
}

export interface VitalSign {
  id: string
  patientId: string
  timestamp: string
  heartRate: number
  bloodPressure: {
    systolic: number
    diastolic: number
  }
  temperature: number
  oxygenSaturation: number
  recordedBy: string
}

export interface Report {
  id: string
  patientId: string
  type: 'shift' | 'medical' | 'lab'
  title: string
  content: string
  generatedAt: string
  generatedBy: string
}

// Mock Users Database
export const mockUsers: User[] = [
  // Admins
  {
    id: 'admin-1',
    email: 'om@meditrackadmin.ai',
    password: 'password123',
    name: 'Om Sharma',
    role: 'admin',
    isActive: true,
    createdAt: '2024-01-15'
  },
  {
    id: 'admin-2',
    email: 'sakshi@meditrackadmin.ai',
    password: 'password123',
    name: 'Sakshi Patel',
    role: 'admin',
    isActive: true,
    createdAt: '2024-01-20'
  },
  
  // Doctors
  {
    id: 'doc-1',
    email: 'dipanshu@meditrackcardiology.ai',
    password: 'password123',
    name: 'Dr. Dipanshu Sharma',
    role: 'doctor',
    department: 'Cardiology',
    specialization: 'Interventional Cardiology',
    isActive: true,
    createdAt: '2024-02-01'
  },
  {
    id: 'doc-2',
    email: 'ayush@meditrackneurology.ai',
    password: 'password123',
    name: 'Dr. Ayush Kumar',
    role: 'doctor',
    department: 'Neurology',
    specialization: 'Neurology',
    isActive: true,
    createdAt: '2024-02-05'
  },
  {
    id: 'doc-3',
    email: 'tanmay@meditrackpediatrics.ai',
    password: 'password123',
    name: 'Dr. Tanmay Singh',
    role: 'doctor',
    department: 'Pediatrics',
    specialization: 'Pediatrics',
    isActive: true,
    createdAt: '2024-02-10'
  },
  {
    id: 'doc-4',
    email: 'chetan@meditrackoncology.ai',
    password: 'password123',
    name: 'Dr. Chetan Kumar',
    role: 'doctor',
    department: 'Oncology',
    specialization: 'Medical Oncology',
    isActive: true,
    createdAt: '2024-02-15'
  },
  {
    id: 'doc-5',
    email: 'monir@meditrackorthopedics.ai',
    password: 'password123',
    name: 'Dr. Monir Ali',
    role: 'doctor',
    department: 'Orthopedics',
    specialization: 'Orthopedic Surgery',
    isActive: true,
    createdAt: '2024-02-20'
  },
  
  // Nurses
  {
    id: 'nurse-1',
    email: 'nurseone@meditrackcardiology.ai',
    password: 'password123',
    name: 'Nurse Sarah Johnson',
    role: 'nurse',
    department: 'Cardiology',
    isActive: true,
    createdAt: '2024-03-01'
  },
  {
    id: 'nurse-2',
    email: 'nursetwo@meditrackneurology.ai',
    password: 'password123',
    name: 'Nurse Emily Davis',
    role: 'nurse',
    department: 'Neurology',
    isActive: true,
    createdAt: '2024-03-05'
  },
  {
    id: 'nurse-3',
    email: 'nursethree@meditrackpediatrics.ai',
    password: 'password123',
    name: 'Nurse Jessica Wilson',
    role: 'nurse',
    department: 'Pediatrics',
    isActive: true,
    createdAt: '2024-03-10'
  },
  {
    id: 'nurse-4',
    email: 'nursefour@meditrackoncology.ai',
    password: 'password123',
    name: 'Nurse Ashley Brown',
    role: 'nurse',
    department: 'Oncology',
    isActive: true,
    createdAt: '2024-03-15'
  },
  {
    id: 'nurse-5',
    email: 'nursefive@meditrackorthopedics.ai',
    password: 'password123',
    name: 'Nurse Michelle Lee',
    role: 'nurse',
    department: 'Orthopedics',
    isActive: true,
    createdAt: '2024-03-20'
  }
]

// Mock Patients Database
export const mockPatients: Patient[] = [
  {
    id: 'patient-1',
    patientIdentifier: 'PT-001',
    name: 'John Doe',
    email: 'john@patient.com',
    password: 'password123',
    age: 45,
    gender: 'Male',
    roomNumber: 'A-101',
    condition: 'Post-Surgery Recovery',
    admittedDate: '2024-04-15',
    doctorId: 'doc-1',
    nurseId: 'nurse-1',
    mobileNumber: '+1-555-0123-4567',
    guardianName: 'Jane Doe',
    guardianMobile: '+1-555-0123-4568',
    vitals: [
      {
        id: 'vital-1',
        patientId: 'patient-1',
        timestamp: '2024-04-20T08:00:00Z',
        heartRate: 72,
        bloodPressure: { systolic: 120, diastolic: 80 },
        temperature: 98.6,
        oxygenSaturation: 98,
        recordedBy: 'nurse-1'
      },
      {
        id: 'vital-2',
        patientId: 'patient-1',
        timestamp: '2024-04-20T12:00:00Z',
        heartRate: 75,
        bloodPressure: { systolic: 122, diastolic: 82 },
        temperature: 98.8,
        oxygenSaturation: 97,
        recordedBy: 'nurse-1'
      },
      {
        id: 'vital-3',
        patientId: 'patient-1',
        timestamp: '2024-04-20T16:00:00Z',
        heartRate: 70,
        bloodPressure: { systolic: 118, diastolic: 78 },
        temperature: 98.4,
        oxygenSaturation: 99,
        recordedBy: 'nurse-1'
      }
    ],
    reports: []
  },
  {
    id: 'patient-2',
    patientIdentifier: 'PT-002',
    name: 'Jane Smith',
    email: 'jane@patient.com',
    password: 'password123',
    age: 32,
    gender: 'Female',
    roomNumber: 'B-205',
    condition: 'Cardiac Monitoring',
    admittedDate: '2024-04-18',
    doctorId: 'doc-1',
    nurseId: 'nurse-1',
    mobileNumber: '+1-555-0123-4569',
    guardianName: 'Robert Smith',
    guardianMobile: '+1-555-0123-4570',
    vitals: [
      {
        id: 'vital-4',
        patientId: 'patient-2',
        timestamp: '2024-04-20T09:00:00Z',
        heartRate: 85,
        bloodPressure: { systolic: 130, diastolic: 85 },
        temperature: 99.2,
        oxygenSaturation: 96,
        recordedBy: 'nurse-1'
      },
      {
        id: 'vital-5',
        patientId: 'patient-2',
        timestamp: '2024-04-20T13:00:00Z',
        heartRate: 88,
        bloodPressure: { systolic: 132, diastolic: 86 },
        temperature: 99.4,
        oxygenSaturation: 95,
        recordedBy: 'nurse-1'
      }
    ],
    reports: []
  }
]

// Database Functions
export const authenticateUser = (email: string, password: string): User | null => {
  const user = mockUsers.find(u => u.email === email && u.password === password && u.isActive)
  return user || null
}

export const getPatientsByDoctor = (doctorId: string): Patient[] => {
  return mockPatients.filter(p => p.doctorId === doctorId)
}

export const getPatientsByNurse = (nurseId: string): Patient[] => {
  return mockPatients.filter(p => p.nurseId === nurseId)
}

export const addVitalSign = (patientId: string, vital: Omit<VitalSign, 'id' | 'timestamp'>): VitalSign => {
  const newVital: VitalSign = {
    ...vital,
    id: `vital-${Date.now()}`,
    timestamp: new Date().toISOString()
  }
  
  const patient = mockPatients.find(p => p.id === patientId)
  if (patient) {
    patient.vitals.push(newVital)
  }
  
  return newVital
}

export const generateReport = (patientId: string, report: Omit<Report, 'id' | 'generatedAt'>): Report => {
  const newReport: Report = {
    ...report,
    id: `report-${Date.now()}`,
    generatedAt: new Date().toISOString()
  }
  
  const patient = mockPatients.find(p => p.id === patientId)
  if (patient) {
    patient.reports.push(newReport)
  }
  
  return newReport
}

export const addNewUser = (user: Omit<User, 'id' | 'createdAt'>): User => {
  const newUser: User = {
    ...user,
    id: `user-${Date.now()}`,
    createdAt: new Date().toISOString()
  }
  mockUsers.push(newUser)
  return newUser
}

export const updateUser = (id: string, updates: Partial<User>): User | null => {
  const userIndex = mockUsers.findIndex(u => u.id === id)
  if (userIndex === -1) return null
  
  mockUsers[userIndex] = { ...mockUsers[userIndex], ...updates }
  return mockUsers[userIndex]
}

type NewPatientInput = Omit<Patient, 'id' | 'patientIdentifier' | 'vitals' | 'reports'> & {
  patientIdentifier?: string
}

export const addNewPatient = (patient: NewPatientInput): Patient => {
  const newPatient: Patient = {
    ...patient,
    id: `patient-${Date.now()}`,
    patientIdentifier: patient.patientIdentifier || `PT-${String(mockPatients.length + 1).padStart(3, '0')}`,
    vitals: [],
    reports: []
  }
  mockPatients.push(newPatient)
  return newPatient
}

export const updatePatient = (id: string, updates: Partial<Patient>): Patient | null => {
  const patientIndex = mockPatients.findIndex(p => p.id === id)
  if (patientIndex === -1) return null
  
  mockPatients[patientIndex] = { ...mockPatients[patientIndex], ...updates }
  return mockPatients[patientIndex]
}
