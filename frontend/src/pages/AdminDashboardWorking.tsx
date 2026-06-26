import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { AuthService } from '../auth/AuthService'
import { mockUsers, mockPatients, addNewUser, updateUser, addNewPatient, updatePatient } from '../database/mockDatabase'

export default function AdminDashboardWorking() {
  const [currentUser, setCurrentUser] = useState<any>(null)
  const [activeTab, setActiveTab] = useState<'dashboard' | 'users' | 'patients' | 'alerts' | 'financial' | 'reports'>('dashboard')
  const [userType, setUserType] = useState<'doctors' | 'nurses' | 'viewers' | 'all'>('all')
  const [showUserModal, setShowUserModal] = useState(false)
  const [showPatientModal, setShowPatientModal] = useState(false)
  const [editingUser, setEditingUser] = useState<any>(null)
  const [editingPatient, setEditingPatient] = useState<any>(null)
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
    nurseId: '',
    mobileNumber: '',
    guardianName: '',
    guardianMobile: ''
  })

  useEffect(() => {
    const user = AuthService.getCurrentUser()
    setCurrentUser(user)
  }, [])

  // Mock data for alerts
  const [alerts] = useState([
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
    userType === 'all' ? true : u.role === userType.slice(0, -1)
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
    
    try {
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
      alert('User saved successfully!')
    } catch (error) {
      alert('Error saving user. Please try again.')
    }
  }

  const handlePatientSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      if (editingPatient) {
        updatePatient(editingPatient.id, patientFormData)
      } else {
        addNewPatient({
          ...patientFormData,
          vitals: [],
          reports: []
        })
      }
      
      setPatientFormData({
        name: '', email: '', password: '', age: 0, gender: 'Male',
        roomNumber: '', condition: 'Stable', admittedDate: '', doctorId: '', nurseId: '',
        mobileNumber: '', guardianName: '', guardianMobile: ''
      })
      setEditingPatient(null)
      setShowPatientModal(false)
      alert('Patient saved successfully!')
    } catch (error) {
      alert('Error saving patient. Please try again.')
    }
  }

  const handleBulkAction = (action: 'activate' | 'deactivate' | 'delete') => {
    try {
      selectedUsers.forEach(userId => {
        if (action === 'delete') {
          console.log(`Deleting user: ${userId}`)
        } else {
          updateUser(userId, { isActive: action === 'activate' })
        }
      })
      setSelectedUsers([])
      alert(`Bulk ${action} completed successfully!`)
    } catch (error) {
      alert('Error performing bulk action. Please try again.')
    }
  }

  const handleAlertAction = (alertId: string, action: 'resolve' | 'escalate') => {
    try {
      const alert = alerts.find(a => a.id === alertId)
      if (alert) {
        alert.status = action === 'resolve' ? 'RESOLVED' : 'ESCALATED'
        alert(`Alert ${action}d successfully!`)
      }
    } catch (error) {
      alert('Error updating alert. Please try again.')
    }
  }

  const handleEditUser = (user: any) => {
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

  const handleEditPatient = (patient: any) => {
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
      nurseId: patient.nurseId,
      mobileNumber: patient.mobileNumber || '',
      guardianName: patient.guardianName || '',
      guardianMobile: patient.guardianMobile || ''
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

  const generateReport = (type: string) => {
    alert(`${type} report generated successfully!`)
  }

  const sendNotification = (audience: string) => {
    alert(`Notification sent to ${audience}!`)
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
            { id: 'reports', label: '📁 Reports' }
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
            </div>
          </motion.div>
        )}

        {/* Users Tab */}
        {activeTab === 'users' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-bold text-white">User Management</h2>
                <button
                  onClick={() => setShowUserModal(true)}
                  className="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-all"
                >
                  ➕ Add New User
                </button>
              </div>

              {/* Users Table */}
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-white/20">
                      <th className="text-left p-3 text-white/60">Name</th>
                      <th className="text-left p-3 text-white/60">Email</th>
                      <th className="text-left p-3 text-white/60">Role</th>
                      <th className="text-left p-3 text-white/60">Department</th>
                      <th className="text-left p-3 text-white/60">Status</th>
                      <th className="text-left p-3 text-white/60">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.map((user) => (
                      <tr key={user.id} className="border-b border-white/10 hover:bg-white/5">
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

        {/* Patients Tab */}
        {activeTab === 'patients' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-bold text-white">Patient Management</h2>
                <button
                  onClick={() => setShowPatientModal(true)}
                  className="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-all"
                >
                  ➕ Add New Patient
                </button>
              </div>

              {/* Patients Table */}
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-white/20">
                      <th className="text-left p-3 text-white/60">Patient ID</th>
                      <th className="text-left p-3 text-white/60">Name</th>
                      <th className="text-left p-3 text-white/60">Age</th>
                      <th className="text-left p-3 text-white/60">Room</th>
                      <th className="text-left p-3 text-white/60">Condition</th>
                      <th className="text-left p-3 text-white/60">Mobile</th>
                      <th className="text-left p-3 text-white/60">Guardian</th>
                      <th className="text-left p-3 text-white/60">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {mockPatients.map((patient) => (
                      <tr key={patient.id} className="border-b border-white/10 hover:bg-white/5">
                        <td className="p-3 text-white">{patient.patientIdentifier}</td>
                        <td className="p-3 text-white">{patient.name}</td>
                        <td className="p-3 text-white">{patient.age}</td>
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
                        <td className="p-3 text-white/80">{patient.mobileNumber || '-'}</td>
                        <td className="p-3 text-white/80">{patient.guardianName || '-'}</td>
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
            </div>
          </motion.div>
        )}

        {/* Alerts Tab */}
        {activeTab === 'alerts' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
              <h2 className="text-xl font-bold text-white mb-4">Alert Management</h2>
              
              <div className="space-y-4">
                {alerts.map((alert) => (
                  <div key={alert.id} className="bg-white/5 rounded-lg p-4 border border-white/10">
                    <div className="flex justify-between items-start">
                      <div>
                        <div className="flex items-center space-x-2 mb-2">
                          <span className={`px-2 py-1 rounded-full text-xs ${
                            alert.type === 'CRITICAL' ? 'bg-red-500/20 text-red-300' :
                            alert.type === 'HIGH' ? 'bg-orange-500/20 text-orange-300' :
                            alert.type === 'MEDIUM' ? 'bg-yellow-500/20 text-yellow-300' :
                            'bg-blue-500/20 text-blue-300'
                          }`}>
                            {alert.type}
                          </span>
                          <span className="text-white/60 text-sm">
                            {new Date(alert.timestamp).toLocaleString()}
                          </span>
                        </div>
                        <p className="text-white mb-2">{alert.message}</p>
                        <p className="text-white/60 text-sm">
                          Patient: {mockPatients.find(p => p.id === alert.patientId)?.name || 'Unknown'}
                        </p>
                      </div>
                      <div className="flex space-x-2">
                        <button
                          onClick={() => handleAlertAction(alert.id, 'resolve')}
                          className="px-3 py-1 bg-green-500/20 text-green-300 rounded hover:bg-green-500/30 transition-all"
                        >
                          ✅ Resolve
                        </button>
                        <button
                          onClick={() => handleAlertAction(alert.id, 'escalate')}
                          className="px-3 py-1 bg-orange-500/20 text-orange-300 rounded hover:bg-orange-500/30 transition-all"
                        >
                          ⚠️ Escalate
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        )}

        {/* Financial Tab */}
        {activeTab === 'financial' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
              <h2 className="text-xl font-bold text-white mb-4">Financial Management</h2>
              
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div className="bg-white/5 rounded-lg p-4">
                  <h3 className="text-white font-semibold mb-2">Total Revenue</h3>
                  <p className="text-2xl font-bold text-green-400">$125,430</p>
                  <p className="text-white/60 text-sm">+12% from last month</p>
                </div>
                <div className="bg-white/5 rounded-lg p-4">
                  <h3 className="text-white font-semibold mb-2">Pending Invoices</h3>
                  <p className="text-2xl font-bold text-yellow-400">$18,250</p>
                  <p className="text-white/60 text-sm">15 pending payments</p>
                </div>
                <div className="bg-white/5 rounded-lg p-4">
                  <h3 className="text-white font-semibold mb-2">Total Expenses</h3>
                  <p className="text-2xl font-bold text-red-400">$42,180</p>
                  <p className="text-white/60 text-sm">-5% from last month</p>
                </div>
              </div>

              <div className="flex space-x-4">
                <button
                  onClick={() => generateReport('Invoice')}
                  className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-all"
                >
                  📄 Generate Invoice
                </button>
                <button
                  onClick={() => generateReport('Payment')}
                  className="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-all"
                >
                  💰 Track Payments
                </button>
                <button
                  onClick={() => generateReport('Financial')}
                  className="px-4 py-2 bg-purple-500 text-white rounded-lg hover:bg-purple-600 transition-all"
                >
                  📊 Export Report
                </button>
              </div>
            </div>
          </motion.div>
        )}

        {/* Reports Tab */}
        {activeTab === 'reports' && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
              <h2 className="text-xl font-bold text-white mb-4">Reports & Analytics</h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="bg-white/5 rounded-lg p-4">
                  <h3 className="text-white font-semibold mb-3">Patient Reports</h3>
                  <div className="space-y-2">
                    <button
                      onClick={() => generateReport('Patient Summary')}
                      className="w-full px-4 py-2 bg-blue-500/20 text-blue-300 rounded-lg hover:bg-blue-500/30 transition-all text-left"
                    >
                      📋 Patient Summary Report
                    </button>
                    <button
                      onClick={() => generateReport('Patient Details')}
                      className="w-full px-4 py-2 bg-blue-500/20 text-blue-300 rounded-lg hover:bg-blue-500/30 transition-all text-left"
                    >
                      📄 Detailed Patient Report
                    </button>
                  </div>
                </div>

                <div className="bg-white/5 rounded-lg p-4">
                  <h3 className="text-white font-semibold mb-3">Department Reports</h3>
                  <div className="space-y-2">
                    <button
                      onClick={() => generateReport('Department Performance')}
                      className="w-full px-4 py-2 bg-purple-500/20 text-purple-300 rounded-lg hover:bg-purple-500/30 transition-all text-left"
                    >
                      🏥 Department Performance
                    </button>
                    <button
                      onClick={() => generateReport('Staff Activity')}
                      className="w-full px-4 py-2 bg-purple-500/20 text-purple-300 rounded-lg hover:bg-purple-500/30 transition-all text-left"
                    >
                      👥 Staff Activity Report
                    </button>
                  </div>
                </div>
              </div>

              <div className="mt-6 flex space-x-4">
                <button
                  onClick={() => alert('Report exported as PDF!')}
                  className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-all"
                >
                  📄 Export as PDF
                </button>
                <button
                  onClick={() => alert('Report exported as CSV!')}
                  className="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-all"
                >
                  📊 Export as CSV
                </button>
              </div>
            </div>
          </motion.div>
        )}
      </main>

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
                  <label className="block text-white/60 text-sm mb-1">Mobile Number</label>
                  <input
                    type="tel"
                    value={patientFormData.mobileNumber}
                    onChange={(e) => setPatientFormData({ ...patientFormData, mobileNumber: e.target.value })}
                    className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                    placeholder="+1-555-0123-4567"
                  />
                </div>
                <div>
                  <label className="block text-white/60 text-sm mb-1">Guardian Name</label>
                  <input
                    type="text"
                    value={patientFormData.guardianName}
                    onChange={(e) => setPatientFormData({ ...patientFormData, guardianName: e.target.value })}
                    className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                    placeholder="Guardian name"
                  />
                </div>
                <div>
                  <label className="block text-white/60 text-sm mb-1">Guardian Mobile</label>
                  <input
                    type="tel"
                    value={patientFormData.guardianMobile}
                    onChange={(e) => setPatientFormData({ ...patientFormData, guardianMobile: e.target.value })}
                    className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                    placeholder="+1-555-0123-4568"
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
                      roomNumber: '', condition: 'Stable', admittedDate: '', doctorId: '', nurseId: '',
                      mobileNumber: '', guardianName: '', guardianMobile: ''
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
