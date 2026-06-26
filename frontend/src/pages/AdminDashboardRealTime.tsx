import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { AuthService } from '../auth/AuthService'
import { User } from '../types/user'
import WebSocketService from '../services/websocketService'
import { UserService } from '../services/userService'

export default function AdminDashboardRealTime() {
  const [currentUser, setCurrentUser] = useState<User | null>(null)
  const [users, setUsers] = useState<User[]>([])
  const [patients, setPatients] = useState<any[]>([])
  const [alerts, setAlerts] = useState<any[]>([])
  const [departments, setDepartments] = useState<any[]>([])
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState<'dashboard' | 'users' | 'patients' | 'alerts'>('dashboard')
  const [realTimeAlerts, setRealTimeAlerts] = useState<any[]>([])
  const [wsConnected, setWsConnected] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [userStats, setUserStats] = useState<any>(null)
  void isLoading
  void userStats

  // WebSocket service instance
  const wsService = WebSocketService.getInstance()

  // Load data on component mount
  useEffect(() => {
    const user = AuthService.getCurrentUser()
    setCurrentUser(user)
    if (user) {
      loadDashboardData()
      connectWebSocket()
    }
    
    return () => {
      disconnectWebSocket()
    }
  }, [])

  const connectWebSocket = async () => {
    try {
      await wsService.connect()
      setWsConnected(true)
      
      // Subscribe to admin alerts
      wsService.subscribeToAdminAlerts((alertData) => {
        console.log('Real-time admin alert:', alertData)
        setRealTimeAlerts(prev => [alertData, ...prev.slice(0, 19)])
        setAlerts(prev => [alertData, ...prev.slice(0, 49)])
      })
      
      wsService.subscribeToAdminVitals((vitalData) => {
        console.log('Real-time vital update:', vitalData)
      })
      
      wsService.subscribeToAdminPredictions((predictionData) => {
        console.log('Real-time prediction update:', predictionData)
      })
      
    } catch (error) {
      console.error('WebSocket connection failed:', error)
      setError('Real-time updates unavailable')
    }
  }

  const disconnectWebSocket = () => {
    wsService.disconnect()
    setWsConnected(false)
  }

  const loadDashboardData = async () => {
    setIsLoading(true)
    setError('')
    
    try {
      // Load users
      const usersData = await UserService.getAllUsers()
      setUsers(usersData)
      
      // Load user stats
      const statsData = await UserService.getUserStats()
      setUserStats(statsData)
      
      // Load departments
      const deptData = await UserService.getDepartments()
      setDepartments(deptData)
      
      // Load patients (mock for now)
      setPatients([
        { id: 'patient-1', patientIdentifier: 'PT-001', firstName: 'John', lastName: 'Doe', condition: 'STABLE', roomNumber: 'A-101' },
        { id: 'patient-2', patientIdentifier: 'PT-002', firstName: 'Jane', lastName: 'Smith', condition: 'CRITICAL', roomNumber: 'B-205' },
      ])
      
    } catch (error) {
      console.error('Error loading dashboard data:', error)
      setError('Failed to load dashboard data')
    } finally {
      setIsLoading(false)
    }
  }

  const getConditionColor = (condition: string) => {
    switch (condition) {
      case 'STABLE': return 'bg-green-500/20 text-green-300 border-green-500/30'
      case 'CRITICAL': return 'bg-red-500/20 text-red-300 border-red-500/30'
      case 'EMERGENCY': return 'bg-orange-500/20 text-orange-300 border-orange-500/30'
      default: return 'bg-gray-500/20 text-gray-300 border-gray-500/30'
    }
  }

  const getSeverityColor = (severity: string) => {
    switch (severity) {
      case 'LOW': return 'bg-green-500/20 text-green-300'
      case 'MEDIUM': return 'bg-yellow-500/20 text-yellow-300'
      case 'HIGH': return 'bg-orange-500/20 text-orange-300'
      case 'CRITICAL': return 'bg-red-500/20 text-red-300'
      default: return 'bg-gray-500/20 text-gray-300'
    }
  }

  if (!currentUser) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-white">Loading...</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-600 to-blue-700 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-white">Admin Dashboard (Real-Time)</h1>
              <p className="text-white/80">{currentUser.firstName} {currentUser.lastName}</p>
              <div className="flex items-center space-x-4 mt-2">
                <span className="text-white/60">Role: {currentUser.roleName}</span>
                <span className={`px-2 py-1 rounded text-xs ${wsConnected ? 'bg-green-500/20 text-green-300' : 'bg-red-500/20 text-red-300'}`}>
                  {wsConnected ? '🟢 Connected' : '🔴 Offline'}
                </span>
              </div>
            </div>
            <button
              onClick={() => AuthService.logout()}
              className="bg-red-500 text-white px-4 py-2 rounded-lg hover:bg-red-600"
            >
              Logout
            </button>
          </div>
        </div>

        {/* Real-time Alerts Feed */}
        {realTimeAlerts.length > 0 && (
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-4 mb-6">
            <h3 className="text-lg font-semibold text-white mb-3">🔴 Live Alerts</h3>
            <div className="space-y-2 max-h-32 overflow-y-auto">
              {realTimeAlerts.map((alert, index) => (
                <div key={index} className="text-white/80 text-sm flex items-center space-x-2">
                  <span className="text-xs">{new Date(alert.timestamp).toLocaleTimeString()}</span>
                  <span className={`px-2 py-1 rounded text-xs ${getSeverityColor(alert.severity)}`}>
                    {alert.severity}
                  </span>
                  <span>Patient: {alert.patientId}</span>
                  <span>{alert.message}</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-5 gap-6 mb-6">
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Total Patients</h3>
            <p className="text-3xl font-bold text-white">{patients.length}</p>
          </div>
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Total Doctors</h3>
            <p className="text-3xl font-bold text-white">{users.filter(u => u.roleName === 'doctor').length}</p>
          </div>
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Total Nurses</h3>
            <p className="text-3xl font-bold text-white">{users.filter(u => u.roleName === 'nurse').length}</p>
          </div>
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Critical Patients</h3>
            <p className="text-3xl font-bold text-white">{patients.filter(p => p.condition === 'CRITICAL').length}</p>
          </div>
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Active Alerts</h3>
            <p className="text-3xl font-bold text-white">{alerts.length}</p>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="bg-white/10 backdrop-blur-lg rounded-xl p-2 mb-6">
          <div className="flex space-x-2">
            <button
              onClick={() => setActiveTab('dashboard')}
              className={`px-4 py-2 rounded-lg ${activeTab === 'dashboard' ? 'bg-white/20 text-white' : 'text-white/60'}`}
            >
              Dashboard
            </button>
            <button
              onClick={() => setActiveTab('users')}
              className={`px-4 py-2 rounded-lg ${activeTab === 'users' ? 'bg-white/20 text-white' : 'text-white/60'}`}
            >
              User Management
            </button>
            <button
              onClick={() => setActiveTab('patients')}
              className={`px-4 py-2 rounded-lg ${activeTab === 'patients' ? 'bg-white/20 text-white' : 'text-white/60'}`}
            >
              Patient Management
            </button>
            <button
              onClick={() => setActiveTab('alerts')}
              className={`px-4 py-2 rounded-lg ${activeTab === 'alerts' ? 'bg-white/20 text-white' : 'text-white/60'}`}
            >
              Alerts
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
          {error && (
            <div className="bg-red-500/20 border border-red-500/30 rounded-lg p-3 mb-4">
              <p className="text-white">{error}</p>
            </div>
          )}

          {activeTab === 'dashboard' && (
            <div>
              <h2 className="text-2xl font-bold text-white mb-4">Hospital Overview</h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="bg-white/10 rounded-lg p-6">
                  <h3 className="text-lg font-semibold text-white mb-4">Department Statistics</h3>
                  {departments.map((dept) => (
                    <div key={dept.id} className="flex justify-between items-center mb-3 text-white/80">
                      <span>{dept.departmentName}</span>
                      <span className="font-semibold">{dept.active ? 'Active' : 'Inactive'}</span>
                    </div>
                  ))}
                </div>
                
                <div className="bg-white/10 rounded-lg p-6">
                  <h3 className="text-lg font-semibold text-white mb-4">Recent Alerts</h3>
                  {alerts.slice(0, 5).map((alert, index) => (
                    <div key={index} className="flex justify-between items-center mb-3 text-white/80">
                      <span>{alert.patientId}</span>
                      <span className={`px-2 py-1 rounded text-xs ${getSeverityColor(alert.severity)}`}>
                        {alert.severity}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'users' && (
            <div>
              <h2 className="text-2xl font-bold text-white mb-4">User Management</h2>
              
              <div className="overflow-x-auto">
                <table className="w-full text-white">
                  <thead>
                    <tr className="border-b border-white/20">
                      <th className="text-left p-3">Name</th>
                      <th className="text-left p-3">Email</th>
                      <th className="text-left p-3">Role</th>
                      <th className="text-left p-3">Department</th>
                      <th className="text-left p-3">Status</th>
                      <th className="text-left p-3">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((user) => (
                      <tr key={user.id} className="border-b border-white/10 hover:bg-white/5">
                        <td className="p-3">{user.firstName} {user.lastName}</td>
                        <td className="p-3">{user.email}</td>
                        <td className="p-3">{user.roleName}</td>
                        <td className="p-3">{user.departmentName || '-'}</td>
                        <td className="p-3">
                          <span className={`px-2 py-1 rounded text-xs ${user.isActive ? 'bg-green-500/20 text-green-300' : 'bg-red-500/20 text-red-300'}`}>
                            {user.isActive ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                        <td className="p-3">
                          <button className="text-blue-400 hover:text-blue-300 mr-2">Edit</button>
                          <button className="text-red-400 hover:text-red-300">Delete</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === 'patients' && (
            <div>
              <h2 className="text-2xl font-bold text-white mb-4">Patient Management</h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {patients.map((patient) => (
                  <motion.div
                    key={patient.id}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="bg-white/10 rounded-lg p-4"
                  >
                    <div className="flex justify-between items-start mb-2">
                      <div>
                        <h3 className="text-lg font-semibold text-white">
                          {patient.firstName} {patient.lastName}
                        </h3>
                        <p className="text-white/60 text-sm">{patient.patientIdentifier}</p>
                      </div>
                      <span className={`px-2 py-1 rounded text-xs border ${getConditionColor(patient.condition)}`}>
                        {patient.condition}
                      </span>
                    </div>
                    
                    <div className="space-y-1 text-sm text-white/80">
                      <p>Room: {patient.roomNumber}</p>
                    </div>
                  </motion.div>
                ))}
              </div>
            </div>
          )}

          {activeTab === 'alerts' && (
            <div>
              <h2 className="text-2xl font-bold text-white mb-4">Alert History</h2>
              
              <div className="space-y-4">
                {alerts.map((alert, index) => (
                  <div key={index} className="bg-white/10 rounded-lg p-4 border-l-4 border-red-500">
                    <div className="flex justify-between items-start">
                      <div>
                        <h4 className="text-white font-semibold">Patient: {alert.patientId}</h4>
                        <p className="text-white/80">{alert.message}</p>
                      </div>
                      <div className="text-right">
                        <span className={`px-3 py-1 rounded text-sm ${getSeverityColor(alert.severity)}`}>
                          {alert.severity}
                        </span>
                        <p className="text-white/60 text-sm mt-1">
                          {new Date(alert.timestamp).toLocaleString()}
                        </p>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
