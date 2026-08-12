import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { AuthService } from '../auth/AuthService'
import { User } from '../types/user'
import { vitalsApi } from '../services/api'
import WebSocketService from '../services/websocketService'

const LOCAL_NURSE_VITAL_EVENT = 'meditrack:nurse-vital'
const LOCAL_NURSE_VITAL_STORAGE_KEY = 'meditrack:last-nurse-vital'

function buildVitalPayloadsList({
  patientIdentifier,
  patientId,
  heartRate,
  systolic,
  diastolic,
  oxygenSaturation,
  respiratoryRate,
  temperature,
  nurseId,
  department,
  notes,
}: {
  patientIdentifier: string;
  patientId: string | number;
  heartRate: number;
  systolic: number;
  diastolic: number;
  oxygenSaturation: number;
  respiratoryRate: number;
  temperature: number;
  nurseId: string;
  department: string;
  notes: string;
}) {
  const common = {
    patientIdentifier,
    patientId,
    readingTimestamp: new Date().toISOString(),
    source: 'MANUAL',
    location: department,
    department,
    notes: notes || `Recorded by ${nurseId}`,
    qualityScore: 0.95,
    nurseId,
  }

  const payloads: any[] = []

  if (heartRate) {
    payloads.push({ ...common, vitalType: 'HEART_RATE', value: heartRate, unit: 'bpm' })
  }

  if (systolic && diastolic) {
    payloads.push({ ...common, vitalType: 'BLOOD_PRESSURE', value: systolic, systolic, diastolic, unit: 'mmHg' })
  }

  if (oxygenSaturation) {
    payloads.push({ ...common, vitalType: 'SPO2', value: oxygenSaturation, unit: '%' })
  }

  if (respiratoryRate) {
    payloads.push({ ...common, vitalType: 'RESPIRATORY_RATE', value: respiratoryRate, unit: 'bpm' })
  }

  if (temperature) {
    payloads.push({ ...common, vitalType: 'TEMPERATURE', value: temperature, unit: 'F' })
  }

  return payloads
}

function getDisplayName(value: any) {
  const parts = [
    value?.name,
    value?.fullName,
    value?.firstName && value?.lastName ? `${value.firstName} ${value.lastName}` : value?.firstName,
    value?.lastName,
  ].filter(Boolean)
  return String(parts.join(' ').trim() || value?.email || 'Nurse')
}

function getDisplayDepartment(value: any) {
  return String(value?.departmentName || value?.department || 'General').trim() || 'General'
}

