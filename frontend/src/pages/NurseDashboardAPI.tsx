import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  AlertCircle,
  Calendar as CalendarIcon,
  Download,
  FileText,
  HeartPulse,
  MessageSquare,
  RefreshCw,
  Save,
  Settings as SettingsIcon,
  Users,
  Wifi,
  WifiOff,
  Heart,
  Activity,
  Thermometer,
  Wind,
  Clock,
  User,
  Plus,
  Send,
  Phone,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  Sliders,
  Volume2,
  VolumeX
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import Sidebar from '../components/Sidebar';
import TopNavbar from '../components/TopNavbar';
import { patientApi, reportApi, vitalsApi } from '../services/api';
import {
  mockUsers,
  getPatientsByNurse,
  addVitalSign,
  getVitalSignsByPatient,
  updatePatient,
  mockPatients
} from '../database/mockDatabaseFromSeed';

type PatientRecord = {
  id: number;
  patientIdentifier: string;
  firstName: string;
  lastName: string;
  department?: string | null;
  wardNumber?: string | null;
  bedNumber?: string | null;
  clinicalStatus?: string | null;
  assignedClinicianName?: string | null;
  mobileNumber?: string | null;
};

type VitalsFormState = {
  heartRate: string;
  systolic: string;
  diastolic: string;
  spo2: string;
  temperature: string;
};

const initialVitalsForm: VitalsFormState = {
  heartRate: '',
  systolic: '',
  diastolic: '',
  spo2: '',
  temperature: '',
};

type QueuedVitalPayload = Record<string, string | number | null>;
const OFFLINE_QUEUE_KEY = 'meditrack_nurse_vitals_queue';

type ChatMessage = {
  id: string;
  sender: 'nurse' | 'doctor' | 'system';
  senderName: string;
  text: string;
  timestamp: string;
};

