import { useState, useEffect, useRef } from 'react'
import { AuthService } from '../auth/AuthService'
import { generateMedicalReport, getPatientsByDoctor, getVitalSignsByPatient, Patient, VitalSign } from '../database/mockDatabaseFromSeed'
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend, LineChart, Line } from 'recharts'
import { Activity, Heart, Thermometer, Flame, Phone, Download, Copy, LogOut, User, FileText, ShieldAlert, Award, Bell, X, FlaskConical } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'

type NotificationItem = { id: string; title: string; detail: string; timestamp: string; read: boolean }

const SEED_NOTIFICATIONS: NotificationItem[] = [
  { id: 'notif-1', title: 'System Alert', detail: 'Scheduled maintenance tonight 11 PM–1 AM. Save all work.', timestamp: new Date(Date.now() - 60 * 60 * 1000).toISOString(), read: false },
  { id: 'notif-2', title: 'Critical Protocol Update', detail: 'New triage protocol effective from today. Check Cardiology bulletin.', timestamp: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(), read: false },
  { id: 'notif-3', title: 'Staff Meeting', detail: 'Department heads meeting tomorrow at 9 AM in Conference Room B.', timestamp: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(), read: true },
]

const LAB_ANALYTICS: Record<string, { test: string; normal: number; abnormal: number; pending: number }[]> = {
  Cardiology: [
    { test: 'Troponin I', normal: 14, abnormal: 6, pending: 2 },
    { test: 'BNP/NT-proBNP', normal: 10, abnormal: 8, pending: 3 },
    { test: 'Lipid Panel', normal: 18, abnormal: 4, pending: 1 },
    { test: 'CK-MB', normal: 12, abnormal: 5, pending: 2 },
    { test: 'ECG Markers', normal: 16, abnormal: 3, pending: 0 },
  ],
  Pediatrics: [
    { test: 'CBC', normal: 20, abnormal: 3, pending: 1 },
    { test: 'Metabolic Panel', normal: 18, abnormal: 4, pending: 2 },
    { test: 'Thyroid Function', normal: 15, abnormal: 2, pending: 0 },
    { test: 'Iron Studies', normal: 12, abnormal: 6, pending: 1 },
    { test: 'Bilirubin', normal: 14, abnormal: 3, pending: 2 },
  ],
  Neurology: [
    { test: 'CSF Analysis', normal: 8, abnormal: 5, pending: 3 },
    { test: 'Ammonia Levels', normal: 10, abnormal: 4, pending: 1 },
    { test: 'Lactate/Pyruvate', normal: 12, abnormal: 3, pending: 0 },
    { test: 'EEG Markers', normal: 9, abnormal: 6, pending: 2 },
    { test: 'MRI Contrast', normal: 14, abnormal: 2, pending: 1 },
  ],
  Oncology: [
    { test: 'Tumor Markers', normal: 6, abnormal: 10, pending: 4 },
    { test: 'CBC w/ Differential', normal: 8, abnormal: 9, pending: 2 },
    { test: 'Liver Function', normal: 11, abnormal: 6, pending: 1 },
    { test: 'Kidney Function', normal: 13, abnormal: 4, pending: 2 },
    { test: 'Coagulation', normal: 10, abnormal: 5, pending: 3 },
  ],
  Orthopedics: [
    { test: 'Calcium/Phosphate', normal: 16, abnormal: 3, pending: 1 },
    { test: 'Vitamin D', normal: 12, abnormal: 7, pending: 0 },
    { test: 'CRP/ESR', normal: 14, abnormal: 5, pending: 2 },
    { test: 'Synovial Fluid', normal: 8, abnormal: 4, pending: 3 },
    { test: 'Bone Density', normal: 10, abnormal: 6, pending: 1 },
  ],
}