export default function NurseDashboardRealTime() {
  const [currentUser, setCurrentUser] = useState<User | null>(null)
  const [assignedPatients, setAssignedPatients] = useState<any[]>([])
  const [selectedPatient, setSelectedPatient] = useState<any>(null)
  const [vitalsForm, setVitalsForm] = useState({
    heartRate: '',
    systolicBp: '',
    diastolicBp: '',
    oxygenSaturation: '',
    respiratoryRate: '',
    temperature: '',
    glucose: '',
    hemoglobin: '',
    notes: ''
  })
  const [recentReadings, setRecentReadings] = useState<any[]>([])
  const [alerts, setAlerts] = useState<any[]>([])
  const [patientAnalysis, setPatientAnalysis] = useState<any>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState<'patients' | 'vitals' | 'history'>('patients')
  const [realTimeAlerts, setRealTimeAlerts] = useState<any[]>([])
  const [wsConnected, setWsConnected] = useState(false)
  const [showVitalsModal, setShowVitalsModal] = useState(false)

  // WebSocket service instance
  const wsService = WebSocketService.getInstance()

  // Load data on component mount
  useEffect(() => {
    const user = AuthService.getCurrentUser()
    setCurrentUser(user)
    if (user) {
      void loadAssignedPatients(user)
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
      console.log('WebSocket connected for nurse dashboard')
    } catch (error) {
      console.error('WebSocket connection failed:', error)
      setError('Real-time updates unavailable')
    }
  }

  const disconnectWebSocket = () => {
    wsService.disconnect()
    setWsConnected(false)
  }

  // Subscribe to real-time updates when patient is selected
  useEffect(() => {
    if (selectedPatient) {
      void loadPatientAnalysis(selectedPatient.id)
    } else {
      setPatientAnalysis(null)
    }
  }, [selectedPatient])

  useEffect(() => {
    if (selectedPatient && wsConnected) {
      const department = selectedPatient.department || currentUser?.departmentName
      if (!department) {
        setError('Department is required for real-time updates')
        return
      }
      const topicDepartment = department.trim().toLowerCase().replace(/[^a-z0-9_-]/g, '-')
      // Clear previous subscriptions
      wsService.unsubscribe(`/topic/nurse-vitals/department/${topicDepartment}`)
      wsService.unsubscribe(`/topic/nurse-alerts/department/${topicDepartment}`)

      wsService.subscribeToNurseVitals(department, (vitalData) => {
        if (String(vitalData.patientId) !== String(selectedPatient.id)) return
        console.log('Real-time vital update:', vitalData)
        setRecentReadings(prev => [vitalData, ...prev.slice(0, 49)])
      })

      wsService.subscribeToNurseAlerts(department, (alertData) => {
        if (String(alertData.patientId) !== String(selectedPatient.id)) return
        console.log('Real-time alert:', alertData)
        setAlerts(prev => [alertData, ...prev.slice(0, 49)])
        setRealTimeAlerts(prev => [alertData, ...prev.slice(0, 19)])
      })
    }

    return () => {
      if (selectedPatient) {
        const department = selectedPatient.department || currentUser?.departmentName
        if (!department) return
        const topicDepartment = department.trim().toLowerCase().replace(/[^a-z0-9_-]/g, '-')
        wsService.unsubscribe(`/topic/nurse-vitals/department/${topicDepartment}`)
        wsService.unsubscribe(`/topic/nurse-alerts/department/${topicDepartment}`)
      }
    }
  }, [selectedPatient, wsConnected, currentUser])

  const loadPatientAnalysis = async (patientId: string | number) => {
    try {
      const response = await vitalsApi.getSummary(patientId)
      setPatientAnalysis(response.data ?? null)
    } catch {
      setPatientAnalysis(null)
    }
  }

  const loadAssignedPatients = async (user: User | null) => {
    setIsLoading(true)
    setError('')
    
    const authUser = user ?? AuthService.getCurrentUser()

    try {
      const patients = await AuthService.getPatientsForCurrentUser()
      const mappedPatients = patients.map((patient: any) => ({
        id: patient.id,
        patientIdentifier: patient.patientIdentifier || `PT-${patient.id}`,
        firstName: patient.firstName || patient.fullName?.split(' ')[0] || 'Patient',
        lastName: patient.lastName || patient.fullName?.split(' ')[1] || '',
        condition: patient.condition || patient.clinicalStatus || 'STABLE',
        roomNumber: patient.wardNumber || patient.roomNumber || 'WARD',
        bedNumber: patient.bedNumber || 'N/A',
        nurseId: authUser?.id || patient.nurseId,
        department: patient.department || patient.departmentName || authUser?.departmentName || 'General'
      }))

      setAssignedPatients(mappedPatients)
      if (mappedPatients.length > 0) {
        setSelectedPatient(mappedPatients[0])
      }
    } catch (error) {
      console.error('Error loading assigned patients:', error)
      setError('Failed to load assigned patients')
    } finally {
      setIsLoading(false)
    }
  }

  const handleVitalsSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    if (!selectedPatient) {
      setError('Please select a patient first')
      return
    }

    // Enforce nurse department permission: only nurse assigned to patient's department can submit
    const authUser = AuthService.getCurrentUser()
    if (authUser && authUser.roleName === 'nurse' && authUser.departmentName && selectedPatient.department) {
      if (String(authUser.departmentName).toLowerCase() !== String(selectedPatient.department).toLowerCase()) {
        setError('You are not authorized to record vitals for this patient (department mismatch).')
        return
      }
    }

    try {
      const payloads = buildVitalPayloadsList({
        patientIdentifier: selectedPatient.patientIdentifier || `PT-${selectedPatient.id}`,
        patientId: selectedPatient.id,
        heartRate: parseInt(vitalsForm.heartRate, 10) || 0,
        systolic: parseInt(vitalsForm.systolicBp, 10) || 0,
        diastolic: parseInt(vitalsForm.diastolicBp, 10) || 0,
        oxygenSaturation: parseInt(vitalsForm.oxygenSaturation, 10) || 0,
        respiratoryRate: parseInt(vitalsForm.respiratoryRate, 10) || 0,
        temperature: parseFloat(vitalsForm.temperature) || 0,
        nurseId: currentUser?.id || currentUser?.email || 'nurse',
        department: selectedPatient.department || currentUser?.departmentName || 'General',
        notes: vitalsForm.notes,
      })

      if (payloads.length === 0) {
        setError('Please enter at least one vital value')
        return
      }

      const responses = await Promise.all(payloads.map((payload) => vitalsApi.createVital(payload)))
      const success = responses.every((response) => response.status >= 200 && response.status < 300)

      if (success) {
        const localEvents = payloads.map((payload) => ({
          eventId: `nurse-local-${payload.patientId}-${payload.vitalType}-${Date.now()}`,
          eventType: 'VITAL_RECORDED',
          timestamp: payload.readingTimestamp,
          patientId: payload.patientId,
          vitalType: payload.vitalType,
          value: payload.value,
          unit: payload.unit,
          systolic: payload.systolic,
          diastolic: payload.diastolic,
          department: payload.department,
          nurseId: payload.nurseId,
          createdBy: currentUser?.id || payload.nurseId,
          role: 'NURSE',
          message: payload.notes,
          recordedBy: currentUser ? `${currentUser.firstName || ''} ${currentUser.lastName || ''}`.trim() || payload.nurseId : payload.nurseId,
        }))

        setRecentReadings(prev => [{
          timestamp: new Date().toISOString(),
          heartRate: parseInt(vitalsForm.heartRate, 10) || null,
          systolicBp: parseInt(vitalsForm.systolicBp, 10) || null,
          diastolicBp: parseInt(vitalsForm.diastolicBp, 10) || null,
          oxygenSaturation: parseInt(vitalsForm.oxygenSaturation, 10) || null,
          respiratoryRate: parseInt(vitalsForm.respiratoryRate, 10) || null,
          temperature: parseFloat(vitalsForm.temperature) || null,
          notes: vitalsForm.notes,
          patientId: selectedPatient.id,
        }, ...prev.slice(0, 49)])

        try {
          window.localStorage.setItem(LOCAL_NURSE_VITAL_STORAGE_KEY, JSON.stringify(localEvents[localEvents.length - 1]))
        } catch {
          // ignore local storage failures
        }
        window.dispatchEvent(new CustomEvent(LOCAL_NURSE_VITAL_EVENT, { detail: localEvents }))

        setVitalsForm({
          heartRate: '',
          systolicBp: '',
          diastolicBp: '',
          oxygenSaturation: '',
          respiratoryRate: '',
          temperature: '',
          glucose: '',
          hemoglobin: '',
          notes: ''
        })

        setShowVitalsModal(false)
        void loadPatientAnalysis(selectedPatient.id)
        alert('Vitals recorded successfully')
      } else {
        setError('Failed to record vitals')
      }
    } catch (error) {
      console.error('Error recording vitals:', error)
      setError('Failed to record vitals')
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
    <div className="min-h-screen bg-gradient-to-br from-teal-600 to-blue-700 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-white">MediTrack Nurse Dashboard</h1>
              <p className="text-white/80">{getDisplayName(currentUser)}</p>
              <div className="flex items-center space-x-4 mt-2">
                <span className="text-white/60">Department: {getDisplayDepartment(currentUser)}</span>
                <span className={`px-2 py-1 rounded text-xs ${wsConnected ? 'bg-green-500/20 text-green-300' : 'bg-red-500/20 text-red-300'}`}>
                  {wsConnected ? '🟢 Connected' : '🔴 Offline'}
                </span>
              </div>
            </div>
            <button
              onClick={() => AuthService.logout()}
              className="bg-red-500 text-white px-4 py-2 rounded-lg hover:bg-red-600"
            >
              Sign Out
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

        {selectedPatient && (
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-5 mb-6">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
              <div>
                <h3 className="text-lg font-semibold text-white">Care focus for {selectedPatient?.firstName || selectedPatient?.lastName ? `${selectedPatient.firstName ?? ''} ${selectedPatient.lastName ?? ''}`.trim() : getDisplayName(selectedPatient)}</h3>
                <p className="text-white/70 text-sm">{patientAnalysis?.overallStatus || 'STABLE'} · Risk {patientAnalysis?.riskLevel || 'LOW'}</p>
              </div>
              <div className={`px-3 py-1 rounded-full text-sm ${patientAnalysis?.overallStatus === 'ALERT' ? 'bg-red-500/20 text-red-300' : 'bg-green-500/20 text-green-300'}`}>
                {patientAnalysis?.overallStatus || 'STABLE'}
              </div>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mt-4">
              <div className="bg-white/10 rounded-lg p-3">
                <p className="text-white/60 text-xs uppercase tracking-wider">Critical</p>
                <p className="text-xl font-semibold text-white">{patientAnalysis?.criticalCount ?? 0}</p>
              </div>
              <div className="bg-white/10 rounded-lg p-3">
                <p className="text-white/60 text-xs uppercase tracking-wider">Abnormal</p>
                <p className="text-xl font-semibold text-white">{patientAnalysis?.abnormalCount ?? 0}</p>
              </div>
              <div className="bg-white/10 rounded-lg p-3">
                <p className="text-white/60 text-xs uppercase tracking-wider">Recent readings</p>
                <p className="text-xl font-semibold text-white">{patientAnalysis?.recentReadingCount ?? 0}</p>
              </div>
            </div>
          </div>
        )}

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Assigned Patients</h3>
            <p className="text-3xl font-bold text-white">{assignedPatients.length}</p>
          </div>
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Critical Cases</h3>
            <p className="text-3xl font-bold text-white">
              {assignedPatients.filter(p => p.condition === 'CRITICAL').length}
            </p>
          </div>
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Readings Today</h3>
            <p className="text-3xl font-bold text-white">{recentReadings.length}</p>
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
              onClick={() => setActiveTab('patients')}
              className={`px-4 py-2 rounded-lg ${activeTab === 'patients' ? 'bg-white/20 text-white' : 'text-white/60'}`}
            >
              My Patients
            </button>
            <button
              onClick={() => {
                if (selectedPatient) {
                  setShowVitalsModal(true)
                } else {
                  setError('Please select a patient first')
                }
              }}
              className={`px-4 py-2 rounded-lg ${activeTab === 'vitals' ? 'bg-white/20 text-white' : 'text-white/60'}`}
              disabled={!selectedPatient}
            >
              Record Vitals
            </button>
            <button
              onClick={() => setActiveTab('history')}
              className={`px-4 py-2 rounded-lg ${activeTab === 'history' ? 'bg-white/20 text-white' : 'text-white/60'}`}
              disabled={!selectedPatient}
            >
              Reading History
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

          {activeTab === 'patients' && (
            <div>
              <h2 className="text-2xl font-bold text-white mb-4">My Assigned Patients</h2>
              
              {isLoading ? (
                <div className="text-white/80">Loading patients...</div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {assignedPatients.map((patient) => (
                    <motion.div
                      key={patient.id}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      className={`bg-white/10 rounded-lg p-4 cursor-pointer hover:bg-white/20 transition-colors ${
                        selectedPatient?.id === patient.id ? 'ring-2 ring-white/50' : ''
                      }`}
                      onClick={() => setSelectedPatient(patient)}
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
                        <p>Room: {patient.roomNumber} - Bed {patient.bedNumber}</p>
                      </div>
                      
                      <div className="mt-3 pt-3 border-t border-white/20">
                        <button className="text-blue-400 hover:text-blue-300 text-sm">
                          {selectedPatient?.id === patient.id ? 'Selected ✓' : 'Select Patient'}
                        </button>
                      </div>
                    </motion.div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'history' && selectedPatient && (
            <div>
              <h2 className="text-2xl font-bold text-white mb-4">
                Reading History - {selectedPatient.firstName} {selectedPatient.lastName}
              </h2>
              
              <div className="space-y-4">
                {recentReadings.map((reading, index) => (
                  <div key={index} className="bg-white/10 rounded-lg p-4">
                    <div className="flex justify-between items-start mb-3">
                      <div>
                        <h4 className="text-white font-semibold">
                          {new Date(reading.timestamp).toLocaleString()}
                        </h4>
                        <p className="text-white/60 text-sm">Recorded by: {currentUser?.firstName}</p>
                      </div>
                    </div>
                    
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                      <div>
                        <h5 className="text-white/80 text-sm">Heart Rate</h5>
                        <p className="text-xl font-bold text-white">{reading.heartRate} bpm</p>
                      </div>
                      <div>
                        <h5 className="text-white/80 text-sm">Blood Pressure</h5>
                        <p className="text-xl font-bold text-white">{reading.systolicBp}/{reading.diastolicBp} mmHg</p>
                      </div>
                      <div>
                        <h5 className="text-white/80 text-sm">SpO2</h5>
                        <p className="text-xl font-bold text-white">{reading.oxygenSaturation}%</p>
                      </div>
                      <div>
                        <h5 className="text-white/80 text-sm">Temperature</h5>
                        <p className="text-xl font-bold text-white">{reading.temperature}°C</p>
                      </div>
                    </div>
                    
                    {reading.notes && (
                      <div className="mt-3 pt-3 border-t border-white/20">
                        <h5 className="text-white/80 text-sm mb-1">Notes</h5>
                        <p className="text-white/80">{reading.notes}</p>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Vitals Modal */}
        {showVitalsModal && selectedPatient && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white rounded-2xl p-8 max-w-2xl w-full mx-4">
              <h2 className="text-2xl font-bold text-gray-800 mb-6">
                Record Vitals - {selectedPatient.firstName} {selectedPatient.lastName}
              </h2>
              
              <form onSubmit={handleVitalsSubmit} className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Heart Rate (bpm)</label>
                    <input
                      type="number"
                      value={vitalsForm.heartRate}
                      onChange={(e) => setVitalsForm({...vitalsForm, heartRate: e.target.value})}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Systolic BP (mmHg)</label>
                    <input
                      type="number"
                      value={vitalsForm.systolicBp}
                      onChange={(e) => setVitalsForm({...vitalsForm, systolicBp: e.target.value})}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Diastolic BP (mmHg)</label>
                    <input
                      type="number"
                      value={vitalsForm.diastolicBp}
                      onChange={(e) => setVitalsForm({...vitalsForm, diastolicBp: e.target.value})}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">SpO2 (%)</label>
                    <input
                      type="number"
                      value={vitalsForm.oxygenSaturation}
                      onChange={(e) => setVitalsForm({...vitalsForm, oxygenSaturation: e.target.value})}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Respiratory Rate (/min)</label>
                    <input
                      type="number"
                      value={vitalsForm.respiratoryRate}
                      onChange={(e) => setVitalsForm({...vitalsForm, respiratoryRate: e.target.value})}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Temperature (°C)</label>
                    <input
                      type="number"
                      step="0.1"
                      value={vitalsForm.temperature}
                      onChange={(e) => setVitalsForm({...vitalsForm, temperature: e.target.value})}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Glucose (mg/dL)</label>
                    <input
                      type="number"
                      value={vitalsForm.glucose}
                      onChange={(e) => setVitalsForm({...vitalsForm, glucose: e.target.value})}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">Hemoglobin (g/dL)</label>
                    <input
                      type="number"
                      value={vitalsForm.hemoglobin}
                      onChange={(e) => setVitalsForm({...vitalsForm, hemoglobin: e.target.value})}
                      className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    />
                  </div>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
                  <textarea
                    value={vitalsForm.notes}
                    onChange={(e) => setVitalsForm({...vitalsForm, notes: e.target.value})}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                    rows={3}
                  />
                </div>
                
                <div className="flex space-x-4">
                  <button
                    type="submit"
                    className="flex-1 bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700"
                  >
                    Record Vitals
                  </button>
                  <button
                    type="button"
                    onClick={() => setShowVitalsModal(false)}
                    className="flex-1 bg-gray-300 text-gray-700 py-2 rounded-lg hover:bg-gray-400"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