export default function NurseDashboardAPI() {
  const { user } = useAuth();
  const [activePage, setActivePage] = useState('Dashboard');
  const [patients, setPatients] = useState<PatientRecord[]>([]);
  const [selectedPatientId, setSelectedPatientId] = useState<string>('');
  const [vitals, setVitals] = useState<VitalsFormState>(initialVitalsForm);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [reporting, setReporting] = useState(false);
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [notification, setNotification] = useState('');
  
  // Settings Console States
  const [tempUnit, setTempUnit] = useState<'C' | 'F'>('C');
  const [soundAlerts, setSoundAlerts] = useState(true);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [refreshRate, setRefreshRate] = useState('5000');

  // Live Shift Countdown Timer (Simulating remaining shift duration)
  const [shiftSecondsRemaining, setShiftSecondsRemaining] = useState(19905); // ~5.5 hours

  // Live Chat Simulator States
  const [chatChannel, setChatChannel] = useState<'doctors' | 'icu' | 'ward'>('doctors');
  const [chatInput, setChatInput] = useState('');
  const [chatMessages, setChatMessages] = useState<Record<string, ChatMessage[]>>({
    doctors: [
      { id: '1', sender: 'doctor', senderName: 'Dr. Dipanshu Sharma', text: 'Sarah, did we record the latest vitals for Jane Smith in Bed B-205?', timestamp: '12:15 PM' },
      { id: '2', sender: 'nurse', senderName: 'Sarah Johnson', text: 'Logging them now, Doctor. Her SpO2 looks much more stable after the oxygen therapy.', timestamp: '12:18 PM' },
      { id: '3', sender: 'doctor', senderName: 'Dr. Dipanshu Sharma', text: 'Perfect. Let me know if her heart rate exceeds 110. Thanks!', timestamp: '12:20 PM' }
    ],
    icu: [
      { id: '1', sender: 'system', senderName: 'System Telemetry', text: 'Patient PT-002 (Jane Smith) moved to B-205 ICU secondary desk.', timestamp: '09:00 AM' },
      { id: '2', sender: 'doctor', senderName: 'Dr. Ayush Singh', text: 'Sarah, check Bed C-301. SpO2 fluctuated slightly at shift change.', timestamp: '10:14 AM' }
    ],
    ward: [
      { id: '1', sender: 'nurse', senderName: 'Nurse Emily Davis', text: 'General roster shift handovers are uploaded. Let me know if you need physical printouts.', timestamp: '11:00 AM' }
    ]
  });

  // Keep Track of Heart Rate beat for telemetry panel pulsing
  const [telemetryPulse, setTelemetryPulse] = useState(false);

  // Active Patient Vitals History from API or mock database fallback
  const [activePatientVitalsHistory, setActivePatientVitalsHistory] = useState<any[]>([]);

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      void flushQueuedVitals(true);
    };
    const handleOffline = () => setIsOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    void loadPatients();

    // Pulse telemetry indicator icon
    const pulseTimer = setInterval(() => {
      setTelemetryPulse(p => !p);
    }, 800);

    // Shift Countdown Timer
    const shiftTimer = setInterval(() => {
      setShiftSecondsRemaining(prev => (prev > 0 ? prev - 1 : 28800));
    }, 1000);

    if (navigator.onLine) {
      void flushQueuedVitals(false);
    }

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      clearInterval(pulseTimer);
      clearInterval(shiftTimer);
    };
  }, []);

  // Periodic Auto Refresh
  useEffect(() => {
    if (!autoRefresh) return;
    const intervalId = setInterval(() => {
      void loadPatients();
    }, Number(refreshRate));
    return () => clearInterval(intervalId);
  }, [autoRefresh, refreshRate]);

  const selectedPatient = useMemo(
    () => patients.find((patient) => String(patient.id) === selectedPatientId) || null,
    [patients, selectedPatientId]
  );

  const latestVitals = useMemo(() => {
    if (activePatientVitalsHistory.length === 0) return null;
    return activePatientVitalsHistory[activePatientVitalsHistory.length - 1];
  }, [activePatientVitalsHistory]);

  // Load patient vitals history from API or mock fallback
  useEffect(() => {
    if (selectedPatient) {
      void loadVitalsHistory();
    } else {
      setActivePatientVitalsHistory([]);
    }
  }, [selectedPatientId, patients]);

  async function loadVitalsHistory() {
    if (!selectedPatient) return;
    const patientDbId = `patient-${selectedPatient.id}`;
    try {
      const response = await vitalsApi.getPatientReadings(patientDbId);
      const rawReadings = response.data;
      if (Array.isArray(rawReadings) && rawReadings.length > 0) {
        // Group individual vitals by timestamp (since microservices save separate metrics)
        const grouped = rawReadings.reduce((acc: any, curr: any) => {
          const ts = curr.readingTimestamp || curr.timestamp;
          if (!acc[ts]) {
            acc[ts] = {
              id: curr.id,
              patientId: curr.patientId,
              timestamp: ts,
              heartRate: 0,
              bloodPressure: { systolic: 0, diastolic: 0 },
              temperature: 98.6,
              oxygenSaturation: 98,
              recordedBy: curr.recordedBy || 'nurse'
            };
          }
          if (curr.vitalType === 'HEART_RATE') acc[ts].heartRate = curr.value;
          else if (curr.vitalType === 'SPO2') acc[ts].oxygenSaturation = curr.value;
          else if (curr.vitalType === 'TEMPERATURE') acc[ts].temperature = curr.value;
          else if (curr.vitalType === 'BLOOD_PRESSURE') {
            acc[ts].bloodPressure.systolic = curr.systolic || curr.value;
            acc[ts].bloodPressure.diastolic = curr.diastolic || curr.value;
          }
          return acc;
        }, {});
        setActivePatientVitalsHistory(Object.values(grouped));
      } else {
        // If API returns empty, use local mock database
        setActivePatientVitalsHistory(getVitalSignsByPatient(patientDbId));
      }
    } catch (err) {
      // API Offline Fallback
      setActivePatientVitalsHistory(getVitalSignsByPatient(patientDbId));
    }
  }

  async function loadPatients() {
    try {
      // 1. Try loading from API gateway
      const response = await patientApi.getAll(0, 100);
      const fetchedPatients = extractCollection<any>(response.data).map((patient: any) => ({
        id: Number(patient.id.replace?.('patient-', '') ?? patient.id),
        patientIdentifier: patient.patientIdentifier,
        firstName: patient.firstName || patient.name?.split?.(' ')[0] || 'Patient',
        lastName: patient.lastName || patient.name?.split?.(' ')[1] || '',
        department: patient.department || 'General',
        wardNumber: patient.wardNumber || patient.roomNumber?.split?.('-')[0] || 'WARD',
        bedNumber: patient.bedNumber || patient.roomNumber?.split?.('-')[1] || 'N/A',
        clinicalStatus: patient.clinicalStatus || patient.condition || 'STABLE',
        assignedClinicianName: patient.assignedClinicianName || null,
        mobileNumber: patient.mobileNumber || null
      }));
      setPatients(fetchedPatients);
      if (fetchedPatients.length > 0 && !selectedPatientId) {
        setSelectedPatientId(String(fetchedPatients[0].id));
      }
      setIsOnline(true);
    } catch (error) {
      console.warn('API loading patients failed. Falling back to local mock database...');
      // 2. Fallback to mock database (EHR offline mode)
      if (user) {
        const nursePatients = getPatientsByNurse(user.id);
        const formattedPatients = nursePatients.map(patient => ({
          id: parseInt(patient.id.split('-')[1]),
          patientIdentifier: patient.patientIdentifier,
          firstName: patient.name.split(' ')[0],
          lastName: patient.name.split(' ')[1] || '',
          department: mockUsers.find(u => u.id === patient.doctorId)?.department || null,
          wardNumber: patient.roomNumber.split('-')[0] || null,
          bedNumber: patient.roomNumber.split('-')[1] || null,
          clinicalStatus: patient.condition,
          assignedClinicianName: mockUsers.find(u => u.id === patient.doctorId)?.name || null,
          mobileNumber: patient.mobileNumber || null
        }));
        setPatients(formattedPatients);
        if (formattedPatients.length > 0 && !selectedPatientId) {
          setSelectedPatientId(String(formattedPatients[0].id));
        }
      }
    } finally {
      setLoading(false);
    }
  }

  // Handle vitals form submission
  async function handleVitalsSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!selectedPatient) {
      setNotification('⚠️ Select a patient before saving vitals.');
      return;
    }

    if (user && user.department && selectedPatient.department) {
      if (String(user.department).toLowerCase() !== String(selectedPatient.department).toLowerCase()) {
        setNotification('❌ Error: You are not authorized to record vitals for this patient (department mismatch).');
        return;
      }
    }

    const hrInput = vitals.heartRate.trim();
    const sysInput = vitals.systolic.trim();
    const diaInput = vitals.diastolic.trim();
    const spo2Input = vitals.spo2.trim();
    const tempInput = vitals.temperature.trim();

    if (!hrInput && !sysInput && !diaInput && !spo2Input && !tempInput) {
      setNotification('⚠️ Enter at least one vital sign reading.');
      return;
    }

    setSubmitting(true);
    try {
      const patientDbId = `patient-${selectedPatient.id}`;
      
      // Load previous vitals for merge fallback (don't overwrite empty fields with zero)
      const prevVital = latestVitals;

      const heartRate = hrInput ? Number(hrInput) : (prevVital?.heartRate ?? 72);
      const systolic = sysInput ? Number(sysInput) : (prevVital?.bloodPressure.systolic ?? 120);
      const diastolic = diaInput ? Number(diaInput) : (prevVital?.bloodPressure.diastolic ?? 80);
      const oxygenSaturation = spo2Input ? Number(spo2Input) : (prevVital?.oxygenSaturation ?? 98);
      
      // Celsius to Fahrenheit Conversion
      let temperature = prevVital?.temperature ?? 98.6;
      if (tempInput) {
        const tempVal = Number(tempInput);
        temperature = tempUnit === 'C' 
          ? Math.round(((tempVal * 9/5) + 32) * 10) / 10 
          : Math.round(tempVal * 10) / 10;
      }

      // Re-evaluate condition
      let newCondition: 'STABLE' | 'EMERGENCY' | 'CRITICAL' = 'STABLE';
      if (oxygenSaturation < 90 || heartRate > 120 || systolic > 165 || systolic < 85) {
        newCondition = 'CRITICAL';
      } else if (oxygenSaturation < 94 || heartRate > 100 || systolic > 140 || temperature > 100.8) {
        newCondition = 'EMERGENCY';
      }

      // Build vital sign payloads (microservices take individual records)
      const payloads = buildVitalPayloadsList({
        patientIdentifier: selectedPatient.patientIdentifier,
        patientDbId,
        heartRate,
        systolic,
        diastolic,
        temperature,
        oxygenSaturation,
        recordedBy: user?.id || 'nurse-sarah',
        location: selectedPatient.wardNumber || 'General Ward'
      });

      // Submit payloads to API or queue them if offline
      if (!isOnline) {
        queueVitals(payloads);
        setNotification(`Offline: queued ${payloads.length} vital reading${payloads.length > 1 ? 's' : ''} for sync.`);
      } else {
        try {
          await Promise.all(payloads.map((payload) => vitalsApi.createVital(payload)));
          await patientApi.updateCondition(patientDbId, newCondition);
          setNotification(`✓ Vitals logged and synced. Status: ${newCondition}`);
        } catch (apiErr) {
          // If API fails during call, fallback to offline queue
          queueVitals(payloads);
          setNotification('⚠️ Service offline. Vitals queued for synchronization.');
        }
      }

      // Sync local database backup so doctor charts update regardless of online/offline backend
      addVitalSign({
        patientId: patientDbId,
        timestamp: new Date().toISOString(),
        heartRate,
        bloodPressure: { systolic, diastolic },
        temperature,
        oxygenSaturation,
        recordedBy: user?.id || 'nurse-sarah'
      });
      updatePatient(patientDbId, { condition: newCondition });

      // Reset form & reload records
      setVitals(initialVitalsForm);
      await loadPatients();
      await loadVitalsHistory();
    } catch (error) {
      console.error('Unable to save vitals', error);
      setNotification('❌ Error: Unable to save vital signs records.');
    } finally {
      setSubmitting(false);
      window.setTimeout(() => setNotification(''), 4000);
    }
  }

  // Trigger local Handover Report file generation
  async function handleDownloadHandoverReport() {
    setReporting(true);
    try {
      if (!selectedPatient) {
        setNotification('⚠️ Select a patient before generating report.');
        return;
      }
      
      const patientDbId = `patient-${selectedPatient.id}`;
      const dbPatient = mockPatients.find(p => p.id === patientDbId);
      const doc = mockUsers.find(u => u.id === dbPatient?.doctorId);

      let reportContent = '';
      
      // Try to download PDF if online
      if (isOnline) {
        try {
          const response = await reportApi.downloadNurseHandoverReport();
          downloadBlob(response.data, `Clinical_Handover_${selectedPatient.patientIdentifier}.pdf`);
          setNotification('✓ Shift handover report generated and downloaded.');
          setReporting(false);
          return;
        } catch (apiErr) {
          console.warn('API report download failed. Generating plain text handover instead...');
        }
      }

      // Text Report Fallback (Offline Mode)
      reportContent = `
MEDITRACK AI CLINICAL SHIFT HANDOVER REPORT
==========================================
Generated: ${new Date().toLocaleString()}
Staff On Duty: Nurse ${user?.name || 'Staff Nurse'}
Department: ${user?.department || 'Cardiology'} Ward

PATIENT GENERAL RECORD
----------------------
Identifier: ${selectedPatient.patientIdentifier}
Name: ${selectedPatient.firstName} ${selectedPatient.lastName}
Demographics: ${dbPatient?.age || 'N/A'} Yrs / ${dbPatient?.gender || 'N/A'}
Ward Room: ${selectedPatient.wardNumber}-${selectedPatient.bedNumber}
Assigned Attending Clinician: Dr. ${doc?.name || 'Unassigned'}
Current Assessment Status: ${selectedPatient.clinicalStatus}

BEDSIDE VITAL SIGNS CHRONOLOGY
------------------------------
${activePatientVitalsHistory.length === 0 ? 'No vitals signs currently logged on this shift.' : 
  activePatientVitalsHistory.map(v => `[${new Date(v.timestamp).toLocaleTimeString()}]
  - HR: ${v.heartRate} bpm
  - BP: ${v.bloodPressure.systolic}/${v.bloodPressure.diastolic} mmHg
  - Temp: ${v.temperature}°F (${Math.round((v.temperature - 32) * 5/9 * 10)/10}°C)
  - SpO2: ${v.oxygenSaturation}%
  - Recorded by: ${v.recordedBy === user?.id ? 'Self' : v.recordedBy}
  `).join('\n')}

CLINICAL RECOMMENDATION SUMMARY
-------------------------------
General condition is evaluated as ${selectedPatient.clinicalStatus}.
Observations: ${
  selectedPatient.clinicalStatus === 'CRITICAL' 
    ? 'CRITICAL ALERT - Patient telemetry shows abnormal vital limits. Requires active critical monitoring, cardiac telemetry sync, and priority clinical team call.'
    : selectedPatient.clinicalStatus === 'EMERGENCY'
      ? 'EMERGENCY - Elevated clinical warning scores. Perform vitals check every 2 hours. Notify attending physician.'
      : 'STABLE - Roster guidelines. Routine bedside assessments every 4 hours.'
}

--
Validated By: Registered Nurse ${user?.name}
Department Console Sign-off: [ONLINE SYNCED]
      `.trim();
      
      const blob = new Blob([reportContent], { type: 'text/plain' });
      downloadBlob(blob, `Clinical_Handover_${selectedPatient.patientIdentifier}_${new Date().toISOString().split('T')[0]}.txt`);
      
      setNotification('✓ Shift handover report downloaded successfully.');
    } catch (error) {
      console.error('Unable to generate handover report', error);
      setNotification('❌ Unable to compile handover report data.');
    } finally {
      setReporting(false);
      window.setTimeout(() => setNotification(''), 3500);
    }
  }

  // Chat message simulator sending
  const handleSendMessage = () => {
    if (!chatInput.trim()) return;

    const newMsg: ChatMessage = {
      id: `msg-${Date.now()}`,
      sender: 'nurse',
      senderName: user?.name || 'Sarah Johnson',
      text: chatInput,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setChatMessages(prev => ({
      ...prev,
      [chatChannel]: [...prev[chatChannel], newMsg]
    }));
    setChatInput('');

    // Trigger mock doctor reply
    setTimeout(() => {
      const replies = [
        "Acknowledged. Thank you for the live telemetry update.",
        "Understood, Nurse. I am currently in surgery but will review the charts shortly.",
        "Got it, make sure the vital alarms thresholds are correctly adjusted.",
        "Excellent monitoring. Please log a temperature scan in another hour."
      ];
      const randomReply = replies[Math.floor(Math.random() * replies.length)];

      const doctorMsg: ChatMessage = {
        id: `reply-${Date.now()}`,
        sender: 'doctor',
        senderName: chatChannel === 'doctors' ? 'Dr. Dipanshu Sharma' : 'ICU Duty Officer',
        text: randomReply,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };

      setChatMessages(prev => ({
        ...prev,
        [chatChannel]: [...prev[chatChannel], doctorMsg]
      }));
    }, 1800);
  };

  const wardSummary = useMemo(() => {
    return {
      totalPatients: patients.length,
      selectedWard: selectedPatient?.wardNumber || user?.department || 'General Cardiology',
      pendingReview: patients.filter((patient) => patient.clinicalStatus && patient.clinicalStatus !== 'STABLE').length,
    };
  }, [patients, selectedPatient, user?.department]);

  // Shift countdown display builder
  const formattedShiftTime = useMemo(() => {
    const hrs = Math.floor(shiftSecondsRemaining / 3600);
    const mins = Math.floor((shiftSecondsRemaining % 3600) / 60);
    const secs = shiftSecondsRemaining % 60;
    const pad = (num: number) => String(num).padStart(2, '0');
    return `${pad(hrs)}h ${pad(mins)}m ${pad(secs)}s`;
  }, [shiftSecondsRemaining]);

  const offlineQueueCount = useMemo(() => readQueue().length, [notification, isOnline]);

  return (
    <div className="min-h-screen w-full flex bg-dark-950 overflow-hidden relative text-white font-sans selection:bg-primary-500/30">
      {/* Dynamic Background Glows */}
      <div className="absolute top-0 right-0 w-[600px] h-[600px] bg-primary-500/5 rounded-full blur-[130px] pointer-events-none z-0" />
      <div className="absolute bottom-0 left-1/4 w-[600px] h-[600px] bg-secondary-500/5 rounded-full blur-[130px] pointer-events-none z-0" />
      
      <Sidebar onNavClick={setActivePage} activeNav={activePage} />

      <div className="flex-1 flex flex-col h-screen overflow-hidden z-10">
        <TopNavbar onNavClick={setActivePage} activeNav={activePage} />

        <div className="flex-1 overflow-y-auto px-8 pb-8 pt-2 relative">
          
          {/* Toast Notification Container */}
          <AnimatePresence>
            {notification && (
              <motion.div 
                initial={{ opacity: 0, y: -20, x: '-50%' }}
                animate={{ opacity: 1, y: 0, x: '-50%' }}
                exit={{ opacity: 0, y: -20, x: '-50%' }}
                className="fixed top-20 left-1/2 z-50 bg-dark-900/95 border border-primary-500/30 text-primary-200 px-6 py-3.5 rounded-2xl shadow-glow text-sm font-semibold flex items-center gap-3 backdrop-blur-md"
              >
                <div className="w-2 h-2 rounded-full bg-primary-400 animate-ping" />
                {notification}
              </motion.div>
            )}
          </AnimatePresence>

          {/* TAB 1: DASHBOARD CONSOLE */}
          {activePage === 'Dashboard' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
              
              {/* Header Status Bar */}
              <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4 bg-dark-900/20 p-5 rounded-2xl border border-white/5 backdrop-blur-sm">
                <div>
                  <h1 className="text-2xl font-bold text-white tracking-wide flex items-center gap-3">
                    <span className="w-9 h-9 rounded-xl bg-primary-500/10 flex items-center justify-center border border-primary-500/20 text-primary-400">
                      <HeartPulse size={18} />
                    </span>
                    Nursing Bedside Console (API Mode)
                  </h1>
                  <p className="text-xs text-dark-300 mt-1">Record real-time diagnostic vitals, verify telemetry graphs, and compile shift reports.</p>
                </div>
                
                <div className="flex items-center gap-3">
                  {/* Shift timer */}
                  <div className="flex items-center gap-2 px-3 py-1.5 rounded-xl bg-white/5 border border-white/5 text-dark-200 text-xs font-semibold">
                    <Clock size={14} className="text-primary-400" />
                    <span>Shift: {formattedShiftTime}</span>
                  </div>

                  <div className={`flex items-center gap-2 px-4 py-1.5 rounded-xl border text-xs font-bold ${
                    isOnline ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400 shadow-[0_0_15px_rgba(16,185,129,0.05)]' : 'bg-red-500/10 border-red-500/20 text-red-400'
                  }`}>
                    {isOnline ? <Wifi size={14} className="animate-pulse" /> : <WifiOff size={14} />}
                    {isOnline ? 'EHR Live Network Connected' : 'Offline Mode - Save Suspended'}
                  </div>
                </div>
              </div>

              {/* Stats Cards Grid */}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <MetricCard 
                  label="Patients Under Watch" 
                  value={wardSummary.totalPatients} 
                  icon={<Users size={20} />} 
                  trend="All synced to cardiology module"
                  color="primary"
                />
                <MetricCard 
                  label="Critical Ward Alerts" 
                  value={wardSummary.pendingReview} 
                  icon={<AlertCircle size={20} />} 
                  trend={`${wardSummary.pendingReview > 0 ? 'Requires clinical focus' : 'No warnings flagged'}`}
                  color={wardSummary.pendingReview > 0 ? 'error' : 'success'}
                />
                <MetricCard 
                  label="Assigned Telemetry Station" 
                  value={wardSummary.selectedWard} 
                  icon={<Clock size={20} />} 
                  trend="Logged: Nurse Sarah Johnson"
                  color="secondary"
                />
                <MetricCard 
                  label="Offline Vitals Queue" 
                  value={offlineQueueCount} 
                  icon={<FileText size={20} />} 
                  trend={`${offlineQueueCount > 0 ? 'Readings waiting to sync' : 'Sync buffer clear'}`}
                  color={offlineQueueCount > 0 ? 'warning' : 'success'}
                />
              </div>

              {/* Main Workspace (Split Form and Telemetry) */}
              <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                
                {/* Vitals Entry Form Container */}
                <div className="glass-panel p-6 xl:col-span-2 border border-white/5 flex flex-col justify-between">
                  <div>
                    <div className="flex items-center justify-between border-b border-white/5 pb-4 mb-5">
                      <div>
                        <h2 className="text-lg font-bold text-white tracking-wide">Bedside Diagnostic Form</h2>
                        <p className="text-xs text-dark-400 mt-0.5">Logging counts directly update cardiac trending graphs.</p>
                      </div>
                      <button 
                        type="button"
                        onClick={loadPatients} 
                        className="p-2 bg-dark-800 hover:bg-dark-700 text-dark-200 hover:text-white rounded-xl border border-white/5 hover:scale-105 transition-all flex items-center gap-1.5 text-xs font-semibold"
                      >
                        <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
                        Refresh Patients
                      </button>
                    </div>

                    <form onSubmit={handleVitalsSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-5">
                      <div className="md:col-span-2">
                        <label className="text-xs text-dark-300 font-semibold mb-1.5 block">Target Patient Roster</label>
                        <div className="relative">
                          <select
                            required
                            value={selectedPatientId}
                            onChange={(event) => setSelectedPatientId(event.target.value)}
                            className="input-field w-full pl-10 pr-4 py-3 bg-dark-900/60 border border-white/5 rounded-xl text-white focus:border-primary-500/40 focus:ring-1 focus:ring-primary-500/40 outline-none transition-all text-sm"
                          >
                            <option value="">-- Click to choose patient --</option>
                            {patients.map((patient) => (
                              <option key={patient.id} value={patient.id}>
                                {patient.patientIdentifier} - {patient.firstName} {patient.lastName} ({patient.clinicalStatus})
                              </option>
                            ))}
                          </select>
                          <User className="absolute left-3.5 top-3.5 text-dark-400" size={16} />
                        </div>
                      </div>

                      {/* Heart Rate */}
                      <div className="space-y-1">
                        <label className="text-xs text-dark-300 font-semibold mb-1.5 flex items-center gap-2">
                          <Heart size={14} className="text-red-400" />
                          Heart Rate (bpm)
                        </label>
                        <input
                          type="number"
                          min="0"
                          step="1"
                          value={vitals.heartRate}
                          onChange={(event) => setVitals({ ...vitals, heartRate: event.target.value })}
                          className="input-field w-full px-4 py-3 bg-dark-900/60 border border-white/5 rounded-xl text-white text-sm outline-none transition-all placeholder:text-dark-500 focus:border-primary-500/40"
                          placeholder="e.g. 72"
                        />
                      </div>

                      {/* SpO2 */}
                      <div className="space-y-1">
                        <label className="text-xs text-dark-300 font-semibold mb-1.5 flex items-center gap-2">
                          <Wind size={14} className="text-cyan-400" />
                          SpO2 Oxygen (%)
                        </label>
                        <input
                          type="number"
                          min="0"
                          max="100"
                          step="1"
                          value={vitals.spo2}
                          onChange={(event) => setVitals({ ...vitals, spo2: event.target.value })}
                          className="input-field w-full px-4 py-3 bg-dark-900/60 border border-white/5 rounded-xl text-white text-sm outline-none transition-all placeholder:text-dark-500 focus:border-primary-500/40"
                          placeholder="e.g. 98"
                        />
                      </div>

                      {/* Systolic BP */}
                      <div className="space-y-1">
                        <label className="text-xs text-dark-300 font-semibold mb-1.5 flex items-center gap-2">
                          <Activity size={14} className="text-primary-400" />
                          Systolic BP (mmHg)
                        </label>
                        <input
                          type="number"
                          min="0"
                          step="1"
                          value={vitals.systolic}
                          onChange={(event) => setVitals({ ...vitals, systolic: event.target.value })}
                          className="input-field w-full px-4 py-3 bg-dark-900/60 border border-white/5 rounded-xl text-white text-sm outline-none transition-all placeholder:text-dark-500 focus:border-primary-500/40"
                          placeholder="e.g. 120"
                        />
                      </div>

                      {/* Diastolic BP */}
                      <div className="space-y-1">
                        <label className="text-xs text-dark-300 font-semibold mb-1.5 flex items-center gap-2">
                          <Activity size={14} className="text-secondary-400" />
                          Diastolic BP (mmHg)
                        </label>
                        <input
                          type="number"
                          min="0"
                          step="1"
                          value={vitals.diastolic}
                          onChange={(event) => setVitals({ ...vitals, diastolic: event.target.value })}
                          className="input-field w-full px-4 py-3 bg-dark-900/60 border border-white/5 rounded-xl text-white text-sm outline-none transition-all placeholder:text-dark-500 focus:border-primary-500/40"
                          placeholder="e.g. 80"
                        />
                      </div>

                      {/* Temperature with Toggle Unit Context */}
                      <div className="md:col-span-2 space-y-1">
                        <div className="flex justify-between items-center mb-1">
                          <label className="text-xs text-dark-300 font-semibold flex items-center gap-2">
                            <Thermometer size={14} className="text-amber-400" />
                            Bedside Temperature ({tempUnit === 'C' ? '°C' : '°F'})
                          </label>
                          <div className="flex bg-dark-800/80 border border-white/5 rounded-lg p-0.5 text-[10px] font-bold">
                            <button
                              type="button"
                              onClick={() => setTempUnit('C')}
                              className={`px-2 py-0.5 rounded ${tempUnit === 'C' ? 'bg-primary-500 text-white' : 'text-dark-400 hover:text-white'}`}
                            >
                              °C
                            </button>
                            <button
                              type="button"
                              onClick={() => setTempUnit('F')}
                              className={`px-2 py-0.5 rounded ${tempUnit === 'F' ? 'bg-primary-500 text-white' : 'text-dark-400 hover:text-white'}`}
                            >
                              °F
                            </button>
                          </div>
                        </div>
                        <input
                          type="number"
                          step="0.1"
                          min="0"
                          value={vitals.temperature}
                          onChange={(event) => setVitals({ ...vitals, temperature: event.target.value })}
                          className="input-field w-full px-4 py-3 bg-dark-900/60 border border-white/5 rounded-xl text-white text-sm outline-none transition-all placeholder:text-dark-500 focus:border-primary-500/40"
                          placeholder={tempUnit === 'C' ? 'e.g. 37.2' : 'e.g. 98.9'}
                        />
                      </div>

                      <div className="md:col-span-2 flex flex-col sm:flex-row gap-3 pt-3">
                        <button 
                          type="submit" 
                          className="btn-primary flex-1 flex justify-center items-center gap-2 py-3 rounded-xl bg-primary-500 hover:bg-primary-600 text-white font-semibold shadow-glow hover:shadow-glow-lg transition-all" 
                          disabled={submitting}
                        >
                          <Save size={16} />
                          {submitting ? 'Transmitting EHR Records...' : 'Save & Sync Vitals'}
                        </button>
                        <button
                          type="button"
                          onClick={() => setVitals(initialVitalsForm)}
                          className="btn-secondary flex-1 flex justify-center items-center gap-2 py-3 rounded-xl bg-white/5 border border-white/5 text-dark-200 hover:bg-white/10 hover:text-white transition-all"
                        >
                          <RotateCcw size={16} />
                          Reset Form
                        </button>
                      </div>
                    </form>
                  </div>
                </div>

                {/* Interactive Bedside Monitor Panel */}
                <div className="glass-panel p-6 border border-white/5 flex flex-col justify-between bg-dark-900/40">
                  <div className="flex items-center justify-between border-b border-white/5 pb-4 mb-4">
                    <div>
                      <h2 className="text-lg font-bold text-white flex items-center gap-2">
                        <Activity className="text-emerald-400 animate-pulse" size={18} />
                        Telemetry Monitor
                      </h2>
                      <p className="text-xs text-dark-400 mt-0.5">Real-time bedside diagnostics simulator.</p>
                    </div>
                    {selectedPatient && (
                      <span className={`flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[10px] font-bold border border-white/5 animate-pulse ${
                        selectedPatient.clinicalStatus === 'CRITICAL' ? 'bg-red-500/20 text-red-400 border-red-500/30' :
                        selectedPatient.clinicalStatus === 'EMERGENCY' ? 'bg-orange-500/20 text-orange-400 border-orange-500/30' :
                        'bg-emerald-500/20 text-emerald-400 border-emerald-500/30'
                      }`}>
                        {selectedPatient.clinicalStatus}
                      </span>
                    )}
                  </div>

                  {!selectedPatient ? (
                    <div className="flex-1 flex flex-col items-center justify-center py-16 text-center text-dark-400 space-y-3">
                      <div className="w-14 h-14 rounded-full bg-dark-900/80 border border-white/5 flex items-center justify-center text-dark-500 shadow-inner">
                        <Activity size={24} />
                      </div>
                      <div>
                        <p className="font-semibold text-sm">Monitor Awaiting Input</p>
                        <p className="text-xs text-dark-500 max-w-[200px] mt-1">Select a patient on the left to start cardiac telemetry monitoring.</p>
                      </div>
                    </div>
                  ) : (
                    <div className="flex-1 flex flex-col justify-between space-y-4">
                      
                      {/* Patient Details Header */}
                      <div className="flex justify-between items-center bg-black/40 border border-white/5 rounded-xl p-3">
                        <div>
                          <div className="text-[10px] uppercase tracking-wider text-dark-400">Bed Monitor ID</div>
                          <div className="font-bold text-sm text-white mt-0.5">
                            {selectedPatient.firstName} {selectedPatient.lastName}
                          </div>
                        </div>
                        <div className="text-right">
                          <div className="text-[10px] uppercase tracking-wider text-dark-400">Room / Bed</div>
                          <div className="font-mono text-sm text-white mt-0.5">
                            {selectedPatient.wardNumber}-{selectedPatient.bedNumber}
                          </div>
                        </div>
                      </div>

                      {/* Continuous ECG wave drawing (Phosphor animation sweep) */}
                      <div className="relative group rounded-xl overflow-hidden border border-emerald-500/10 shadow-inner">
                        {/* Green Grid and Wave */}
                        <svg viewBox="0 0 300 90" className="w-full h-20 text-emerald-400 bg-black/80 p-1">
                          <defs>
                            <linearGradient id="sweep-gradient-ecg" x1="0%" y1="0%" x2="100%" y2="0%">
                              <stop offset="0%" stopColor="#10b981" stopOpacity="0.1" />
                              <stop offset="85%" stopColor="#10b981" stopOpacity="1" />
                              <stop offset="100%" stopColor="#fff" stopOpacity="1" />
                            </linearGradient>
                            <mask id="sweep-mask-ecg">
                              <rect x="-300" y="0" width="300" height="90" fill="url(#sweep-gradient-ecg)">
                                <animateTransform attributeName="transform" type="translate" from="0,0" to="600,0" dur="2s" repeatCount="indefinite" />
                              </rect>
                            </mask>
                          </defs>
                          <g stroke="rgba(16, 185, 129, 0.06)" strokeWidth="0.5">
                            <line x1="0" y1="18" x2="300" y2="18" />
                            <line x1="0" y1="36" x2="300" y2="36" />
                            <line x1="0" y1="45" x2="300" y2="45" />
                            <line x1="0" y1="54" x2="300" y2="54" />
                            <line x1="0" y1="72" x2="300" y2="72" />
                            <line x1="50" y1="0" x2="50" y2="90" />
                            <line x1="100" y1="0" x2="100" y2="90" />
                            <line x1="150" y1="0" x2="150" y2="90" />
                            <line x1="200" y1="0" x2="200" y2="90" />
                            <line x1="250" y1="0" x2="250" y2="90" />
                          </g>
                          <path 
                            d="M 0 45 L 30 45 L 34 38 L 38 52 L 42 45 L 60 45 L 66 10 L 72 80 L 78 45 L 94 45 L 98 38 L 102 52 L 106 45 L 140 45 L 170 45 L 174 38 L 178 52 L 182 45 L 200 45 L 206 10 L 212 80 L 218 45 L 234 45 L 238 38 L 242 52 M 246 45 L 300 45" 
                            fill="none" 
                            stroke="currentColor" 
                            strokeWidth="2" 
                            strokeLinecap="round" 
                            strokeLinejoin="round" 
                            mask="url(#sweep-mask-ecg)" 
                          />
                        </svg>
                        <span className="absolute bottom-1 right-2 text-[8px] font-mono text-emerald-400/50 uppercase tracking-widest">ECG (Cardio Telemetry)</span>
                      </div>

                      {/* Continuous SpO2 plethysmography wave sweep */}
                      <div className="relative group rounded-xl overflow-hidden border border-cyan-500/10 shadow-inner">
                        <svg viewBox="0 0 300 90" className="w-full h-20 text-cyan-400 bg-black/80 p-1">
                          <defs>
                            <linearGradient id="sweep-gradient-spo2" x1="0%" y1="0%" x2="100%" y2="0%">
                              <stop offset="0%" stopColor="#06b6d4" stopOpacity="0.1" />
                              <stop offset="85%" stopColor="#06b6d4" stopOpacity="1" />
                              <stop offset="100%" stopColor="#fff" stopOpacity="1" />
                            </linearGradient>
                            <mask id="sweep-mask-spo2">
                              <rect x="-300" y="0" width="300" height="90" fill="url(#sweep-gradient-spo2)">
                                <animateTransform attributeName="transform" type="translate" from="0,0" to="600,0" dur="2.8s" repeatCount="indefinite" />
                              </rect>
                            </mask>
                          </defs>
                          <g stroke="rgba(6, 182, 212, 0.06)" strokeWidth="0.5">
                            <line x1="0" y1="18" x2="300" y2="18" />
                            <line x1="0" y1="36" x2="300" y2="36" />
                            <line x1="0" y1="45" x2="300" y2="45" />
                            <line x1="0" y1="54" x2="300" y2="54" />
                            <line x1="0" y1="72" x2="300" y2="72" />
                            <line x1="50" y1="0" x2="50" y2="90" />
                            <line x1="100" y1="0" x2="100" y2="90" />
                            <line x1="150" y1="0" x2="150" y2="90" />
                            <line x1="200" y1="0" x2="200" y2="90" />
                            <line x1="250" y1="0" x2="250" y2="90" />
                          </g>
                          <path 
                            d="M 0 45 C 10 30, 20 30, 30 45 C 35 52, 40 48, 45 45 C 55 30, 65 30, 75 45 C 80 52, 85 48, 90 45 C 100 30, 110 30, 120 45 C 125 52, 130 48, 135 45 C 145 30, 155 30, 165 45 C 170 52, 175 48, 180 45 C 190 30, 200 30, 210 45 C 215 52, 220 48, 225 45 C 235 30, 245 30, 255 45 C 260 52, 265 48, 270 45 C 280 30, 290 30, 300 45" 
                            fill="none" 
                            stroke="currentColor" 
                            strokeWidth="2" 
                            strokeLinecap="round" 
                            strokeLinejoin="round" 
                            mask="url(#sweep-mask-spo2)" 
                          />
                        </svg>
                        <span className="absolute bottom-1 right-2 text-[8px] font-mono text-cyan-400/50 uppercase tracking-widest">Plethysmograph (SpO2)</span>
                      </div>

                      {/* Numerical Telemetry Readouts */}
                      <div className="grid grid-cols-2 gap-3 text-white">
                        
                        {/* Heart Rate Display */}
                        <div className="bg-black/30 border border-white/5 rounded-xl p-3 flex justify-between items-center">
                          <div>
                            <span className="text-[10px] text-dark-400 font-bold uppercase tracking-wider block">Heart Rate</span>
                            <span className="text-2xl font-extrabold tracking-tight mt-1 block font-mono">
                              {latestVitals?.heartRate || 72}
                            </span>
                            <span className="text-[9px] text-dark-500 uppercase block">BPM</span>
                          </div>
                          <Heart 
                            size={20} 
                            className={`text-red-500 ${telemetryPulse ? 'scale-110 opacity-100' : 'scale-90 opacity-60'} transition-all duration-300`} 
                          />
                        </div>

                        {/* SpO2 Oxygen saturation display */}
                        <div className="bg-black/30 border border-white/5 rounded-xl p-3 flex justify-between items-center">
                          <div>
                            <span className="text-[10px] text-dark-400 font-bold uppercase tracking-wider block">Pulse O2</span>
                            <span className="text-2xl font-extrabold tracking-tight mt-1 block font-mono">
                              {latestVitals?.oxygenSaturation || 98}
                            </span>
                            <span className="text-[9px] text-dark-500 uppercase block">% Sat</span>
                          </div>
                          <Wind size={20} className="text-cyan-400" />
                        </div>

                        {/* Blood pressure display */}
                        <div className="bg-black/30 border border-white/5 rounded-xl p-3 flex justify-between items-center">
                          <div>
                            <span className="text-[10px] text-dark-400 font-bold uppercase tracking-wider block">Blood Pres</span>
                            <span className="text-xl font-extrabold tracking-tight mt-1.5 block font-mono">
                              {latestVitals?.bloodPressure.systolic || 120}/{latestVitals?.bloodPressure.diastolic || 80}
                            </span>
                            <span className="text-[9px] text-dark-500 uppercase block">mmHg NIBP</span>
                          </div>
                          <Activity size={20} className="text-primary-400" />
                        </div>

                        {/* Temperature display with fallback */}
                        <div className="bg-black/30 border border-white/5 rounded-xl p-3 flex justify-between items-center">
                          <div>
                            <span className="text-[10px] text-dark-400 font-bold uppercase tracking-wider block">Bed Temp</span>
                            <span className="text-xl font-extrabold tracking-tight mt-1.5 block font-mono">
                              {latestVitals?.temperature 
                                ? `${Math.round((latestVitals.temperature - 32) * 5/9 * 10)/10}°C` 
                                : '37.0°C'}
                            </span>
                            <span className="text-[9px] text-dark-500 uppercase block">
                              {latestVitals?.temperature ? `${latestVitals.temperature}°F` : '98.6°F'}
                            </span>
                          </div>
                          <Thermometer size={20} className="text-amber-400" />
                        </div>
                      </div>

                      {/* Clinical evaluation alert banner */}
                      {selectedPatient.clinicalStatus && selectedPatient.clinicalStatus !== 'STABLE' ? (
                        <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-2.5 rounded-xl text-center flex items-center justify-center gap-2 text-xs font-semibold animate-pulse">
                          <AlertTriangle size={14} />
                          WARNING: PATIENT STATUS OUT-OF-BOUNDS
                        </div>
                      ) : (
                        <div className="bg-emerald-500/5 border border-emerald-500/10 text-emerald-400 p-2.5 rounded-xl text-center flex items-center justify-center gap-2 text-xs font-semibold">
                          <CheckCircle2 size={14} />
                          WARD TELEMETRY SYNCED STABLE
                        </div>
                      )}
                    </div>
                  )}
                </div>
              </div>
            </motion.div>
          )}

          {/* TAB 2: PATIENTS LIST */}
          {activePage === 'Patients' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
              
              <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-dark-900/20 p-5 border border-white/5 rounded-2xl">
                <div>
                  <h1 className="text-2xl font-bold text-white tracking-wide flex items-center gap-3">
                    <span className="w-9 h-9 rounded-xl bg-primary-500/10 flex items-center justify-center border border-primary-500/20 text-primary-400">
                      <Users size={18} />
                    </span>
                    Ward Patient Directory
                  </h1>
                  <p className="text-xs text-dark-300 mt-1">Review the medical status indicators and log records for your assigned patients.</p>
                </div>
                <button
                  onClick={loadPatients}
                  className="px-4 py-2 bg-dark-800 hover:bg-dark-700 text-white rounded-xl border border-white/5 text-xs font-semibold flex items-center gap-2 transition-all"
                >
                  <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
                  Reload Directory
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
                {patients.map((patient) => {
                  const selected = String(patient.id) === selectedPatientId;
                  const pastVitals = getVitalSignsByPatient(`patient-${patient.id}`);
                  const latest = pastVitals.length > 0 ? pastVitals[pastVitals.length - 1] : null;

                  return (
                    <div
                      key={patient.id}
                      className={`glass-panel p-5 space-y-4 border transition-all duration-300 relative group overflow-hidden ${
                        selected 
                          ? 'border-primary-500/40 bg-primary-500/10 shadow-glow' 
                          : 'border-white/5 bg-dark-900/30 hover:border-white/10 hover:bg-dark-900/50'
                      }`}
                    >
                      {/* Avatar & Condition */}
                      <div className="flex justify-between items-start">
                        <div className="flex items-center gap-3">
                          <div className="w-11 h-11 rounded-full bg-dark-800 border border-white/5 flex items-center justify-center font-bold text-sm text-primary-400">
                            {patient.firstName[0]}{patient.lastName[0]}
                          </div>
                          <div>
                            <h3 className="font-bold text-white group-hover:text-primary-300 transition-colors">
                              {patient.firstName} {patient.lastName}
                            </h3>
                            <p className="text-xs text-dark-400 mt-0.5">{patient.patientIdentifier} • Room {patient.wardNumber}-{patient.bedNumber}</p>
                          </div>
                        </div>

                        <span className={`text-[10px] px-2.5 py-0.5 rounded-full font-bold border ${
                          patient.clinicalStatus === 'CRITICAL' ? 'bg-red-500/25 border-red-500/30 text-red-300 animate-pulse' :
                          patient.clinicalStatus === 'EMERGENCY' ? 'bg-orange-500/25 border-orange-500/30 text-orange-300' :
                          'bg-emerald-500/25 border-emerald-500/30 text-emerald-300'
                        }`}>
                          {patient.clinicalStatus || 'STABLE'}
                        </span>
                      </div>

                      {/* Vital status bars summary */}
                      <div className="grid grid-cols-2 gap-2.5 bg-black/20 border border-white/5 rounded-xl p-3 text-xs">
                        <div>
                          <span className="text-dark-500 uppercase tracking-wider text-[9px] block">Heart Rate</span>
                          <span className="font-semibold text-white mt-0.5 block">
                            {latest?.heartRate ? `${latest.heartRate} bpm` : 'N/A'}
                          </span>
                        </div>
                        <div>
                          <span className="text-dark-500 uppercase tracking-wider text-[9px] block">SpO2 Oxygen</span>
                          <span className="font-semibold text-white mt-0.5 block">
                            {latest?.oxygenSaturation ? `${latest.oxygenSaturation}%` : 'N/A'}
                          </span>
                        </div>
                        <div className="col-span-2 border-t border-white/5 pt-2 mt-1.5 flex justify-between items-center text-[10px] text-dark-300">
                          <span>Doctor: Dr. {patient.assignedClinicianName?.split(' ')[1] || 'Staff'}</span>
                          <span className="font-mono text-[9px] text-dark-400">
                            Last check: {latest?.timestamp ? new Date(latest.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'}) : 'None'}
                          </span>
                        </div>
                      </div>

                      {/* Quick Action Button row */}
                      <div className="flex gap-2 pt-1">
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedPatientId(String(patient.id));
                            setActivePage('Dashboard');
                          }}
                          className="flex-1 py-2 rounded-xl bg-primary-500/10 hover:bg-primary-500 text-primary-300 hover:text-white border border-primary-500/20 text-xs font-semibold flex items-center justify-center gap-1.5 transition-all"
                        >
                          <Plus size={14} />
                          Log Bedside Vitals
                        </button>
                        <button
                          type="button"
                          onClick={() => {
                            if (patient.mobileNumber) {
                              window.open(`tel:${patient.mobileNumber}`, '_self');
                            } else {
                              setNotification('⚠️ Mobile contact unavailable for patient.');
                              setTimeout(() => setNotification(''), 3000);
                            }
                          }}
                          className="p-2 rounded-xl bg-dark-800 hover:bg-dark-700 text-dark-200 border border-white/5 hover:scale-105 transition-all"
                          title="Call Attending/Guardian"
                        >
                          <Phone size={14} />
                        </button>
                      </div>
                    </div>
                  );
                })}

                {!loading && patients.length === 0 && (
                  <div className="glass-panel p-8 text-center text-dark-400 col-span-full border border-white/5">
                    No active patients currently assigned to your ward roster account.
                  </div>
                )}
              </div>
            </motion.div>
          )}

          {/* TAB 3: REPORTS SHIFT HANDOVER */}
          {activePage === 'Reports' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
              
              <div className="bg-dark-900/20 p-5 border border-white/5 rounded-2xl">
                <h1 className="text-2xl font-bold text-white tracking-wide flex items-center gap-3">
                  <span className="w-9 h-9 rounded-xl bg-primary-500/10 flex items-center justify-center border border-primary-500/20 text-primary-400">
                    <FileText size={18} />
                  </span>
                  Clinical Handover Console
                </h1>
                <p className="text-xs text-dark-300 mt-1">Compile and print direct handover records for nurse station shifts changes.</p>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                
                {/* Operations column */}
                <div className="glass-panel p-6 border border-white/5 h-fit space-y-5">
                  <div className="w-12 h-12 rounded-xl bg-primary-500/10 border border-primary-500/20 flex items-center justify-center text-primary-400 shadow-glow">
                    <FileText size={22} />
                  </div>
                  <div>
                    <h2 className="text-base font-bold text-white">Generate Handover File</h2>
                    <p className="text-xs text-dark-400 mt-1">Select a patient on the console to format their chronological chart logs.</p>
                  </div>

                  <div className="space-y-3 pt-2">
                    <label className="text-xs text-dark-300 font-semibold block">Select Roster Patient</label>
                    <select
                      value={selectedPatientId}
                      onChange={(event) => setSelectedPatientId(event.target.value)}
                      className="input-field w-full px-3 py-2.5 bg-dark-900/60 border border-white/5 rounded-xl text-white text-xs outline-none focus:border-primary-500/40"
                    >
                      <option value="">-- Choose Patient --</option>
                      {patients.map((patient) => (
                        <option key={patient.id} value={patient.id}>
                          {patient.patientIdentifier} - {patient.firstName} {patient.lastName}
                        </option>
                      ))}
                    </select>
                  </div>

                  <button
                    onClick={handleDownloadHandoverReport}
                    className="w-full btn-primary py-3 rounded-xl bg-primary-500 hover:bg-primary-600 text-white font-semibold text-xs flex items-center justify-center gap-2 shadow-glow transition-all"
                    disabled={reporting || !selectedPatient}
                  >
                    <Download size={14} />
                    {reporting ? 'Compiling Report...' : 'Download Shift Handover Report'}
                  </button>
                  {!selectedPatient && (
                    <span className="text-[10px] text-error-400 text-center block">⚠️ Select a patient to enable compilation button.</span>
                  )}
                </div>

                {/* Report Preview */}
                <div className="glass-panel p-6 border border-white/5 lg:col-span-2 flex flex-col justify-between">
                  <div className="border-b border-white/5 pb-3.5 mb-4 flex justify-between items-center">
                    <h3 className="text-sm font-bold text-white uppercase tracking-wider">Clinical Document Preview</h3>
                    <span className="text-[10px] text-dark-500 font-mono">FORMAT: UTF-8 PLAIN TEXT</span>
                  </div>

                  {selectedPatient ? (
                    <div className="flex-1 bg-black/60 border border-white/5 rounded-xl p-4 font-mono text-xs text-emerald-400 leading-relaxed overflow-x-auto max-h-[380px] overflow-y-auto">
                      <pre className="whitespace-pre-wrap">
{`MEDITRACK AI CLINICAL SHIFT HANDOVER REPORT
==========================================
Generated: ${new Date().toLocaleDateString()}
Staff On Duty: Nurse Sarah Johnson
Department: Cardiology Ward

PATIENT GENERAL RECORD
----------------------
Identifier: ${selectedPatient.patientIdentifier}
Name: ${selectedPatient.firstName} ${selectedPatient.lastName}
Ward Room: ${selectedPatient.wardNumber}-${selectedPatient.bedNumber}
Current Assessment Status: ${selectedPatient.clinicalStatus}

BEDSIDE VITAL SIGNS CHRONOLOGY
------------------------------
${activePatientVitalsHistory.map(v => `[${new Date(v.timestamp).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}] HR: ${v.heartRate} | BP: ${v.bloodPressure.systolic}/${v.bloodPressure.diastolic} | Temp: ${v.temperature}°F | SpO2: ${v.oxygenSaturation}%`).join('\n') || 'No vitals logged yet.'}

CLINICAL RECOMMENDATION SUMMARY
-------------------------------
General condition is evaluated as ${selectedPatient.clinicalStatus}.
Care instructions are dynamically updated on clinical telemetry panel.`}
                      </pre>
                    </div>
                  ) : (
                    <div className="flex-1 py-20 text-center text-dark-400 flex flex-col items-center justify-center">
                      <FileText size={32} className="text-dark-500 mb-2" />
                      <p className="text-sm font-semibold">Preview Awaiting Data Selection</p>
                      <p className="text-xs text-dark-500 mt-1">Select a patient on the left to review their live handover chronology sheet.</p>
                    </div>
                  )}
                </div>
              </div>
            </motion.div>
          )}

          {/* TAB 4: CALENDAR / ROSTER SHIFT TIMELINE */}
          {activePage === 'Calendar' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
              
              <div className="bg-dark-900/20 p-5 border border-white/5 rounded-2xl">
                <h1 className="text-2xl font-bold text-white tracking-wide flex items-center gap-3">
                  <span className="w-9 h-9 rounded-xl bg-primary-500/10 flex items-center justify-center border border-primary-500/20 text-primary-400">
                    <CalendarIcon size={18} />
                  </span>
                  Shift Planner & Roster
                </h1>
                <p className="text-xs text-dark-300 mt-1">Verify your ward assignations and live clinical scheduling logs.</p>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                
                {/* Active countdown tracker */}
                <div className="glass-panel p-6 border border-white/5 flex flex-col justify-between h-fit space-y-6">
                  <div>
                    <span className="text-[10px] uppercase font-bold tracking-widest text-primary-400 block mb-1">Live Shift Watch</span>
                    <h2 className="text-xl font-extrabold text-white tracking-tight">Active Duty Timer</h2>
                    <p className="text-xs text-dark-400 mt-1">Count down until shift handover checks lock.</p>
                  </div>

                  <div className="py-6 text-center bg-black/35 border border-white/5 rounded-2xl my-2 font-mono">
                    <span className="text-3xl font-bold text-primary-400 tracking-wider block">{formattedShiftTime}</span>
                    <span className="text-[9px] uppercase tracking-widest text-dark-500 mt-1.5 block">Hours remaining</span>
                  </div>

                  <div className="space-y-2.5 text-xs text-dark-300 border-t border-white/5 pt-4">
                    <div className="flex justify-between">
                      <span>Shift Type</span>
                      <span className="font-semibold text-white">Cardiology Morning Duty</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Total Time</span>
                      <span className="font-semibold text-white">08:00 AM - 04:00 PM</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Break Allocated</span>
                      <span className="font-semibold text-white">45 Mins</span>
                    </div>
                  </div>
                </div>

                {/* Calendar Roster grid */}
                <div className="glass-panel p-6 border border-white/5 lg:col-span-2 space-y-4">
                  <h3 className="text-sm font-bold text-white uppercase tracking-wider border-b border-white/5 pb-3">Upcoming Ward Assignments</h3>
                  
                  <div className="space-y-3.5">
                    <div className="flex items-center justify-between p-4 bg-primary-500/5 hover:bg-primary-500/10 border border-primary-500/20 rounded-xl transition-all">
                      <div className="space-y-1">
                        <span className="text-xs font-bold text-primary-300 block">Morning Shift - General Ward</span>
                        <span className="text-[10px] text-dark-300">Role: Primary Ward Coordinator | Location: Floor 2B</span>
                      </div>
                      <span className="text-xs font-semibold font-mono text-primary-200">Today • 8AM-4PM</span>
                    </div>

                    <div className="flex items-center justify-between p-4 bg-secondary-500/5 hover:bg-secondary-500/10 border border-secondary-500/20 rounded-xl transition-all">
                      <div className="space-y-1">
                        <span className="text-xs font-bold text-secondary-300 block">Night Shift - ICU Sub-Desk</span>
                        <span className="text-[10px] text-dark-300">Role: Emergency Critical Monitor | Location: Floor 1 ICU</span>
                      </div>
                      <span className="text-xs font-semibold font-mono text-secondary-200">Tomorrow • 10PM-6AM</span>
                    </div>

                    <div className="flex items-center justify-between p-4 bg-dark-900/50 hover:bg-dark-800/80 border border-white/5 rounded-xl transition-all">
                      <div className="space-y-1">
                        <span className="text-xs font-bold text-dark-200 block">Morning Shift - General Pediatrics</span>
                        <span className="text-[10px] text-dark-300">Role: Ward Cover Duty | Location: Floor 3A</span>
                      </div>
                      <span className="text-xs font-semibold font-mono text-dark-300">Wednesday • 8AM-4PM</span>
                    </div>
                  </div>
                </div>
              </div>
            </motion.div>
          )}

          {/* TAB 5: INTERNAL CHAT CONSOLE */}
          {activePage === 'Chat' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
              
              <div className="bg-dark-900/20 p-5 border border-white/5 rounded-2xl">
                <h1 className="text-2xl font-bold text-white tracking-wide flex items-center gap-3">
                  <span className="w-9 h-9 rounded-xl bg-primary-500/10 flex items-center justify-center border border-primary-500/20 text-primary-400">
                    <MessageSquare size={18} />
                  </span>
                  Clinical Chat Console
                </h1>
                <p className="text-xs text-dark-300 mt-1">Secure communication channel with attending physicians and ICU telemetry coordinators.</p>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 bg-dark-900/20 border border-white/5 rounded-2xl overflow-hidden h-[460px] backdrop-blur-sm">
                
                {/* Chat Channels Sidebar */}
                <div className="border-r border-white/5 bg-dark-950/40 p-4 flex flex-col gap-3">
                  <span className="text-[9px] uppercase tracking-widest font-extrabold text-dark-500 px-2">Secure Channels</span>
                  
                  <button
                    onClick={() => setChatChannel('doctors')}
                    className={`text-left p-3 rounded-xl flex items-center justify-between text-xs transition-all ${
                      chatChannel === 'doctors' ? 'bg-primary-500/10 text-primary-300 border border-primary-500/20 font-bold' : 'text-dark-300 hover:bg-white/5 border border-transparent'
                    }`}
                  >
                    <span>Attending Physicians</span>
                    <span className="w-2 h-2 rounded-full bg-primary-400" />
                  </button>

                  <button
                    onClick={() => setChatChannel('icu')}
                    className={`text-left p-3 rounded-xl flex items-center justify-between text-xs transition-all ${
                      chatChannel === 'icu' ? 'bg-primary-500/10 text-primary-300 border border-primary-500/20 font-bold' : 'text-dark-300 hover:bg-white/5 border border-transparent'
                    }`}
                  >
                    <span>ICU Telemetry Desk</span>
                    <span className="w-2 h-2 rounded-full bg-secondary-400" />
                  </button>

                  <button
                    onClick={() => setChatChannel('ward')}
                    className={`text-left p-3 rounded-xl flex items-center justify-between text-xs transition-all ${
                      chatChannel === 'ward' ? 'bg-primary-500/10 text-primary-300 border border-primary-500/20 font-bold' : 'text-dark-300 hover:bg-white/5 border border-transparent'
                    }`}
                  >
                    <span>Ward desk channel</span>
                  </button>
                </div>

                {/* Messages stream */}
                <div className="lg:col-span-3 flex flex-col justify-between h-full bg-black/15">
                  {/* Title Bar */}
                  <div className="px-5 py-3 border-b border-white/5 flex items-center justify-between bg-dark-950/20 text-xs">
                    <span className="font-bold text-white">
                      Active: {chatChannel === 'doctors' ? 'Attending Doctor Consult Team' : chatChannel === 'icu' ? 'ICU Telemetry Desk Monitor' : 'Ward Desk General Channel'}
                    </span>
                    <span className="text-[10px] text-dark-400 flex items-center gap-1.5">
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping" />
                      EHR Tunnel Encrypted
                    </span>
                  </div>

                  {/* Messages container */}
                  <div className="flex-1 p-5 overflow-y-auto space-y-4 text-xs">
                    {chatMessages[chatChannel]?.map((msg) => {
                      const isSelf = msg.sender === 'nurse';
                      const isSys = msg.sender === 'system';
                      
                      if (isSys) {
                        return (
                          <div key={msg.id} className="w-full text-center text-dark-500 py-1 font-mono text-[10px]">
                            {msg.timestamp} - {msg.text}
                          </div>
                        );
                      }

                      return (
                        <div key={msg.id} className={`flex flex-col max-w-[70%] ${isSelf ? 'ml-auto items-end' : 'mr-auto items-start'}`}>
                          <div className="flex items-center gap-2 mb-1.5 text-[10px] text-dark-400 font-bold">
                            <span>{msg.senderName}</span>
                            <span>•</span>
                            <span>{msg.timestamp}</span>
                          </div>
                          <div className={`p-3 rounded-2xl border ${
                            isSelf 
                              ? 'bg-primary-500/10 border-primary-500/30 text-primary-100 rounded-tr-none' 
                              : 'bg-dark-900/80 border-white/5 text-dark-200 rounded-tl-none'
                          }`}>
                            {msg.text}
                          </div>
                        </div>
                      );
                    })}
                  </div>

                  {/* Input form */}
                  <div className="p-4 border-t border-white/5 bg-dark-950/20 flex gap-2">
                    <input
                      type="text"
                      value={chatInput}
                      onChange={(e) => setChatInput(e.target.value)}
                      onKeyDown={(e) => { if (e.key === 'Enter') handleSendMessage(); }}
                      placeholder={`Enter message on secure channel...`}
                      className="flex-1 bg-dark-900 border border-white/5 rounded-xl px-4 py-2.5 outline-none focus:border-primary-500/40 text-xs text-white"
                    />
                    <button
                      onClick={handleSendMessage}
                      className="p-3 bg-primary-500 hover:bg-primary-600 rounded-xl text-white hover:scale-105 transition-all shadow-glow flex items-center justify-center"
                    >
                      <Send size={14} />
                    </button>
                  </div>
                </div>
              </div>
            </motion.div>
          )}

          {/* TAB 6: SETTINGS & PREFERENCES */}
          {activePage === 'Settings' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6 max-w-4xl">
              
              <div className="bg-dark-900/20 p-5 border border-white/5 rounded-2xl">
                <h1 className="text-2xl font-bold text-white tracking-wide flex items-center gap-3">
                  <span className="w-9 h-9 rounded-xl bg-primary-500/10 flex items-center justify-center border border-primary-500/20 text-primary-400">
                    <SettingsIcon size={18} />
                  </span>
                  Nursing Console Preferences
                </h1>
                <p className="text-xs text-dark-300 mt-1">Configure layout units, alerts, sound triggers and local telemetry telemetry.</p>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                
                {/* Profile detail card */}
                <div className="glass-panel p-6 border border-white/5 flex flex-col items-center text-center space-y-4">
                  <div className="w-20 h-20 rounded-full bg-dark-800 overflow-hidden border-2 border-primary-500/20 p-1 relative shadow-inner">
                    <img src={user?.avatar || '/avatar1.png'} alt="Nurse Profile" className="w-full h-full object-cover rounded-full" />
                  </div>
                  
                  <div>
                    <h2 className="font-extrabold text-lg text-white">{user?.name || 'Nurse Sarah Johnson'}</h2>
                    <span className="text-xs text-primary-400 font-semibold uppercase tracking-widest mt-0.5 block">Registered Nurse</span>
                  </div>

                  <div className="w-full space-y-2 text-xs text-dark-300 border-t border-white/5 pt-4 text-left">
                    <div className="flex justify-between">
                      <span className="text-dark-500">Facility ID</span>
                      <span className="font-mono text-white">{user?.id || 'nurse-sarah'}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-dark-500">Department</span>
                      <span className="font-semibold text-white">{user?.department || 'Cardiology'} Ward</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-dark-500">Access Role</span>
                      <span className="font-semibold text-emerald-400 uppercase">Clinician Level 2</span>
                    </div>
                  </div>
                </div>

                {/* Settings list form */}
                <div className="glass-panel p-6 border border-white/5 lg:col-span-2 space-y-6">
                  
                  {/* Preferences Header */}
                  <div className="flex items-center gap-2 text-white border-b border-white/5 pb-3">
                    <Sliders size={16} className="text-primary-400" />
                    <h3 className="text-sm font-bold uppercase tracking-wider">Console Config</h3>
                  </div>

                  <div className="space-y-5">
                    {/* Temperature Units selection */}
                    <div className="flex justify-between items-center bg-black/10 border border-white/5 rounded-xl p-4">
                      <div>
                        <span className="text-xs font-bold text-white block">Temperature Scale Unit</span>
                        <span className="text-[10px] text-dark-400">Controls Celsius input conversions during vitals submission.</span>
                      </div>
                      <div className="flex bg-dark-800 border border-white/5 rounded-lg p-0.5 text-xs font-bold">
                        <button
                          type="button"
                          onClick={() => setTempUnit('C')}
                          className={`px-3 py-1 rounded-md transition-all ${tempUnit === 'C' ? 'bg-primary-500 text-white shadow-glow' : 'text-dark-400 hover:text-white'}`}
                        >
                          Celsius
                        </button>
                        <button
                          type="button"
                          onClick={() => setTempUnit('F')}
                          className={`px-3 py-1 rounded-md transition-all ${tempUnit === 'F' ? 'bg-primary-500 text-white shadow-glow' : 'text-dark-400 hover:text-white'}`}
                        >
                          Fahrenheit
                        </button>
                      </div>
                    </div>

                    {/* Alarms audio simulation */}
                    <div className="flex justify-between items-center bg-black/10 border border-white/5 rounded-xl p-4">
                      <div>
                        <span className="text-xs font-bold text-white block">Audible Alarms Simulation</span>
                        <span className="text-[10px] text-dark-400 font-medium">Flash alert banners and sound tones on out-of-bound vitals.</span>
                      </div>
                      <button
                        type="button"
                        onClick={() => setSoundAlerts(!soundAlerts)}
                        className={`p-2 rounded-xl transition-all border ${
                          soundAlerts ? 'bg-primary-500/10 border-primary-500/30 text-primary-300' : 'bg-dark-800 border-white/5 text-dark-400'
                        }`}
                      >
                        {soundAlerts ? <Volume2 size={18} /> : <VolumeX size={18} />}
                      </button>
                    </div>

                    {/* Automatic refresh intervals */}
                    <div className="flex justify-between items-center bg-black/10 border border-white/5 rounded-xl p-4">
                      <div>
                        <span className="text-xs font-bold text-white block">Telemetry Auto-Refresh</span>
                        <span className="text-[10px] text-dark-400">Manage directory loading cycles to prevent network overheads.</span>
                      </div>
                      <div className="flex items-center gap-3.5">
                        <label className="relative flex items-center cursor-pointer">
                          <input 
                            type="checkbox" 
                            checked={autoRefresh} 
                            onChange={(e) => setAutoRefresh(e.target.checked)} 
                            className="sr-only peer" 
                          />
                          <div className="w-9 h-5 bg-dark-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-dark-400 after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-primary-500 peer-checked:after:bg-white" />
                        </label>

                        {autoRefresh && (
                          <select
                            value={refreshRate}
                            onChange={(e) => setRefreshRate(e.target.value)}
                            className="bg-dark-800 border border-white/5 rounded-lg px-2 py-1 text-xs text-white outline-none"
                          >
                            <option value="3000">3 Seconds</option>
                            <option value="5000">5 Seconds</option>
                            <option value="10000">10 Seconds</option>
                          </select>
                        )}
                      </div>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => {
                      setNotification('✓ Preference settings updated locally.');
                      setTimeout(() => setNotification(''), 3000);
                    }}
                    className="w-full py-3 bg-primary-500 hover:bg-primary-600 text-white font-bold rounded-xl text-xs shadow-glow transition-all"
                  >
                    Commit Changes
                  </button>
                </div>
              </div>
            </motion.div>
          )}

        </div>
      </div>
    </div>
  );
}

