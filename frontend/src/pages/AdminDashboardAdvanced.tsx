import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { AuthService } from '../auth/AuthService'
import { mockUsers, mockPatients, addNewUser, updateUser, addNewPatient, updatePatient } from '../database/mockDatabase'

type User = {
  id: string
  email: string
  password: string
  name: string
  role: 'admin' | 'doctor' | 'nurse' | 'viewer'
  department?: string
  specialization?: string
  isActive: boolean
  createdAt: string
}

type Patient = {
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
  vitals: any[]
  reports: any[]
}

type Alert = {
  id: string
  patientId: string
  type: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  message: string
  timestamp: string
  status: 'ACTIVE' | 'RESOLVED' | 'ESCALATED'
  assignedTo?: string
}

export default function AdminDashboardAdvanced() {
  const [currentUser, setCurrentUser] = useState<User | null>(null)
  const [activeTab, setActiveTab] = useState<'users' | 'patients' | 'dashboard' | 'alerts' | 'financial' | 'reports' | 'audit' | 'config' | 'notifications' | 'data' | 'ai' | 'workload'>('dashboard')
  const [userType, setUserType] = useState<'doctors' | 'nurses' | 'viewers' | 'all'>('all')
  const [showUserModal, setShowUserModal] = useState(false)
  const [showPatientModal, setShowPatientModal] = useState(false)
  const [showAlertModal, setShowAlertModal] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [editingPatient, setEditingPatient] = useState<Patient | null>(null)
  const [selectedUsers, setSelectedUsers] = useState<string[]>([])
  
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    department: '',
    specialization: '',
    role: 'doctor' as 'doctor' | 'nurse' | 'viewer'
  })

  const [patientFormData, setPatientFormData] = useState({
    name: '',
    email: '',
    password: '',
    age: 0,
    gender: 'Male',
    roomNumber: '',
    condition: 'Stable',
    admittedDate: '',
    doctorId: '',
    nurseId: ''
  })

  const [alertFormData, setAlertFormData] = useState({
    type: 'MEDIUM' as 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL',
    message: '',
    patientId: '',
    assignedTo: ''
  })

  useEffect(() => {
    const user = AuthService.getCurrentUser()
    setCurrentUser(user)
  }, [])

  // Mock data for alerts
  const [alerts] = useState<Alert[]>([
    {
      id: 'alert-1',
      patientId: 'patient-1',
      type: 'CRITICAL',
      message: 'Heart rate dangerously high - 145 BPM',
      timestamp: new Date().toISOString(),
      status: 'ACTIVE',
      assignedTo: 'doc-1'
    },
    {
      id: 'alert-2',
      patientId: 'patient-2',
      type: 'HIGH',
      message: 'Blood pressure elevated - 140/95',
      timestamp: new Date(Date.now() - 3600000).toISOString(),
      status: 'ACTIVE',
      assignedTo: 'doc-1'
    }
  ])

  const filteredUsers = mockUsers.filter(u => 
    userType === 'all' ? true : u.role === userType.slice(0, -1) as 'doctor' | 'nurse' | 'viewer'
  )

  const globalStats = {
    totalPatients: mockPatients.length,
    activeCases: mockPatients.filter(p => p.condition.includes('Critical') || p.condition.includes('Emergency')).length,
    criticalCases: mockPatients.filter(p => p.condition.includes('Critical')).length,
    totalDoctors: mockUsers.filter(u => u.role === 'doctor' && u.isActive).length,
    totalNurses: mockUsers.filter(u => u.role === 'nurse' && u.isActive).length,
    activeAlerts: alerts.filter(a => a.status === 'ACTIVE').length
  }

  const handleUserSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    
    if (editingUser) {
      updateUser(editingUser.id, {
        ...formData,
        isActive: true
      })
    } else {
      addNewUser({
        ...formData,
        isActive: true
      })
    }
    
    setFormData({ name: '', email: '', password: '', department: '', specialization: '', role: 'doctor' })
    setEditingUser(null)
    setShowUserModal(false)
  }

  const handlePatientSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    
    if (editingPatient) {
      updatePatient(editingPatient.id, patientFormData)
    } else {
      addNewPatient({
        ...patientFormData,
        id: `patient-${Date.now()}`,
        patientIdentifier: `PT-${String(mockPatients.length + 1).padStart(3, '0')}`,
        vitals: [],
        reports: []
      })
    }
    
    setPatientFormData({
      name: '', email: '', password: '', age: 0, gender: 'Male',
      roomNumber: '', condition: 'Stable', admittedDate: '', doctorId: '', nurseId: ''
    })
    setEditingPatient(null)
    setShowPatientModal(false)
  }

  const handleBulkAction = (action: 'activate' | 'deactivate' | 'delete') => {
    selectedUsers.forEach(userId => {
      if (action === 'delete') {
        // In real app, this would call API to delete
        console.log(`Deleting user: ${userId}`)
      } else {
        updateUser(userId, { isActive: action === 'activate' })
      }
    })
    setSelectedUsers([])
  }

  const handleAlertAction = (alertId: string, action: 'resolve' | 'escalate') => {
    const alert = alerts.find(a => a.id === alertId)
    if (alert) {
      alert.status = action === 'resolve' ? 'RESOLVED' : 'ESCALATED'
      // In real app, this would call API
    }
  }

  const handleEditUser = (user: User) => {
    setEditingUser(user)
    setFormData({
      name: user.name,
      email: user.email,
      password: '',
      department: user.department || '',
      specialization: user.specialization || '',
      role: user.role
    })
    setShowUserModal(true)
  }

  const handleEditPatient = (patient: Patient) => {
    setEditingPatient(patient)
    setPatientFormData({
      name: patient.name,
      email: patient.email,
      password: patient.password,
      age: patient.age,
      gender: patient.gender,
      roomNumber: patient.roomNumber,
      condition: patient.condition,
      admittedDate: patient.admittedDate,
      doctorId: patient.doctorId,
      nurseId: patient.nurseId
    })
    setShowPatientModal(true)
  }

  const toggleUserSelection = (userId: string) => {
    setSelectedUsers(prev => 
      prev.includes(userId) 
        ? prev.filter(id => id !== userId)
        : [...prev, userId]
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-violet-900">
      {/* Animated Background */}
      <div className="absolute inset-0">
        <div className="absolute top-20 left-20 w-72 h-72 bg-purple-500 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-blob"></div>
        <div className="absolute top-40 right-20 w-72 h-72 bg-indigo-500 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-blob animation-delay-2000"></div>
      </div>

      {/* Header */}
      <motion.header
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative z-10 bg-white/10 backdrop-blur-lg border-b border-white/20"
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-2xl font-bold text-white">Admin Dashboard</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-white/80">Welcome, {currentUser?.name}</span>
              <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                <span className="text-white font-medium">{currentUser?.name.charAt(0)}</span>
              </div>
              <button
                onClick={() => AuthService.logout()}
                className="px-4 py-2 bg-red-500/20 text-red-300 rounded-lg hover:bg-red-500/30 transition-all"
              >
                Logout
              </button>
            </div>
          </div>
        </div>
      </motion.header>

      {/* Main Content */}
      <main className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Navigation Tabs */}
        <div className="flex space-x-2 mb-8 overflow-x-auto">
          {[
            { id: 'dashboard', label: '📊 Dashboard', count: globalStats.activeAlerts },
            { id: 'users', label: '👥 Users', count: mockUsers.filter(u => u.isActive).length },
            { id: 'patients', label: '🏥 Patients', count: mockPatients.length },
            { id: 'alerts', label: '🚨 Alerts', count: globalStats.activeAlerts },
            { id: 'financial', label: '💰 Financial' },
            { id: 'reports', label: '📁 Reports' },
            { id: 'audit', label: '📋 Audit' },
            { id: 'config', label: '⚙️ Config' },
            { id: 'notifications', label: '🔔 Notifications' },
            { id: 'data', label: '💾 Data' },
            { id: 'ai', label: '🤖 AI' },
            { id: 'workload', label: '📈 Workload' }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`px-4 py-2 rounded-lg transition-all whitespace-nowrap ${
                activeTab === tab.id
                  ? 'bg-blue-500 text-white'
                  : 'bg-white/10 text-white/60 hover:bg-white/20'
              }`}
            >
              {tab.label}
              {tab.count !== undefined && (
                <span className="ml-2 px-2 py-1 bg-red-500 text-white text-xs rounded-full">
                  {tab.count}
                </span>
              )}
            </button>
          ))}
        </div>

        {/* Dashboard Tab */}
        {activeTab === 'dashboard' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            {/* Global Stats */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-white/60 text-sm">Total Patients</p>
                    <p className="text-3xl font-bold text-white">{globalStats.totalPatients}</p>
                  </div>
                  <div className="w-12 h-12 bg-blue-500/20 rounded-full flex items-center justify-center">
                    <span className="text-2xl">🏥</span>
                  </div>
                </div>
              </div>

              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-white/60 text-sm">Active Cases</p>
                    <p className="text-3xl font-bold text-white">{globalStats.activeCases}</p>
                  </div>
                  <div className="w-12 h-12 bg-green-500/20 rounded-full flex items-center justify-center">
                    <span className="text-2xl">📊</span>
                  </div>
                </div>
              </div>

              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-white/60 text-sm">Critical Cases</p>
                    <p className="text-3xl font-bold text-white">{globalStats.criticalCases}</p>
                  </div>
                  <div className="w-12 h-12 bg-red-500/20 rounded-full flex items-center justify-center">
                    <span className="text-2xl">🚨</span>
                  </div>
                </div>
              </div>

              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-white/60 text-sm">Total Doctors</p>
                    <p className="text-3xl font-bold text-white">{globalStats.totalDoctors}</p>
                  </div>
                  <div className="w-12 h-12 bg-purple-500/20 rounded-full flex items-center justify-center">
                    <span className="text-2xl">👨‍⚕️</span>
                  </div>
                </div>
              </div>

              <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-white/60 text-sm">Total Nurses</p>
                    <p className="text-3xl font-bold text-white">{globalStats.totalNurses}</p>
                  </div>
                  <div className="w-12 h-12 bg-pink-500/20 rounded-full flex items-center justify-center">
                    <span className="text-2xl">👩‍⚕️</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Department-wise Stats */}
            <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
              <h2 className="text-xl font-bold text-white mb-4">Department Statistics</h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {['Cardiology', 'Neurology', 'Pediatrics', 'Oncology', 'Orthopedics'].map(dept => (
                  <div key={dept} className="bg-white/5 rounded-lg p-4">
                    <h3 className="font-semibold text-white mb-2">{dept}</h3>
                    <div className="space-y-1">
                      <div className="flex justify-between text-sm">
                        <span className="text-white/60">Patients:</span>
                        <span className="text-white font-medium">
                          {mockPatients.filter(p => 
                            mockUsers.find(u => u.id === p.doctorId)?.department === dept
                          ).length
                        }
                        </span>
                      </div>
                      <div className="flex justify-between text-sm">
                        <span className="text-white/60">Doctors:</span>
                        <span className="text-white font-medium">
                          {mockUsers.filter(u => u.role === 'doctor' && u.department === dept).length}
                        </span>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        )}

        {/* Patients Tab */}
        {activeTab === 'patients' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            {/* Patient Assignment Control */}
            <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-bold text-white">Patient Assignment Control</h2>
                <button
                  onClick={() => setShowPatientModal(true)}
                  className="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-all"
                >
                  ➕ Add New Patient
                </button>
              </div>

              {/* Assignment Tools */}
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div className="bg-white/5 rounded-lg p-4">
                  <h3 className="font-semibold text-white mb-3">Bulk Assignment</h3>
                  <div className="space-y-3">
                    <select className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-white/50">
                      <option value="">Select Doctor</option>
                      {mockUsers.filter(u => u.role === 'doctor').map(doctor => (
                        <option key={doctor.id} value={doctor.id}>
                          Dr. {doctor.name} ({doctor.department})
                        </option>
                      ))}
                    </select>
                    <select className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-white/50">
                      <option value="">Select Patients</option>
                      {mockPatients.map(patient => (
                        <option key={patient.id} value={patient.id}>
                          {patient.name} ({patient.patientIdentifier})
                        </option>
                      ))}
                    </select>
                    <button className="w-full px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-all">
                      Assign Selected
                    </button>
                  </div>
                </div>

                <div className="bg-white/5 rounded-lg p-4">
                  <h3 className="font-semibold text-white mb-3">Department Transfer</h3>
                  <div className="space-y-3">
                    <select className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-white/50">
                      <option value="">From Department</option>
                      <option value="cardiology">Cardiology</option>
                      <option value="neurology">Neurology</option>
                      <option value="pediatrics">Pediatrics</option>
                    </select>
                    <select className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-white/50">
                      <option value="">To Department</option>
                      <option value="cardiology">Cardiology</option>
                      <option value="neurology">Neurology</option>
                      <option value="pediatrics">Pediatrics</option>
                    </select>
                    <button className="w-full px-4 py-2 bg-purple-500 text-white rounded-lg hover:bg-purple-600 transition-all">
                      Transfer Patients
                    </button>
                  </div>
                </div>

                <div className="bg-white/5 rounded-lg p-4">
                  <h3 className="font-semibold text-white mb-3">Quick Stats</h3>
                  <div className="space-y-2">
                    <div className="flex justify-between text-sm">
                      <span className="text-white/60">Unassigned Patients:</span>
                      <span className="text-white font-medium">
                        {mockPatients.filter(p => !p.doctorId).length}
                      </span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-white/60">Critical Patients:</span>
                      <span className="text-red-400 font-medium">
                        {mockPatients.filter(p => p.condition.includes('Critical')).length}
                      </span>
                    </div>
                    <div className="flex justify-between text-sm">
                      <span className="text-white/60">Emergency Cases:</span>
                      <span className="text-yellow-400 font-medium">
                        {mockPatients.filter(p => p.condition.includes('Emergency')).length}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Patients Table */}
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-white/20">
                    <th className="text-left p-3 text-white/60">
                      <input
                        type="checkbox"
                        className="rounded"
                      />
                    </th>
                    <th className="text-left p-3 text-white/60">Patient ID</th>
                    <th className="text-left p-3 text-white/60">Name</th>
                    <th className="text-left p-3 text-white/60">Age</th>
                    <th className="text-left p-3 text-white/60">Gender</th>
                    <th className="text-left p-3 text-white/60">Room</th>
                    <th className="text-left p-3 text-white/60">Condition</th>
                    <th className="text-left p-3 text-white/60">Assigned Doctor</th>
                    <th className="text-left p-3 text-white/60">Assigned Nurse</th>
                    <th className="text-left p-3 text-white/60">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {mockPatients.map((patient, index) => (
                    <tr key={patient.id} className="border-b border-white/10 hover:bg-white/5">
                      <td className="p-3">
                        <input
                          type="checkbox"
                          className="rounded"
                        />
                      </td>
                      <td className="p-3 text-white">{patient.patientIdentifier}</td>
                      <td className="p-3 text-white">{patient.name}</td>
                      <td className="p-3 text-white">{patient.age}</td>
                      <td className="p-3 text-white">{patient.gender}</td>
                      <td className="p-3 text-white">{patient.roomNumber}</td>
                      <td className="p-3">
                        <span className={`px-2 py-1 rounded-full text-xs ${
                          patient.condition.includes('Critical') ? 'bg-red-500/20 text-red-300' :
                          patient.condition.includes('Emergency') ? 'bg-yellow-500/20 text-yellow-300' :
                          'bg-green-500/20 text-green-300'
                        }`}>
                          {patient.condition}
                        </span>
                      </td>
                      <td className="p-3 text-white">
                        {patient.doctorId ? (
                          <span className="text-blue-300">
                            Dr. {mockUsers.find(u => u.id === patient.doctorId)?.name}
                          </span>
                        ) : (
                          <span className="text-gray-400">Unassigned</span>
                        )}
                      </td>
                      <td className="p-3 text-white">
                        {patient.nurseId ? (
                          <span className="text-green-300">
                            {mockUsers.find(u => u.id === patient.nurseId)?.name}
                          </span>
                        ) : (
                          <span className="text-gray-400">Unassigned</span>
                        )}
                      </td>
                      <td className="p-3">
                        <div className="flex space-x-2">
                          <button
                            onClick={() => handleEditPatient(patient)}
                            className="px-3 py-1 bg-blue-500/20 text-blue-300 rounded hover:bg-blue-500/30 transition-all"
                          >
                            ✏️ Edit
                          </button>
                          <button
                            onClick={() => updatePatient(patient.id, { doctorId: '', nurseId: '' })}
                            className="px-3 py-1 bg-red-500/20 text-red-300 rounded hover:bg-red-500/30 transition-all"
                          >
                            🚫 Unassign
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </motion.div>
        )}

        {/* Add/Edit Patient Modal */}
        {showPatientModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 w-full max-w-2xl border border-white/20"
            >
              <h3 className="text-xl font-bold text-white mb-4">
                {editingPatient ? 'Edit Patient' : 'Add New Patient'}
              </h3>
              <form onSubmit={handlePatientSubmit} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-white/60 text-sm mb-1">Name</label>
                    <input
                      type="text"
                      value={patientFormData.name}
                      onChange={(e) => setPatientFormData({ ...patientFormData, name: e.target.value })}
                      className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                      placeholder="Enter patient name"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-white/60 text-sm mb-1">Email</label>
                    <input
                      type="email"
                      value={patientFormData.email}
                      onChange={(e) => setPatientFormData({ ...patientFormData, email: e.target.value })}
                      className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                      placeholder="Enter email"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-white/60 text-sm mb-1">Age</label>
                    <input
                      type="number"
                      value={patientFormData.age}
                      onChange={(e) => setPatientFormData({ ...patientFormData, age: parseInt(e.target.value) })}
                      className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                      placeholder="Enter age"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-white/60 text-sm mb-1">Gender</label>
                    <select
                      value={patientFormData.gender}
                      onChange={(e) => setPatientFormData({ ...patientFormData, gender: e.target.value })}
                      className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-white/50"
                    >
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-white/60 text-sm mb-1">Room Number</label>
                    <input
                      type="text"
                      value={patientFormData.roomNumber}
                      onChange={(e) => setPatientFormData({ ...patientFormData, roomNumber: e.target.value })}
                      className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                      placeholder="e.g., A-101"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-white/60 text-sm mb-1">Condition</label>
                    <select
                      value={patientFormData.condition}
                      onChange={(e) => setPatientFormData({ ...patientFormData, condition: e.target.value })}
                      className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-white/50"
                    >
                      <option value="Stable">Stable</option>
                      <option value="Critical">Critical</option>
                      <option value="Emergency">Emergency</option>
                      <option value="Post-Surgery Recovery">Post-Surgery Recovery</option>
                      <option value="Cardiac Monitoring">Cardiac Monitoring</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-white/60 text-sm mb-1">Admitted Date</label>
                    <input
                      type="date"
                      value={patientFormData.admittedDate}
                      onChange={(e) => setPatientFormData({ ...patientFormData, admittedDate: e.target.value })}
                      className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-white/50"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-white/60 text-sm mb-1">Assigned Doctor</label>
                    <select
                      value={patientFormData.doctorId}
                      onChange={(e) => setPatientFormData({ ...patientFormData, doctorId: e.target.value })}
                      className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-white/50"
                    >
                      <option value="">Select Doctor</option>
                      {mockUsers.filter(u => u.role === 'doctor').map(doctor => (
                        <option key={doctor.id} value={doctor.id}>
                          Dr. {doctor.name} ({doctor.department})
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-white/60 text-sm mb-1">Assigned Nurse</label>
                    <select
                      value={patientFormData.nurseId}
                      onChange={(e) => setPatientFormData({ ...patientFormData, nurseId: e.target.value })}
                      className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-white/50"
                    >
                      <option value="">Select Nurse</option>
                      {mockUsers.filter(u => u.role === 'nurse').map(nurse => (
                        <option key={nurse.id} value={nurse.id}>
                          {nurse.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                <div className="flex space-x-3 pt-4">
                  <button
                    type="submit"
                    className="flex-1 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-all"
                  >
                    {editingPatient ? 'Update' : 'Add'} Patient
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowPatientModal(false)
                      setEditingPatient(null)
                      setPatientFormData({
                        name: '', email: '', password: '', age: 0, gender: 'Male',
                        roomNumber: '', condition: 'Stable', admittedDate: '', doctorId: '', nurseId: ''
                      })
                    }}
                    className="flex-1 py-2 bg-white/20 text-white rounded-lg hover:bg-white/30 transition-all"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}

        {/* Users Tab */}
        {activeTab === 'users' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            {/* User Type Filter */}
            <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-bold text-white">User Management</h2>
                <div className="flex space-x-2">
                  <button
                    onClick={() => setUserType('all')}
                    className={`px-4 py-2 rounded-lg transition-all ${
                      userType === 'all' ? 'bg-blue-500 text-white' : 'bg-white/10 text-white/60'
                    }`}
                  >
                    All ({mockUsers.length})
                  </button>
                  <button
                    onClick={() => setUserType('doctors')}
                    className={`px-4 py-2 rounded-lg transition-all ${
                      userType === 'doctors' ? 'bg-blue-500 text-white' : 'bg-white/10 text-white/60'
                    }`}
                  >
                    Doctors ({mockUsers.filter(u => u.role === 'doctor').length})
                  </button>
                  <button
                    onClick={() => setUserType('nurses')}
                    className={`px-4 py-2 rounded-lg transition-all ${
                      userType === 'nurses' ? 'bg-blue-500 text-white' : 'bg-white/10 text-white/60'
                    }`}
                  >
                    Nurses ({mockUsers.filter(u => u.role === 'nurse').length})
                  </button>
                  <button
                    onClick={() => setUserType('viewers')}
                    className={`px-4 py-2 rounded-lg transition-all ${
                      userType === 'viewers' ? 'bg-blue-500 text-white' : 'bg-white/10 text-white/60'
                    }`}
                  >
                    Viewers ({mockUsers.filter(u => u.role === 'viewer').length})
                  </button>
                </div>
                <div className="flex space-x-2">
                  <button
                    onClick={() => setShowUserModal(true)}
                    className="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-all"
                  >
                    ➕ Add New User
                  </button>
                  {selectedUsers.length > 0 && (
                    <>
                      <button
                        onClick={() => handleBulkAction('activate')}
                        className="px-4 py-2 bg-green-500/20 text-green-300 rounded-lg hover:bg-green-500/30 transition-all"
                      >
                        ✅ Activate Selected
                      </button>
                      <button
                        onClick={() => handleBulkAction('deactivate')}
                        className="px-4 py-2 bg-yellow-500/20 text-yellow-300 rounded-lg hover:bg-yellow-500/30 transition-all"
                      >
                        ⏸️ Deactivate Selected
                      </button>
                      <button
                        onClick={() => handleBulkAction('delete')}
                        className="px-4 py-2 bg-red-500/20 text-red-300 rounded-lg hover:bg-red-500/30 transition-all"
                      >
                        🗑️ Delete Selected
                      </button>
                    </>
                  )}
                </div>
              </div>

              {/* Users Table */}
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-white/20">
                      <th className="text-left p-3 text-white/60">
                        <input
                          type="checkbox"
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedUsers(filteredUsers.map(u => u.id))
                            } else {
                              setSelectedUsers([])
                            }
                          }}
                          className="rounded"
                        />
                      </th>
                      <th className="text-left p-3 text-white/60">Name</th>
                      <th className="text-left p-3 text-white/60">Email</th>
                      <th className="text-left p-3 text-white/60">Role</th>
                      <th className="text-left p-3 text-white/60">Department</th>
                      <th className="text-left p-3 text-white/60">Status</th>
                      <th className="text-left p-3 text-white/60">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.map((user, index) => (
                      <tr key={user.id} className="border-b border-white/10 hover:bg-white/5">
                        <td className="p-3">
                          <input
                            type="checkbox"
                            checked={selectedUsers.includes(user.id)}
                            onChange={() => toggleUserSelection(user.id)}
                            className="rounded"
                          />
                        </td>
                        <td className="p-3 text-white">{user.name}</td>
                        <td className="p-3 text-white/80">{user.email}</td>
                        <td className="p-3">
                          <span className={`px-2 py-1 rounded-full text-xs ${
                            user.role === 'admin' ? 'bg-purple-500/20 text-purple-300' :
                            user.role === 'doctor' ? 'bg-blue-500/20 text-blue-300' :
                            user.role === 'nurse' ? 'bg-green-500/20 text-green-300' :
                            'bg-gray-500/20 text-gray-300'
                          }`}>
                            {user.role}
                          </span>
                        </td>
                        <td className="p-3 text-white/80">{user.department || '-'}</td>
                        <td className="p-3">
                          <span className={`px-2 py-1 rounded-full text-xs ${
                            user.isActive ? 'bg-green-500/20 text-green-300' : 'bg-red-500/20 text-red-300'
                          }`}>
                            {user.isActive ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                        <td className="p-3">
                          <div className="flex space-x-2">
                            <button
                              onClick={() => handleEditUser(user)}
                              className="px-3 py-1 bg-blue-500/20 text-blue-300 rounded hover:bg-blue-500/30 transition-all"
                            >
                              ✏️ Edit
                            </button>
                            <button
                              onClick={() => updateUser(user.id, { isActive: !user.isActive })}
                              className={`px-3 py-1 rounded hover:opacity-80 transition-all ${
                                user.isActive ? 'bg-red-500/20 text-red-300' : 'bg-green-500/20 text-green-300'
                              }`}
                            >
                              {user.isActive ? '🔒 Lock' : '🔓 Unlock'}
                            </button>
                            <button
                              onClick={() => updateUser(user.id, { password: 'temp123' })}
                              className="px-3 py-1 bg-yellow-500/20 text-yellow-300 rounded hover:bg-yellow-500/30 transition-all"
                            >
                              🔑 Reset Password
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </motion.div>
        )}

        {/* Add/Edit User Modal */}
        {showUserModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 w-full max-w-md border border-white/20"
            >
              <h3 className="text-xl font-bold text-white mb-4">
                {editingUser ? 'Edit User' : 'Add New User'}
              </h3>
              <form onSubmit={handleUserSubmit} className="space-y-4">
                <div>
                  <label className="block text-white/60 text-sm mb-1">Name</label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                    placeholder="Enter name"
                    required
                  />
                </div>
                <div>
                  <label className="block text-white/60 text-sm mb-1">Email</label>
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                    placeholder="Enter email"
                    required
                  />
                </div>
                {!editingUser && (
                  <div>
                    <label className="block text-white/60 text-sm mb-1">Password</label>
                    <input
                      type="password"
                      value={formData.password}
                      onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                      className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                      placeholder="Enter password"
                      required
                    />
                  </div>
                )}
                <div>
                  <label className="block text-white/60 text-sm mb-1">Role</label>
                  <select
                    value={formData.role}
                    onChange={(e) => setFormData({ ...formData, role: e.target.value as any })}
                    className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-white/50"
                  >
                    <option value="doctor">Doctor</option>
                    <option value="nurse">Nurse</option>
                    <option value="viewer">Viewer</option>
                  </select>
                </div>
                <div>
                  <label className="block text-white/60 text-sm mb-1">Department</label>
                  <input
                    type="text"
                    value={formData.department}
                    onChange={(e) => setFormData({ ...formData, department: e.target.value })}
                    className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                    placeholder="e.g., Cardiology, Neurology"
                  />
                </div>
                {formData.role === 'doctor' && (
                  <div>
                    <label className="block text-white/60 text-sm mb-1">Specialization</label>
                    <input
                      type="text"
                      value={formData.specialization}
                      onChange={(e) => setFormData({ ...formData, specialization: e.target.value })}
                      className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                      placeholder="e.g., Interventional Cardiology"
                    />
                  </div>
                )}
                <div className="flex space-x-3 pt-4">
                  <button
                    type="submit"
                    className="flex-1 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-all"
                  >
                    {editingUser ? 'Update' : 'Add'} User
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowUserModal(false)
                      setEditingUser(null)
                      setFormData({ name: '', email: '', password: '', department: '', specialization: '', role: 'doctor' })
                    }}
                    className="flex-1 py-2 bg-white/20 text-white rounded-lg hover:bg-white/30 transition-all"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </main>

      <style jsx>{`
        @keyframes blob {
          0% { transform: translate(0px, 0px) scale(1); }
          33% { transform: translate(30px, -50px) scale(1.1); }
          66% { transform: translate(-20px, 20px) scale(0.9); }
          100% { transform: translate(0px, 0px) scale(1); }
        }
        .animate-blob {
          animation: blob 7s infinite;
        }
        .animation-delay-2000 {
          animation-delay: 2s;
        }
      `}</style>
    </div>
  )
}
