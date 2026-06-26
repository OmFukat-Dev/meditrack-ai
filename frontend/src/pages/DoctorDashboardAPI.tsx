import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { AuthService } from '../auth/AuthService'
import { User } from '../types/user'
import { GATEWAY_URL } from '../services/api'

export default function DoctorDashboardAPI() {
  const [currentUser, setCurrentUser] = useState<User | null>(null)
  const [assignedPatients, setAssignedPatients] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  const [selectedPatient, setSelectedPatient] = useState<any>(null)
  const [patientVitals, setPatientVitals] = useState<any[]>([])
  const [activeTab, setActiveTab] = useState<'patients' | 'vitals' | 'predictions'>('patients')

  // Load data on component mount
  useEffect(() => {
    const user = AuthService.getCurrentUser()
    setCurrentUser(user)
    if (user) {
      loadAssignedPatients(user.id)
    }
  }, [])

  const loadAssignedPatients = async (doctorId: string) => {
    setIsLoading(true)
    setError('')
    
    try {
      // This would call the patient service to get assigned patients
      const response = await fetch(`${GATEWAY_URL}/patients/doctor/${doctorId}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('sessionToken')}`
        }
      })

      if (response.ok) {
        const patients = await response.json()
        setAssignedPatients(patients)
      } else {
        // Fallback to mock data for now
        setAssignedPatients([
          {
            id: 'patient-1',
            patientIdentifier: 'PT-001',
            firstName: 'John',
            lastName: 'Doe',
            condition: 'STABLE',
            roomNumber: 'A-101',
            bedNumber: '1',
            admissionDate: '2024-04-15',
            doctorId: doctorId
          },
          {
            id: 'patient-2',
            patientIdentifier: 'PT-002',
            firstName: 'Jane',
            lastName: 'Smith',
            condition: 'CRITICAL',
            roomNumber: 'B-205',
            bedNumber: '2',
            admissionDate: '2024-04-18',
            doctorId: doctorId
          }
        ])
      }
    } catch (error) {
      console.error('Error loading assigned patients:', error)
      setError('Failed to load assigned patients')
    } finally {
      setIsLoading(false)
    }
  }

  const loadPatientVitals = async (patientId: string) => {
    try {
      const response = await fetch(`${GATEWAY_URL}/vitals/patient/${patientId}`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('sessionToken')}`
        }
      })

      if (response.ok) {
        const vitals = await response.json()
        setPatientVitals(vitals)
      } else {
        // Fallback mock vitals
        setPatientVitals([
          {
            id: 1,
            vitalType: 'Heart Rate',
            value: 72,
            unit: 'bpm',
            readingTimestamp: new Date().toISOString(),
            vitalStatus: 'NORMAL'
          },
          {
            id: 2,
            vitalType: 'Blood Pressure',
            systolic: 120,
            diastolic: 80,
            unit: 'mmHg',
            readingTimestamp: new Date().toISOString(),
            vitalStatus: 'NORMAL'
          },
          {
            id: 3,
            vitalType: 'Temperature',
            value: 98.6,
            unit: '°F',
            readingTimestamp: new Date().toISOString(),
            vitalStatus: 'NORMAL'
          },
          {
            id: 4,
            vitalType: 'Oxygen Saturation',
            value: 98,
            unit: '%',
            readingTimestamp: new Date().toISOString(),
            vitalStatus: 'NORMAL'
          }
        ])
      }
    } catch (error) {
      console.error('Error loading patient vitals:', error)
    }
  }

  const handlePatientSelect = (patient: any) => {
    setSelectedPatient(patient)
    loadPatientVitals(patient.id)
    setActiveTab('vitals')
  }

  const getConditionColor = (condition: string) => {
    switch (condition) {
      case 'STABLE': return 'bg-green-500/20 text-green-300 border-green-500/30'
      case 'CRITICAL': return 'bg-red-500/20 text-red-300 border-red-500/30'
      case 'EMERGENCY': return 'bg-orange-500/20 text-orange-300 border-orange-500/30'
      default: return 'bg-gray-500/20 text-gray-300 border-gray-500/30'
    }
  }

  const getVitalStatusColor = (status: string) => {
    switch (status) {
      case 'NORMAL': return 'text-green-400'
      case 'ELEVATED': return 'text-yellow-400'
      case 'CRITICAL': return 'text-red-400'
      default: return 'text-gray-400'
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
    <div className="min-h-screen bg-gradient-to-br from-blue-600 to-purple-700 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-white">Doctor Dashboard</h1>
              <p className="text-white/80">Dr. {currentUser.firstName} {currentUser.lastName}</p>
              <p className="text-white/60">Department: {currentUser.departmentName || 'General'}</p>
            </div>
            <button
              onClick={() => AuthService.logout()}
              className="bg-red-500 text-white px-4 py-2 rounded-lg hover:bg-red-600"
            >
              Logout
            </button>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
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
            <h3 className="text-white/80 text-sm">Stable Cases</h3>
            <p className="text-3xl font-bold text-white">
              {assignedPatients.filter(p => p.condition === 'STABLE').length}
            </p>
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
              onClick={() => setActiveTab('vitals')}
              className={`px-4 py-2 rounded-lg ${activeTab === 'vitals' ? 'bg-white/20 text-white' : 'text-white/60'}`}
              disabled={!selectedPatient}
            >
              Patient Vitals
            </button>
            <button
              onClick={() => setActiveTab('predictions')}
              className={`px-4 py-2 rounded-lg ${activeTab === 'predictions' ? 'bg-white/20 text-white' : 'text-white/60'}`}
              disabled={!selectedPatient}
            >
              AI Predictions
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
                      className="bg-white/10 rounded-lg p-4 cursor-pointer hover:bg-white/20 transition-colors"
                      onClick={() => handlePatientSelect(patient)}
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
                        <p>Admitted: {new Date(patient.admissionDate).toLocaleDateString()}</p>
                      </div>
                      
                      <div className="mt-3 pt-3 border-t border-white/20">
                        <button className="text-blue-400 hover:text-blue-300 text-sm">
                          View Details →
                        </button>
                      </div>
                    </motion.div>
                  ))}
                </div>
              )}
            </div>
          )}

          {activeTab === 'vitals' && selectedPatient && (
            <div>
              <h2 className="text-2xl font-bold text-white mb-4">
                Vitals - {selectedPatient.firstName} {selectedPatient.lastName}
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                {patientVitals.map((vital) => (
                  <div key={vital.id} className="bg-white/10 rounded-lg p-4">
                    <h3 className="text-white/80 text-sm mb-1">{vital.vitalType}</h3>
                    <p className={`text-2xl font-bold ${getVitalStatusColor(vital.vitalStatus)}`}>
                      {vital.systolic && vital.diastolic 
                        ? `${vital.systolic}/${vital.diastolic}`
                        : vital.value
                      }
                      <span className="text-sm ml-1">{vital.unit}</span>
                    </p>
                    <p className="text-white/60 text-xs mt-1">
                      {new Date(vital.readingTimestamp).toLocaleTimeString()}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {activeTab === 'predictions' && selectedPatient && (
            <div>
              <h2 className="text-2xl font-bold text-white mb-4">
                AI Predictions - {selectedPatient.firstName} {selectedPatient.lastName}
              </h2>
              
              <div className="bg-white/10 rounded-lg p-6">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <h3 className="text-lg font-semibold text-white mb-3">NEWS Score</h3>
                    <div className="space-y-2">
                      <div className="flex justify-between text-white/80">
                        <span>Total Score:</span>
                        <span className="font-bold">3</span>
                      </div>
                      <div className="flex justify-between text-white/80">
                        <span>Risk Level:</span>
                        <span className="bg-yellow-500/20 text-yellow-300 px-2 py-1 rounded text-sm">MEDIUM</span>
                      </div>
                    </div>
                  </div>
                  
                  <div>
                    <h3 className="text-lg font-semibold text-white mb-3">Risk Prediction</h3>
                    <div className="space-y-2">
                      <div className="flex justify-between text-white/80">
                        <span>Deterioration Risk:</span>
                        <span className="font-bold text-yellow-400">25%</span>
                      </div>
                      <div className="flex justify-between text-white/80">
                        <span>Confidence:</span>
                        <span className="font-bold">87%</span>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div className="mt-6 pt-6 border-t border-white/20">
                  <h3 className="text-lg font-semibold text-white mb-3">Recommendations</h3>
                  <ul className="space-y-2 text-white/80">
                    <li>• Monitor vital signs every 2 hours</li>
                    <li>• Consider additional cardiac monitoring</li>
                    <li>• Review medication effectiveness</li>
                  </ul>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
