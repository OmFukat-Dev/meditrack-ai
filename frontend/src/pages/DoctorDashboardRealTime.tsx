import { useState, useEffect, useMemo, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Activity, ArrowRight, Bell, Download, FileText, HeartPulse,
  ShieldAlert, Users, X, FlaskConical, TrendingUp, Thermometer,
  Wind, Droplets, CheckCircle2,
} from 'lucide-react'
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer,
} from 'recharts'
import { useAuth } from '../context/AuthContext'
import { patientApi, reportApi, vitalsApi, alertApi } from '../services/api'
import WebSocketService from '../services/websocketService'
import { mockPatients, mockVitalSigns, mockUsers } from '../database/mockDatabaseFromSeed'

// ─── Types ────────────────────────────────────────────────────────────────────
type TabKey = 'patients' | 'vitals' | 'predictions' | 'alerts' | 'timeline' | 'lab'
type NotificationItem = { id: string; title: string; detail: string; timestamp: string; read: boolean }

// ─── Helpers ─────────────────────────────────────────────────────────────────
function extractCollection<T>(value: unknown): T[] {
  if (Array.isArray(value)) return value as T[]
  if (value && typeof value === 'object' && Array.isArray((value as any).content))
    return (value as any).content as T[]
  return []
}

function getConditionColor(c: string) {
  if (c === 'STABLE') return 'bg-green-500/20 text-green-300 border-green-500/30'
  if (c === 'CRITICAL') return 'bg-red-500/20 text-red-300 border-red-500/30'
  if (c === 'EMERGENCY') return 'bg-orange-500/20 text-orange-300 border-orange-500/30'
  return 'bg-gray-500/20 text-gray-300 border-gray-500/30'
}

function getRiskColor(r: string) {
  if (r === 'LOW') return 'bg-green-500/20 text-green-300'
  if (r === 'MEDIUM') return 'bg-yellow-500/20 text-yellow-300'
  if (r === 'HIGH') return 'bg-orange-500/20 text-orange-300'
  return 'bg-red-500/20 text-red-300'
}