export default function DoctorDashboard() {
  const [patients, setPatients] = useState<Patient[]>([])
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null)
  const [reportContent, setReportContent] = useState('')
  const [showReportModal, setShowReportModal] = useState(false)
  const [showLabModal, setShowLabModal] = useState(false)
  const currentUser = AuthService.getCurrentUser()
  const [mockAlerts, setMockAlerts] = useState<any[]>([])
  const [resolvedAlertIds, setResolvedAlertIds] = useState<string[]>([])
  const [vitalsHistory, setVitalsHistory] = useState<VitalSign[]>([])
  const [notifications, setNotifications] = useState<NotificationItem[]>(() => {
    try {
      const stored = localStorage.getItem('app_notifications')
      return stored ? JSON.parse(stored) as NotificationItem[] : SEED_NOTIFICATIONS
    } catch { return SEED_NOTIFICATIONS }
  })
  const [showNotifPanel, setShowNotifPanel] = useState(false)
  const notifRef = useRef<HTMLDivElement>(null)
  const unreadCount = notifications.filter(n => !n.read).length

  // Load mock alerts, filtering out resolved ones
  useEffect(() => {
    if (selectedPatient) {
      const condition = selectedPatient.condition;
      const alerts = [];
      if (condition === 'CRITICAL' || condition === 'EMERGENCY') {
        alerts.push({
          id: `alert-1-${selectedPatient.id}`,
          patientId: selectedPatient.id,
          message: `CRITICAL: Elevated Heart Rate detected at ${vitalsHistory[vitalsHistory.length - 1]?.heartRate || 105} bpm`,
          priority: 'CRITICAL',
          status: 'ACTIVE',
          createdAt: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
          vitalType: 'HEART_RATE',
          vitalValue: vitalsHistory[vitalsHistory.length - 1]?.heartRate || 105
        });
        alerts.push({
          id: `alert-2-${selectedPatient.id}`,
          patientId: selectedPatient.id,
          message: `HIGH: Oxygen Saturation dropped below threshold to ${vitalsHistory[vitalsHistory.length - 1]?.oxygenSaturation || 92}%`,
          priority: 'HIGH',
          status: 'ACKNOWLEDGED',
          createdAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
          vitalType: 'SPO2',
          vitalValue: vitalsHistory[vitalsHistory.length - 1]?.oxygenSaturation || 92
        });
      } else {
        alerts.push({
          id: `alert-3-${selectedPatient.id}`,
          patientId: selectedPatient.id,
          message: `LOW: Patient vitals checked and normal`,
          priority: 'LOW',
          status: 'RESOLVED',
          createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString()
        });
      }
      
      const activeAlerts = alerts.filter(a => !resolvedAlertIds.includes(a.id));
      setMockAlerts(activeAlerts);
    }
  }, [selectedPatient, resolvedAlertIds, vitalsHistory]);

  const handleUpdateMockAlertStatus = (alertId: string, newStatus: string) => {
    if (newStatus === 'RESOLVED') {
      setResolvedAlertIds(prev => [...prev, alertId]);
    } else {
      setMockAlerts(prev => prev.map(alert => alert.id === alertId ? { ...alert, status: newStatus } : alert));
    }
  };

  // Sync vitals from the mock database every 3 seconds
  useEffect(() => {
    if (!selectedPatient) return;
    
    const updateVitals = () => {
      setVitalsHistory(getVitalSignsByPatient(selectedPatient.id))
    }
    
    updateVitals()
    const interval = setInterval(updateVitals, 3000)
    return () => clearInterval(interval)
  }, [selectedPatient])

  // Sync notifications across windows/tabs
  useEffect(() => {
    function onStorage(e: StorageEvent) {
      if (e.key === 'app_notifications') {
        try {
          const parsed = JSON.parse(e.newValue || '[]') as NotificationItem[]
          setNotifications(parsed)
        } catch {}
      }
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  // Close notifications panel on click outside
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) {
        setShowNotifPanel(false)
      }
    }
    if (showNotifPanel) document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [showNotifPanel])

  const markNotifRead = (id: string) => {
    setNotifications(prev => {
      const updated = prev.map(n => n.id === id ? { ...n, read: true } : n)
      try { localStorage.setItem('app_notifications', JSON.stringify(updated)) } catch {}
      return updated
    })
  }

  const markAllRead = () => {
    setNotifications(prev => {
      const updated = prev.map(n => ({ ...n, read: true }))
      try { localStorage.setItem('app_notifications', JSON.stringify(updated)) } catch {}
      return updated
    })
  }

  useEffect(() => {
    if (currentUser) {
      const doctorPatients = getPatientsByDoctor(currentUser.id)
      setPatients(doctorPatients)
      if (doctorPatients.length > 0) {
        setSelectedPatient(doctorPatients[0])
      }
    }
  }, [currentUser])

  const handleGenerateReport = (type: 'shift' | 'medical') => {
    if (!selectedPatient) return

    const content = type === 'medical'
      ? generateMedicalReport(selectedPatient.id)
      : generateReportContent(type, selectedPatient)

    // Save report to patient's mock reports list
    const newReport = {
      id: `report-${Date.now()}`,
      patientId: selectedPatient.id,
      doctorId: currentUser?.id || '',
      title: `${type === 'shift' ? 'Shift' : 'Medical'} Report - ${selectedPatient.name}`,
      content: content,
      createdAt: new Date().toISOString()
    }

    if (!selectedPatient.reports) {
      selectedPatient.reports = []
    }
    selectedPatient.reports.push(newReport)

    setReportContent(content)
    setShowReportModal(true)
  }

  const generateReportContent = (type: 'shift' | 'medical', patient: Patient): string => {
    const timestamp = new Date().toLocaleString()
    
    if (type === 'shift') {
      return `
SHIFT HANDOVER REPORT
======================
Patient: ${patient.name}
Room: ${patient.roomNumber}
Condition: ${patient.condition}
Generated: ${timestamp}

VITAL SIGNS SUMMARY:
${patient.vitals.map(vital => `
- Time: ${new Date(vital.timestamp).toLocaleString()}
- Heart Rate: ${vital.heartRate} bpm
- Blood Pressure: ${vital.bloodPressure.systolic}/${vital.bloodPressure.diastolic} mmHg
- Temperature: ${vital.temperature}°F
- Oxygen Saturation: ${vital.oxygenSaturation}%
- Recorded by: ${vital.recordedBy}
`).join('')}

OBSERVATIONS:
- Patient is ${patient.vitals[patient.vitals.length - 1]?.heartRate > 100 ? 'tachycardic' : 'normocardic'}
- Blood pressure ${patient.vitals[patient.vitals.length - 1]?.bloodPressure.systolic > 140 ? 'elevated' : 'stable'}
- Temperature ${patient.vitals[patient.vitals.length - 1]?.temperature > 99 ? 'elevated' : 'normal'}
- Oxygen saturation ${patient.vitals[patient.vitals.length - 1]?.oxygenSaturation < 95 ? 'low' : 'good'}

PLAN:
- Continue current treatment
- Monitor vital signs every 4 hours
- Administer prescribed medications
- Report any changes immediately

NOTES:
Patient appears stable. No immediate concerns noted.
      `.trim()
    } else {
      return `
MEDICAL REPORT
==============
Patient: ${patient.name}
Age: ${patient.age}
Gender: ${patient.gender}
Room: ${patient.roomNumber}
Admitted: ${patient.admittedDate}
Condition: ${patient.condition}
Mobile: ${patient.mobileNumber || 'Not provided'}
Guardian: ${patient.guardianName || 'Not provided'}
Guardian Mobile: ${patient.guardianMobile || 'Not provided'}
Generated: ${timestamp}

CURRENT STATUS:
- General condition: ${patient.condition}
- Alert and oriented
- Following commands appropriately

VITAL SIGNS TREND:
${patient.vitals.map(vital => `
- ${new Date(vital.timestamp).toLocaleString()}: HR ${vital.heartRate}, BP ${vital.bloodPressure.systolic}/${vital.bloodPressure.diastolic}, T ${vital.temperature}°F, O2 ${vital.oxygenSaturation}%
`).join('')}

ASSESSMENT:
Patient is showing ${patient.vitals[patient.vitals.length - 1]?.heartRate > 100 ? 'signs of tachycardia' : 'normal cardiac rhythm'}
Blood pressure is ${patient.vitals[patient.vitals.length - 1]?.bloodPressure.systolic > 140 ? 'elevated and requires monitoring' : 'within normal limits'}
Temperature is ${patient.vitals[patient.vitals.length - 1]?.temperature > 99 ? 'elevated, possible infection' : 'normal'}

TREATMENT PLAN:
1. Continue current medications
2. Monitor vital signs every 4 hours
3. Daily physical assessment
4. Laboratory tests as needed
5. Consultation with specialists if condition changes

PROGNOSIS:
Good, with expected improvement in ${patient.condition.includes('Post') ? 'post-operative recovery' : 'current condition'}.

  // ATTENDING PHYSICIAN:
  Dr. ${currentUser?.firstName} ${currentUser?.lastName}
  ${currentUser?.departmentName || 'Medical Staff'}
      `.trim()
    }
  }

  const renderVitalChart = (vitals: VitalSign[], vitalType: string) => {
    if (!vitals || vitals.length === 0) {
      return (
        <div className="h-64 bg-dark-900/50 rounded-2xl flex items-center justify-center border border-white/5">
          <p className="text-dark-400 text-sm">No vital data available</p>
        </div>
      )
    }

    const data = vitals.map(vital => ({
      time: new Date(vital.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      value: vitalType === 'heartRate' ? vital.heartRate :
             vitalType === 'temperature' ? vital.temperature :
             vital.oxygenSaturation
    }))

    const getChartColor = () => {
      switch(vitalType) {
        case 'heartRate': return '#ef4444' // Red
        case 'temperature': return '#fbbf24' // Orange
        case 'oxygenSaturation': return '#10b981' // Green
        default: return '#38bdf8'
      }
    }

    const getUnit = () => {
      switch(vitalType) {
        case 'heartRate': return 'bpm'
        case 'temperature': return '°F'
        case 'oxygenSaturation': return '%'
        default: return ''
      }
    }

    const color = getChartColor();

    return (
      <div className="h-64 bg-dark-900/30 rounded-2xl p-4 border border-white/5">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
            <defs>
              <linearGradient id={`gradient-${vitalType}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor={color} stopOpacity={0.2}/>
                <stop offset="95%" stopColor={color} stopOpacity={0}/>
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
            <XAxis 
              dataKey="time" 
              stroke="#64748b"
              fontSize={11}
              tickLine={false}
            />
            <YAxis 
              stroke="#64748b"
              fontSize={11}
              tickLine={false}
              axisLine={false}
              domain={['auto', 'auto']}
            />
            <Tooltip 
              contentStyle={{ 
                backgroundColor: '#0f172a', 
                border: '1px solid rgba(255,255,255,0.1)',
                borderRadius: '12px',
                color: '#fff',
                fontSize: '12px'
              }}
              formatter={(value: number) => [`${value} ${getUnit()}`, vitalType === 'heartRate' ? 'Heart Rate' : vitalType === 'temperature' ? 'Temperature' : 'SpO2']}
            />
            <Area 
              type="monotone" 
              dataKey="value" 
              stroke={color}
              strokeWidth={2}
              fillOpacity={1} 
              fill={`url(#gradient-${vitalType})`}
              dot={{ fill: color, strokeWidth: 1, r: 3 }}
              activeDot={{ r: 5 }}
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    )
  }

  const renderBloodPressureChart = (vitals: VitalSign[]) => {
    if (!vitals || vitals.length === 0) {
      return (
        <div className="h-64 bg-dark-900/50 rounded-2xl flex items-center justify-center border border-white/5">
          <p className="text-dark-400 text-sm">No blood pressure data available</p>
        </div>
      )
    }

    const data = vitals.map(vital => ({
      time: new Date(vital.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      systolic: vital.bloodPressure.systolic,
      diastolic: vital.bloodPressure.diastolic
    }))

    return (
      <div className="h-64 bg-dark-900/30 rounded-2xl p-4 border border-white/5">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
            <XAxis 
              dataKey="time" 
              stroke="#64748b"
              fontSize={11}
              tickLine={false}
            />
            <YAxis 
              stroke="#64748b"
              fontSize={11}
              tickLine={false}
              axisLine={false}
            />
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
            <Line 
              type="monotone" 
              dataKey="systolic" 
              stroke="#38bdf8"
              strokeWidth={2}
              name="Systolic (mmHg)"
              dot={{ fill: '#38bdf8', strokeWidth: 1, r: 3 }}
              activeDot={{ r: 5 }}
            />
            <Line 
              type="monotone" 
              dataKey="diastolic" 
              stroke="#06b6d4"
              strokeWidth={2}
              name="Diastolic (mmHg)"
              dot={{ fill: '#06b6d4', strokeWidth: 1, r: 3 }}
              activeDot={{ r: 5 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </div>
    )
  }

  const handleCall = (phoneNumber: string) => {
    window.open(`tel:${phoneNumber}`, '_self')
  }

  if (!currentUser) {
    return (
      <div className="min-h-screen bg-dark-950 flex items-center justify-center">
        <div className="glass-panel p-8 text-center max-w-sm">
          <ShieldAlert className="mx-auto text-error-400 mb-4" size={48} />
          <h2 className="text-xl font-bold text-white mb-2">Access Restrained</h2>
          <p className="text-dark-300 text-sm mb-4">Please log in first to view the clinical command console.</p>
          <button onClick={() => window.location.href = '/login'} className="btn-primary w-full">Go to Sign In</button>
        </div>
      </div>
    )
  }

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
                <p className="text-[10px] text-dark-400 uppercase tracking-widest">Attending Physician Panel</p>
              </div>
            </div>
                    <div className="flex items-center space-x-4">
              {/* Notification Bell */}
              <div className="relative" ref={notifRef}>
                <button onClick={() => setShowNotifPanel(v => !v)}
                  className="relative w-11 h-11 rounded-full bg-dark-800/80 border border-white/10 flex items-center justify-center text-dark-300 hover:text-white hover:bg-dark-700 transition-colors">
                  <Bell size={18} />
                  {unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 w-5 h-5 rounded-full bg-error-500 text-white text-xs flex items-center justify-center font-bold">
                      {unreadCount}
                    </span>
                  )}
                </button>

                <AnimatePresence>
                  {showNotifPanel && (
                    <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: 8 }}
                      className="absolute right-0 top-14 w-80 rounded-2xl border border-white/10 bg-dark-900/95 backdrop-blur-xl shadow-2xl z-50 p-4">
                      <div className="flex items-center justify-between mb-3">
                        <h3 className="font-bold text-white">Notifications</h3>
                        <div className="flex gap-2">
                          {unreadCount > 0 && (
                            <button onClick={markAllRead} className="text-xs text-primary-300 hover:text-primary-200">Mark all read</button>
                          )}
                          <button onClick={() => setShowNotifPanel(false)} className="text-dark-400 hover:text-white"><X size={14} /></button>
                        </div>
                      </div>
                      <div className="space-y-2 max-h-72 overflow-y-auto">
                        {notifications.length === 0 && (
                          <p className="text-dark-400 text-sm text-center py-4">No notifications</p>
                        )}
                        {notifications.map(n => (
                          <div key={n.id} onClick={() => markNotifRead(n.id)}
                            className={`rounded-xl p-3 cursor-pointer transition-colors ${n.read ? 'bg-dark-800/50' : 'bg-primary-500/10 border border-primary-500/20'}`}>
                            <div className="flex items-start justify-between gap-2">
                              <span className="font-semibold text-sm text-white">{n.title}</span>
                              {!n.read && <span className="w-2 h-2 rounded-full bg-primary-400 mt-1 shrink-0" />}
                            </div>
                            <p className="text-xs text-dark-300 mt-1">{n.detail}</p>
                            <p className="text-xs text-dark-500 mt-1">{new Date(n.timestamp).toLocaleString()}</p>
                          </div>
                        ))}
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </div>

              <div className="flex items-center space-x-3 bg-white/5 border border-white/5 py-1.5 px-3 rounded-full">
                <Award size={16} className="text-primary-400" />
                <span className="text-sm font-medium text-dark-200">
                  Dr. {currentUser.firstName} {currentUser.lastName}
                </span>
                <span className="text-[10px] bg-primary-500/20 text-primary-300 px-2 py-0.5 rounded-full font-semibold">
                  {currentUser.departmentName || 'Staff'}
                </span>
              </div>
              <button 
                onClick={() => AuthService.logout()}
                className="p-2.5 bg-error-500/10 hover:bg-error-500/20 text-error-400 hover:text-error-300 rounded-xl border border-error-500/20 transition-all flex items-center gap-2 text-sm font-semibold"
              >
                <LogOut size={16} />
                Logout
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Panel */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full flex-1 relative z-10 space-y-6">
        
        {/* Patient Selection list */}
        <div className="glass-panel p-6">
          <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2">
            <User size={18} className="text-primary-400" /> Active Roster ({patients.length} Patients)
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {patients.map(patient => {
              const isSelected = selectedPatient?.id === patient.id;
              const condColor = patient.condition === 'CRITICAL' ? 'text-red-400 bg-red-400/10 border-red-500/20' :
                                patient.condition === 'EMERGENCY' ? 'text-orange-400 bg-orange-400/10 border-orange-500/20' :
                                'text-green-400 bg-green-400/10 border-green-500/20';
              return (
                <div
                  key={patient.id}
                  onClick={() => setSelectedPatient(patient)}
                  className={`p-4 rounded-xl border cursor-pointer transition-all duration-300 relative group overflow-hidden ${
                    isSelected 
                      ? 'border-primary-500/40 bg-primary-500/10 shadow-glow' 
                      : 'border-white/5 bg-dark-900/30 hover:border-white/10 hover:bg-dark-900/50'
                  }`}
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <h3 className="font-semibold text-white group-hover:text-primary-300 transition-colors">{patient.name}</h3>
                      <p className="text-xs text-dark-400 mt-0.5">{patient.patientIdentifier} • Room {patient.roomNumber}</p>
                      <span className={`inline-block text-[10px] px-2 py-0.5 rounded-full font-bold border mt-2 ${condColor}`}>
                        {patient.condition}
                      </span>
                    </div>
                    <button
                      onClick={(e) => {
                        e.stopPropagation()
                        handleCall(patient.mobileNumber || '1234567890')
                      }}
                      className="p-2.5 bg-success-500/10 hover:bg-success-500/20 text-success-400 rounded-xl border border-success-500/20 hover:scale-105 transition-all"
                    >
                      <Phone size={14} />
                    </button>
                  </div>
                </div>
              )
            })}
          </div>
        </div>

        {selectedPatient && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Patient Overview */}
            <div className="lg:col-span-2 glass-panel p-6 flex flex-col justify-between">
              <div>
                <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2 border-b border-white/5 pb-2">
                  <User size={18} className="text-primary-400" /> Patient Medical File
                </h2>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-6 text-sm">
                  <div>
                    <span className="text-xs text-dark-400 uppercase tracking-widest block">Full Name</span>
                    <span className="font-semibold text-white mt-1 block">{selectedPatient.name}</span>
                  </div>
                  <div>
                    <span className="text-xs text-dark-400 uppercase tracking-widest block">Age / Gender</span>
                    <span className="font-semibold text-white mt-1 block">{selectedPatient.age} Yrs / {selectedPatient.gender}</span>
                  </div>
                  <div>
                    <span className="text-xs text-dark-400 uppercase tracking-widest block">Room & Bed</span>
                    <span className="font-semibold text-white mt-1 block">{selectedPatient.roomNumber}</span>
                  </div>
                  <div>
                    <span className="text-xs text-dark-400 uppercase tracking-widest block">Clinical Condition</span>
                    <span className="font-semibold text-white mt-1 block">
                      <span className={`inline-block text-xs font-bold ${
                        selectedPatient.condition === 'CRITICAL' ? 'text-red-400' :
                        selectedPatient.condition === 'EMERGENCY' ? 'text-orange-400' : 'text-green-400'
                      }`}>
                        {selectedPatient.condition}
                      </span>
                    </span>
                  </div>
                  <div>
                    <span className="text-xs text-dark-400 uppercase tracking-widest block">Admission Date</span>
                    <span className="font-semibold text-white mt-1 block">{selectedPatient.admittedDate}</span>
                  </div>
                  <div>
                    <span className="text-xs text-dark-400 uppercase tracking-widest block">Attending Physician</span>
                    <span className="font-semibold text-white mt-1 block">Dr. {currentUser.lastName}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Quick Actions */}
            <div className="glass-panel p-6">
              <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2 border-b border-white/5 pb-2">
                <FileText size={18} className="text-primary-400" /> Operations
              </h2>
              <div className="space-y-3">
                <button
                  onClick={() => handleGenerateReport('shift')}
                  className="btn-primary w-full py-3 text-sm flex items-center justify-center gap-2"
                >
                  <FileText size={16} />
                  Generate Shift Report
                </button>
                <button
                  onClick={() => handleGenerateReport('medical')}
                  className="w-full bg-secondary-500 hover:bg-secondary-600 text-white font-semibold py-3 px-6 rounded-xl shadow-glow hover:shadow-glow-lg transition-all duration-300 transform hover:-translate-y-0.5 flex items-center justify-center gap-2 text-sm"
                >
                  <Award size={16} />
                  Generate Medical Report
                </button>
                <button 
                  onClick={() => setShowLabModal(true)}
                  className="btn-secondary w-full py-3 text-sm flex items-center justify-center gap-2"
                >
                  <Activity size={16} />
                  View Laboratory Analytics
                </button>
              </div>
            </div>

            {/* Vital Signs Charts */}
            <div className="lg:col-span-3 glass-panel p-6 space-y-6">
              <h2 className="text-lg font-bold text-white flex items-center gap-2 border-b border-white/5 pb-2">
                <Heart className="text-red-400 animate-pulse" size={18} /> Vitals Trends (Live Bedside Data)
              </h2>
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div>
                  <h3 className="text-sm font-semibold text-dark-200 mb-3 flex items-center gap-2">
                    <Heart size={14} className="text-red-400" /> Heart Rate (BPM)
                  </h3>
                  {renderVitalChart(vitalsHistory, 'heartRate')}
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-dark-200 mb-3 flex items-center gap-2">
                    <Activity size={14} className="text-primary-400" /> Blood Pressure Trend (mmHg)
                  </h3>
                  {renderBloodPressureChart(vitalsHistory)}
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-dark-200 mb-3 flex items-center gap-2">
                    <Thermometer size={14} className="text-warning-400" /> Temperature (°F)
                  </h3>
                  {renderVitalChart(vitalsHistory, 'temperature')}
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-dark-200 mb-3 flex items-center gap-2">
                    <Flame size={14} className="text-success-400" /> Oxygen Saturation (SpO2 %)
                  </h3>
                  {renderVitalChart(vitalsHistory, 'oxygenSaturation')}
                </div>
              </div>
            </div>

            {/* Patient Alerts Section */}
            <div className="lg:col-span-3 glass-panel p-6">
              <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2 border-b border-white/5 pb-2">
                <ShieldAlert size={18} className="text-warning-400" /> Patient Alerts
              </h2>
              {mockAlerts.length === 0 ? (
                <p className="text-dark-400 text-sm py-4">No alerts for this patient.</p>
              ) : (
                <div className="space-y-4">
                  {mockAlerts.map(alert => (
                    <div key={alert.id} className="p-4 rounded-xl bg-white/5 border border-white/5 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                      <div>
                        <div className="flex items-center gap-2 mb-2">
                          <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold border ${
                            alert.priority === 'CRITICAL' ? 'text-red-400 bg-red-400/10 border-red-500/20' :
                            alert.priority === 'HIGH' ? 'text-orange-400 bg-orange-400/10 border-orange-500/20' :
                            'text-yellow-400 bg-yellow-400/10 border-yellow-500/20'
                          }`}>
                            {alert.priority}
                          </span>
                          <span className={`text-[10px] px-2 py-0.5 rounded-full font-bold border ${
                            alert.status === 'RESOLVED' ? 'text-green-400 bg-green-400/10 border-green-500/20' :
                            alert.status === 'IN_PROGRESS' ? 'text-blue-400 bg-blue-400/10 border-blue-500/20' :
                            alert.status === 'ACKNOWLEDGED' ? 'text-purple-400 bg-purple-400/10 border-purple-500/20' :
                            'text-red-400 bg-red-400/10 border-red-500/20'
                          }`}>
                            {alert.status}
                          </span>
                        </div>
                        <p className="text-sm font-semibold text-white">{alert.message}</p>
                        <p className="text-xs text-dark-400 mt-1">Logged: {new Date(alert.createdAt).toLocaleString()}</p>
                      </div>
                      
                      {alert.status !== 'RESOLVED' && (
                        <div className="flex flex-wrap gap-2">
                          {alert.status === 'ACTIVE' && (
                            <button
                              onClick={() => handleUpdateMockAlertStatus(alert.id, 'ACKNOWLEDGED')}
                              className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-purple-500/30 bg-purple-500/10 hover:bg-purple-500/20 text-purple-300 transition-all"
                            >
                              Acknowledge
                            </button>
                          )}
                          {(alert.status === 'ACTIVE' || alert.status === 'ACKNOWLEDGED') && (
                            <button
                              onClick={() => handleUpdateMockAlertStatus(alert.id, 'IN_PROGRESS')}
                              className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-blue-500/30 bg-blue-500/10 hover:bg-blue-500/20 text-blue-300 transition-all"
                            >
                              Track Action
                            </button>
                          )}
                          <button
                            onClick={() => handleUpdateMockAlertStatus(alert.id, 'RESOLVED')}
                            className="px-3 py-1.5 text-xs font-semibold rounded-lg border border-green-500/30 bg-green-500/10 hover:bg-green-500/20 text-green-300 transition-all"
                          >
                            Resolve
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Recent Vitals Table */}
            <div className="lg:col-span-3 glass-panel p-6">
              <h2 className="text-lg font-bold text-white mb-4 flex items-center gap-2 border-b border-white/5 pb-2">
                <Activity size={18} className="text-primary-400" /> Recent Vitals Records
              </h2>
              <div className="overflow-x-auto rounded-xl border border-white/5">
                <table className="min-w-full divide-y divide-white/5 text-left text-sm">
                  <thead className="bg-white/5 text-xs text-dark-300 uppercase tracking-widest">
                    <tr>
                      <th className="px-6 py-4">Timestamp</th>
                      <th className="px-6 py-4">Heart Rate</th>
                      <th className="px-6 py-4">Blood Pressure</th>
                      <th className="px-6 py-4">Temperature</th>
                      <th className="px-6 py-4">O2 Saturation</th>
                      <th className="px-6 py-4">Clinician</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 bg-dark-900/10 text-dark-200">
                    {vitalsHistory.slice().reverse().map(vital => (
                      <tr key={vital.id} className="hover:bg-white/5 transition-all">
                        <td className="px-6 py-4 font-mono text-xs">
                          {new Date(vital.timestamp).toLocaleString()}
                        </td>
                        <td className="px-6 py-4 font-semibold text-red-400">
                          {vital.heartRate} bpm
                        </td>
                        <td className="px-6 py-4 font-semibold text-primary-400">
                          {vital.bloodPressure.systolic}/{vital.bloodPressure.diastolic} mmHg
                        </td>
                        <td className="px-6 py-4 font-semibold text-warning-400">
                          {vital.temperature}°F
                        </td>
                        <td className="px-6 py-4 font-semibold text-success-400">
                          {vital.oxygenSaturation}%
                        </td>
                        <td className="px-6 py-4 text-xs">
                          {vital.recordedBy === 'nurse-sarah' ? 'Nurse Sarah Johnson' : 
                           vital.recordedBy === 'nurse-emily' ? 'Nurse Emily Davis' :
                           vital.recordedBy === 'nurse-jessica' ? 'Nurse Jessica Brown' :
                           vital.recordedBy === 'nurse-monalisa' ? 'Nurse Monalisa Khan' :
                           vital.recordedBy === 'nurse-lana' ? 'Nurse Lana Wilson' :
                           vital.recordedBy}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* Report Modal */}
        <AnimatePresence>
          {showReportModal && (
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
                className="bg-dark-900 border border-white/10 rounded-2xl p-6 max-w-2xl w-full flex flex-col max-h-[85vh] shadow-2xl relative"
              >
                <div className="flex justify-between items-center border-b border-white/5 pb-3 mb-4">
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <FileText className="text-primary-400" /> Clinical Handover Document
                  </h2>
                  <button
                    onClick={() => setShowReportModal(false)}
                    className="text-dark-400 hover:text-white bg-white/5 hover:bg-white/10 p-1.5 rounded-lg transition-all"
                  >
                    ✕
                  </button>
                </div>
                <div className="flex-1 overflow-y-auto bg-dark-950 rounded-xl p-4 border border-white/5 font-mono text-xs leading-relaxed text-dark-100 max-h-[50vh]">
                  <pre className="whitespace-pre-wrap">{reportContent}</pre>
                </div>
                <div className="mt-5 flex justify-end space-x-3 border-t border-white/5 pt-4">
                  <button
                    onClick={() => {
                      navigator.clipboard.writeText(reportContent)
                      alert('Report copied to clipboard!')
                    }}
                    className="px-4 py-2 bg-dark-800 hover:bg-dark-700 text-white rounded-xl border border-white/5 text-sm font-semibold flex items-center gap-2 transition-all"
                  >
                    <Copy size={16} />
                    Copy to Clipboard
                  </button>
                  <button
                    onClick={() => {
                      const blob = new Blob([reportContent], { type: 'text/plain' })
                      const url = window.URL.createObjectURL(blob)
                      const a = document.createElement('a')
                      a.href = url
                      a.download = `report-${selectedPatient?.name.replace(/\s+/g, '_')}-${Date.now()}.txt`
                      a.click()
                      window.URL.revokeObjectURL(url)
                    }}
                    className="px-4 py-2 bg-primary-500 hover:bg-primary-600 text-white rounded-xl text-sm font-semibold flex items-center gap-2 shadow-glow transition-all"
                  >
                    <Download size={16} />
                    Download File
                  </button>
                </div>
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Laboratory Analytics Modal */}
        <AnimatePresence>
          {showLabModal && (
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
                className="bg-dark-900 border border-white/10 rounded-2xl p-6 max-w-3xl w-full flex flex-col max-h-[85vh] shadow-2xl relative"
              >
                <div className="flex justify-between items-center border-b border-white/5 pb-3 mb-4">
                  <h2 className="text-lg font-bold text-white flex items-center gap-2">
                    <FlaskConical className="text-primary-400" /> Laboratory Analytics - {currentUser?.departmentName || 'Cardiology'}
                  </h2>
                  <button
                    onClick={() => setShowLabModal(false)}
                    className="text-dark-400 hover:text-white bg-white/5 hover:bg-white/10 p-1.5 rounded-lg transition-all"
                  >
                    ✕
                  </button>
                </div>
                <div className="flex-1 overflow-y-auto space-y-6 pr-1">
                  {/* Summary Cards */}
                  <div className="grid grid-cols-3 gap-4">
                    <div className="bg-white/5 border border-white/5 rounded-xl p-4">
                      <p className="text-[10px] uppercase tracking-wider text-dark-400">Total Tests</p>
                      <p className="text-xl font-bold text-white mt-1">
                        {(LAB_ANALYTICS[currentUser?.departmentName || 'Cardiology'] || LAB_ANALYTICS['Cardiology']).reduce((acc, curr) => acc + curr.normal + curr.abnormal + curr.pending, 0)}
                      </p>
                    </div>
                    <div className="bg-error-500/10 border border-error-500/20 rounded-xl p-4">
                      <p className="text-[10px] uppercase tracking-wider text-error-400">Abnormal</p>
                      <p className="text-xl font-bold text-error-400 mt-1">
                        {(LAB_ANALYTICS[currentUser?.departmentName || 'Cardiology'] || LAB_ANALYTICS['Cardiology']).reduce((acc, curr) => acc + curr.abnormal, 0)}
                      </p>
                    </div>
                    <div className="bg-warning-500/10 border border-warning-500/20 rounded-xl p-4">
                      <p className="text-[10px] uppercase tracking-wider text-warning-400">Pending</p>
                      <p className="text-xl font-bold text-warning-400 mt-1">
                        {(LAB_ANALYTICS[currentUser?.departmentName || 'Cardiology'] || LAB_ANALYTICS['Cardiology']).reduce((acc, curr) => acc + curr.pending, 0)}
                      </p>
                    </div>
                  </div>

                  {/* AreaChart comparing normal vs abnormal values */}
                  <div className="h-64 w-full bg-dark-950/50 p-4 rounded-xl border border-white/5">
                    <ResponsiveContainer width="100%" height="100%">
                      <AreaChart
                        data={LAB_ANALYTICS[currentUser?.departmentName || 'Cardiology'] || LAB_ANALYTICS['Cardiology']}
                        margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                      >
                        <defs>
                          <linearGradient id="colorNormal" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#10b981" stopOpacity={0.2}/>
                            <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                          </linearGradient>
                          <linearGradient id="colorAbnormal" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="#ef4444" stopOpacity={0.2}/>
                            <stop offset="95%" stopColor="#ef4444" stopOpacity={0}/>
                          </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="rgba(255, 255, 255, 0.05)" />
                        <XAxis dataKey="test" stroke="#94a3b8" fontSize={10} tickLine={false} />
                        <YAxis stroke="#94a3b8" fontSize={10} tickLine={false} />
                        <Tooltip 
                          contentStyle={{ backgroundColor: '#0f172a', borderColor: 'rgba(255,255,255,0.1)', color: '#fff' }} 
                        />
                        <Legend wrapperStyle={{ fontSize: 11 }} />
                        <Area type="monotone" dataKey="normal" stroke="#10b981" fillOpacity={1} fill="url(#colorNormal)" name="Normal Results" />
                        <Area type="monotone" dataKey="abnormal" stroke="#ef4444" fillOpacity={1} fill="url(#colorAbnormal)" name="Abnormal Results" />
                      </AreaChart>
                    </ResponsiveContainer>
                  </div>

                  {/* Progress bars of abnormal rates */}
                  <div className="space-y-4">
                    <h3 className="text-sm font-semibold text-white">Abnormal Rate Breakdown</h3>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {(LAB_ANALYTICS[currentUser?.departmentName || 'Cardiology'] || LAB_ANALYTICS['Cardiology']).map(item => {
                        const total = item.normal + item.abnormal + item.pending;
                        const rate = total > 0 ? Math.round((item.abnormal / total) * 100) : 0;
                        return (
                          <div key={item.test} className="bg-white/5 border border-white/5 rounded-xl p-4 space-y-2">
                            <div className="flex justify-between items-center text-xs font-semibold">
                              <span className="text-white">{item.test}</span>
                              <span className={rate > 30 ? 'text-red-400' : 'text-dark-300'}>{rate}% Abnormal</span>
                            </div>
                            <div className="w-full bg-dark-950 h-2 rounded-full overflow-hidden">
                              <div 
                                className={`h-full rounded-full transition-all duration-500 ${
                                  rate > 40 ? 'bg-gradient-to-r from-red-500 to-orange-500' : 
                                  rate > 20 ? 'bg-gradient-to-r from-yellow-500 to-orange-400' : 
                                  'bg-gradient-to-r from-emerald-500 to-teal-400'
                                }`} 
                                style={{ width: `${rate}%` }}
                              />
                            </div>
                            <div className="flex justify-between text-[10px] text-dark-400">
                              <span>Normal: {item.normal}</span>
                              <span>Abnormal: {item.abnormal}</span>
                              <span>Pending: {item.pending}</span>
                            </div>
                          </div>
                        )
                      })}
                    </div>
                  </div>
                </div>
                <div className="mt-5 flex justify-end border-t border-white/5 pt-4">
                  <button
                    onClick={() => setShowLabModal(false)}
                    className="px-5 py-2.5 bg-dark-800 hover:bg-dark-700 text-white rounded-xl border border-white/5 text-sm font-semibold transition-all"
                  >
                    Close
                  </button>
                </div>
              </motion.div>
            </motion.div>
          )}
        </AnimatePresence>
      </main>
    </div>
  )
}
