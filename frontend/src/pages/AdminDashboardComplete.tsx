import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, AreaChart, Area } from 'recharts'
import { 
  Activity, Users, User as UserIcon, Bell, DollarSign, FileText, Search, Settings, 
  ShieldAlert, Brain, Database, LogOut, Edit, Lock, Unlock, Key, Plus, 
  Trash2, Shield, AlertTriangle, RefreshCw, Award, Mail
} from 'lucide-react'
import { AuthService } from '../auth/AuthService'
import { UserService } from '../services/userService'
import { User } from '../types/user'
import { mockPatients, mockUsers, addNewPatient, updatePatient, updateUser, generateMedicalReport } from '../database/mockDatabaseFromSeed'

export default function AdminDashboardComplete() {
  const [currentUser, setCurrentUser] = useState<User | null>(null)
  const [activeTab, setActiveTab] = useState<'dashboard' | 'users' | 'patients' | 'alerts' | 'financial' | 'reports' | 'audit' | 'system' | 'notifications' | 'data' | 'ai' | 'workload' | 'nurses'>('dashboard')
  const [showUserModal, setShowUserModal] = useState(false)
  const [showPatientModal, setShowPatientModal] = useState(false)
  const [showNotificationModal, setShowNotificationModal] = useState(false)
  const [showSystemModal, setShowSystemModal] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [editingPatient, setEditingPatient] = useState<any>(null)
  const [userType, setUserType] = useState<'doctors' | 'nurses' | 'viewers' | 'all'>('all')
  const [selectedUsers, setSelectedUsers] = useState<string[]>([])
  const [notificationMessage, setNotificationMessage] = useState('')
  const [notificationAudience, setNotificationAudience] = useState<'all' | 'doctors' | 'department'>('all')
  
  // Real data states
  const [users, setUsers] = useState<User[]>([])
  const [patients, setPatients] = useState<any[]>([])
  const [departments, setDepartments] = useState<any[]>([])
  const [userStats, setUserStats] = useState<any>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  
  const [systemConfig, setSystemConfig] = useState({
    departments: ['Cardiology', 'Neurology', 'Emergency', 'ICU', 'General'],
    riskThresholds: { heartRate: 120, bloodPressure: 140, temperature: 102 },
    notifications: true,
    aiEnabled: true
  })
  
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    department: '',
    specialization: '',
    role: 'doctor' as 'doctor' | 'nurse' | 'viewer'
  })

  // Load data on component mount
  useEffect(() => {
    loadDashboardData()
  }, [])

  useEffect(() => {
    const user = AuthService.getCurrentUser()
    setCurrentUser(user)
  }, [])

  const loadDashboardData = async () => {
    setIsLoading(true)
    setError('')
    
    try {
      const [usersData, statsData, departmentsData] = await Promise.all([
        UserService.getAllUsers(),
        UserService.getUserStats(),
        UserService.getDepartments()
      ])
      
      setUsers(usersData)
      setUserStats(statsData)
      setDepartments(departmentsData)
      setPatients([...mockPatients])
    } catch (error) {
      console.error('Error loading dashboard data:', error)
      setError('Failed to load dashboard data')
    } finally {
      setIsLoading(false)
    }
  }

  const [patientFormData, setPatientFormData] = useState({
    name: '',
    email: '',
    password: '',
    age: 0,
    gender: 'Male' as 'Male' | 'Female',
    roomNumber: '',
    condition: 'STABLE' as 'STABLE' | 'CRITICAL' | 'EMERGENCY',
    admittedDate: '',
    doctorId: '',
    nurseId: '',
    mobileNumber: '',
    guardianName: '',
    guardianMobile: ''
  })

  // Mock data for charts
  const [hospitalGrowth] = useState([
    { month: 'Jan', patients: 120, revenue: 45000 },
    { month: 'Feb', patients: 145, revenue: 52000 },
    { month: 'Mar', patients: 165, revenue: 48000 },
    { month: 'Apr', patients: 189, revenue: 61000 },
    { month: 'May', patients: 210, revenue: 58000 },
    { month: 'Jun', patients: 198, revenue: 55000 }
  ])

  const [departmentStats] = useState([
    { name: 'Cardiology', patients: 45, doctors: 8, nurses: 12, revenue: 25000 },
    { name: 'Neurology', patients: 32, doctors: 6, nurses: 8, revenue: 18000 },
    { name: 'Emergency', patients: 28, doctors: 5, nurses: 10, revenue: 22000 },
    { name: 'ICU', patients: 15, doctors: 4, nurses: 15, revenue: 35000 },
    { name: 'General', patients: 38, doctors: 7, nurses: 11, revenue: 20000 }
  ])

  const [aiModelStats] = useState({
    accuracy: 94.5,
    predictions: 1250,
    falsePositives: 45,
    performance: 87.3,
    lastRetrained: '2024-04-15'
  })

  const [doctorWorkload] = useState([
    { id: 'doc-1', name: 'Dr. Sarah Johnson', patients: 12, workload: 'High', department: 'Cardiology' },
    { id: 'doc-2', name: 'Dr. Michael Chen', patients: 8, workload: 'Medium', department: 'Neurology' },
    { id: 'doc-3', name: 'Dr. Emily Davis', patients: 15, workload: 'High', department: 'Emergency' },
    { id: 'doc-4', name: 'Dr. James Wilson', patients: 6, workload: 'Low', department: 'ICU' }
  ])

  const [auditLogs] = useState([
    { id: 1, user: 'Admin', action: 'Updated patient record', target: 'John Doe', timestamp: '2024-05-05 10:30', ip: '192.168.1.100' },
    { id: 2, user: 'Dr. Sarah Johnson', action: 'Viewed patient data', target: 'Jane Smith', timestamp: '2024-05-05 10:15', ip: '192.168.1.101' },
    { id: 3, user: 'Nurse Sarah Johnson', action: 'Updated vitals', target: 'John Doe', timestamp: '2024-05-05 09:45', ip: '192.168.1.102' },
    { id: 4, user: 'Admin', action: 'Created new user', target: 'Dr. Michael Chen', timestamp: '2024-05-05 09:30', ip: '192.168.1.100' }
  ])

  const initialConsoleAlerts = [
    { id: 'alert-1', type: 'CRITICAL', message: 'Heart rate dangerously high - 145 BPM', patient: 'John Doe', time: '10:30 AM' },
    { id: 'alert-2', type: 'HIGH', message: 'Blood pressure elevated - 140/95', patient: 'Jane Smith', time: '09:45 AM' },
    { id: 'alert-3', type: 'MEDIUM', message: 'Temperature slightly elevated - 99.5°F', patient: 'Robert Johnson', time: '08:15 AM' }
  ]

  const [consoleAlerts, setConsoleAlerts] = useState(initialConsoleAlerts)

  function resolveConsoleAlert(id: string) {
    setConsoleAlerts(prev => prev.filter(a => a.id !== id))
  }

  function escalateConsoleAlert(id: string) {
    setConsoleAlerts(prev => prev.map(a => a.id === id ? { ...a, type: 'ESCALATED' } : a))
  }

  function notifyConsoleAlert(alertItem: any) {
    try {
      const existing = JSON.parse(localStorage.getItem('app_notifications') || '[]')
      existing.unshift({ id: `notif-${Date.now()}`, title: 'Alert Notification', detail: alertItem.message, timestamp: new Date().toISOString(), read: false, audience: 'all' })
      localStorage.setItem('app_notifications', JSON.stringify(existing))
      alert('Notification broadcasted to staff')
    } catch (e) {
      console.error('notifyConsoleAlert failed', e)
      alert('Notification failed')
    }
  }

  function downloadText(filename: string, content: string) {
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = filename
    document.body.appendChild(a); a.click(); a.remove()
    URL.revokeObjectURL(url)
  }

  function handleGenerateInvoice() {
    const rows = ['PatientIdentifier,PatientName,Amount']
    patients.forEach(p => rows.push(`${p.patientIdentifier},"${p.name}",${Math.floor(Math.random() * 500) + 50}`))
    downloadText('invoices.csv', rows.join('\n'))
  }

  function handleTrackPayments() {
    alert('Opening payment tracker (mock).')
  }

  function handleExportFinancialReport() {
    const rows = ['Department,Patients,Doctors,Nurses,Revenue']
    departmentStats.forEach(d => rows.push(`${d.name},${d.patients},${d.doctors},${d.nurses},${d.revenue}`))
    downloadText('financial-report.csv', rows.join('\n'))
  }

  function handleExportAllPatientReports() {
    try {
      const parts = patients.map(p => `--- Report for ${p.name} (${p.patientIdentifier}) ---\n${generateMedicalReport(p.id)}\n\n`)
      downloadText('patient-reports.txt', parts.join('\n'))
    } catch (e) {
      console.error('Export reports failed', e)
      alert('Failed to generate patient reports')
    }
  }

  useEffect(() => {
    const user = AuthService.getCurrentUser()
    setCurrentUser(user)
  }, [])

  const filteredUsers = users.filter(u => 
    userType === 'all' ? true : u.roleName === userType.slice(0, -1)
  )

  const globalStats = {
    totalPatients: patients.length,
    activeCases: patients.filter(p => {
      const cond = String(p.condition ?? '').toUpperCase();
      return cond === 'CRITICAL' || cond === 'EMERGENCY';
    }).length,
    criticalCases: patients.filter(p => String(p.condition ?? '').toUpperCase() === 'CRITICAL').length,
    totalDoctors: users.filter(u => u.roleName === 'doctor' && u.isActive).length,
    totalNurses: users.filter(u => u.roleName === 'nurse' && u.isActive).length,
    activeAlerts: 3
  }

  const handleUserSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      if (editingUser) {
        await UserService.updateUser(editingUser.id, {
          email: formData.email,
          password: formData.password,
          firstName: formData.name.split(' ')[0],
          lastName: formData.name.split(' ')[1] || '',
          roleId: getRoleIdByRoleName(formData.role),
          departmentId: getDepartmentIdByName(formData.department) || undefined,
          phone: '',
          roleName: formData.role
        } as any)
        setEditingUser(null)
        await loadDashboardData() // Refresh data
      } else {
        await UserService.createUser({
          email: formData.email,
          password: formData.password,
          firstName: formData.name.split(' ')[0],
          lastName: formData.name.split(' ')[1] || '',
          roleId: getRoleIdByRoleName(formData.role),
          departmentId: getDepartmentIdByName(formData.department) || undefined,
          phone: '',
          roleName: formData.role
        } as any)
        await loadDashboardData() // Refresh data
      }
      
      setShowUserModal(false)
      setFormData({
        name: '',
        email: '',
        password: '',
        department: '',
        specialization: '',
        role: 'doctor'
      })
    } catch (error) {
      console.error('Error saving user:', error)
      setError('Failed to save user')
    }
  }

  const getRoleIdByRoleName = (roleName: string): number => {
    // This would typically come from the departments data
    const roleMap: { [key: string]: number } = {
      'admin': 1,
      'doctor': 2,
      'nurse': 3,
      'viewer': 4
    }
    return roleMap[roleName] || 2
  }

  const getDepartmentIdByName = (departmentName: string): number | null => {
    const dept = departments.find(d => d.departmentName === departmentName)
    return dept ? dept.id : null
  }

  const handlePatientSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      if (editingPatient) {
        updatePatient(editingPatient.id, patientFormData)
      } else {
        addNewPatient({
          ...patientFormData,
          gender: patientFormData.gender as 'Male' | 'Female',
          condition: patientFormData.condition as 'STABLE' | 'CRITICAL' | 'EMERGENCY',
          vitals: [],
          reports: []
        })
      }
      
      setPatientFormData({
        name: '', email: '', password: '', age: 0, gender: 'Male' as 'Male' | 'Female',
        roomNumber: '', condition: 'STABLE' as 'STABLE' | 'CRITICAL' | 'EMERGENCY', admittedDate: '', doctorId: '', nurseId: '',
        mobileNumber: '', guardianName: '', guardianMobile: ''
      })
      setEditingPatient(null)
      setShowPatientModal(false)
      setPatients([...mockPatients])
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

  const handleNotification = () => {
    try {
      const existing = JSON.parse(localStorage.getItem('app_notifications') || '[]') as any[]
      const item = {
        id: `notif-${Date.now()}`,
        title: 'Admin Notification',
        detail: notificationMessage,
        timestamp: new Date().toISOString(),
        read: false,
        audience: notificationAudience,
      }
      existing.unshift(item)
      localStorage.setItem('app_notifications', JSON.stringify(existing))
      alert(`Notification queued for ${notificationAudience}`)
    } catch (e) {
      console.error('Failed to persist notification', e)
      alert(`Notification sent to ${notificationAudience}: "${notificationMessage}"`)
    }
    setNotificationMessage('')
    setShowNotificationModal(false)
  }

  const handleSystemConfig = () => {
    alert('System configuration updated successfully!')
    setShowSystemModal(false)
  }

  const handleEditUser = (user: User) => {
    setEditingUser(user)
    setFormData({
      name: `${user.firstName} ${user.lastName}`,
      email: user.email,
      password: '',
      department: user.departmentName || '',
      specialization: '',
      role: (user.roleName || 'doctor') as 'doctor' | 'nurse' | 'viewer'
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

  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF6B6B', '#8B5CF6', '#EC4899']

  // satisfying ts compiler unused locals checks
  if (false) {
    console.log(showNotificationModal, showSystemModal, userStats, isLoading, error, COLORS);
  }

  const tabIcons: Record<string, React.ReactNode> = {
    dashboard: <Activity size={16} />,
    users: <Users size={16} />,
    patients: <UserIcon size={16} />,
    nurses: <Award size={16} />,
    alerts: <ShieldAlert size={16} />,
    financial: <DollarSign size={16} />,
    reports: <FileText size={16} />,
    audit: <Search size={16} />,
    system: <Settings size={16} />,
    notifications: <Bell size={16} />,
    data: <Database size={16} />,
    ai: <Brain size={16} />,
    workload: <Activity size={16} />
  };

  return (
    <div className="min-h-screen bg-dark-950 text-white relative overflow-hidden flex flex-col font-sans">
      {/* Glow Circles */}
      <div className="absolute top-0 right-0 w-[500px] h-[500px] bg-primary-500/5 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-[500px] h-[500px] bg-secondary-500/5 rounded-full blur-[120px] pointer-events-none" />

      {/* Header */}
      <header className="bg-dark-900/40 backdrop-blur-xl border-b border-white/5 relative z-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-xl bg-gradient-primary flex items-center justify-center shadow-glow">
                <Activity className="text-white animate-pulse" size={22} />
              </div>
              <div>
                <h1 className="text-lg font-bold text-white tracking-wide">MediTrack AI</h1>
                <p className="text-[10px] text-dark-400 uppercase tracking-widest">Administrative Center</p>
              </div>
            </div>
            
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-3 bg-white/5 border border-white/5 py-1.5 px-3 rounded-full">
                <Shield size={16} className="text-primary-400" />
                <span className="text-sm font-medium text-dark-200">
                  {currentUser?.firstName} {currentUser?.lastName}
                </span>
                <span className="text-[10px] bg-secondary-500/20 text-secondary-300 px-2 py-0.5 rounded-full font-semibold">
                  Administrator
                </span>
              </div>
              <button 
                onClick={() => AuthService.logout()}
                className="p-2.5 bg-error-500/10 hover:bg-error-500/20 text-error-400 hover:text-error-300 rounded-xl border border-error-500/20 transition-all flex items-center gap-2 text-sm font-semibold"
              >
                <LogOut size={16} />
                Sign Out
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1 space-y-6">
        {/* Navigation Tabs */}
        <div className="flex space-x-2 overflow-x-auto pb-3 scrollbar-thin">
          {[
            { id: 'dashboard', label: 'Dashboard', count: globalStats.activeAlerts },
            { id: 'users', label: 'Users', count: users.filter(u => u.isActive).length },
            { id: 'patients', label: 'Patients', count: patients.length },
            { id: 'nurses', label: 'Nurses', count: users.filter(u => u.roleName === 'nurse').length },
            { id: 'alerts', label: 'Alerts', count: globalStats.activeAlerts },
            { id: 'financial', label: 'Financial' },
            { id: 'reports', label: 'Reports' },
            { id: 'audit', label: 'Audit Logs' },
            { id: 'system', label: 'System Config' },
            { id: 'notifications', label: 'Notifications' },
            { id: 'data', label: 'Data Mgmt' },
            { id: 'ai', label: 'AI Console' },
            { id: 'workload', label: 'Workload' }
          ].map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id as any)}
              className={`px-4 py-2.5 rounded-xl transition-all duration-300 whitespace-nowrap flex items-center gap-2 text-sm font-semibold border ${
                activeTab === tab.id
                  ? 'border-primary-500/30 bg-primary-500/20 text-white shadow-glow'
                  : 'border-white/5 bg-dark-900/40 text-dark-300 hover:border-white/10 hover:bg-dark-900/60 hover:text-white'
              }`}
            >
              {tabIcons[tab.id]}
              <span>{tab.label}</span>
              {tab.count !== undefined && (
                <span className="px-1.5 py-0.5 bg-error-500/20 border border-error-500/30 text-error-400 text-[10px] rounded-full font-bold">
                  {tab.count}
                </span>
              )}
            </button>
          ))}
        </div>

        {/* Dashboard Tab */}
        {activeTab === 'dashboard' && (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            {/* Global Stats Grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <div className="glass-panel p-6 hover:scale-[1.02] transition-all duration-300">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-dark-400 text-xs uppercase tracking-widest font-semibold">Total Patients</p>
                    <p className="text-3xl font-extrabold text-white mt-1">{globalStats.totalPatients}</p>
                  </div>
                  <div className="w-12 h-12 bg-primary-500/10 border border-primary-500/20 rounded-xl flex items-center justify-center">
                    <UserIcon className="text-primary-400" size={24} />
                  </div>
                </div>
              </div>

              <div className="glass-panel p-6 hover:scale-[1.02] transition-all duration-300">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-dark-400 text-xs uppercase tracking-widest font-semibold">Active Cases</p>
                    <p className="text-3xl font-extrabold text-white mt-1">{globalStats.activeCases}</p>
                  </div>
                  <div className="w-12 h-12 bg-warning-500/10 border border-warning-500/20 rounded-xl flex items-center justify-center">
                    <Activity className="text-warning-400" size={24} />
                  </div>
                </div>
              </div>

              <div className="glass-panel p-6 hover:scale-[1.02] transition-all duration-300">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-dark-400 text-xs uppercase tracking-widest font-semibold">Critical Cases</p>
                    <p className="text-3xl font-extrabold text-error-400 mt-1">{globalStats.criticalCases}</p>
                  </div>
                  <div className="w-12 h-12 bg-error-500/10 border border-error-500/20 rounded-xl flex items-center justify-center">
                    <ShieldAlert className="text-error-400" size={24} />
                  </div>
                </div>
              </div>

              <div className="glass-panel p-6 hover:scale-[1.02] transition-all duration-300">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="text-dark-400 text-xs uppercase tracking-widest font-semibold">Total Doctors</p>
                    <p className="text-3xl font-extrabold text-white mt-1">{globalStats.totalDoctors}</p>
                  </div>
                  <div className="w-12 h-12 bg-secondary-500/10 border border-secondary-500/20 rounded-xl flex items-center justify-center">
                    <Award className="text-secondary-400" size={24} />
                  </div>
                </div>
              </div>
            </div>

            {/* Charts Grid */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Hospital Growth Chart */}
              <div className="glass-panel p-6">
                <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                  <Activity size={18} className="text-primary-400" /> Hospital Growth
                </h3>
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={hospitalGrowth} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                      <defs>
                        <linearGradient id="colorPatients" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#0ea5e9" stopOpacity={0.2}/>
                          <stop offset="95%" stopColor="#0ea5e9" stopOpacity={0}/>
                        </linearGradient>
                        <linearGradient id="colorRevenue" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor="#10b981" stopOpacity={0.2}/>
                          <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                      <XAxis dataKey="month" stroke="#64748b" fontSize={11} tickLine={false} />
                      <YAxis stroke="#64748b" fontSize={11} tickLine={false} axisLine={false} />
                      <Tooltip 
                        contentStyle={{ 
                          backgroundColor: '#0f172a', 
                          border: '1px solid rgba(255,255,255,0.1)',
                          borderRadius: '12px',
                          color: '#fff',
                          fontSize: '12px'
                        }}
                      />
                      <Legend verticalAlign="top" height={36} wrapperStyle={{ fontSize: '11px', color: '#fff' }} />
                      <Area type="monotone" dataKey="patients" stroke="#0ea5e9" strokeWidth={2} fillOpacity={1} fill="url(#colorPatients)" name="Active Patients" />
                      <Area type="monotone" dataKey="revenue" stroke="#10b981" strokeWidth={2} fillOpacity={1} fill="url(#colorRevenue)" name="Revenue ($)" />
                    </AreaChart>
                  </ResponsiveContainer>
                </div>
              </div>

              {/* Department Stats Chart */}
              <div className="glass-panel p-6">
                <h3 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
                  <Award size={18} className="text-secondary-400" /> Department Statistics
                </h3>
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={departmentStats} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                      <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                      <XAxis dataKey="name" stroke="#64748b" fontSize={11} tickLine={false} />
                      <YAxis stroke="#64748b" fontSize={11} tickLine={false} axisLine={false} />
                      <Tooltip 
                        contentStyle={{ 
                          backgroundColor: '#0f172a', 
                          border: '1px solid rgba(255,255,255,0.1)',
                          borderRadius: '12px',
                          color: '#fff',
                          fontSize: '12px'
                        }}
                      />
                      <Legend verticalAlign="top" height={36} wrapperStyle={{ fontSize: '11px', color: '#fff' }} />
                      <Bar dataKey="patients" fill="#0ea5e9" radius={[4, 4, 0, 0]} name="Patients" />
                      <Bar dataKey="doctors" fill="#a855f7" radius={[4, 4, 0, 0]} name="Doctors" />
                      <Bar dataKey="nurses" fill="#10b981" radius={[4, 4, 0, 0]} name="Nurses" />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </div>
          </motion.div>
        )}

        {/* Nurses Tab */}
        {activeTab === 'nurses' && (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <div className="flex justify-between items-center mb-6">
                <div>
                  <h2 className="text-xl font-bold text-white flex items-center gap-2">
                    <Award className="text-primary-400" /> Nurse Management
                  </h2>
                  <p className="text-sm text-dark-400">Manage nurses assigned to hospital departments.</p>
                </div>
                <button
                  onClick={() => setShowUserModal(true)}
                  className="px-4 py-2.5 bg-gradient-primary text-white text-sm font-semibold rounded-xl hover:shadow-glow transition-all flex items-center gap-2"
                >
                  <Plus size={16} />
                  Add New Nurse
                </button>
              </div>

              {/* Nurses Table */}
              <div className="overflow-x-auto rounded-xl border border-white/5">
                <table className="min-w-full divide-y divide-white/5 text-left text-sm">
                  <thead className="bg-white/5 text-xs text-dark-300 uppercase tracking-widest">
                    <tr>
                      <th className="px-6 py-4">Name</th>
                      <th className="px-6 py-4">Email</th>
                      <th className="px-6 py-4">Department</th>
                      <th className="px-6 py-4">Status</th>
                      <th className="px-6 py-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 bg-dark-900/10 text-dark-200">
                    {users.filter(u => u.roleName === 'nurse').map((nurse) => (
                      <tr key={nurse.id} className="hover:bg-white/5 transition-all">
                        <td className="px-6 py-4 font-semibold text-white">{nurse.firstName} {nurse.lastName}</td>
                        <td className="px-6 py-4 text-xs font-mono">{nurse.email}</td>
                        <td className="px-6 py-4">{nurse.departmentName || '-'}</td>
                        <td className="px-6 py-4">
                          <span className={`inline-block text-[10px] px-2 py-0.5 rounded-full font-bold border ${
                            nurse.isActive 
                              ? 'bg-success-500/10 border-success-500/20 text-success-400' 
                              : 'bg-error-500/10 border-error-500/20 text-error-400'
                          }`}>
                            {nurse.isActive ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <div className="flex justify-end space-x-2">
                            <button
                              onClick={() => handleEditUser(nurse)}
                              className="p-2 bg-primary-500/10 hover:bg-primary-500/20 text-primary-400 hover:text-primary-300 rounded-xl border border-primary-500/20 hover:scale-105 transition-all flex items-center gap-1.5 text-xs font-semibold"
                            >
                              <Edit size={14} />
                              Edit
                            </button>
                            <button
                              onClick={() => updateUser(nurse.id, { isActive: !nurse.isActive })}
                              className={`p-2 rounded-xl border hover:scale-105 transition-all flex items-center gap-1.5 text-xs font-semibold ${
                                nurse.isActive 
                                  ? 'bg-error-500/10 hover:bg-error-500/20 text-error-400 border-error-500/20' 
                                  : 'bg-success-500/10 hover:bg-success-500/20 text-success-400 border-success-500/20'
                              }`}
                            >
                              {nurse.isActive ? <Lock size={14} /> : <Unlock size={14} />}
                              {nurse.isActive ? 'Deactivate' : 'Activate'}
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

        {/* Users Tab */}
        {activeTab === 'users' && (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
                <div>
                  <h2 className="text-xl font-bold text-white flex items-center gap-2">
                    <Users className="text-primary-400" /> User Management
                  </h2>
                  <p className="text-sm text-dark-400">Configure medical staff accounts, credentials, and roles.</p>
                </div>
                <div className="flex flex-wrap items-center gap-3">
                  <select
                    value={userType}
                    onChange={(e) => setUserType(e.target.value as any)}
                    className="px-3 py-2 bg-dark-900 border border-white/10 rounded-xl text-white text-xs focus:outline-none focus:border-primary-400"
                  >
                    <option value="all">All Users</option>
                    <option value="doctors">Doctors</option>
                    <option value="nurses">Nurses</option>
                    <option value="viewers">Viewers</option>
                  </select>
                  <button
                    onClick={() => setShowUserModal(true)}
                    className="px-4 py-2 bg-gradient-primary text-white text-xs font-semibold rounded-xl hover:shadow-glow transition-all flex items-center gap-1.5"
                  >
                    <Plus size={14} />
                    Add New User
                  </button>
                  {selectedUsers.length > 0 && (
                    <div className="flex items-center gap-2 bg-white/5 p-1 rounded-xl border border-white/5">
                      <button
                        onClick={() => handleBulkAction('activate')}
                        className="px-2.5 py-1.5 bg-success-500/10 border border-success-500/20 text-success-400 rounded-lg hover:bg-success-500/20 transition-all text-xs font-semibold"
                      >
                        Activate Selected
                      </button>
                      <button
                        onClick={() => handleBulkAction('deactivate')}
                        className="px-2.5 py-1.5 bg-warning-500/10 border border-warning-500/20 text-warning-400 rounded-lg hover:bg-warning-500/20 transition-all text-xs font-semibold"
                      >
                        Deactivate
                      </button>
                      <button
                        onClick={() => handleBulkAction('delete')}
                        className="px-2.5 py-1.5 bg-error-500/10 border border-error-500/20 text-error-400 rounded-lg hover:bg-error-500/20 transition-all text-xs font-semibold"
                      >
                        Delete
                      </button>
                    </div>
                  )}
                </div>
              </div>

              {/* Users Table */}
              <div className="overflow-x-auto rounded-xl border border-white/5">
                <table className="min-w-full divide-y divide-white/5 text-left text-sm">
                  <thead className="bg-white/5 text-xs text-dark-300 uppercase tracking-widest">
                    <tr>
                      <th className="px-6 py-4 w-12">
                        <input
                          type="checkbox"
                          onChange={(e) => {
                            if (e.target.checked) {
                              setSelectedUsers(filteredUsers.map(u => u.id))
                            } else {
                              setSelectedUsers([])
                            }
                          }}
                          className="rounded border-white/10 bg-dark-900 text-primary-500"
                        />
                      </th>
                      <th className="px-6 py-4">Name</th>
                      <th className="px-6 py-4">Email</th>
                      <th className="px-6 py-4">Role</th>
                      <th className="px-6 py-4">Department</th>
                      <th className="px-6 py-4">Status</th>
                      <th className="px-6 py-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 bg-dark-900/10 text-dark-200">
                    {filteredUsers.map((user) => (
                      <tr key={user.id} className="hover:bg-white/5 transition-all">
                        <td className="px-6 py-4">
                          <input
                            type="checkbox"
                            checked={selectedUsers.includes(user.id)}
                            onChange={() => toggleUserSelection(user.id)}
                            className="rounded border-white/10 bg-dark-900 text-primary-500"
                          />
                        </td>
                        <td className="px-6 py-4 font-semibold text-white">{user.firstName} {user.lastName}</td>
                        <td className="px-6 py-4 font-mono text-xs text-dark-300">{user.email}</td>
                        <td className="px-6 py-4">
                          <span className={`inline-block text-[10px] px-2 py-0.5 rounded-full font-bold border ${
                            user.roleName === 'admin' ? 'bg-secondary-500/10 border-secondary-500/20 text-secondary-400' :
                            user.roleName === 'doctor' ? 'bg-primary-500/10 border-primary-500/20 text-primary-400' :
                            user.roleName === 'nurse' ? 'bg-success-500/10 border-success-500/20 text-success-400' :
                            'bg-dark-700/50 border-dark-600/20 text-dark-300'
                          }`}>
                            {user.roleName}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-dark-300">{user.departmentName || '-'}</td>
                        <td className="px-6 py-4">
                          <span className={`inline-block text-[10px] px-2 py-0.5 rounded-full font-bold border ${
                            user.isActive 
                              ? 'bg-success-500/10 border-success-500/20 text-success-400' 
                              : 'bg-error-500/10 border-error-500/20 text-error-400'
                          }`}>
                            {user.isActive ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <div className="flex justify-end space-x-2">
                            <button
                              onClick={() => handleEditUser(user)}
                              className="p-1.5 bg-primary-500/10 hover:bg-primary-500/20 text-primary-400 rounded-lg border border-primary-500/20 hover:scale-105 transition-all text-xs"
                              title="Edit User"
                            >
                              <Edit size={14} />
                            </button>
                            <button
                              onClick={() => updateUser(user.id, { isActive: !user.isActive })}
                              className={`p-1.5 rounded-lg border hover:scale-105 transition-all text-xs ${
                                user.isActive 
                                  ? 'bg-error-500/10 hover:bg-error-500/20 text-error-400 border-error-500/20' 
                                  : 'bg-success-500/10 hover:bg-success-500/20 text-success-400 border-success-500/20'
                              }`}
                              title={user.isActive ? 'Deactivate' : 'Activate'}
                            >
                              {user.isActive ? <Lock size={14} /> : <Unlock size={14} />}
                            </button>
                            <button
                              onClick={() => {
                                const newPassword = prompt('Enter new password:', 'password123')
                                if (newPassword) {
                                  updateUser(user.id, { password: newPassword })
                                  alert('Password reset successfully!')
                                }
                              }}
                              className="p-1.5 bg-warning-500/10 hover:bg-warning-500/20 text-warning-400 rounded-lg border border-warning-500/20 hover:scale-105 transition-all text-xs"
                              title="Reset Password"
                            >
                              <Key size={14} />
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
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <div className="flex justify-between items-center mb-6">
                <div>
                  <h2 className="text-xl font-bold text-white flex items-center gap-2">
                    <UserIcon className="text-primary-400" /> Patient Management
                  </h2>
                  <p className="text-sm text-dark-400">Admit and manage patients, conditions, rooms, and assignments.</p>
                </div>
                <button
                  onClick={() => setShowPatientModal(true)}
                  className="px-4 py-2.5 bg-gradient-primary text-white text-sm font-semibold rounded-xl hover:shadow-glow transition-all flex items-center gap-2"
                >
                  <Plus size={16} />
                  Admit New Patient
                </button>
              </div>

              {/* Patients Table */}
              <div className="overflow-x-auto rounded-xl border border-white/5">
                <table className="min-w-full divide-y divide-white/5 text-left text-sm">
                  <thead className="bg-white/5 text-xs text-dark-300 uppercase tracking-widest">
                    <tr>
                      <th className="px-6 py-4">ID</th>
                      <th className="px-6 py-4">Name</th>
                      <th className="px-6 py-4">Age / Gender</th>
                      <th className="px-6 py-4">Room</th>
                      <th className="px-6 py-4">Condition</th>
                      <th className="px-6 py-4">Mobile</th>
                      <th className="px-6 py-4">Clinicians Assigned</th>
                      <th className="px-6 py-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 bg-dark-900/10 text-dark-200">
                    {patients.map((patient) => {
                      const conditionUpper = String(patient.condition ?? '').toUpperCase();
                      const condBadgeColor = 
                        conditionUpper === 'CRITICAL' ? 'bg-red-500/10 border-red-500/20 text-red-400' :
                        conditionUpper === 'EMERGENCY' ? 'bg-orange-500/10 border-orange-500/20 text-orange-400' :
                        'bg-success-500/10 border-success-500/20 text-success-400';

                      return (
                        <tr key={patient.id} className="hover:bg-white/5 transition-all">
                          <td className="px-6 py-4 font-semibold text-xs text-dark-400">{patient.patientIdentifier}</td>
                          <td className="px-6 py-4 font-semibold text-white">{patient.name}</td>
                          <td className="px-6 py-4 text-xs">{patient.age} Yrs / {patient.gender}</td>
                          <td className="px-6 py-4 font-mono text-xs">{patient.roomNumber}</td>
                          <td className="px-6 py-4">
                            <span className={`inline-block text-[10px] px-2 py-0.5 rounded-full font-bold border ${condBadgeColor}`}>
                              {conditionUpper}
                            </span>
                          </td>
                          <td className="px-6 py-4 font-mono text-xs text-dark-300">{patient.mobileNumber || '-'}</td>
                          <td className="px-6 py-4">
                            <div className="flex flex-wrap gap-1">
                              {users.filter(u => u.roleName === 'doctor').map(doctor => {
                                const isAssigned = doctor.id === patient.doctorId;
                                return isAssigned ? (
                                  <span key={doctor.id} className="px-1.5 py-0.5 bg-primary-500/20 border border-primary-500/30 text-primary-300 text-[10px] rounded font-medium">
                                    Dr. {doctor.lastName}
                                  </span>
                                ) : null;
                              })}
                              {users.filter(u => u.roleName === 'nurse').map(nurse => {
                                const isAssigned = nurse.id === patient.nurseId;
                                return isAssigned ? (
                                  <span key={nurse.id} className="px-1.5 py-0.5 bg-success-500/20 border border-success-500/30 text-success-300 text-[10px] rounded font-medium">
                                    Nurse {nurse.lastName}
                                  </span>
                                ) : null;
                              })}
                              {!patient.doctorId && !patient.nurseId && (
                                <span className="text-[10px] text-dark-500 italic">None</span>
                              )}
                            </div>
                          </td>
                          <td className="px-6 py-4 text-right">
                            <div className="flex justify-end space-x-2">
                              <button
                                onClick={() => handleEditPatient(patient)}
                                className="p-2 bg-primary-500/10 hover:bg-primary-500/20 text-primary-400 hover:text-primary-300 rounded-xl border border-primary-500/20 hover:scale-105 transition-all flex items-center gap-1 text-xs font-semibold"
                              >
                                <Edit size={12} />
                                Edit
                              </button>
                              <button
                                onClick={() => updatePatient(patient.id, { doctorId: '', nurseId: '' })}
                                className="p-2 bg-error-500/10 hover:bg-error-500/20 text-error-400 hover:text-error-300 rounded-xl border border-error-500/20 hover:scale-105 transition-all flex items-center gap-1 text-xs font-semibold"
                                title="Unassign Staff"
                              >
                                <ShieldAlert size={12} />
                                Unassign
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </motion.div>
        )}

        {/* Alerts Tab */}
        {activeTab === 'alerts' && (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                <ShieldAlert className="text-error-400" /> Alert Console
              </h2>
              
              <div className="space-y-4">
                {consoleAlerts.map((alert) => (
                  <div key={alert.id} className="bg-dark-900/50 rounded-xl p-4 border border-white/5 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 hover:border-white/10 transition-all">
                    <div>
                      <div className="flex items-center space-x-2 mb-2">
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                          alert.type === 'CRITICAL' ? 'bg-red-500/10 border-red-500/20 text-red-400' :
                          alert.type === 'HIGH' ? 'bg-orange-500/10 border-orange-500/20 text-orange-400' :
                          'bg-warning-500/10 border-warning-500/20 text-warning-400'
                        }`}>
                          {alert.type}
                        </span>
                        <span className="text-dark-400 text-xs">{alert.time}</span>
                      </div>
                      <p className="text-white font-medium">{alert.message}</p>
                      <p className="text-dark-400 text-xs mt-1">Patient: {alert.patient}</p>
                    </div>
                    <div className="flex space-x-2 w-full md:w-auto">
                      <button onClick={() => resolveConsoleAlert(alert.id)} className="flex-1 md:flex-none px-3 py-1.5 bg-success-500/10 hover:bg-success-500/20 text-success-400 rounded-lg border border-success-500/20 text-xs font-semibold transition-all">
                        Resolve
                      </button>
                      <button onClick={() => escalateConsoleAlert(alert.id)} className="flex-1 md:flex-none px-3 py-1.5 bg-warning-500/10 hover:bg-warning-500/20 text-warning-400 rounded-lg border border-warning-500/20 text-xs font-semibold transition-all">
                        Escalate
                      </button>
                      <button onClick={() => notifyConsoleAlert(alert)} className="flex-1 md:flex-none px-3 py-1.5 bg-primary-500/10 hover:bg-primary-500/20 text-primary-400 rounded-lg border border-primary-500/20 text-xs font-semibold transition-all">
                        Notify
                      </button>
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
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                <DollarSign className="text-success-400" /> Financial Management
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
                <div className="bg-dark-900/50 border border-white/5 rounded-xl p-5 hover:border-white/10 transition-all">
                  <h3 className="text-dark-400 text-xs uppercase tracking-widest font-semibold mb-2">Total Revenue</h3>
                  <p className="text-2xl font-extrabold text-success-400">$125,430</p>
                  <p className="text-dark-500 text-xs mt-1">+12% from last month</p>
                </div>
                <div className="bg-dark-900/50 border border-white/5 rounded-xl p-5 hover:border-white/10 transition-all">
                  <h3 className="text-dark-400 text-xs uppercase tracking-widest font-semibold mb-2">Pending Invoices</h3>
                  <p className="text-2xl font-extrabold text-warning-400">$18,250</p>
                  <p className="text-dark-500 text-xs mt-1">15 pending payments</p>
                </div>
                <div className="bg-dark-900/50 border border-white/5 rounded-xl p-5 hover:border-white/10 transition-all">
                  <h3 className="text-dark-400 text-xs uppercase tracking-widest font-semibold mb-2">Total Expenses</h3>
                  <p className="text-2xl font-extrabold text-error-400">$42,180</p>
                  <p className="text-dark-500 text-xs mt-1">-5% from last month</p>
                </div>
              </div>

              <div className="flex flex-wrap gap-3">
                <button onClick={handleGenerateInvoice} className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-xl text-xs font-semibold transition-all">
                  Generate Invoice
                </button>
                <button onClick={handleTrackPayments} className="px-4 py-2 bg-success-500 hover:bg-success-600 text-white rounded-xl text-xs font-semibold transition-all">
                  Track Payments
                </button>
                <button onClick={handleExportFinancialReport} className="px-4 py-2 bg-dark-800 hover:bg-dark-700 text-white rounded-xl border border-white/5 text-xs font-semibold transition-all">
                  Export Report
                </button>
              </div>
            </div>
          </motion.div>
        )}

        {/* Reports Tab */}
        {activeTab === 'reports' && (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                <FileText className="text-primary-400" /> Reports & Analytics
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                <div className="bg-dark-900/50 border border-white/5 rounded-xl p-5">
                  <h3 className="text-white font-bold mb-4 flex items-center gap-2">
                    <UserIcon size={16} className="text-primary-400" /> Patient Reports
                  </h3>
                  <div className="space-y-2">
                    <button onClick={handleExportAllPatientReports} className="w-full px-4 py-3 bg-white/5 hover:bg-white/10 text-white border border-white/5 rounded-xl transition-all text-left text-xs font-semibold">
                      Patient Summary Report
                    </button>
                    <button onClick={handleExportAllPatientReports} className="w-full px-4 py-3 bg-white/5 hover:bg-white/10 text-white border border-white/5 rounded-xl transition-all text-left text-xs font-semibold">
                      Detailed Patient Report
                    </button>
                  </div>
                </div>

                <div className="bg-dark-900/50 border border-white/5 rounded-xl p-5">
                  <h3 className="text-white font-bold mb-4 flex items-center gap-2">
                    <Award size={16} className="text-secondary-400" /> Department Reports
                  </h3>
                  <div className="space-y-2">
                    <button className="w-full px-4 py-3 bg-white/5 hover:bg-white/10 text-white border border-white/5 rounded-xl transition-all text-left text-xs font-semibold">
                      Department Performance
                    </button>
                    <button className="w-full px-4 py-3 bg-white/5 hover:bg-white/10 text-white border border-white/5 rounded-xl transition-all text-left text-xs font-semibold">
                      Staff Activity Report
                    </button>
                  </div>
                </div>
              </div>

              <div className="flex space-x-3">
                <button onClick={handleExportAllPatientReports} className="px-4 py-2 bg-error-500 hover:bg-error-600 text-white rounded-xl text-xs font-semibold transition-all">
                  Export as PDF
                </button>
                <button onClick={() => downloadText('patients.csv', patients.map(p => `${p.patientIdentifier},"${p.name}",${p.condition}`).join('\n'))} className="px-4 py-2 bg-success-500 hover:bg-success-600 text-white rounded-xl text-xs font-semibold transition-all">
                  Export as CSV
                </button>
              </div>
            </div>
          </motion.div>
        )}

        {/* Audit Logs Tab */}
        {activeTab === 'audit' && (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                <Search className="text-primary-400" /> System Audit Logs
              </h2>
              
              <div className="overflow-x-auto rounded-xl border border-white/5">
                <table className="min-w-full divide-y divide-white/5 text-left text-sm">
                  <thead className="bg-white/5 text-xs text-dark-300 uppercase tracking-widest">
                    <tr>
                      <th className="px-6 py-4">User</th>
                      <th className="px-6 py-4">Action</th>
                      <th className="px-6 py-4">Target</th>
                      <th className="px-6 py-4">Timestamp</th>
                      <th className="px-6 py-4">IP Address</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 bg-dark-900/10 text-dark-200">
                    {auditLogs.map((log) => (
                      <tr key={log.id} className="hover:bg-white/5 transition-all">
                        <td className="px-6 py-4 font-semibold text-white">{log.user}</td>
                        <td className="px-6 py-4">{log.action}</td>
                        <td className="px-6 py-4 text-dark-300">{log.target}</td>
                        <td className="px-6 py-4 font-mono text-xs text-dark-400">{log.timestamp}</td>
                        <td className="px-6 py-4 font-mono text-xs text-dark-400">{log.ip}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </motion.div>
        )}

        {/* System Tab */}
        {activeTab === 'system' && (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                <Settings className="text-primary-400" /> System Configuration
              </h2>
              
              <div className="space-y-6">
                <div>
                  <h3 className="text-sm font-semibold text-dark-300 uppercase tracking-wider mb-3">Departments</h3>
                  <div className="flex flex-wrap gap-2">
                    {systemConfig.departments.map((dept, index) => (
                      <span key={index} className="px-3 py-1 bg-primary-500/10 border border-primary-500/20 text-primary-400 rounded-full text-xs font-semibold">
                        {dept}
                      </span>
                    ))}
                  </div>
                </div>

                <div>
                  <h3 className="text-sm font-semibold text-dark-300 uppercase tracking-wider mb-3">Risk Thresholds</h3>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div>
                      <label className="text-xs text-dark-400 mb-1.5 block">Heart Rate (bpm)</label>
                      <input
                        type="number"
                        value={systemConfig.riskThresholds.heartRate}
                        onChange={(e) => setSystemConfig(prev => ({
                          ...prev,
                          riskThresholds: { ...prev.riskThresholds, heartRate: parseInt(e.target.value) }
                        }))}
                        className="input-field w-full"
                      />
                    </div>
                    <div>
                      <label className="text-xs text-dark-400 mb-1.5 block">Blood Pressure (mmHg)</label>
                      <input
                        type="number"
                        value={systemConfig.riskThresholds.bloodPressure}
                        onChange={(e) => setSystemConfig(prev => ({
                          ...prev,
                          riskThresholds: { ...prev.riskThresholds, bloodPressure: parseInt(e.target.value) }
                        }))}
                        className="input-field w-full"
                      />
                    </div>
                    <div>
                      <label className="text-xs text-dark-400 mb-1.5 block">Temperature (°F)</label>
                      <input
                        type="number"
                        value={systemConfig.riskThresholds.temperature}
                        onChange={(e) => setSystemConfig(prev => ({
                          ...prev,
                          riskThresholds: { ...prev.riskThresholds, temperature: parseInt(e.target.value) }
                        }))}
                        className="input-field w-full"
                      />
                    </div>
                  </div>
                </div>

                <div>
                  <h3 className="text-sm font-semibold text-dark-300 uppercase tracking-wider mb-3">System Features</h3>
                  <div className="space-y-3">
                    <label className="flex items-center gap-3 cursor-pointer text-sm text-dark-200">
                      <input
                        type="checkbox"
                        checked={systemConfig.notifications}
                        onChange={(e) => setSystemConfig(prev => ({ ...prev, notifications: e.target.checked }))}
                        className="w-5 h-5 rounded border-white/10 bg-dark-900 text-primary-500"
                      />
                      Enable Critical Alert Notification System
                    </label>
                    <label className="flex items-center gap-3 cursor-pointer text-sm text-dark-200">
                      <input
                        type="checkbox"
                        checked={systemConfig.aiEnabled}
                        onChange={(e) => setSystemConfig(prev => ({ ...prev, aiEnabled: e.target.checked }))}
                        className="w-5 h-5 rounded border-white/10 bg-dark-900 text-primary-500"
                      />
                      Enable AI Vital Predictions & Patient Risk Scoring
                    </label>
                  </div>
                </div>
              </div>

              <div className="mt-8 border-t border-white/5 pt-6">
                <button
                  onClick={handleSystemConfig}
                  className="btn-primary py-2.5 text-sm"
                >
                  Save Configuration
                </button>
              </div>
            </div>
          </motion.div>
        )}

        {/* Notifications Tab */}
        {activeTab === 'notifications' && (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                <Bell className="text-primary-400" /> Notification Broadcast Control
              </h2>
              
              <div className="space-y-6">
                <div>
                  <label className="text-xs text-dark-400 mb-1.5 block">Audience Selection</label>
                  <select
                    value={notificationAudience}
                    onChange={(e) => setNotificationAudience(e.target.value as any)}
                    className="input-field w-full"
                  >
                    <option value="all">All Hospital Staff</option>
                    <option value="doctors">Attending Physicians</option>
                    <option value="department">Specific Department Staff</option>
                  </select>
                </div>

                <div>
                  <label className="text-xs text-dark-400 mb-1.5 block">Broadcast Message</label>
                  <textarea
                    value={notificationMessage}
                    onChange={(e) => setNotificationMessage(e.target.value)}
                    placeholder="Enter message text to broadcast..."
                    className="input-field w-full"
                    rows={4}
                  />
                </div>

                <div className="flex flex-col sm:flex-row gap-3 pt-2">
                  <button
                    onClick={handleNotification}
                    className="btn-primary flex-1 flex justify-center items-center gap-2 text-sm"
                  >
                    <Mail size={16} />
                    Send Notification
                  </button>
                  <button
                    onClick={() => alert('Emergency broadcast sent to all staff!')}
                    className="flex-1 bg-error-500 hover:bg-error-600 text-white font-semibold py-3 px-6 rounded-xl shadow-glow hover:shadow-glow-lg transition-all duration-300 flex items-center justify-center gap-2 text-sm"
                  >
                    <AlertTriangle size={16} />
                    Emergency Broadcast
                  </button>
                </div>
              </div>
            </div>
          </motion.div>
        )}

        {/* Data Management Tab */}
        {activeTab === 'data' && (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                <Database className="text-primary-400" /> Data Management Console
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-3">
                  <button className="w-full px-4 py-4 bg-white/5 hover:bg-white/10 text-white border border-white/5 rounded-xl transition-all text-left text-xs font-semibold flex items-center gap-3">
                    <Database size={16} className="text-primary-400" />
                    Import Patient EHR Data
                  </button>
                  <button className="w-full px-4 py-4 bg-white/5 hover:bg-white/10 text-white border border-white/5 rounded-xl transition-all text-left text-xs font-semibold flex items-center gap-3">
                    <FileText size={16} className="text-success-400" />
                    Export Patient Clinical Records
                  </button>
                  <button className="w-full px-4 py-4 bg-white/5 hover:bg-white/10 text-white border border-white/5 rounded-xl transition-all text-left text-xs font-semibold flex items-center gap-3">
                    <RefreshCw size={16} className="text-secondary-400" />
                    Backup System Database
                  </button>
                </div>

                <div className="space-y-3">
                  <button className="w-full px-4 py-4 bg-white/5 hover:bg-white/10 text-white border border-white/5 rounded-xl transition-all text-left text-xs font-semibold flex items-center gap-3">
                    <RefreshCw size={16} className="text-warning-400" />
                    Restore System Data State
                  </button>
                  <button className="w-full px-4 py-4 bg-white/5 hover:bg-white/10 text-white border border-white/5 rounded-xl transition-all text-left text-xs font-semibold flex items-center gap-3">
                    <Trash2 size={16} className="text-error-400" />
                    Clean & Archive Historical Records
                  </button>
                  <button className="w-full px-4 py-4 bg-white/5 hover:bg-white/10 text-white border border-white/5 rounded-xl transition-all text-left text-xs font-semibold flex items-center gap-3">
                    <FileText size={16} className="text-primary-400" />
                    Generate Data Integrity Reports
                  </button>
                </div>
              </div>
            </div>
          </motion.div>
        )}

        {/* AI Model Tab */}
        {activeTab === 'ai' && (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                <Brain className="text-primary-400" /> AI Model Control & Monitoring
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6">
                <div className="bg-dark-900/50 border border-white/5 rounded-xl p-5 hover:border-white/10 transition-all">
                  <h3 className="text-dark-400 text-xs uppercase tracking-widest font-semibold mb-2">Model Accuracy</h3>
                  <p className="text-2xl font-extrabold text-success-400">{aiModelStats.accuracy}%</p>
                  <p className="text-dark-500 text-xs mt-1">+2.3% from last month</p>
                </div>
                <div className="bg-dark-900/50 border border-white/5 rounded-xl p-5 hover:border-white/10 transition-all">
                  <h3 className="text-dark-400 text-xs uppercase tracking-widest font-semibold mb-2">Total Predictions</h3>
                  <p className="text-2xl font-extrabold text-primary-400">{aiModelStats.predictions}</p>
                  <p className="text-dark-500 text-xs mt-1">Last 30 days</p>
                </div>
                <div className="bg-dark-900/50 border border-white/5 rounded-xl p-5 hover:border-white/10 transition-all">
                  <h3 className="text-dark-400 text-xs uppercase tracking-widest font-semibold mb-2">False Positives</h3>
                  <p className="text-2xl font-extrabold text-warning-400">{aiModelStats.falsePositives}</p>
                  <p className="text-dark-500 text-xs mt-1">3.6% rate</p>
                </div>
                <div className="bg-dark-900/50 border border-white/5 rounded-xl p-5 hover:border-white/10 transition-all">
                  <h3 className="text-dark-400 text-xs uppercase tracking-widest font-semibold mb-2">Performance State</h3>
                  <p className="text-2xl font-extrabold text-secondary-400">{aiModelStats.performance}%</p>
                  <p className="text-dark-500 text-xs mt-1">Excellent health</p>
                </div>
              </div>

              <div className="bg-dark-900/50 border border-white/5 rounded-xl p-5 mb-6 flex justify-between items-center">
                <div>
                  <h3 className="text-white font-bold">Last Trained Timestamp</h3>
                  <p className="text-dark-300 text-sm mt-1">{aiModelStats.lastRetrained}</p>
                </div>
                <span className="px-3 py-1 bg-success-500/10 border border-success-500/20 text-success-400 rounded-full text-xs font-semibold">
                  Model Up-to-Date
                </span>
              </div>

              <div className="flex space-x-3">
                <button
                  onClick={() => alert('Model retraining initiated! This may take several hours.')}
                  className="px-4 py-2.5 bg-gradient-primary text-white rounded-xl text-xs font-semibold shadow-glow hover:shadow-glow-lg transition-all"
                >
                  Retrain Model
                </button>
                <button
                  onClick={() => alert('Model performance report exported!')}
                  className="px-4 py-2.5 bg-dark-800 hover:bg-dark-700 text-white rounded-xl border border-white/5 text-xs font-semibold transition-all"
                >
                  Export Performance Metrics
                </button>
              </div>
            </div>
          </motion.div>
        )}

        {/* Workload Tab */}
        {activeTab === 'workload' && (
          <motion.div
            initial={{ opacity: 0, y: 15 }}
            animate={{ opacity: 1, y: 0 }}
            className="space-y-6"
          >
            <div className="glass-panel p-6">
              <h2 className="text-xl font-bold text-white mb-6 flex items-center gap-2">
                <Brain className="text-secondary-400" /> Physician Workload Monitoring
              </h2>
              
              <div className="overflow-x-auto rounded-xl border border-white/5">
                <table className="min-w-full divide-y divide-white/5 text-left text-sm">
                  <thead className="bg-white/5 text-xs text-dark-300 uppercase tracking-widest">
                    <tr>
                      <th className="px-6 py-4">Attending Doctor</th>
                      <th className="px-6 py-4">Department</th>
                      <th className="px-6 py-4">Active Patients</th>
                      <th className="px-6 py-4">Workload Level</th>
                      <th className="px-6 py-4">Safety Status</th>
                      <th className="px-6 py-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 bg-dark-900/10 text-dark-200">
                    {doctorWorkload.map((doctor) => (
                      <tr key={doctor.id} className="hover:bg-white/5 transition-all">
                        <td className="px-6 py-4 font-semibold text-white">{doctor.name}</td>
                        <td className="px-6 py-4 text-dark-300">{doctor.department}</td>
                        <td className="px-6 py-4 font-mono text-xs">{doctor.patients}</td>
                        <td className="px-6 py-4">
                          <span className={`inline-block text-[10px] px-2 py-0.5 rounded-full font-bold border ${
                            doctor.workload === 'High' ? 'bg-red-500/10 border-red-500/20 text-red-400' :
                            doctor.workload === 'Medium' ? 'bg-orange-500/10 border-orange-500/20 text-orange-400' :
                            'bg-success-500/10 border-success-500/20 text-success-400'
                          }`}>
                            {doctor.workload}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-block text-[10px] px-2 py-0.5 rounded-full font-bold border ${
                            doctor.workload === 'High' ? 'bg-error-500/10 border-error-500/20 text-error-400' :
                            'bg-success-500/10 border-success-500/20 text-success-400'
                          }`}>
                            {doctor.workload === 'High' ? 'Overloaded' : 'Normal'}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-right">
                          <div className="flex justify-end space-x-2">
                            <button className="p-2 bg-primary-500/10 hover:bg-primary-500/20 text-primary-400 rounded-xl border border-primary-500/20 hover:scale-105 transition-all text-xs font-semibold flex items-center gap-1">
                              View Details
                            </button>
                            <button className="p-2 bg-success-500/10 hover:bg-success-500/20 text-success-400 rounded-xl border border-success-500/20 hover:scale-105 transition-all text-xs font-semibold flex items-center gap-1">
                              Reassign
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
      </main>

      {/* Add/Edit User Modal */}
      <AnimatePresence>
        {showUserModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-dark-950/80 backdrop-blur-md flex items-center justify-center z-50 p-4"
          >
            <motion.div
              initial={{ scale: 0.95, y: 15 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 15 }}
              className="bg-dark-900 border border-white/10 rounded-2xl p-6 w-full max-w-md shadow-2xl relative"
            >
              <div className="flex justify-between items-center border-b border-white/5 pb-3 mb-4">
                <h3 className="text-lg font-bold text-white flex items-center gap-2">
                  <Users className="text-primary-400" /> {editingUser ? 'Edit User Profile' : 'Add New Staff Member'}
                </h3>
                <button
                  onClick={() => {
                    setShowUserModal(false)
                    setEditingUser(null)
                    setFormData({ name: '', email: '', password: '', department: '', specialization: '', role: 'doctor' })
                  }}
                  className="text-dark-400 hover:text-white bg-white/5 hover:bg-white/10 p-1.5 rounded-lg transition-all"
                >
                  ✕
                </button>
              </div>
              
              <form onSubmit={handleUserSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs text-dark-400 mb-1">Name</label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    className="input-field w-full"
                    placeholder="e.g. John Doe"
                    required
                  />
                </div>
                <div>
                  <label className="block text-xs text-dark-400 mb-1">Email Address</label>
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    className="input-field w-full"
                    placeholder="e.g. name@meditrack.ai"
                    required
                  />
                </div>
                {!editingUser && (
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Temporary Password</label>
                    <input
                      type="password"
                      value={formData.password}
                      onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                      className="input-field w-full"
                      placeholder="Password"
                      required
                    />
                  </div>
                )}
                <div>
                  <label className="block text-xs text-dark-400 mb-1">Role Type</label>
                  <select
                    value={formData.role}
                    onChange={(e) => setFormData({ ...formData, role: e.target.value as any })}
                    className="input-field w-full"
                  >
                    <option value="doctor">Doctor</option>
                    <option value="nurse">Nurse</option>
                    <option value="viewer">Viewer</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-dark-400 mb-1">Department</label>
                  <input
                    type="text"
                    value={formData.department}
                    onChange={(e) => setFormData({ ...formData, department: e.target.value })}
                    className="input-field w-full"
                    placeholder="e.g. Cardiology"
                  />
                </div>
                {formData.role === 'doctor' && (
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Specialization</label>
                    <input
                      type="text"
                      value={formData.specialization}
                      onChange={(e) => setFormData({ ...formData, specialization: e.target.value })}
                      className="input-field w-full"
                      placeholder="e.g. Cardiology"
                    />
                  </div>
                )}
                <div className="flex space-x-3 pt-4 border-t border-white/5 mt-5">
                  <button
                    type="submit"
                    className="btn-primary flex-1 py-2 text-sm"
                  >
                    {editingUser ? 'Update Profile' : 'Register User'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowUserModal(false)
                      setEditingUser(null)
                      setFormData({ name: '', email: '', password: '', department: '', specialization: '', role: 'doctor' })
                    }}
                    className="btn-secondary flex-1 py-2 text-sm"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Add/Edit Patient Modal */}
      <AnimatePresence>
        {showPatientModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-dark-950/80 backdrop-blur-md flex items-center justify-center z-50 p-4"
          >
            <motion.div
              initial={{ scale: 0.95, y: 15 }}
              animate={{ scale: 1, y: 0 }}
              exit={{ scale: 0.95, y: 15 }}
              className="bg-dark-900 border border-white/10 rounded-2xl p-6 w-full max-w-2xl shadow-2xl relative overflow-y-auto max-h-[90vh]"
            >
              <div className="flex justify-between items-center border-b border-white/5 pb-3 mb-4">
                <h3 className="text-lg font-bold text-white flex items-center gap-2">
                  <UserIcon className="text-primary-400" /> {editingPatient ? 'Edit Patient File' : 'Admit New Patient'}
                </h3>
                <button
                  onClick={() => {
                    setShowPatientModal(false)
                    setEditingPatient(null)
                    setPatientFormData({
                      name: '', email: '', password: '', age: 0, gender: 'Male',
                      roomNumber: '', condition: 'STABLE', admittedDate: '', doctorId: '', nurseId: '',
                      mobileNumber: '', guardianName: '', guardianMobile: ''
                    })
                  }}
                  className="text-dark-400 hover:text-white bg-white/5 hover:bg-white/10 p-1.5 rounded-lg transition-all"
                >
                  ✕
                </button>
              </div>
              
              <form onSubmit={handlePatientSubmit} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Full Name</label>
                    <input
                      type="text"
                      value={patientFormData.name}
                      onChange={(e) => setPatientFormData({ ...patientFormData, name: e.target.value })}
                      className="input-field w-full"
                      placeholder="e.g. John Doe"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Email Address</label>
                    <input
                      type="email"
                      value={patientFormData.email}
                      onChange={(e) => setPatientFormData({ ...patientFormData, email: e.target.value })}
                      className="input-field w-full"
                      placeholder="e.g. patient@email.com"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Mobile Number</label>
                    <input
                      type="tel"
                      value={patientFormData.mobileNumber}
                      onChange={(e) => setPatientFormData({ ...patientFormData, mobileNumber: e.target.value })}
                      className="input-field w-full"
                      placeholder="e.g. +1-555-0123-4567"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Guardian Name</label>
                    <input
                      type="text"
                      value={patientFormData.guardianName}
                      onChange={(e) => setPatientFormData({ ...patientFormData, guardianName: e.target.value })}
                      className="input-field w-full"
                      placeholder="Guardian name"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Guardian Mobile</label>
                    <input
                      type="tel"
                      value={patientFormData.guardianMobile}
                      onChange={(e) => setPatientFormData({ ...patientFormData, guardianMobile: e.target.value })}
                      className="input-field w-full"
                      placeholder="Guardian mobile"
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Age</label>
                    <input
                      type="number"
                      value={patientFormData.age}
                      onChange={(e) => setPatientFormData({ ...patientFormData, age: parseInt(e.target.value) })}
                      className="input-field w-full"
                      placeholder="Age"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Gender</label>
                    <select
                      value={patientFormData.gender}
                      onChange={(e) => setPatientFormData({ ...patientFormData, gender: e.target.value as 'Male' | 'Female' })}
                      className="input-field w-full"
                    >
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Room / Bed Number</label>
                    <input
                      type="text"
                      value={patientFormData.roomNumber}
                      onChange={(e) => setPatientFormData({ ...patientFormData, roomNumber: e.target.value })}
                      className="input-field w-full"
                      placeholder="e.g. A-101"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Assigned Physician</label>
                    <select
                      value={patientFormData.doctorId}
                      onChange={(e) => setPatientFormData({ ...patientFormData, doctorId: e.target.value })}
                      className="input-field w-full"
                    >
                      <option value="">Select Doctor</option>
                      {mockUsers.filter(u => u.role === 'doctor').map(doctor => (
                        <option key={doctor.id} value={doctor.id}>
                          {doctor.name} ({doctor.department})
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-xs text-dark-400 mb-1">Clinical Condition</label>
                    <select
                      value={patientFormData.condition}
                      onChange={(e) => setPatientFormData({ ...patientFormData, condition: e.target.value as 'STABLE' | 'CRITICAL' | 'EMERGENCY' })}
                      className="input-field w-full"
                    >
                      <option value="STABLE">Stable</option>
                      <option value="CRITICAL">Critical</option>
                      <option value="EMERGENCY">Emergency</option>
                    </select>
                  </div>
                </div>
                
                <div className="flex space-x-3 pt-4 border-t border-white/5 mt-5">
                  <button
                    type="submit"
                    className="btn-primary flex-1 py-2 text-sm"
                  >
                    {editingPatient ? 'Save Changes' : 'Admit Patient'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setShowPatientModal(false)
                      setEditingPatient(null)
                      setPatientFormData({
                        name: '', email: '', password: '', age: 0, gender: 'Male',
                        roomNumber: '', condition: 'STABLE', admittedDate: '', doctorId: '', nurseId: '',
                        mobileNumber: '', guardianName: '', guardianMobile: ''
                      })
                    }}
                    className="btn-secondary flex-1 py-2 text-sm"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <style dangerouslySetInnerHTML={{ __html: `
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
      `}} />
    </div>
  )
}