function buildVitalTrendData(patientId: string) {
  const vitals = mockVitalSigns.filter(v => v.patientId === patientId)
    .sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
  if (vitals.length === 0) return []
  return vitals.map(v => ({
    time: new Date(v.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    hr: v.heartRate,
    systolic: v.bloodPressure.systolic,
    diastolic: v.bloodPressure.diastolic,
    temp: v.temperature,
    spo2: v.oxygenSaturation,
  }))
}

function buildVitalTrendFromVitals(vitals: any[]) {
  if (!vitals || vitals.length === 0) return []
  const sorted = [...vitals].sort((a, b) => new Date(a.readingTimestamp || a.timestamp || 0).getTime() - new Date(b.readingTimestamp || b.timestamp || 0).getTime())
  return sorted.map(v => ({
    time: new Date(v.readingTimestamp || v.timestamp || Date.now()).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    hr: Number(v.heartRate ?? v.value ?? 0),
    systolic: Number(v.systolic ?? v.bloodPressure?.systolic ?? 0),
    diastolic: Number(v.diastolic ?? v.bloodPressure?.diastolic ?? 0),
    temp: Number(v.temperature ?? v.temp ?? 0),
    spo2: Number(v.oxygenSaturation ?? v.spo2 ?? 0),
  }))
}

// Seed notifications that simulate admin broadcasts
const SEED_NOTIFICATIONS: NotificationItem[] = [
  { id: 'notif-1', title: 'System Alert', detail: 'Scheduled maintenance tonight 11 PM–1 AM. Save all work.', timestamp: new Date(Date.now() - 60 * 60 * 1000).toISOString(), read: false },
  { id: 'notif-2', title: 'Critical Protocol Update', detail: 'New triage protocol effective from today. Check Cardiology bulletin.', timestamp: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(), read: false },
  { id: 'notif-3', title: 'Staff Meeting', detail: 'Department heads meeting tomorrow at 9 AM in Conference Room B.', timestamp: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(), read: true },
]

// Lab analytics data keyed by department
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


// ─── Main Component ───────────────────────────────────────────────────────────
export default function DoctorDashboardRealTime() {
  const { user, logout } = useAuth()
  const [assignedPatients, setAssignedPatients] = useState<any[]>([])
  const [selectedPatient, setSelectedPatient] = useState<any>(null)
  const [patientVitals, setPatientVitals] = useState<any[]>([])
  const [patientAlerts, setPatientAlerts] = useState<any[]>([])
  const [patientPredictions, setPatientPredictions] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState<TabKey>('patients')
  const [realTimeUpdates, setRealTimeUpdates] = useState<any[]>([])
  const [wsConnected, setWsConnected] = useState(false)
  const [isDownloadingReport, setIsDownloadingReport] = useState(false)

  // Notifications state (persisted by admin)
  const [notifications, setNotifications] = useState<NotificationItem[]>(() => {
    try {
      const stored = localStorage.getItem('app_notifications')
      return stored ? JSON.parse(stored) as NotificationItem[] : SEED_NOTIFICATIONS
    } catch { return SEED_NOTIFICATIONS }
  })
  const [showNotifPanel, setShowNotifPanel] = useState(false)
  const notifRef = useRef<HTMLDivElement>(null)
  const unreadCount = notifications.filter(n => !n.read).length

  const wsService = WebSocketService.getInstance()

  // Close notif panel when clicking outside
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) {
        setShowNotifPanel(false)
      }
    }
    if (showNotifPanel) document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [showNotifPanel])

  useEffect(() => {
    if (user) {
      loadAssignedPatients()
      connectWebSocket()
    }
    return () => { wsService.disconnect(); setWsConnected(false) }
  }, [user])

  useEffect(() => {
    if (!selectedPatient || !wsConnected) return
    const dept = selectedPatient.department || user?.department || 'General'
    wsService.subscribeToDoctorVitals(dept, (d: any) => {
      if (String(d.patientId) !== String(selectedPatient.id)) return
      setPatientVitals(prev => [d, ...prev.slice(0, 49)])
      addRTU('vital', d)
    })
    wsService.subscribeToDoctorPredictions(dept, (d: any) => {
      if (String(d.patientId) !== String(selectedPatient.id)) return
      setPatientPredictions(prev => [d, ...prev.slice(0, 49)])
      addRTU('prediction', d)
    })
    wsService.subscribeToDoctorAlerts(dept, (d: any) => {
      if (String(d.patientId) !== String(selectedPatient.id)) return
      setPatientAlerts(prev => [d, ...prev.slice(0, 49)])
      addRTU('alert', d)
    })
  }, [selectedPatient, wsConnected])

  async function connectWebSocket() {
    try { await wsService.connect(); setWsConnected(true) }
    catch { /* offline mode */ }
  }

  function addRTU(type: string, data: any) {
    setRealTimeUpdates(prev => [
      { type, data, timestamp: new Date(), id: Math.random().toString(36).slice(2) },
      ...prev.slice(0, 19),
    ])
  }

  async function loadAssignedPatients() {
    setIsLoading(true); setError('')
    try {
      const res = await patientApi.getAll(0, 100)
      const remote = extractCollection<any>(res.data).map((p: any) => ({
        id: p.id,
        patientIdentifier: p.patientIdentifier,
        firstName: p.firstName,
        lastName: p.lastName,
        condition: p.clinicalStatus || p.condition || 'STABLE',
        roomNumber: p.wardNumber || p.roomNumber || 'WARD',
        bedNumber: p.bedNumber || 'N/A',
        admissionDate: p.createdAt || p.updatedAt || new Date().toISOString(),
        department: p.department || 'General',
      }))
      if (remote.length > 0) {
        setAssignedPatients(remote)
        setSelectedPatient(remote[0])
        return
      }
    } catch { /* fall through to mock */ }
    // Fallback: load from mock data filtered by doctor
    const doctorId = user?.id || ''
    const mapped = mockPatients
      .filter(p => p.doctorId === doctorId || doctorId === '')
      .map(p => {
        const nurse = mockUsers.find(u => u.id === p.nurseId)
        return {
          id: p.id,
          patientIdentifier: p.patientIdentifier,
          firstName: p.name.split(' ')[0],
          lastName: p.name.split(' ').slice(1).join(' '),
          fullName: p.name,
          condition: p.condition,
          roomNumber: p.roomNumber,
          bedNumber: 'N/A',
          admissionDate: p.admittedDate,
          department: user?.department || 'General',
          nurseId: p.nurseId,
          nurseName: nurse?.name || 'Assigned Nurse',
        }
      })
    setAssignedPatients(mapped)
    if (mapped.length > 0) setSelectedPatient(mapped[0])
    setIsLoading(false)
  }

  async function handlePatientSelect(patient: any) {
    setSelectedPatient(patient)
    setActiveTab('vitals')
    setPatientVitals([]); setPatientAlerts([]); setPatientPredictions([])
    // Load real vitals
    try {
      const res = await vitalsApi.getPatientReadings(patient.id, { page: 0, size: 50 })
      const remote = extractCollection<any>(res.data)
      if (remote.length > 0) { setPatientVitals(remote); return }
    } catch { /* fall through */ }
    // Fallback: mock vitals
    const mv = mockVitalSigns.filter(v => v.patientId === patient.id)
      .map(v => ({
        ...v, vitalType: 'MULTI', value: v.heartRate,
        readingTimestamp: v.timestamp,
        systolic: v.bloodPressure.systolic,
        diastolic: v.bloodPressure.diastolic,
        unit: 'bpm',
      }))
    setPatientVitals(mv)
    // Load alerts
    try {
      const ar = await alertApi.getByPatient(patient.id)
      setPatientAlerts(extractCollection<any>(ar.data))
    } catch { setPatientAlerts([]) }
  }

  async function handleResolveAlert(alertId: string) {
    try {
      await alertApi.updateStatus(alertId, 'RESOLVED')
    } catch { /* ignore backend errors, still remove locally */ }
    setPatientAlerts(prev => prev.filter(a => a.id !== alertId))
  }

  async function handleUpdateAlertStatus(alertId: string, status: string) {
    try {
      const res = await alertApi.updateStatus(alertId, status)
      setPatientAlerts(prev => prev.map(a => a.id === alertId ? (res.data ?? { ...a, status }) : a))
    } catch {
      setPatientAlerts(prev => prev.map(a => a.id === alertId ? { ...a, status } : a))
    }
  }

  async function handleDownloadPatientReport(patient: any) {
    if (!patient) return
    setIsDownloadingReport(true)
    try {
      const res = await reportApi.downloadPatientReport(String(patient.id))
      downloadBlob(res.data as Blob, `${patient.patientIdentifier || patient.id}-report.pdf`)
    } catch { setError('Failed to download patient report') }
    finally { setIsDownloadingReport(false) }
  }

  function markNotifRead(id: string) {
    setNotifications(prev => {
      const updated = prev.map(n => n.id === id ? { ...n, read: true } : n)
      try { localStorage.setItem('app_notifications', JSON.stringify(updated)) } catch {}
      return updated
    })
  }
  function markAllRead() {
    setNotifications(prev => {
      const updated = prev.map(n => ({ ...n, read: true }))
      try { localStorage.setItem('app_notifications', JSON.stringify(updated)) } catch {}
      return updated
    })
  }

  // Listen for notifications written by admin in other tabs
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

  const criticalCount = useMemo(
    () => assignedPatients.filter(p => p.condition === 'CRITICAL').length,
    [assignedPatients],
  )

  const timelineEvents = useMemo(() =>
    [...patientVitals, ...patientPredictions, ...patientAlerts].sort(
      (a, b) => new Date(b.timestamp || b.readingTimestamp || 0).getTime() -
                new Date(a.timestamp || a.readingTimestamp || 0).getTime(),
    ), [patientAlerts, patientPredictions, patientVitals])

  const vitalTrendData = useMemo(() =>
    selectedPatient ? (patientVitals && patientVitals.length > 0 ? buildVitalTrendFromVitals(patientVitals) : buildVitalTrendData(selectedPatient.id)) : [],
    [selectedPatient, patientVitals])

  const labData = useMemo(() => {
    const dept = user?.department || selectedPatient?.department || 'Cardiology'
    return LAB_ANALYTICS[dept] ?? LAB_ANALYTICS['Cardiology']
  }, [user, selectedPatient])

  function getName(p: any) {
    const full = [p?.firstName, p?.lastName].filter(Boolean).join(' ').trim()
    return full || p?.fullName || p?.name || p?.patientIdentifier || 'Unknown'
  }

  if (!user) return <div className="min-h-screen flex items-center justify-center"><div className="text-white">Loading...</div></div>


  return (
    <div className="min-h-screen relative overflow-hidden bg-dark-950 text-white"
      style={{ backgroundImage: 'radial-gradient(circle at top left, rgba(56,189,248,0.16), transparent 28%), radial-gradient(circle at top right, rgba(168,85,247,0.16), transparent 28%), linear-gradient(180deg,#020617 0%,#0b1020 52%,#111827 100%)' }}>
      <div className="pointer-events-none absolute inset-x-0 top-0 h-80 bg-gradient-to-b from-primary-500/10 to-transparent blur-3xl" />

      <div className="relative z-10 mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8 space-y-6">

        {/* ── HEADER ─────────────────────────────────────────────────────── */}
        <motion.section initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }}
          className="glass-panel p-6 lg:p-8 border-white/10 shadow-card">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
            <div className="space-y-4">
              <div className="flex items-center gap-4">
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-gradient-primary shadow-glow">
                  <HeartPulse size={26} />
                </div>
                <div>
                  <p className="text-xs uppercase tracking-[0.35em] text-primary-300">Clinical command center</p>
                  <h1 className="text-3xl font-black tracking-tight sm:text-4xl">Doctor Dashboard</h1>
                </div>
              </div>
              <div className="flex flex-wrap items-center gap-3">
                <span className="rounded-full border border-white/10 bg-dark-900/70 px-3 py-1 text-sm text-dark-200">{user?.name || 'Doctor'}</span>
                <span className="rounded-full border border-primary-500/20 bg-primary-500/10 px-3 py-1 text-sm text-primary-200">{user?.department || 'General'}</span>
                <span className={`rounded-full border px-3 py-1 text-sm font-medium ${wsConnected ? 'border-success-500/30 bg-success-500/10 text-success-300' : 'border-error-500/30 bg-error-500/10 text-error-300'}`}>
                  {wsConnected ? 'Live connection' : 'Offline monitoring'}
                </span>
              </div>
            </div>

            {/* Right: notification bell + logout */}
            <div className="flex flex-wrap gap-3 lg:justify-end items-center">
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

              <button onClick={logout} className="rounded-xl bg-error-500 px-5 py-3 font-semibold text-white transition-colors hover:bg-error-600">
                Logout
              </button>
            </div>
          </div>
        </motion.section>

        {/* ── STATS ──────────────────────────────────────────────────────── */}
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
          {[
            { label: 'Assigned Patients', value: assignedPatients.length, icon: <Users size={18} />, cls: 'text-primary-300', bg: 'bg-primary-500/10' },
            { label: 'Critical Cases', value: criticalCount, icon: <ShieldAlert size={18} />, cls: 'text-error-300', bg: 'bg-error-500/10' },
            { label: 'Active Alerts', value: patientAlerts.length, icon: <Bell size={18} />, cls: 'text-warning-300', bg: 'bg-warning-500/10' },
            { label: 'AI Predictions', value: patientPredictions.length, icon: <Activity size={18} />, cls: 'text-secondary-300', bg: 'bg-secondary-500/10' },
          ].map(s => (
            <motion.div key={s.label} whileHover={{ y: -4 }} className="glass-card p-5">
              <div className="flex items-start justify-between gap-4">
                <div>
                  <p className="text-sm text-dark-300">{s.label}</p>
                  <p className="mt-2 text-3xl font-black text-white">{s.value}</p>
                </div>
                <div className={`flex h-11 w-11 items-center justify-center rounded-2xl ${s.bg} ${s.cls}`}>{s.icon}</div>
              </div>
            </motion.div>
          ))}
        </div>

        {/* ── TABS ───────────────────────────────────────────────────────── */}
        <section className="glass-panel p-2 border-white/10">
          <div className="grid grid-cols-3 gap-2 lg:grid-cols-6">
            {([
              { id: 'patients', label: 'My Patients', icon: <Users size={16} /> },
              { id: 'vitals', label: 'Vital Trends', icon: <Activity size={16} /> },
              { id: 'predictions', label: 'AI Predictions', icon: <ShieldAlert size={16} /> },
              { id: 'alerts', label: 'Alerts', icon: <Bell size={16} /> },
              { id: 'timeline', label: 'Timeline', icon: <FileText size={16} /> },
              { id: 'lab', label: 'Lab Analytics', icon: <FlaskConical size={16} /> },
            ] as { id: TabKey; label: string; icon: JSX.Element }[]).map(tab => (
              <button key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                disabled={!selectedPatient && tab.id !== 'patients' && tab.id !== 'lab'}
                className={`flex items-center justify-center gap-2 rounded-xl px-3 py-3 text-sm font-semibold transition-all ${activeTab === tab.id ? 'bg-primary-500 text-white shadow-glow' : 'text-dark-300 hover:bg-dark-800/80 hover:text-white disabled:cursor-not-allowed disabled:opacity-40'}`}>
                {tab.icon}<span className="hidden sm:inline">{tab.label}</span>
              </button>
            ))}
          </div>
        </section>

        {/* Selected Patient Banner */}
        {selectedPatient && (
          <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}
            className="glass-panel overflow-hidden border-white/10">
            <div className="flex flex-col gap-4 p-5 lg:flex-row lg:items-center lg:justify-between">
              <div className="space-y-2">
                <p className="text-xs uppercase tracking-[0.3em] text-dark-400">Selected patient</p>
                <h2 className="text-2xl font-bold text-white">{getName(selectedPatient)}</h2>
                <div className="flex flex-wrap items-center gap-2 text-sm text-dark-300">
                  <span>{selectedPatient.patientIdentifier}</span>
                  <span className="h-1 w-1 rounded-full bg-dark-500" />
                  <span>{selectedPatient.department || 'General'}</span>
                  <span className="h-1 w-1 rounded-full bg-dark-500" />
                  <span>Room {selectedPatient.roomNumber}</span>
                </div>
              </div>
              <button onClick={() => handleDownloadPatientReport(selectedPatient)} disabled={isDownloadingReport}
                className="btn-primary px-5 py-3 disabled:opacity-60 flex items-center gap-2">
                <Download size={16} />
                {isDownloadingReport ? 'Downloading...' : 'Download PDF'}
              </button>
            </div>
          </motion.section>
        )}

        {/* ── CONTENT ────────────────────────────────────────────────────── */}
        <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.08 }}
          className="glass-panel p-6 lg:p-8 border-white/10">
          {error && (
            <div className="mb-5 rounded-2xl border border-error-500/20 bg-error-500/10 px-4 py-3 text-sm text-error-100">{error}</div>
          )}


          {/* PATIENTS TAB */}
          {activeTab === 'patients' && (
            <div className="space-y-5">
              <h2 className="text-2xl font-bold text-white">My Assigned Patients</h2>
              {isLoading ? (
                <p className="text-dark-300">Loading patients…</p>
              ) : assignedPatients.length === 0 ? (
                <p className="text-dark-400">No assigned patients found.</p>
              ) : (
                <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                  {assignedPatients.map(patient => (
                    <motion.button key={patient.id} type="button" whileHover={{ y: -4 }}
                      onClick={() => handlePatientSelect(patient)}
                      className={`rounded-3xl border p-5 text-left transition-all ${selectedPatient?.id === patient.id ? 'border-primary-500/30 bg-primary-500/10 shadow-glow' : 'border-white/10 bg-dark-900/70 hover:border-white/20'}`}>
                      <div className="mb-4 flex items-start justify-between gap-3">
                        <div>
                          <h3 className="text-lg font-semibold text-white">{getName(patient)}</h3>
                          <p className="text-sm text-dark-400">{patient.patientIdentifier}</p>
                        </div>
                        <span className={`rounded-full border px-3 py-1 text-xs font-semibold ${getConditionColor(patient.condition)}`}>{patient.condition}</span>
                      </div>
                      <div className="space-y-2 text-sm text-dark-300">
                        <div className="flex justify-between"><span>Department</span><span className="text-white">{patient.department || 'General'}</span></div>
                        <div className="flex justify-between"><span>Room</span><span className="text-white">{patient.roomNumber}</span></div>
                        <div className="flex justify-between"><span>Admitted</span><span className="text-white">{new Date(patient.admissionDate).toLocaleDateString()}</span></div>
                      </div>
                      <div className="mt-4 flex items-center justify-between border-t border-white/5 pt-3">
                        <span className="text-sm text-primary-300">Open patient</span>
                        <ArrowRight size={16} className="text-primary-300" />
                      </div>
                    </motion.button>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* VITAL TRENDS TAB */}
          {activeTab === 'vitals' && selectedPatient && (
            <div className="space-y-6">
              <div>
                <h2 className="text-2xl font-bold text-white">Vital Trends — {getName(selectedPatient)}</h2>
                <p className="text-sm text-dark-300 mt-1">Historical bedside readings rendered as trend charts.</p>
              </div>

              {vitalTrendData.length === 0 ? (
                <p className="text-dark-400">No vital data available for this patient.</p>
              ) : (
                <div className="grid gap-6 md:grid-cols-2">
                  {/* Heart Rate */}
                  <div className="rounded-2xl border border-white/10 bg-dark-900/70 p-5">
                    <div className="flex items-center gap-2 mb-4">
                      <HeartPulse size={16} className="text-red-400" />
                      <h3 className="font-semibold text-white">Heart Rate (bpm)</h3>
                    </div>
                    <ResponsiveContainer width="100%" height={160}>
                      <AreaChart data={vitalTrendData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#243041" />
                        <XAxis dataKey="time" stroke="#9ca3af" tick={{ fontSize: 11 }} />
                        <YAxis stroke="#9ca3af" domain={['auto', 'auto']} />
                        <Tooltip contentStyle={{ backgroundColor: '#0b1220', border: 'none', borderRadius: 8 }} />
                        <Area type="monotone" dataKey="hr" stroke="#f87171" fill="#f87171" fillOpacity={0.15} strokeWidth={2} />
                      </AreaChart>
                    </ResponsiveContainer>
                  </div>

                  {/* Blood Pressure */}
                  <div className="rounded-2xl border border-white/10 bg-dark-900/70 p-5">
                    <div className="flex items-center gap-2 mb-4">
                      <TrendingUp size={16} className="text-blue-400" />
                      <h3 className="font-semibold text-white">Blood Pressure (mmHg)</h3>
                    </div>
                    <ResponsiveContainer width="100%" height={160}>
                      <AreaChart data={vitalTrendData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#243041" />
                        <XAxis dataKey="time" stroke="#9ca3af" tick={{ fontSize: 11 }} />
                        <YAxis stroke="#9ca3af" domain={['auto', 'auto']} />
                        <Tooltip contentStyle={{ backgroundColor: '#0b1220', border: 'none', borderRadius: 8 }} />
                        <Area type="monotone" dataKey="systolic" stroke="#38bdf8" fill="#38bdf8" fillOpacity={0.15} strokeWidth={2} />
                        <Area type="monotone" dataKey="diastolic" stroke="#818cf8" fill="#818cf8" fillOpacity={0.10} strokeWidth={2} />
                      </AreaChart>
                    </ResponsiveContainer>
                    <p className="text-xs text-dark-400 mt-2">Blue = Systolic · Purple = Diastolic</p>
                  </div>

                  {/* Temperature */}
                  <div className="rounded-2xl border border-white/10 bg-dark-900/70 p-5">
                    <div className="flex items-center gap-2 mb-4">
                      <Thermometer size={16} className="text-orange-400" />
                      <h3 className="font-semibold text-white">Temperature (°F)</h3>
                    </div>
                    <ResponsiveContainer width="100%" height={160}>
                      <AreaChart data={vitalTrendData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#243041" />
                        <XAxis dataKey="time" stroke="#9ca3af" tick={{ fontSize: 11 }} />
                        <YAxis stroke="#9ca3af" domain={['auto', 'auto']} />
                        <Tooltip contentStyle={{ backgroundColor: '#0b1220', border: 'none', borderRadius: 8 }} />
                        <Area type="monotone" dataKey="temp" stroke="#fb923c" fill="#fb923c" fillOpacity={0.15} strokeWidth={2} />
                      </AreaChart>
                    </ResponsiveContainer>
                  </div>

                  {/* SpO2 */}
                  <div className="rounded-2xl border border-white/10 bg-dark-900/70 p-5">
                    <div className="flex items-center gap-2 mb-4">
                      <Droplets size={16} className="text-cyan-400" />
                      <h3 className="font-semibold text-white">SpO2 (%)</h3>
                    </div>
                    <ResponsiveContainer width="100%" height={160}>
                      <AreaChart data={vitalTrendData}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#243041" />
                        <XAxis dataKey="time" stroke="#9ca3af" tick={{ fontSize: 11 }} />
                        <YAxis stroke="#9ca3af" domain={[80, 100]} />
                        <Tooltip contentStyle={{ backgroundColor: '#0b1220', border: 'none', borderRadius: 8 }} />
                        <Area type="monotone" dataKey="spo2" stroke="#22d3ee" fill="#22d3ee" fillOpacity={0.15} strokeWidth={2} />
                      </AreaChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              )}

              {/* Latest vital values summary */}
              {patientVitals.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold text-white mb-3">Latest Readings</h3>
                  <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
                    {patientVitals.slice(0, 8).map((v, i) => (
                      <div key={i} className="rounded-2xl border border-white/10 bg-dark-900/70 p-4">
                        <p className="text-xs text-dark-400 mb-1">{v.vitalType || 'Vital'}</p>
                        <p className="text-xl font-bold text-white">
                          {v.systolic && v.diastolic ? `${v.systolic}/${v.diastolic}` : (v.displayValue || v.value || '—')}
                          <span className="ml-1 text-sm text-dark-300">{v.unit || ''}</span>
                        </p>
                        <p className="text-xs text-dark-500 mt-1">
                          {new Date(v.readingTimestamp || v.timestamp || Date.now()).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}


          {/* PREDICTIONS TAB */}
          {activeTab === 'predictions' && selectedPatient && (
            <div className="space-y-5">
              <h2 className="text-2xl font-bold text-white">AI Predictions — {getName(selectedPatient)}</h2>
              {patientPredictions.length === 0 ? (
                <p className="text-dark-400">No AI predictions yet. Connect to live services for real-time analysis.</p>
              ) : (
                <div className="space-y-4">
                  {patientPredictions.map((pred, i) => (
                    <div key={i} className="rounded-3xl border border-white/10 bg-dark-900/70 p-6">
                      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                        <div>
                          <p className="text-xs uppercase tracking-[0.3em] text-dark-400">NEWS analysis</p>
                          <h3 className="mt-1 text-xl font-semibold text-white">Prediction snapshot</h3>
                        </div>
                        <span className={`rounded-full border px-3 py-1 text-sm font-semibold ${getRiskColor(pred.riskLevel)}`}>{pred.riskLevel || 'LOW'}</span>
                      </div>
                      <div className="mt-4 grid gap-4 md:grid-cols-3">
                        <div className="rounded-xl bg-dark-900/80 border border-white/10 p-4"><p className="text-sm text-dark-300">NEWS Score</p><p className="text-2xl font-bold text-white">{pred.newsScore || 0}</p></div>
                        <div className="rounded-xl bg-dark-900/80 border border-white/10 p-4"><p className="text-sm text-dark-300">Confidence</p><p className="text-2xl font-bold text-white">{pred.confidence || 0}%</p></div>
                        <div className="rounded-xl bg-dark-900/80 border border-white/10 p-4"><p className="text-sm text-dark-300">Risk Level</p><p className="text-2xl font-bold text-white">{pred.riskLevel || 'LOW'}</p></div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* ALERTS TAB — with RESOLVE button */}
          {activeTab === 'alerts' && selectedPatient && (
            <div className="space-y-5">
              <div>
                <h2 className="text-2xl font-bold text-white">Patient Alerts — {getName(selectedPatient)}</h2>
                <p className="text-sm text-dark-300 mt-1">Acknowledge, escalate, or resolve ward alerts. Resolved alerts are removed from this list.</p>
              </div>
              {patientAlerts.length === 0 ? (
                <div className="rounded-2xl border border-dashed border-white/10 bg-dark-900/50 p-8 text-center text-dark-300">
                  <CheckCircle2 size={32} className="mx-auto mb-3 text-success-400" />
                  No active alerts for this patient.
                </div>
              ) : (
                <div className="space-y-4">
                  {patientAlerts.map(alert => (
                    <motion.div key={alert.id} layout initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                      className="rounded-3xl border border-white/10 bg-dark-900/70 p-6">
                      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold border ${
                              alert.priority === 'CRITICAL' ? 'bg-red-500/20 text-red-300 border-red-500/30' :
                              alert.priority === 'HIGH' ? 'bg-orange-500/20 text-orange-300 border-orange-500/30' :
                              'bg-yellow-500/20 text-yellow-300 border-yellow-500/30'}`}>
                              {alert.priority}
                            </span>
                            <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold border ${
                              alert.status === 'RESOLVED' ? 'bg-green-500/20 text-green-300 border-green-500/30' :
                              alert.status === 'ESCALATED' ? 'bg-orange-500/20 text-orange-300 border-orange-500/30' :
                              alert.status === 'ACKNOWLEDGED' ? 'bg-purple-500/20 text-purple-300 border-purple-500/30' :
                              'bg-red-500/20 text-red-300 border-red-500/30'}`}>
                              {alert.status}
                            </span>
                          </div>
                          <h3 className="mt-2 text-lg font-bold text-white">{alert.message}</h3>
                          <p className="mt-1 text-sm text-dark-400">Logged: {new Date(alert.createdAt || alert.timestamp).toLocaleString()}</p>
                        </div>
                        <div className="flex flex-wrap gap-2 lg:self-center">
                          {alert.status === 'ACTIVE' && (
                            <button onClick={() => handleUpdateAlertStatus(alert.id, 'ACKNOWLEDGED')}
                              className="px-4 py-2 text-xs font-semibold rounded-xl border border-purple-500/30 bg-purple-500/10 hover:bg-purple-500/20 text-purple-300 transition-all">
                              Acknowledge
                            </button>
                          )}
                          {(alert.status === 'ACTIVE' || alert.status === 'ACKNOWLEDGED') && (
                            <button onClick={() => handleUpdateAlertStatus(alert.id, 'ESCALATED')}
                              className="px-4 py-2 text-xs font-semibold rounded-xl border border-orange-500/30 bg-orange-500/10 hover:bg-orange-500/20 text-orange-300 transition-all">
                              Escalate
                            </button>
                          )}
                          {/* RESOLVE button — removes the alert from the list */}
                          <button onClick={() => handleResolveAlert(alert.id)}
                            className="px-4 py-2 text-xs font-semibold rounded-xl border border-green-500/30 bg-green-500/10 hover:bg-green-500/20 text-green-300 transition-all">
                            RESOLVE
                          </button>
                        </div>
                      </div>
                    </motion.div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* TIMELINE TAB */}
          {activeTab === 'timeline' && selectedPatient && (
            <div className="space-y-5">
              <h2 className="text-2xl font-bold text-white">Patient Timeline — {getName(selectedPatient)}</h2>
              {timelineEvents.length === 0 ? (
                <p className="text-dark-400">No timeline events available yet.</p>
              ) : (
                <div className="relative pl-4">
                  <div className="absolute left-[31px] top-0 h-full w-px bg-white/10" />
                  <div className="space-y-4">
                    {timelineEvents.map((ev, i) => {
                      const isVital = Boolean(ev.vitalType)
                      const isPred = Boolean(ev.riskLevel)
                      const dot = isVital ? 'bg-success-400' : isPred ? 'bg-primary-400' : 'bg-error-400'
                      return (
                        <div key={i} className="relative pl-10">
                          <div className={`absolute left-0 top-5 h-8 w-8 rounded-full border border-white/10 ${dot}`} />
                          <div className="rounded-3xl border border-white/10 bg-dark-900/70 p-5">
                            <div className="flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
                              <h4 className="text-lg font-semibold text-white">
                                {isVital ? `Vital: ${ev.vitalType}` : isPred ? 'AI Prediction' : 'Alert'}
                              </h4>
                              <span className="text-xs text-dark-400">{new Date(ev.timestamp || ev.readingTimestamp).toLocaleString()}</span>
                            </div>
                            <p className="mt-2 text-sm text-dark-300">
                              {isVital ? `${ev.value || ev.displayValue || '—'} ${ev.unit || ''}` :
                               isPred ? `Risk ${ev.riskLevel} — confidence ${ev.confidence || 0}%` :
                               ev.message}
                            </p>
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </div>
              )}
            </div>
          )}


          {/* LAB ANALYTICS TAB */}
          {activeTab === 'lab' && (
            <div className="space-y-6">
              <div>
                <h2 className="text-2xl font-bold text-white">Laboratory Analytics</h2>
                <p className="text-sm text-dark-300 mt-1">Department-wide lab test results summary — {user?.department || 'General'} department.</p>
              </div>

              {/* Summary cards */}
              <div className="grid gap-4 md:grid-cols-3">
                <div className="rounded-2xl border border-white/10 bg-dark-900/70 p-5">
                  <p className="text-sm text-dark-300">Total Tests Processed</p>
                  <p className="text-3xl font-bold text-white mt-2">{labData.reduce((s, r) => s + r.normal + r.abnormal + r.pending, 0)}</p>
                </div>
                <div className="rounded-2xl border border-orange-500/20 bg-orange-500/10 p-5">
                  <p className="text-sm text-orange-300">Abnormal Results</p>
                  <p className="text-3xl font-bold text-white mt-2">{labData.reduce((s, r) => s + r.abnormal, 0)}</p>
                </div>
                <div className="rounded-2xl border border-yellow-500/20 bg-yellow-500/10 p-5">
                  <p className="text-sm text-yellow-300">Pending Results</p>
                  <p className="text-3xl font-bold text-white mt-2">{labData.reduce((s, r) => s + r.pending, 0)}</p>
                </div>
              </div>

              {/* Per-test table */}
              <div className="rounded-2xl border border-white/10 bg-dark-900/70 overflow-hidden">
                <table className="min-w-full">
                  <thead>
                    <tr className="border-b border-white/5 text-left text-xs uppercase tracking-[0.2em] text-dark-400">
                      <th className="px-6 py-4">Test Name</th>
                      <th className="px-6 py-4">Normal</th>
                      <th className="px-6 py-4">Abnormal</th>
                      <th className="px-6 py-4">Pending</th>
                      <th className="px-6 py-4">Abnormal Rate</th>
                    </tr>
                  </thead>
                  <tbody>
                    {labData.map(row => {
                      const total = row.normal + row.abnormal + row.pending
                      const rate = total > 0 ? Math.round((row.abnormal / total) * 100) : 0
                      return (
                        <tr key={row.test} className="border-b border-white/5 hover:bg-white/3">
                          <td className="px-6 py-4 font-medium text-white">{row.test}</td>
                          <td className="px-6 py-4 text-green-300">{row.normal}</td>
                          <td className="px-6 py-4 text-orange-300">{row.abnormal}</td>
                          <td className="px-6 py-4 text-yellow-300">{row.pending}</td>
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-3">
                              <div className="flex-1 h-2 rounded-full bg-dark-800 overflow-hidden">
                                <div className="h-full rounded-full bg-gradient-to-r from-orange-500 to-red-500" style={{ width: `${rate}%` }} />
                              </div>
                              <span className="text-sm text-dark-300 w-10">{rate}%</span>
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>

              {/* Trend chart */}
              <div className="rounded-2xl border border-white/10 bg-dark-900/70 p-5">
                <h3 className="font-semibold text-white mb-4">Normal vs Abnormal — Visual Comparison</h3>
                <ResponsiveContainer width="100%" height={220}>
                  <AreaChart data={labData.map(r => ({ name: r.test.split('/')[0], normal: r.normal, abnormal: r.abnormal }))}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#243041" />
                    <XAxis dataKey="name" stroke="#9ca3af" tick={{ fontSize: 11 }} />
                    <YAxis stroke="#9ca3af" />
                    <Tooltip contentStyle={{ backgroundColor: '#0b1220', border: 'none', borderRadius: 8 }} />
                    <Area type="monotone" dataKey="normal" stroke="#22c55e" fill="#22c55e" fillOpacity={0.15} strokeWidth={2} />
                    <Area type="monotone" dataKey="abnormal" stroke="#f97316" fill="#f97316" fillOpacity={0.15} strokeWidth={2} />
                  </AreaChart>
                </ResponsiveContainer>
                <p className="text-xs text-dark-400 mt-2">Green = Normal · Orange = Abnormal</p>
              </div>
            </div>
          )}

        </motion.section>
      </div>
    </div>
  )
}

function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = filename
  document.body.appendChild(a); a.click(); a.remove()
  window.URL.revokeObjectURL(url)
}