// Sub-component for premium MetricCards
function MetricCard({ 
  label, 
  value, 
  icon, 
  trend, 
  color = 'primary' 
}: { 
  label: string; 
  value: number | string; 
  icon?: React.ReactNode; 
  trend?: string; 
  color?: 'primary' | 'success' | 'warning' | 'error' | 'secondary' 
}) {
  const borderColors = {
    primary: 'border-primary-500/20 hover:border-primary-500/40',
    success: 'border-success-500/20 hover:border-success-500/40',
    warning: 'border-warning-500/20 hover:border-warning-500/40',
    error: 'border-error-500/20 hover:border-error-500/40',
    secondary: 'border-secondary-500/20 hover:border-secondary-500/40',
  };
  
  const bgColors = {
    primary: 'bg-primary-500/5',
    success: 'bg-success-500/5',
    warning: 'bg-warning-500/5',
    error: 'bg-error-500/5',
    secondary: 'bg-secondary-500/5',
  };

  const textColors = {
    primary: 'text-primary-400',
    success: 'text-success-400',
    warning: 'text-warning-400',
    error: 'text-error-400',
    secondary: 'text-secondary-400',
  };

  return (
    <div className={`glass-panel p-6 border ${borderColors[color]} ${bgColors[color]} relative overflow-hidden group transition-all duration-300 hover:scale-[1.01] shadow-card z-10`}>
      <div className="flex justify-between items-start">
        <div>
          <span className="text-xs font-semibold uppercase tracking-wider text-dark-400 block mb-1">{label}</span>
          <span className="text-3xl font-extrabold text-white tracking-tight">{value}</span>
        </div>
        {icon && (
          <div className={`p-3 rounded-xl bg-dark-900/80 border border-white/5 ${textColors[color]} group-hover:scale-110 transition-transform shadow-inner`}>
            {icon}
          </div>
        )}
      </div>
      {trend && (
        <div className="mt-4 flex items-center gap-1.5 text-[10px] text-dark-300">
          <span className={textColors[color]}>●</span>
          <span>{trend}</span>
        </div>
      )}
    </div>
  );
}

