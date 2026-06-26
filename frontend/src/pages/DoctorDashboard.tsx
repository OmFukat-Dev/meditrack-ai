import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  BarChart,
  Bar,
} from 'recharts';
import {
  Download,
  FileText,
  HeartPulse,
  RefreshCw,
  Users,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import Sidebar from '../components/Sidebar';
import TopNavbar from '../components/TopNavbar';
import { patientApi, reportApi, vitalsApi } from '../services/api';

type PatientRecord = {
  id: number;
  patientIdentifier: string;
  firstName: string;
  lastName: string;
  department?: string | null;
  bedNumber?: string | null;
  clinicalStatus?: string | null;
  assignedClinicianName?: string | null;
  assignedClinicianEmail?: string | null;
};

type VitalReading = {
  id: number;
  vitalType: string;
  value?: number | string | null;
  unit?: string | null;
  systolic?: number | string | null;
  diastolic?: number | string | null;
  readingTimestamp: string;
  displayValue?: string | null;
  vitalStatus?: string | null;
  qualityScore?: number | string | null;
};

type SummaryVital = {
  vitalType: string;
  displayValue?: string;
  vitalStatus?: string;
  readingTimestamp?: string;
  qualityScore?: number | string;
};

type ChartPoint = {
  time: string;
  heartRate?: number;
  temperature?: number;
  spo2?: number;
  respiratoryRate?: number;
  systolic?: number;
  diastolic?: number;
};

export default function DoctorDashboard() {
  const { user } = useAuth();
  const [activePage, setActivePage] = useState('Dashboard');
  const [patients, setPatients] = useState<PatientRecord[]>([]);
  const [selectedPatientId, setSelectedPatientId] = useState<string>('');
  const [readings, setReadings] = useState<VitalReading[]>([]);
  const [latestVitals, setLatestVitals] = useState<SummaryVital[]>([]);
  const [loading, setLoading] = useState(true);
  const [patientLoading, setPatientLoading] = useState(false);
  const [reporting, setReporting] = useState(false);
  const [notification, setNotification] = useState('');

  useEffect(() => {
    void loadPatients();
  }, []);

  useEffect(() => {
    if (selectedPatientId) {
      void loadVitals(selectedPatientId);
    }
  }, [selectedPatientId]);

  const selectedPatient = useMemo(
    () => patients.find((patient) => String(patient.id) === selectedPatientId) || null,
    [patients, selectedPatientId]
  );

  async function loadPatients() {
    setLoading(true);
    try {
      const response = await patientApi.getAll(0, 100);
      const content = extractCollection<PatientRecord>(response.data);
      setPatients(content);
      if (content.length > 0) {
        setSelectedPatientId(String(content[0].id));
      }
    } catch (error) {
      console.error('Unable to load patients', error);
      setPatients([]);
    } finally {
      setLoading(false);
    }
  }

  async function loadVitals(patientId: string) {
    setPatientLoading(true);
    try {
      const [readingResponse, summaryResponse] = await Promise.all([
        vitalsApi.getPatientReadings(patientId, { page: 0, size: 50 }),
        vitalsApi.getSummary(patientId),
      ]);

      setReadings(extractCollection<VitalReading>(readingResponse.data));
      setLatestVitals(extractSummary(summaryResponse.data));
    } catch (error) {
      console.error('Unable to load patient vitals', error);
      setReadings([]);
      setLatestVitals([]);
    } finally {
      setPatientLoading(false);
    }
  }

  async function handleDownloadShiftReport() {
    setReporting(true);
    try {
      const response = await reportApi.downloadDoctorShiftReport();
      downloadBlob(response.data, `doctor_shift_report_${user?.name || 'meditrack'}.pdf`);
      setNotification('Doctor shift report generated.');
    } catch (error) {
      console.error('Report generation failed', error);
      setNotification('Unable to generate the shift report.');
    } finally {
      setReporting(false);
      window.setTimeout(() => setNotification(''), 3000);
    }
  }

  async function handleDownloadPatientReport(patient: PatientRecord | null) {
    if (!patient) {
      return;
    }

    try {
      const response = await reportApi.downloadPatientReport(String(patient.id));
      downloadBlob(response.data, `patient_report_${patient.patientIdentifier}.pdf`);
      setNotification(`Patient report generated for ${patient.patientIdentifier}.`);
    } catch (error) {
      console.error('Patient report generation failed', error);
      setNotification('Unable to generate the patient report.');
    } finally {
      window.setTimeout(() => setNotification(''), 3000);
    }
  }

  const chartData = useMemo(() => buildChartData(readings), [readings]);
  const latestSummary = useMemo(() => buildLatestSummary(latestVitals, readings), [latestVitals, readings]);
  const criticalCount = useMemo(
    () => readings.filter((reading) => reading.vitalStatus && reading.vitalStatus !== 'NORMAL').length,
    [readings]
  );
  const abnormalCount = useMemo(
    () => readings.filter((reading) => reading.vitalStatus === 'HIGH' || reading.vitalStatus === 'LOW').length,
    [readings]
  );
  const bloodPressureData = useMemo(() => buildBloodPressureChart(readings), [readings]);

  return (
    <div className="min-h-screen w-full flex bg-dark-950 overflow-hidden relative">
      <div className="absolute top-0 right-0 w-[800px] h-[800px] bg-primary-500/5 rounded-full blur-[120px] pointer-events-none" />
      <Sidebar onNavClick={setActivePage} />

      <div className="flex-1 flex flex-col h-screen overflow-hidden z-10">
        <TopNavbar onNavClick={setActivePage} />

        <div className="flex-1 overflow-y-auto px-8 pb-8 pt-2 relative">
          <AnimateNotifications message={notification} />

          {activePage === 'Dashboard' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-8">
              <div className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-4">
                <div>
                  <h1 className="text-3xl font-bold text-white mb-2 flex items-center gap-3">
                    <HeartPulse className="text-primary-400" /> Doctor Dashboard
                  </h1>
                  <p className="text-dark-300">
                    Clinical view for {user?.department || 'your department'} with real vital readings logged by nursing staff.
                  </p>
                </div>
                <div className="flex flex-wrap gap-3">
                  <button onClick={loadPatients} className="btn-secondary">
                    <RefreshCw size={16} />
                    Refresh Patients
                  </button>
                  <button onClick={handleDownloadShiftReport} className="btn-primary" disabled={reporting}>
                    <Download size={16} />
                    {reporting ? 'Generating...' : 'Generate Shift Report'}
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
                <MetricCard label="Accessible Patients" value={patients.length} />
                <MetricCard label="Critical Readings" value={criticalCount} />
                <MetricCard label="Abnormal Readings" value={abnormalCount} />
                <MetricCard label="Charts Loaded" value={chartData.length} />
              </div>

              <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                <div className="glass-panel p-6 xl:col-span-1">
                  <div className="flex items-center justify-between mb-4">
                    <h2 className="text-lg font-bold text-white">Assigned Patients</h2>
                    <span className="text-xs text-dark-400">{selectedPatient ? selectedPatient.patientIdentifier : 'No selection'}</span>
                  </div>
                  <div className="space-y-3 max-h-[540px] overflow-y-auto pr-1">
                    {patients.map((patient) => {
                      const selected = String(patient.id) === selectedPatientId;
                      return (
                        <button
                          key={patient.id}
                          onClick={() => setSelectedPatientId(String(patient.id))}
                          className={`w-full text-left rounded-2xl border p-4 transition-all ${
                            selected ? 'bg-primary-500/10 border-primary-500/30' : 'bg-dark-900/60 border-white/5 hover:border-white/10'
                          }`}
                        >
                          <div className="flex items-center justify-between gap-3">
                            <div>
                              <div className="font-semibold text-white">
                                {patient.firstName} {patient.lastName}
                              </div>
                              <div className="text-xs text-dark-400 mt-1">
                                {patient.department || 'General'} | Bed {patient.bedNumber || 'N/A'}
                              </div>
                            </div>
                            <span className={`text-xs px-3 py-1 rounded-full ${patient.clinicalStatus ? 'bg-warning-500/10 text-warning-300' : 'bg-dark-800 text-dark-300'}`}>
                              {patient.clinicalStatus || 'Stable'}
                            </span>
                          </div>
                        </button>
                      );
                    })}
                    {!loading && patients.length === 0 && (
                      <div className="rounded-2xl border border-dashed border-white/10 p-6 text-center text-dark-400">
                        No accessible patients yet.
                      </div>
                    )}
                  </div>
                </div>

                <div className="glass-panel p-6 xl:col-span-2">
                  <div className="flex items-center justify-between mb-4">
                    <div>
                      <h2 className="text-lg font-bold text-white">
                        {selectedPatient ? `${selectedPatient.firstName} ${selectedPatient.lastName}` : 'Vitals Overview'}
                      </h2>
                      <p className="text-sm text-dark-400">
                        {selectedPatient
                          ? `${selectedPatient.department || 'General'} - ${selectedPatient.patientIdentifier}`
                          : 'Select a patient to inspect the last 50 readings.'}
                      </p>
                    </div>
                    {selectedPatient && (
                      <button
                        onClick={() => handleDownloadPatientReport(selectedPatient)}
                        className="btn-secondary px-4 py-2 text-sm"
                      >
                        <FileText size={16} />
                        Patient Report
                      </button>
                    )}
                  </div>

                  {patientLoading ? (
                    <div className="h-[420px] flex items-center justify-center text-dark-400">
                      Loading readings...
                    </div>
                  ) : (
                    <>
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                        {latestSummary.slice(0, 3).map((item) => (
                          <div key={item.vitalType} className="bg-dark-900/70 border border-white/5 rounded-2xl p-4">
                            <div className="text-xs uppercase tracking-[0.2em] text-dark-400 mb-2">{item.vitalType.replace('_', ' ')}</div>
                            <div className="text-2xl font-bold text-white">{item.displayValue || '-'}</div>
                            <div className={`text-xs mt-2 ${statusTone(item.vitalStatus)}`}>{item.vitalStatus || 'UNKNOWN'}</div>
                          </div>
                        ))}
                      </div>

                      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                        <div className="bg-dark-900/60 border border-white/5 rounded-3xl p-4">
                          <div className="flex items-center justify-between mb-4">
                            <h3 className="text-base font-semibold text-white">Vital Trends</h3>
                            <span className="text-xs text-dark-400">Generated from nurse readings</span>
                          </div>
                          <div className="h-[360px]">
                            <ResponsiveContainer width="100%" height="100%">
                              <LineChart data={chartData}>
                                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
                                <XAxis dataKey="time" stroke="#64748b" tick={{ fontSize: 11 }} />
                                <YAxis stroke="#64748b" tick={{ fontSize: 11 }} />
                                <Tooltip contentStyle={{ background: '#0f172a', border: '1px solid rgba(255,255,255,0.08)' }} />
                                <Legend />
                                <Line type="monotone" dataKey="heartRate" stroke="#0ea5e9" strokeWidth={2} dot={false} />
                                <Line type="monotone" dataKey="spo2" stroke="#22c55e" strokeWidth={2} dot={false} />
                                <Line type="monotone" dataKey="temperature" stroke="#f97316" strokeWidth={2} dot={false} />
                                <Line type="monotone" dataKey="respiratoryRate" stroke="#a855f7" strokeWidth={2} dot={false} />
                              </LineChart>
                            </ResponsiveContainer>
                          </div>
                        </div>

                        <div className="bg-dark-900/60 border border-white/5 rounded-3xl p-4">
                          <div className="flex items-center justify-between mb-4">
                            <h3 className="text-base font-semibold text-white">Blood Pressure</h3>
                            <span className="text-xs text-dark-400">Systolic and diastolic trend</span>
                          </div>
                          <div className="h-[360px]">
                            <ResponsiveContainer width="100%" height="100%">
                              <BarChart data={bloodPressureData}>
                                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
                                <XAxis dataKey="time" stroke="#64748b" tick={{ fontSize: 11 }} />
                                <YAxis stroke="#64748b" tick={{ fontSize: 11 }} />
                                <Tooltip contentStyle={{ background: '#0f172a', border: '1px solid rgba(255,255,255,0.08)' }} />
                                <Legend />
                                <Bar dataKey="systolic" fill="#38bdf8" radius={[6, 6, 0, 0]} />
                                <Bar dataKey="diastolic" fill="#60a5fa" radius={[6, 6, 0, 0]} />
                              </BarChart>
                            </ResponsiveContainer>
                          </div>
                        </div>
                      </div>
                    </>
                  )}
                </div>
              </div>
            </motion.div>
          )}

          {activePage === 'Patients' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
              <div>
                <h1 className="text-3xl font-bold text-white mb-2 flex items-center gap-3">
                  <Users className="text-primary-400" /> Assigned Patients
                </h1>
                <p className="text-dark-300">Only patients allocated to your account or department are visible here.</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
                {patients.map((patient) => (
                  <div key={patient.id} className="glass-card p-5 space-y-3">
                    <div className="flex items-start justify-between gap-3">
                      <div>
                        <h3 className="text-lg font-semibold text-white">{patient.firstName} {patient.lastName}</h3>
                        <p className="text-sm text-dark-400">{patient.patientIdentifier}</p>
                      </div>
                      <span className="text-xs px-3 py-1 rounded-full bg-success-500/10 text-success-300">{patient.clinicalStatus || 'Stable'}</span>
                    </div>
                    <div className="text-sm text-dark-300 space-y-1">
                      <div>Department: {patient.department || 'General'}</div>
                      <div>Bed: {patient.bedNumber || 'N/A'}</div>
                      <div>Assigned clinician: {patient.assignedClinicianName || 'Unassigned'}</div>
                    </div>
                    <div className="flex flex-wrap gap-2 pt-2">
                      <button onClick={() => setSelectedPatientId(String(patient.id))} className="btn-secondary px-4 py-2 text-sm">
                        View Vitals
                      </button>
                      <button onClick={() => handleDownloadPatientReport(patient)} className="btn-primary px-4 py-2 text-sm">
                        <Download size={14} />
                        Report
                      </button>
                    </div>
                  </div>
                ))}
                {!loading && patients.length === 0 && (
                  <div className="glass-panel p-6 text-dark-400">No patients available.</div>
                )}
              </div>
            </motion.div>
          )}

          {activePage === 'Reports' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6 max-w-3xl">
              <div>
                <h1 className="text-3xl font-bold text-white mb-2 flex items-center gap-3">
                  <FileText className="text-primary-400" /> Reports
                </h1>
                <p className="text-dark-300">Generate shift summaries and patient exports from actual readings.</p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="glass-panel p-6 space-y-4">
                  <h3 className="text-xl font-bold text-white">Shift Report</h3>
                  <p className="text-sm text-dark-300">
                    Generates a doctor shift report using the assigned patient list and the latest vitals captured by nurses.
                  </p>
                  <button onClick={handleDownloadShiftReport} className="btn-primary w-full">
                    <Download size={16} />
                    {reporting ? 'Generating...' : 'Generate Shift Report'}
                  </button>
                </div>

                <div className="glass-panel p-6 space-y-4">
                  <h3 className="text-xl font-bold text-white">Patient Export</h3>
                  <p className="text-sm text-dark-300">
                    Download a clinical PDF for the currently selected patient.
                  </p>
                  <button onClick={() => handleDownloadPatientReport(selectedPatient)} className="btn-secondary w-full">
                    <FileText size={16} />
                    Download Patient Report
                  </button>
                </div>
              </div>
            </motion.div>
          )}

          {activePage === 'Chat' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="glass-panel p-8 max-w-3xl">
              <h1 className="text-2xl font-bold mb-2">Internal Communications</h1>
              <p className="text-dark-300 mb-6">Use the call menu in the header to reach ward desk, admin, or the on-call team quickly.</p>
              <div className="rounded-2xl border border-dashed border-white/10 p-8 text-center text-dark-400">
                Chat integration can be wired here later without changing the access flow.
              </div>
            </motion.div>
          )}

          {activePage === 'Settings' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="glass-panel p-8 max-w-3xl space-y-4">
              <h1 className="text-2xl font-bold">Settings</h1>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-dark-300">
                <div>
                  <div className="text-xs uppercase tracking-[0.2em] text-dark-500">Signed in as</div>
                  <div className="text-white font-medium mt-1">{user?.name}</div>
                </div>
                <div>
                  <div className="text-xs uppercase tracking-[0.2em] text-dark-500">Department</div>
                  <div className="text-white font-medium mt-1">{user?.department || 'General'}</div>
                </div>
              </div>
            </motion.div>
          )}
        </div>
      </div>
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="glass-card p-5">
      <div className="text-dark-400 text-sm">{label}</div>
      <div className="text-2xl font-bold text-white mt-2">{value}</div>
    </div>
  );
}