// Build vital sign payloads (groups by type for API)
function buildVitalPayloadsList({
  patientIdentifier,
  patientDbId,
  heartRate,
  systolic,
  diastolic,
  temperature,
  oxygenSaturation,
  recordedBy,
  location
}: {
  patientIdentifier: string;
  patientDbId: string;
  heartRate: number;
  systolic: number;
  diastolic: number;
  temperature: number;
  oxygenSaturation: number;
  recordedBy: string;
  location: string;
}): QueuedVitalPayload[] {
  const common = {
    patientIdentifier,
    patientId: patientDbId,
    readingTimestamp: new Date().toISOString(),
    source: 'MANUAL',
    location,
    notes: `Recorded by ${recordedBy}`,
    qualityScore: 0.95,
  };

  const payloads: QueuedVitalPayload[] = [];

  if (heartRate) {
    payloads.push({
      ...common,
      vitalType: 'HEART_RATE',
      value: heartRate,
      unit: 'bpm',
    });
  }

  if (systolic && diastolic) {
    payloads.push({
      ...common,
      vitalType: 'BLOOD_PRESSURE',
      value: systolic,
      systolic,
      diastolic,
      unit: 'mmHg',
    });
  }

  if (oxygenSaturation) {
    payloads.push({
      ...common,
      vitalType: 'SPO2',
      value: oxygenSaturation,
      unit: '%',
    });
  }

  if (temperature) {
    payloads.push({
      ...common,
      vitalType: 'TEMPERATURE',
      value: temperature,
      unit: 'F',
    });
  }

  return payloads;
}

// Offline queue helpers
function readQueue(): QueuedVitalPayload[] {
  try {
    const raw = localStorage.getItem(OFFLINE_QUEUE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.warn('Unable to read offline queue', error);
    return [];
  }
}

function writeQueue(payloads: QueuedVitalPayload[]) {
  localStorage.setItem(OFFLINE_QUEUE_KEY, JSON.stringify(payloads));
}

function queueVitals(payloads: QueuedVitalPayload[]) {
  const queue = readQueue();
  writeQueue([...queue, ...payloads]);
}

async function flushQueuedVitals(announce = false) {
  if (!navigator.onLine) return 0;
  const queue = readQueue();
  if (queue.length === 0) return 0;

  try {
    await Promise.all(queue.map((payload) => vitalsApi.createVital(payload)));
    writeQueue([]);
    if (announce) {
      window.dispatchEvent(new CustomEvent('meditrack:notification', { detail: `Synced ${queue.length} queued vital readings.` }));
    }
    return queue.length;
  } catch (error) {
    console.error('Unable to flush queued vitals', error);
    return 0;
  }
}

function extractCollection<T>(value: any): T[] {
  if (Array.isArray(value)) return value;
  if (value && Array.isArray(value.content)) return value.content;
  return [];
}

function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}