function AnimateNotifications({ message }: { message: string }) {
  return message ? (
    <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 bg-success-500/20 border border-success-500/40 text-success-300 px-5 py-3 rounded-full shadow-lg">
      {message}
    </div>
  ) : null;
}

function extractCollection<T>(value: any): T[] {
  if (Array.isArray(value)) {
    return value;
  }
  if (value && Array.isArray(value.content)) {
    return value.content;
  }
  return [];
}

function extractSummary(value: any): SummaryVital[] {
  if (value && Array.isArray(value.latestVitals)) {
    return value.latestVitals as SummaryVital[];
  }
  return [];
}

function buildChartData(readings: VitalReading[]): ChartPoint[] {
  const buckets = new Map<string, ChartPoint>();

  const sorted = [...readings].sort((left, right) => {
    return new Date(left.readingTimestamp).getTime() - new Date(right.readingTimestamp).getTime();
  });

  for (const reading of sorted) {
    const timeKey = new Date(reading.readingTimestamp).toISOString().slice(0, 16);
    const label = new Date(reading.readingTimestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const point = buckets.get(timeKey) ?? { time: label };
    const value = toNumber(reading.value);

    switch (reading.vitalType) {
      case 'HEART_RATE':
        point.heartRate = value;
        break;
      case 'TEMPERATURE':
        point.temperature = value;
        break;
      case 'SPO2':
        point.spo2 = value;
        break;
      case 'RESPIRATORY_RATE':
        point.respiratoryRate = value;
        break;
      default:
        break;
    }

    buckets.set(timeKey, point);
  }

  return Array.from(buckets.values()).slice(-12);
}

function buildBloodPressureChart(readings: VitalReading[]): ChartPoint[] {
  const buckets = new Map<string, ChartPoint>();
  const sorted = [...readings].filter((reading) => reading.vitalType === 'BLOOD_PRESSURE').sort((left, right) => {
    return new Date(left.readingTimestamp).getTime() - new Date(right.readingTimestamp).getTime();
  });

  for (const reading of sorted) {
    const timeKey = new Date(reading.readingTimestamp).toISOString().slice(0, 16);
    const label = new Date(reading.readingTimestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const point = buckets.get(timeKey) ?? { time: label };
    point.systolic = toNumber(reading.systolic ?? reading.value);
    point.diastolic = toNumber(reading.diastolic ?? reading.value);
    buckets.set(timeKey, point);
  }

  return Array.from(buckets.values()).slice(-12);
}

function buildLatestSummary(latestVitals: SummaryVital[], readings: VitalReading[]): SummaryVital[] {
  if (latestVitals.length > 0) {
    return latestVitals;
  }

  const latestByType = new Map<string, SummaryVital>();
  const sorted = [...readings].sort((left, right) => {
    return new Date(right.readingTimestamp).getTime() - new Date(left.readingTimestamp).getTime();
  });

  for (const reading of sorted) {
    if (!latestByType.has(reading.vitalType)) {
      latestByType.set(reading.vitalType, {
        vitalType: reading.vitalType,
        displayValue: reading.displayValue || formatVitalDisplay(reading),
        vitalStatus: reading.vitalStatus || 'UNKNOWN',
        readingTimestamp: reading.readingTimestamp,
        qualityScore: reading.qualityScore || undefined,
      });
    }
  }

  return Array.from(latestByType.values());
}

function formatVitalDisplay(reading: VitalReading) {
  if (reading.vitalType === 'BLOOD_PRESSURE') {
    const systolic = reading.systolic ?? reading.value;
    const diastolic = reading.diastolic ?? reading.value;
    return `${systolic}/${diastolic} ${reading.unit || 'mmHg'}`;
  }

  return `${reading.value ?? '-'} ${reading.unit || ''}`.trim();
}

function statusTone(status?: string) {
  switch ((status || '').toUpperCase()) {
    case 'HIGH':
    case 'CRITICAL':
      return 'text-error-300';
    case 'LOW':
      return 'text-warning-300';
    case 'NORMAL':
      return 'text-success-300';
    default:
      return 'text-dark-300';
  }
}

function toNumber(value: unknown) {
  if (typeof value === 'number') {
    return value;
  }
  if (typeof value === 'string') {
    const parsed = Number(value);
    return Number.isNaN(parsed) ? undefined : parsed;
  }
  return undefined;
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
