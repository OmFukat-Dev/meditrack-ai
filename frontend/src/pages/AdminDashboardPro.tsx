import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react';
import { motion } from 'framer-motion';
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import {
  Activity,
  AlertTriangle,
  ArrowRight,
  Bell,
  Brain,
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  Download,
  FileText,
  HeartPulse,
  LayoutDashboard,
  LogOut,
  PencilLine,
  Plus,
  RefreshCw,
  Search,
  ShieldAlert,
  Sparkles,
  Stethoscope,
  Trash2,
  TrendingUp,
  UserRound,
  Users,
  UsersRound,
  DollarSign,
  Gauge,
  TimerReset,
  Hospital,
  MonitorSmartphone,
} from 'lucide-react';
import QuickCallMenu from '../components/QuickCallMenu';
import { useAuth } from '../context/AuthContext';
import { reportApi, staffApi } from '../services/api';
import { addNewPatient, mockPatients, mockUsers, updatePatient } from '../database/mockDatabase';
import { generateMedicalReport, mockPatients as seedPatients, mockUsers as seedUsers } from '../database/mockDatabaseFromSeed';

type SectionKey = 'overview' | 'alerts' | 'staff' | 'patients' | 'intelligence' | 'operations';
type StaffTab = 'doctors' | 'nurses';
type OperationsTab = 'billing' | 'shifts' | 'audit' | 'notifications' | 'system' | 'reports';
type AlertStatus = 'ACTIVE' | 'ACKNOWLEDGED' | 'ESCALATED';
type AlertSeverity = 'HIGH' | 'CRITICAL';
type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

type StaffRecord = {
  id: string | number;
  sourceId?: string;
  fullName: string;
  email: string;
  role: 'admin' | 'doctor' | 'nurse';
  department: string | null;
  specialization: string | null;
  phoneNumber?: string | null;
  licenseNumber?: string | null;
  active?: boolean;
  createdAt?: string;
  updatedAt?: string;
};

type RawPatient = (typeof mockPatients)[number];

type PatientSnapshot = {
  id: string;
  patientIdentifier: string;
  fullName: string;
  firstName: string;
  lastName: string;
  email: string;
  age: number;
  gender: string;
  roomNumber: string;
  condition: string;
  department: string;
  riskScore: number;
  riskLevel: RiskLevel;
  assignedDoctorId: string;
  assignedDoctorName: string;
  assignedNurseId: string;
  assignedNurseName: string;
  admittedDate: string;
  lastVitalsUpdate: string;
  mobileNumber: string;
  guardianName: string;
  guardianMobile: string;
  latestVitals: {
    heartRate: string;
    bloodPressure: string;
    temperature: string;
    spo2: string;
    respiratoryRate: string;
  };
  timeline: TimelineEvent[];
};

type TimelineEvent = {
  id: string;
  timestamp: string;
  label: string;
  tone: 'neutral' | 'warning' | 'critical';
};

type AlertItem = {
  id: string;
  patientId: string;
  patientName: string;
  patientIdentifier: string;
  department: string;
  severity: AlertSeverity;
  riskScore: number;
  status: AlertStatus;
  timestamp: string;
  assignedDoctor: string;
  summary: string;
};

type AuditEntry = {
  id: string;
  actor: string;
  action: string;
  target: string;
  timestamp: string;
};

type NotificationEntry = {
  id: string;
  title: string;
  detail: string;
  tone: 'info' | 'warning' | 'critical';
  timestamp: string;
};

type BillingEntry = {
  id: string;
  patientName: string;
  patientIdentifier: string;
  insurance: string;
  total: number;
  paid: number;
  outstanding: number;
  status: 'Pending' | 'Partial' | 'Paid';
};

type ShiftAssignment = {
  start: string;
  end: string;
};

const sections: Array<{ id: SectionKey; label: string; icon: ReactNode }> = [
  { id: 'overview', label: 'Overview', icon: <LayoutDashboard size={16} /> },
  { id: 'alerts', label: 'Alerts', icon: <AlertTriangle size={16} /> },
  { id: 'staff', label: 'Staff', icon: <UsersRound size={16} /> },
  { id: 'patients', label: 'Patients', icon: <UserRound size={16} /> },
  { id: 'intelligence', label: 'Intelligence', icon: <Brain size={16} /> },
  { id: 'operations', label: 'Operations', icon: <ClipboardList size={16} /> },
];

const operationsTabs: Array<{ id: OperationsTab; label: string }> = [
  { id: 'billing', label: 'Billing' },
  { id: 'shifts', label: 'Shifts' },
  { id: 'audit', label: 'Audit Logs' },
  { id: 'notifications', label: 'Notifications' },
  { id: 'system', label: 'System Health' },
  { id: 'reports', label: 'Reports' },
];

const staffDepartments = ['Cardiology', 'Neurology', 'Orthopedics', 'Pediatrics', 'Oncology', 'ICU', 'Emergency', 'General'];
const riskFilters: Array<'all' | RiskLevel> = ['all', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const severityFilters: Array<'all' | AlertSeverity> = ['all', 'HIGH', 'CRITICAL'];

const todayString = new Date().toISOString().slice(0, 10);

export default function AdminDashboardPro() {
  const { user, logout } = useAuth();
  const [activeSection, setActiveSection] = useState<SectionKey>('overview');
  const [activeStaffTab, setActiveStaffTab] = useState<StaffTab>('doctors');
  const [activeOperationsTab, setActiveOperationsTab] = useState<OperationsTab>('billing');
  const [searchQuery, setSearchQuery] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('all');
  const [riskFilter, setRiskFilter] = useState<'all' | RiskLevel>('all');
  const [severityFilter, setSeverityFilter] = useState<'all' | AlertSeverity>('all');
  const [loading, setLoading] = useState(true);
  const [staffMembers, setStaffMembers] = useState<StaffRecord[]>([]);
  const [rawPatients, setRawPatients] = useState<RawPatient[]>(() => [...seedPatients] as unknown as RawPatient[]);
  const [alertState, setAlertState] = useState<AlertItem[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditEntry[]>(buildSeedAuditLogs());
  const [notifications, setNotifications] = useState<NotificationEntry[]>(buildSeedNotifications());
  const [shiftAssignments, setShiftAssignments] = useState<Record<string, ShiftAssignment>>({});
  const [selectedPatientId, setSelectedPatientId] = useState<string>('');
  const [selectedAlertId, setSelectedAlertId] = useState<string>('');
  const [toast, setToast] = useState('');
  const [showStaffModal, setShowStaffModal] = useState(false);
  const [showPatientModal, setShowPatientModal] = useState(false);
  const [editingStaff, setEditingStaff] = useState<StaffRecord | null>(null);
  const [editingPatient, setEditingPatient] = useState<RawPatient | null>(null);
  const [savingStaff, setSavingStaff] = useState(false);
  const [savingPatient, setSavingPatient] = useState(false);
  const [notificationDraft, setNotificationDraft] = useState('');
  const [notificationAudience, setNotificationAudience] = useState<'all' | 'doctors' | 'department'>('all');
  const [notificationDepartment, setNotificationDepartment] = useState('Cardiology');
  const [patientForm, setPatientForm] = useState<PatientFormState>(createEmptyPatientForm());
  const [staffForm, setStaffForm] = useState<StaffFormState>(createEmptyStaffForm('doctor'));
  const roster = staffMembers.length > 0 ? staffMembers : buildFallbackRoster();
  const patients = useMemo(() => rawPatients.map((patient) => buildPatientSnapshot(patient, roster)), [rawPatients, roster]);

  const pushNotification = (message: string, _tone: NotificationEntry['tone']) => {
    setToast(message);
    window.setTimeout(() => setToast(''), 3000);
  };

  const appendAuditLog = (action: string, target: string) => {
    setAuditLogs((current) => [
      {
        id: `audit-${Date.now()}`,
        actor: user?.name ?? 'Admin',
        action,
        target,
        timestamp: new Date().toISOString(), // exact ISO timestamp
      },
      ...current,
    ].slice(0, 25));
  };

  useEffect(() => {
    void loadStaff();
  }, []);

  useEffect(() => {
    if (!selectedPatientId && patients.length > 0) {
      setSelectedPatientId(patients[0].id);
    } else if (selectedPatientId && !patients.some((patient) => patient.id === selectedPatientId)) {
      setSelectedPatientId(patients[0]?.id ?? '');
    }
  }, [selectedPatientId, patients]);

  useEffect(() => {
    setShiftAssignments((current) => {
      const next = { ...current };
      staffMembers.forEach((staff, index) => {
        const key = String(staff.id);
        if (!next[key]) {
          next[key] = defaultShiftForStaff(staff, index);
        }
      });
      return next;
    });
  }, [staffMembers]);

  const alerts = useMemo(() => mergeAlertState(alertState, buildAlerts(patients)), [alertState, patients]);
  const selectedPatient = useMemo(
    () => patients.find((patient) => patient.id === selectedPatientId) ?? patients[0] ?? null,
    [patients, selectedPatientId],
  );
  const selectedAlert = useMemo(
    () => alerts.find((alert) => alert.id === selectedAlertId) ?? alerts[0] ?? null,
    [alerts, selectedAlertId],
  );

  const filteredPatients = useMemo(
    () =>
      patients.filter((patient) => {
        const matchesSearch = matchesText(
          `${patient.fullName} ${patient.patientIdentifier} ${patient.department} ${patient.assignedDoctorName} ${patient.condition}`,
          searchQuery,
        );
        const matchesDepartment = departmentFilter === 'all' || patient.department === departmentFilter;
        const matchesRisk = riskFilter === 'all' || patient.riskLevel === riskFilter;
        return matchesSearch && matchesDepartment && matchesRisk;
      }),
    [departmentFilter, patients, riskFilter, searchQuery],
  );

  const filteredAlerts = useMemo(
    () =>
      alerts.filter((alert) => {
        const matchesSearch = matchesText(`${alert.patientName} ${alert.patientIdentifier} ${alert.department} ${alert.assignedDoctor}`, searchQuery);
        const matchesDepartment = departmentFilter === 'all' || alert.department === departmentFilter;
        const matchesSeverity = severityFilter === 'all' || alert.severity === severityFilter;
        return matchesSearch && matchesDepartment && matchesSeverity;
      }),
    [alerts, departmentFilter, searchQuery, severityFilter],
  );

  const filteredDoctors = useMemo(
    () => buildDoctorRows(roster, patients, shiftAssignments).filter((doctor) => matchesText(`${doctor.name} ${doctor.department} ${doctor.shift}`, searchQuery) && (departmentFilter === 'all' || doctor.department === departmentFilter)),
    [departmentFilter, patients, roster, searchQuery, shiftAssignments],
  );
  const filteredNurses = useMemo(
    () => buildNurseRows(roster, patients, shiftAssignments).filter((nurse) => matchesText(`${nurse.name} ${nurse.department} ${nurse.shift}`, searchQuery) && (departmentFilter === 'all' || nurse.department === departmentFilter)),
    [departmentFilter, patients, roster, searchQuery, shiftAssignments],
  );

  const departmentCards = useMemo(() => buildDepartmentCards(patients, roster), [patients, roster]);
  const workloadRows = useMemo(() => buildDoctorWorkloadRows(roster, patients, shiftAssignments), [patients, roster, shiftAssignments]);
  const vitalsFeed = useMemo(() => buildVitalsFeed(patients), [patients]);
  const aiTrendData = useMemo(() => buildAiTrendData(patients), [patients]);
  const billingRows = useMemo(() => buildBillingRows(patients), [patients]);
  const systemHealth = useMemo(() => buildSystemHealth(), []);
  const selectedTimeline = selectedPatient?.timeline ?? [];
  const totalOutstanding = billingRows.reduce((sum, row) => sum + row.outstanding, 0);

  const overviewStats = useMemo(() => {
    const totalPatients = patients.length;
    const totalDoctors = roster.filter((staff) => staff.role === 'doctor' && staff.active !== false).length;
    const totalNurses = roster.filter((staff) => staff.role === 'nurse' && staff.active !== false).length;
    const activeDepartments = new Set([...patients.map((patient) => patient.department), ...roster.map((staff) => staff.department ?? 'General')]).size;
    const criticalPatients = patients.filter((patient) => patient.riskLevel === 'CRITICAL').length;
    const activeAlerts = alerts.filter((alert) => alert.status === 'ACTIVE').length;
    const icuRiskPatients = patients.filter((patient) => patient.riskScore >= 80).length;
    const observationPatients = patients.filter((patient) => patient.riskScore >= 45 && patient.riskScore < 80).length;
    return {
      totalPatients,
      totalDoctors,
      totalNurses,
      activeDepartments,
      criticalPatients,
      activeAlerts,
      icuRiskPatients,
      observationPatients,
      admissionsToday: Math.max(3, Math.round(totalPatients * 0.35)),
      dischargesToday: Math.max(1, Math.round(totalPatients * 0.12)),
      emergencyCases: criticalPatients + Math.max(1, Math.round(totalPatients * 0.1)),
      averageOccupancy: Math.min(98, 58 + totalPatients * 4),
      predictionAccuracy: 94.5,
    };
  }, [alerts, patients, roster]);

  async function loadStaff() {
    setLoading(true);
    try {
      const response = await staffApi.getAll();
      const records = Array.isArray(response.data) && response.data.length > 0 ? (response.data as StaffRecord[]) : buildFallbackRoster();
      setStaffMembers(records);
    } catch (error) {
      console.error('Unable to load staff registry', error);
      setStaffMembers(buildFallbackRoster());
    } finally {
      setLoading(false);
    }
  }

  function reloadPatients() {
    setRawPatients([...seedPatients] as unknown as RawPatient[]);
    pushNotification('Local patient registry refreshed.', 'info');
  }

  function openStaffModal(tab: StaffTab, staff?: StaffRecord) {
    setActiveStaffTab(tab);
    if (staff) {
      setEditingStaff(staff);
      setStaffForm({
        role: tab === 'doctors' ? 'doctor' : 'nurse',
        fullName: staff.fullName,
        email: staff.email,
        department: staff.department || 'Cardiology',
        specialization: staff.specialization || '',
        phoneNumber: staff.phoneNumber || '',
        licenseNumber: staff.licenseNumber || '',
        active: staff.active !== false,
        shiftStart: getShiftAssignment(String(staff.id), shiftAssignments, roster).start,
        shiftEnd: getShiftAssignment(String(staff.id), shiftAssignments, roster).end,
      });
    } else {
      setEditingStaff(null);
      setStaffForm(createEmptyStaffForm(tab === 'doctors' ? 'doctor' : 'nurse'));
    }
    setShowStaffModal(true);
  }

  function openPatientModal(patient?: RawPatient) {
    if (patient) {
      setEditingPatient(patient);
      setPatientForm({
        name: patient.name,
        email: patient.email,
        password: patient.password,
        age: String(patient.age),
        gender: patient.gender,
        roomNumber: patient.roomNumber,
        condition: patient.condition,
        admittedDate: patient.admittedDate,
        doctorId: patient.doctorId,
        nurseId: patient.nurseId,
        mobileNumber: patient.mobileNumber,
        guardianName: patient.guardianName,
        guardianMobile: patient.guardianMobile,
      });
    } else {
      setEditingPatient(null);
      setPatientForm(createEmptyPatientForm());
    }
    setShowPatientModal(true);
  }

  async function handleStaffSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingStaff(true);
    const role = staffForm.role;
    const payload = {
      fullName: staffForm.fullName.trim(),
      email: staffForm.email.trim(),
      role,
      department: staffForm.department.trim(),
      specialization: staffForm.specialization.trim(),
      phoneNumber: staffForm.phoneNumber.trim(),
      licenseNumber: staffForm.licenseNumber.trim(),
      active: staffForm.active,
    };

    try {
      const response = editingStaff
        ? await staffApi.update(editingStaff.id, payload)
        : await staffApi.create(payload);

      const staffId = String(response?.data?.id ?? editingStaff?.id ?? Date.now());
      setShiftAssignments((current) => ({
        ...current,
        [staffId]: {
          start: staffForm.shiftStart,
          end: staffForm.shiftEnd,
        },
      }));
      await loadStaff();
      setShowStaffModal(false);
      setEditingStaff(null);
      pushNotification(`${role === 'doctor' ? 'Doctor' : 'Nurse'} record saved.`, 'info');
      appendAuditLog(`${editingStaff ? 'Updated' : 'Created'}`, `${payload.fullName} (${role})`);
    } catch (error) {
      console.error('Unable to save staff member', error);
      pushNotification('Failed to save staff member.', 'warning');
    } finally {
      setSavingStaff(false);
    }
  }

  async function handleRemoveStaff(staff: StaffRecord) {
    try {
      await staffApi.remove(staff.id);
      setShiftAssignments((current) => {
        const next = { ...current };
        delete next[String(staff.id)];
        return next;
      });
      await loadStaff();
      appendAuditLog('Deactivated', `${staff.fullName} (${staff.role})`);
      pushNotification(`${staff.fullName} deactivated.`, 'warning');
    } catch (error) {
      console.error('Unable to deactivate staff member', error);
      pushNotification('Failed to deactivate staff member.', 'warning');
    }
  }

  async function handlePatientSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingPatient(true);

    const payload = {
      patientIdentifier: editingPatient?.patientIdentifier || `PT-${String(rawPatients.length + 1).padStart(3, '0')}`,
      name: patientForm.name.trim(),
      email: patientForm.email.trim(),
      password: patientForm.password.trim(),
      age: Number(patientForm.age) || 0,
      gender: patientForm.gender,
      roomNumber: patientForm.roomNumber.trim(),
      condition: patientForm.condition,
      admittedDate: patientForm.admittedDate,
      doctorId: patientForm.doctorId,
      nurseId: patientForm.nurseId,
      mobileNumber: patientForm.mobileNumber.trim(),
      guardianName: patientForm.guardianName.trim(),
      guardianMobile: patientForm.guardianMobile.trim(),
    };

    try {
      if (editingPatient) {
        updatePatient(editingPatient.id, payload);
        appendAuditLog('Updated', `${payload.name} patient profile`);
      } else {
        addNewPatient(payload);
        appendAuditLog('Created', `${payload.name} patient profile`);
      }

      setRawPatients([...seedPatients] as unknown as RawPatient[]);
      setShowPatientModal(false);
      setEditingPatient(null);
      pushNotification(`${payload.name} saved successfully.`, 'info');
    } catch (error) {
      console.error('Unable to save patient', error);
      pushNotification('Failed to save patient record.', 'warning');
    } finally {
      setSavingPatient(false);
    }
  }

  function resolveAlert(alertId: string) {
    setAlertState((current) => {
      const base = buildAlerts(patients);
      const merged = mergeAlertState(current, base);
      const exists = merged.some(a => a.id === alertId);
      if (exists) {
        const next = current.filter(a => a.id !== alertId);
        // If not already in alertState, add with RESOLVED status
        const already = current.find(a => a.id === alertId);
        if (!already) {
          return [...next, { ...merged.find(a => a.id === alertId)!, status: 'RESOLVED' as AlertStatus, summary: 'Alert resolved and cleared from console.' }];
        }
        return next.map(a => a.id === alertId ? { ...a, status: 'RESOLVED' as AlertStatus, summary: 'Alert resolved and cleared from console.' } : a);
      }
      return [...current, ...base.filter(a => a.id === alertId).map(a => ({ ...a, status: 'RESOLVED' as AlertStatus, summary: 'Alert resolved and cleared from console.' }))];
    });
    appendAuditLog('Resolved', `Alert ${alertId}`);
    pushNotification('Alert resolved.', 'info');
  }

  function acknowledgeAlert(alertId: string) {
    setAlertState((current) => {
      const base = buildAlerts(patients);
      const merged = mergeAlertState(current, base);
      const exists = current.some(a => a.id === alertId);
      if (exists) return current.map(a => a.id === alertId ? { ...a, status: 'ACKNOWLEDGED' as AlertStatus, summary: 'Doctor notified and acknowledgment logged.' } : a);
      const found = merged.find(a => a.id === alertId);
      return found ? [...current, { ...found, status: 'ACKNOWLEDGED' as AlertStatus, summary: 'Doctor notified and acknowledgment logged.' }] : current;
    });
    appendAuditLog('Acknowledged', `Alert ${alertId}`);
    pushNotification('Alert acknowledged.', 'info');
  }

  function escalateAlert(alertId: string) {
    setAlertState((current) => {
      const base = buildAlerts(patients);
      const merged = mergeAlertState(current, base);
      const exists = current.some(a => a.id === alertId);
      if (exists) return current.map(a => a.id === alertId ? { ...a, status: 'ESCALATED' as AlertStatus, summary: 'Emergency escalation sent to the command center.' } : a);
      const found = merged.find(a => a.id === alertId);
      return found ? [...current, { ...found, status: 'ESCALATED' as AlertStatus, summary: 'Emergency escalation sent to the command center.' }] : current;
    });
    appendAuditLog('Escalated', `Alert ${alertId}`);
    pushNotification('Alert escalated to emergency response.', 'warning');
  }

  function notifyAlert(alertId: string) {
    const alert = alerts.find(a => a.id === alertId);
    if (!alert) return;
    setNotifications(current => [{
      id: `note-${Date.now()}`,
      title: `Alert Notification: ${alert.patientName}`,
      detail: `${alert.summary} — ${alert.department} | Risk ${alert.riskScore}`,
      tone: 'critical' as NotificationEntry['tone'],
      timestamp: new Date().toISOString(),
    }, ...current].slice(0, 10));
    appendAuditLog('Notified', `Alert ${alertId} — ${alert.patientName}`);
    pushNotification(`Notification sent for ${alert.patientName}.`, 'info');
  }

  function handleSendNotification() {
    if (!notificationDraft.trim()) {
      pushNotification('Enter a notification message first.', 'warning');
      return;
    }

    const audience =
      notificationAudience === 'all'
        ? 'all staff'
        : notificationAudience === 'doctors'
          ? 'doctors'
          : `${notificationDepartment} department`;

    const newEntry: NotificationEntry = {
      id: `note-${Date.now()}`,
      title: `Message sent to ${audience}`,
      detail: notificationDraft.trim(),
      tone: (notificationDraft.toLowerCase().includes('critical') ? 'critical' : 'info') as NotificationEntry['tone'],
      timestamp: new Date().toISOString(),
    };

    setNotifications((current) => [newEntry, ...current].slice(0, 10));

    // Broadcast to doctor/nurse dashboards via localStorage so the notification
    // bell in DoctorDashboardRealTime picks it up via the storage event listener.
    try {
      const doctorNotif = {
        id: `admin-notif-${Date.now()}`,
        title: `Admin: ${notificationDraft.trim().slice(0, 60)}${notificationDraft.length > 60 ? '…' : ''}`,
        detail: `Sent to ${audience} • ${new Date().toLocaleTimeString()}`,
        timestamp: new Date().toISOString(),
        read: false,
      };
      const existing: unknown[] = JSON.parse(localStorage.getItem('app_notifications') || '[]');
      const updated = [doctorNotif, ...existing].slice(0, 20);
      localStorage.setItem('app_notifications', JSON.stringify(updated));
    } catch { /* storage not available */ }

    appendAuditLog('Notified', audience);
    pushNotification('Notification sent successfully.', 'info');
    setNotificationDraft('');
  }

  async function handleReport(kind: 'doctor-shift' | 'nurse-handover' | 'patient') {
    try {
      if (kind === 'patient') {
        if (!selectedPatient) {
          pushNotification('Select a patient first.', 'warning');
          return;
        }
        const response = await reportApi.downloadPatientReport(selectedPatient.id);
        downloadBlob(response.data as Blob, `${selectedPatient.patientIdentifier}-report.pdf`);
      } else if (kind === 'doctor-shift') {
        const response = await reportApi.downloadDoctorShiftReport();
        downloadBlob(response.data as Blob, 'doctor-shift-report.pdf');
      } else {
        const response = await reportApi.downloadNurseHandoverReport();
        downloadBlob(response.data as Blob, 'nurse-handover-report.pdf');
      }
      pushNotification('Report generated.', 'info');
      appendAuditLog('Generated report', kind);
    } catch (error) {
      console.error('Unable to generate report', error);
      pushNotification('Failed to generate report.', 'warning');
    }
  }

  function selectTimelinePatient(patientId: string) {
    setSelectedPatientId(patientId);
    setActiveSection('intelligence');
  }

  return (
    <div className="min-h-screen bg-dark-950 text-white relative overflow-hidden">
      <div className="absolute top-0 right-0 w-[720px] h-[720px] bg-primary-500/10 rounded-full blur-[140px] pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-[640px] h-[640px] bg-secondary-500/10 rounded-full blur-[140px] pointer-events-none" />

      <header className="relative z-10 px-6 py-5 border-b border-white/5 bg-dark-950/80 backdrop-blur-xl flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.35em] text-dark-400">Administration Console</p>
          <h1 className="text-3xl font-bold mt-1">MediTrack Control Center</h1>
          <p className="text-sm text-dark-300 mt-1">
            {user?.name ? `Signed in as ${user.name}` : 'Hospital operations, analytics, alerts, and command center'}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="relative">
            <Search size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-dark-400" />
            <input
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder="Search patients, staff, alerts"
              className="input-field pl-10 min-w-[240px]"
            />
          </div>
          <select value={departmentFilter} onChange={(event) => setDepartmentFilter(event.target.value)} className="input-field min-w-[160px]">
            <option value="all">All Departments</option>
            {staffDepartments.map((department) => (
              <option key={department} value={department}>
                {department}
              </option>
            ))}
          </select>
          <select value={riskFilter} onChange={(event) => setRiskFilter(event.target.value as any)} className="input-field min-w-[160px]">
            {riskFilters.map((risk) => (
              <option key={risk} value={risk}>
                {risk === 'all' ? 'All Risks' : `${risk} Risk`}
              </option>
            ))}
          </select>
          <button onClick={reloadPatients} className="btn-secondary px-4 py-3" disabled={loading}>
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            {loading ? 'Loading...' : 'Refresh'}
          </button>
          <QuickCallMenu />
          <button
            onClick={logout}
            className="w-11 h-11 rounded-full bg-error-500/10 border border-error-500/20 flex items-center justify-center text-error-300 hover:text-white hover:bg-error-500 transition-colors"
            title="Sign out"
          >
            <LogOut size={16} />
          </button>
        </div>
      </header>

      <main className="relative z-10 max-w-7xl mx-auto px-6 py-8 space-y-8">
        {toast && <Toast message={toast} />}

        <div className="flex flex-wrap gap-2">
          {sections.map((section) => (
            <TabButton
              key={section.id}
              active={activeSection === section.id}
              onClick={() => setActiveSection(section.id)}
              icon={section.icon}
              label={section.label}
            />
          ))}
        </div>

        {activeSection === 'overview' && (
          <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="space-y-8">
            <SectionHeader
              icon={<Hospital size={20} />}
              title="Hospital Overview Dashboard"
              subtitle="Live operational summary, risk signals, and command center visibility."
            />

            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4">
              <MetricCard icon={<Users size={18} />} label="Total Patients" value={overviewStats.totalPatients} detail="Registered in current registry" />
              <MetricCard icon={<Stethoscope size={18} />} label="Total Doctors" value={overviewStats.totalDoctors} detail="Active doctor accounts" />
              <MetricCard icon={<UsersRound size={18} />} label="Total Nurses" value={overviewStats.totalNurses} detail="Active nurse accounts" />
              <MetricCard icon={<Hospital size={18} />} label="Active Departments" value={overviewStats.activeDepartments} detail="Across staff and admissions" />
              <MetricCard icon={<ShieldAlert size={18} />} label="Critical Patients" value={overviewStats.criticalPatients} detail="Immediate attention required" />
              <MetricCard icon={<AlertTriangle size={18} />} label="Active Alerts" value={overviewStats.activeAlerts} detail="High and critical alerts" />
              <MetricCard icon={<Gauge size={18} />} label="ICU Risk Patients" value={overviewStats.icuRiskPatients} detail="Needs close observation" />
              <MetricCard icon={<MonitorSmartphone size={18} />} label="Under Observation" value={overviewStats.observationPatients} detail="Monitored by staff" />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-5 gap-4">
              <LiveStatCard icon={<ArrowRight size={18} />} label="Admissions Today" value={overviewStats.admissionsToday} />
              <LiveStatCard icon={<CheckCircle2 size={18} />} label="Discharges Today" value={overviewStats.dischargesToday} />
              <LiveStatCard icon={<AlertTriangle size={18} />} label="Emergency Cases" value={overviewStats.emergencyCases} />
              <LiveStatCard icon={<Activity size={18} />} label="Average Occupancy" value={`${overviewStats.averageOccupancy}%`} />
              <LiveStatCard icon={<Sparkles size={18} />} label="Prediction Accuracy" value={`${overviewStats.predictionAccuracy}%`} />
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
              <div className="glass-panel p-6 xl:col-span-2">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h2 className="text-xl font-bold">Emergency Command Center</h2>
                    <p className="text-sm text-dark-300">Priority response for the highest risk case in the registry.</p>
                  </div>
                  <div className="px-3 py-1 rounded-full bg-error-500/15 text-error-300 text-xs font-semibold">
                    {selectedAlert?.severity || 'NONE'}
                  </div>
                </div>

                {selectedAlert ? (
                  <div className="rounded-2xl border border-white/5 bg-dark-900/80 p-5">
                    <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                      <div>
                        <p className="text-sm text-dark-400">Patient</p>
                        <h3 className="text-2xl font-bold mt-1">{selectedAlert.patientName}</h3>
                        <p className="text-dark-300 mt-1">
                          {selectedAlert.department} | Risk {selectedAlert.riskScore} | {selectedAlert.assignedDoctor}
                        </p>
                      </div>
                      <div className="flex flex-wrap gap-3">
                        <button onClick={() => acknowledgeAlert(selectedAlert.id)} className="btn-secondary px-4 py-3">
                          <CheckCircle2 size={16} />
                          Acknowledge
                        </button>
                        <button onClick={() => escalateAlert(selectedAlert.id)} className="btn-primary px-4 py-3">
                          <AlertTriangle size={16} />
                          Escalate
                        </button>
                        <button onClick={() => notifyAlert(selectedAlert.id)} className="px-4 py-3 rounded-xl border border-primary-500/30 bg-primary-500/10 hover:bg-primary-500/20 text-primary-300 font-semibold flex items-center gap-2 text-sm transition-colors">
                          <Bell size={16} />
                          Notify
                        </button>
                        <button onClick={() => resolveAlert(selectedAlert.id)} className="px-4 py-3 rounded-xl border border-green-500/30 bg-green-500/10 hover:bg-green-500/20 text-green-300 font-semibold flex items-center gap-2 text-sm transition-colors">
                          <CheckCircle2 size={16} />
                          Resolve
                        </button>
                      </div>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-5">
                      <InfoChip label="Severity" value={selectedAlert.severity} />
                      <InfoChip label="Status" value={selectedAlert.status} />
                      <InfoChip label="Timestamp" value={formatDateTime(selectedAlert.timestamp)} />
                    </div>
                    <p className="mt-4 text-dark-200">{selectedAlert.summary}</p>
                  </div>
                ) : (
                  <div className="rounded-2xl border border-white/5 bg-dark-900/60 p-5 text-dark-300">No alerts available.</div>
                )}
              </div>

              <div className="glass-panel p-6">
                <h2 className="text-xl font-bold mb-4">Live Stats</h2>
                <div className="space-y-4">
                  <div className="rounded-2xl bg-dark-900/70 border border-white/5 p-4">
                    <div className="text-dark-400 text-sm">Prediction Accuracy</div>
                    <div className="text-3xl font-bold mt-1">{overviewStats.predictionAccuracy}%</div>
                    <div className="mt-3 h-2 rounded-full bg-dark-800 overflow-hidden">
                      <div className="h-full rounded-full bg-gradient-to-r from-primary-500 to-secondary-500" style={{ width: `${overviewStats.predictionAccuracy}%` }} />
                    </div>
                  </div>
                  <div className="rounded-2xl bg-dark-900/70 border border-white/5 p-4">
                    <div className="text-dark-400 text-sm">Billing Outstanding</div>
                    <div className="text-3xl font-bold mt-1">{formatCurrency(totalOutstanding)}</div>
                    <div className="text-sm text-dark-400 mt-2">Pending invoices across registered patients</div>
                  </div>
                </div>
              </div>
            </div>
          </motion.section>
        )}

        {activeSection === 'alerts' && (
          <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
            <SectionHeader
              icon={<AlertTriangle size={20} />}
              title="Real-Time Alert Center"
              subtitle="Filter, acknowledge, and escalate high-severity patient alerts."
            />

            <div className="glass-panel p-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <select value={severityFilter} onChange={(event) => setSeverityFilter(event.target.value as any)} className="input-field">
                  {severityFilters.map((severity) => (
                    <option key={severity} value={severity}>
                      {severity === 'all' ? 'All Severity Levels' : `${severity} Alerts`}
                    </option>
                  ))}
                </select>
                <select value={departmentFilter} onChange={(event) => setDepartmentFilter(event.target.value)} className="input-field">
                  <option value="all">All Departments</option>
                  {staffDepartments.map((department) => (
                    <option key={department} value={department}>
                      {department}
                    </option>
                  ))}
                </select>
                <div className="text-sm text-dark-300 flex items-center justify-center md:justify-end">
                  {filteredAlerts.length} alert{filteredAlerts.length === 1 ? '' : 's'} visible
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
              <div className="glass-panel xl:col-span-2 overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="min-w-full">
                    <thead>
                      <tr className="border-b border-white/5 text-left text-xs uppercase tracking-[0.2em] text-dark-400">
                        <th className="px-6 py-4">Patient</th>
                        <th className="px-6 py-4">Risk</th>
                        <th className="px-6 py-4">Department</th>
                        <th className="px-6 py-4">Status</th>
                        <th className="px-6 py-4">Doctor</th>
                        <th className="px-6 py-4 text-right">Actions</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredAlerts.map((alert) => (
                        <tr key={alert.id} className={`border-b border-white/5 hover:bg-white/3 transition-colors ${selectedAlertId === alert.id ? 'bg-white/5' : ''}`}>
                          <td className="px-6 py-4">
                            <button onClick={() => setSelectedAlertId(alert.id)} className="text-left">
                              <div className="font-semibold">{alert.patientName}</div>
                              <div className="text-xs text-dark-400">{alert.patientIdentifier}</div>
                            </button>
                          </td>
                          <td className="px-6 py-4">
                            <Pill tone={alert.severity === 'CRITICAL' ? 'critical' : 'warning'}>{alert.riskScore}</Pill>
                          </td>
                          <td className="px-6 py-4 text-dark-300">{alert.department}</td>
                          <td className="px-6 py-4">
                            <Pill tone={alert.status === 'ACTIVE' ? 'warning' : alert.status === 'ESCALATED' ? 'critical' : 'success'}>{alert.status}</Pill>
                          </td>
                          <td className="px-6 py-4 text-dark-300">{alert.assignedDoctor}</td>
                          <td className="px-6 py-4">
                            <div className="flex items-center justify-end gap-2">
                              <button onClick={() => setSelectedAlertId(alert.id)} className="w-9 h-9 rounded-full bg-dark-800/80 border border-white/5 flex items-center justify-center text-dark-300 hover:text-white hover:bg-dark-700" title="View">
                                <ArrowRight size={14} />
                              </button>
                              <button onClick={() => acknowledgeAlert(alert.id)} className="w-9 h-9 rounded-full bg-success-500/10 border border-success-500/20 flex items-center justify-center text-success-300 hover:bg-success-500 hover:text-white" title="Acknowledge">
                                <CheckCircle2 size={14} />
                              </button>
                              <button onClick={() => escalateAlert(alert.id)} className="w-9 h-9 rounded-full bg-error-500/10 border border-error-500/20 flex items-center justify-center text-error-300 hover:bg-error-500 hover:text-white" title="Escalate">
                                <AlertTriangle size={14} />
                              </button>
                              <button onClick={() => notifyAlert(alert.id)} className="w-9 h-9 rounded-full bg-primary-500/10 border border-primary-500/20 flex items-center justify-center text-primary-300 hover:bg-primary-500 hover:text-white" title="Notify">
                                <Bell size={14} />
                              </button>
                              <button onClick={() => resolveAlert(alert.id)} className="px-3 h-9 rounded-full bg-dark-700 border border-white/10 text-xs font-semibold text-dark-200 hover:bg-green-600 hover:text-white transition-colors" title="Resolve">
                                RESOLVE
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="glass-panel p-5">
                <h3 className="text-lg font-bold mb-4">Emergency Popup Panel</h3>
                {selectedAlert ? (
                  <div className="space-y-4">
                    <div>
                      <div className="text-dark-400 text-xs uppercase tracking-[0.24em]">Selected Alert</div>
                      <div className="text-2xl font-bold mt-1">{selectedAlert.patientName}</div>
                      <div className="text-sm text-dark-300 mt-1">{selectedAlert.department}</div>
                    </div>
                    <div className="space-y-2">
                      <InfoLine label="Risk Score" value={selectedAlert.riskScore} />
                      <InfoLine label="Assigned Doctor" value={selectedAlert.assignedDoctor} />
                      <InfoLine label="Timestamp" value={formatDateTime(selectedAlert.timestamp)} />
                    </div>
                    <div className="flex gap-3 flex-wrap">
                      <button onClick={() => acknowledgeAlert(selectedAlert.id)} className="btn-secondary flex-1 px-4 py-3">
                        Acknowledge
                      </button>
                      <button onClick={() => escalateAlert(selectedAlert.id)} className="btn-primary flex-1 px-4 py-3">
                        Escalate
                      </button>
                      <button onClick={() => notifyAlert(selectedAlert.id)} className="flex-1 px-4 py-3 rounded-xl border border-primary-500/30 bg-primary-500/10 hover:bg-primary-500/20 text-primary-300 font-semibold text-sm transition-colors">
                        Notify
                      </button>
                      <button onClick={() => resolveAlert(selectedAlert.id)} className="flex-1 px-4 py-3 rounded-xl border border-green-500/30 bg-green-500/10 hover:bg-green-500/20 text-green-300 font-semibold text-sm transition-colors">
                        Resolve
                      </button>
                    </div>
                  </div>
                ) : (
                  <p className="text-dark-300">Select an alert to open the command panel.</p>
                )}
              </div>
            </div>
          </motion.section>
        )}

        {activeSection === 'staff' && (
          <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
            <SectionHeader
              icon={<UsersRound size={20} />}
              title="Doctor and Nurse Management"
              subtitle="Add, update, deactivate, and balance staff workload."
            />

            <div className="flex flex-wrap gap-2">
              <TabButton active={activeStaffTab === 'doctors'} onClick={() => setActiveStaffTab('doctors')} icon={<Stethoscope size={16} />} label={`Doctors (${filteredDoctors.length})`} />
              <TabButton active={activeStaffTab === 'nurses'} onClick={() => setActiveStaffTab('nurses')} icon={<Users size={16} />} label={`Nurses (${filteredNurses.length})`} />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <MetricCard icon={<Stethoscope size={18} />} label="Active Doctors" value={filteredDoctors.filter((staff) => staff.active !== false).length} detail="Current roster" />
              <MetricCard icon={<Users size={18} />} label="Active Nurses" value={filteredNurses.filter((staff) => staff.active !== false).length} detail="Current roster" />
              <MetricCard icon={<Activity size={18} />} label="Doctor Workload" value={Math.round(avg(filteredDoctors.map((doctor) => doctor.workload)))} detail="Current average load" />
              <MetricCard icon={<TimerReset size={18} />} label="Vitals Entry Rate" value={filteredNurses.reduce((sum, nurse) => sum + nurse.enteredVitalsCount, 0)} detail="Logged readings" />
            </div>

            <div className="glass-panel overflow-hidden">
              <div className="flex items-center justify-between px-6 py-4 border-b border-white/5">
                <div>
                  <h3 className="text-xl font-bold">Staff Registry</h3>
                  <p className="text-sm text-dark-300">Shift timings are editable per staff record and stored locally for the demo.</p>
                </div>
                <button onClick={() => openStaffModal(activeStaffTab)} className="btn-primary px-4 py-3">
                  <Plus size={16} />
                  Add {activeStaffTab === 'doctors' ? 'Doctor' : 'Nurse'}
                </button>
              </div>

              <div className="overflow-x-auto">
                <table className="min-w-full">
                  <thead>
                    <tr className="border-b border-white/5 text-left text-xs uppercase tracking-[0.2em] text-dark-400">
                      <th className="px-6 py-4">Name</th>
                      <th className="px-6 py-4">Department</th>
                      <th className="px-6 py-4">{activeStaffTab === 'doctors' ? 'Assigned Patients' : 'Vitals Entered'}</th>
                      <th className="px-6 py-4">Active Cases</th>
                      <th className="px-6 py-4">Shift</th>
                      <th className="px-6 py-4">Status</th>
                      <th className="px-6 py-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {activeStaffTab === 'doctors'
                      ? filteredDoctors.map((staff) => {
                          const staffRecord = roster.find((member) => String(member.id) === staff.id) ?? undefined;
                          return (
                            <tr key={staff.id} className="border-b border-white/5 hover:bg-white/3 transition-colors">
                              <td className="px-6 py-4">
                                <div className="font-semibold">{staff.name}</div>
                                <div className="text-xs text-dark-400">{staff.email}</div>
                              </td>
                              <td className="px-6 py-4 text-dark-300">{staff.department}</td>
                              <td className="px-6 py-4">{staff.assignedPatients}</td>
                              <td className="px-6 py-4">{staff.criticalPatients}</td>
                              <td className="px-6 py-4 text-dark-300">{shiftLabel(staff.shift)}</td>
                              <td className="px-6 py-4">
                                <Pill tone={staff.active ? 'success' : 'critical'}>{staff.active ? 'Active' : 'Inactive'}</Pill>
                              </td>
                              <td className="px-6 py-4">
                                <div className="flex items-center justify-end gap-2">
                                  <button onClick={() => openStaffModal(activeStaffTab, staffRecord)} className="w-9 h-9 rounded-full bg-dark-800/80 border border-white/5 flex items-center justify-center text-dark-300 hover:text-white hover:bg-dark-700">
                                    <PencilLine size={14} />
                                  </button>
                                  <button onClick={() => staffRecord && handleRemoveStaff(staffRecord)} className="w-9 h-9 rounded-full bg-error-500/10 border border-error-500/20 flex items-center justify-center text-error-300 hover:bg-error-500 hover:text-white">
                                    <Trash2 size={14} />
                                  </button>
                                </div>
                              </td>
                            </tr>
                          );
                        })
                      : filteredNurses.map((staff) => {
                          const staffRecord = roster.find((member) => String(member.id) === staff.id) ?? undefined;
                          return (
                            <tr key={staff.id} className="border-b border-white/5 hover:bg-white/3 transition-colors">
                              <td className="px-6 py-4">
                                <div className="font-semibold">{staff.name}</div>
                                <div className="text-xs text-dark-400">{staff.email}</div>
                              </td>
                              <td className="px-6 py-4 text-dark-300">{staff.department}</td>
                              <td className="px-6 py-4">{staff.enteredVitalsCount}</td>
                              <td className="px-6 py-4">{staff.monitoredPatients}</td>
                              <td className="px-6 py-4 text-dark-300">{shiftLabel(staff.shift)}</td>
                              <td className="px-6 py-4">
                                <Pill tone={staff.active ? 'success' : 'critical'}>{staff.active ? 'Active' : 'Inactive'}</Pill>
                              </td>
                              <td className="px-6 py-4">
                                <div className="flex items-center justify-end gap-2">
                                  <button onClick={() => openStaffModal(activeStaffTab, staffRecord)} className="w-9 h-9 rounded-full bg-dark-800/80 border border-white/5 flex items-center justify-center text-dark-300 hover:text-white hover:bg-dark-700">
                                    <PencilLine size={14} />
                                  </button>
                                  <button onClick={() => staffRecord && handleRemoveStaff(staffRecord)} className="w-9 h-9 rounded-full bg-error-500/10 border border-error-500/20 flex items-center justify-center text-error-300 hover:bg-error-500 hover:text-white">
                                    <Trash2 size={14} />
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
          </motion.section>
        )}

        {activeSection === 'patients' && (
          <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
            <SectionHeader
              icon={<UserRound size={20} />}
              title="Patient Management"
              subtitle="Search patients, edit care assignments, and inspect current vitals."
            />

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <MetricCard icon={<UserRound size={18} />} label="Patients in Registry" value={patients.length} detail="All active admissions" />
              <MetricCard icon={<ShieldAlert size={18} />} label="Risk Level High+" value={patients.filter((patient) => patient.riskScore >= 70).length} detail="High and critical patients" />
              <MetricCard icon={<HeartPulse size={18} />} label="Recent Vitals" value={vitalsFeed.length} detail="Latest monitoring feed" />
              <MetricCard icon={<DollarSign size={18} />} label="Outstanding Billing" value={formatCurrency(totalOutstanding)} detail="Unpaid invoices" />
            </div>

            <div className="glass-panel p-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <input value={searchQuery} onChange={(event) => setSearchQuery(event.target.value)} placeholder="Search patients" className="input-field" />
                <select value={departmentFilter} onChange={(event) => setDepartmentFilter(event.target.value)} className="input-field">
                  <option value="all">All Departments</option>
                  {staffDepartments.map((department) => (
                    <option key={department} value={department}>
                      {department}
                    </option>
                  ))}
                </select>
                <select value={riskFilter} onChange={(event) => setRiskFilter(event.target.value as any)} className="input-field">
                  {riskFilters.map((risk) => (
                    <option key={risk} value={risk}>
                      {risk === 'all' ? 'All Risk Levels' : `${risk} Risk`}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="glass-panel overflow-hidden">
              <div className="flex items-center justify-between px-6 py-4 border-b border-white/5">
                <div>
                  <h3 className="text-xl font-bold">Patient Registry</h3>
                  <p className="text-sm text-dark-300">Assigned doctor determines department and transfer path.</p>
                </div>
                <button onClick={() => openPatientModal()} className="btn-primary px-4 py-3">
                  <Plus size={16} />
                  Add Patient
                </button>
              </div>

              <div className="overflow-x-auto">
                <table className="min-w-full">
                  <thead>
                    <tr className="border-b border-white/5 text-left text-xs uppercase tracking-[0.2em] text-dark-400">
                      <th className="px-6 py-4">Patient</th>
                      <th className="px-6 py-4">Department</th>
                      <th className="px-6 py-4">Risk</th>
                      <th className="px-6 py-4">Doctor</th>
                      <th className="px-6 py-4">Vitals</th>
                      <th className="px-6 py-4">Admission</th>
                      <th className="px-6 py-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredPatients.map((patient) => (
                      <tr key={patient.id} className="border-b border-white/5 hover:bg-white/3 transition-colors">
                        <td className="px-6 py-4">
                          <div className="font-semibold">{patient.fullName}</div>
                          <div className="text-xs text-dark-400">{patient.patientIdentifier}</div>
                        </td>
                        <td className="px-6 py-4 text-dark-300">{patient.department}</td>
                        <td className="px-6 py-4">
                          <Pill tone={patient.riskLevel === 'CRITICAL' ? 'critical' : patient.riskLevel === 'HIGH' ? 'warning' : 'success'}>{patient.riskLevel}</Pill>
                        </td>
                        <td className="px-6 py-4 text-dark-300">{patient.assignedDoctorName}</td>
                        <td className="px-6 py-4 text-dark-300">
                          <div className="space-y-1 text-xs">
                            <div>HR {patient.latestVitals.heartRate}</div>
                            <div>BP {patient.latestVitals.bloodPressure}</div>
                            <div>SpO2 {patient.latestVitals.spo2}</div>
                          </div>
                        </td>
                        <td className="px-6 py-4 text-dark-300">{formatDate(patient.admittedDate)}</td>
                        <td className="px-6 py-4">
                          <div className="flex items-center justify-end gap-2">
                            <button onClick={() => selectTimelinePatient(patient.id)} className="w-9 h-9 rounded-full bg-dark-800/80 border border-white/5 flex items-center justify-center text-dark-300 hover:text-white hover:bg-dark-700">
                              <Activity size={14} />
                            </button>
                            <button onClick={() => openPatientModal(seedPatients.find((item) => item.id === patient.id) as unknown as RawPatient ?? undefined)} className="w-9 h-9 rounded-full bg-dark-800/80 border border-white/5 flex items-center justify-center text-dark-300 hover:text-white hover:bg-dark-700">
                              <PencilLine size={14} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </motion.section>
        )}

        {activeSection === 'intelligence' && (
          <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
            <SectionHeader
              icon={<Brain size={20} />}
              title="AI Monitoring & Analytics"
              subtitle="Prediction trends, department analytics, workload balancing, vitals, and patient timeline."
            />

            <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
              <MetricCard icon={<Sparkles size={18} />} label="Prediction Accuracy" value="94.5%" detail="Live service telemetry" />
              <MetricCard icon={<TrendingUp size={18} />} label="Deterioration Probability" value={`${Math.max(18, Math.round(avg(patients.map((patient) => patient.riskScore))))}%`} detail="Aggregate risk signal" />
              <MetricCard icon={<Brain size={18} />} label="Confidence" value="91.2%" detail="Prediction confidence" />
              <MetricCard icon={<ShieldAlert size={18} />} label="Critical Ranking" value={patients.filter((patient) => patient.riskLevel === 'CRITICAL').length} detail="Top-risk patients" />
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
              <div className="glass-panel p-5">
                <h3 className="text-lg font-bold mb-4">Risk Trend</h3>
                <ResponsiveContainer width="100%" height={260}>
                  <AreaChart data={aiTrendData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#243041" />
                    <XAxis dataKey="day" stroke="#9ca3af" />
                    <YAxis stroke="#9ca3af" />
                    <Tooltip contentStyle={{ backgroundColor: '#0b1220', border: 'none' }} />
                    <Area type="monotone" dataKey="risk" stroke="#f97316" fill="#f97316" fillOpacity={0.2} />
                    <Area type="monotone" dataKey="accuracy" stroke="#38bdf8" fill="#38bdf8" fillOpacity={0.1} />
                  </AreaChart>
                </ResponsiveContainer>
              </div>

              <div className="glass-panel p-5">
                <h3 className="text-lg font-bold mb-4">Critical Patient Ranking</h3>
                <ResponsiveContainer width="100%" height={260}>
                  <BarChart data={[...patients].sort((left, right) => right.riskScore - left.riskScore).slice(0, 6)}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#243041" />
                    <XAxis dataKey="fullName" stroke="#9ca3af" tick={{ fontSize: 11 }} interval={0} />
                    <YAxis stroke="#9ca3af" />
                    <Tooltip contentStyle={{ backgroundColor: '#0b1220', border: 'none' }} />
                    <Bar dataKey="riskScore" fill="#f97316" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
              <div className="glass-panel p-5 xl:col-span-2">
                <h3 className="text-lg font-bold mb-4">Department Analytics</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                  {departmentCards.map((card) => (
                    <div key={card.department} className="rounded-2xl border border-white/5 bg-dark-900/80 p-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="text-white font-semibold">{card.department}</div>
                          <div className="text-xs text-dark-400">{card.patientCount} patients</div>
                        </div>
                        <Pill tone={card.averageRisk >= 90 ? 'critical' : card.averageRisk >= 70 ? 'warning' : 'success'}>{card.averageRisk}</Pill>
                      </div>
                      <div className="mt-3 space-y-2 text-sm text-dark-300">
                        <InfoLine label="Doctors" value={card.doctorCount} />
                        <InfoLine label="Nurses" value={card.nurseCount} />
                        <InfoLine label="Critical Cases" value={card.criticalCount} />
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="glass-panel p-5">
                <h3 className="text-lg font-bold mb-4">Workload Balancer</h3>
                <div className="space-y-3">
                  {workloadRows.slice(0, 5).map((row) => (
                    <div key={row.id} className="rounded-2xl bg-dark-900/80 border border-white/5 p-4">
                      <div className="flex items-center justify-between">
                        <div>
                          <div className="font-semibold">{row.name}</div>
                          <div className="text-xs text-dark-400">{row.department}</div>
                        </div>
                        <Pill tone={row.workload >= 75 ? 'critical' : row.workload >= 50 ? 'warning' : 'success'}>{row.workload}%</Pill>
                      </div>
                      <div className="mt-3 h-2 rounded-full bg-dark-800 overflow-hidden">
                        <div className="h-full rounded-full bg-gradient-to-r from-primary-500 to-secondary-500" style={{ width: `${row.workload}%` }} />
                      </div>
                      <div className="text-xs text-dark-400 mt-2">{row.assignedPatients} patients | {row.criticalPatients} critical</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">
              <div className="glass-panel p-5">
                <h3 className="text-lg font-bold mb-4">Live Vitals Monitoring</h3>
                <div className="overflow-x-auto">
                  <table className="min-w-full">
                    <thead>
                      <tr className="border-b border-white/5 text-left text-xs uppercase tracking-[0.2em] text-dark-400">
                        <th className="px-4 py-3">Patient</th>
                        <th className="px-4 py-3">HR</th>
                        <th className="px-4 py-3">BP</th>
                        <th className="px-4 py-3">SpO2</th>
                        <th className="px-4 py-3">Temp</th>
                        <th className="px-4 py-3">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {vitalsFeed.slice(0, 8).map((row) => (
                        <tr key={`${row.patientId}-${row.timestamp}`} className="border-b border-white/5">
                          <td className="px-4 py-3 text-sm">{row.patientName}</td>
                          <td className="px-4 py-3 text-sm">{row.heartRate}</td>
                          <td className="px-4 py-3 text-sm">{row.bloodPressure}</td>
                          <td className="px-4 py-3 text-sm">{row.spo2}</td>
                          <td className="px-4 py-3 text-sm">{row.temperature}</td>
                          <td className="px-4 py-3 text-sm">
                            <Pill tone={row.status === 'CRITICAL' ? 'critical' : row.status === 'HIGH' ? 'warning' : 'success'}>{row.status}</Pill>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="glass-panel p-5">
                <h3 className="text-lg font-bold mb-4">Patient Timeline Tracking</h3>
                {selectedPatient ? (
                  <div className="space-y-3">
                    <div className="rounded-2xl bg-dark-900/80 border border-white/5 p-4">
                      <div className="font-semibold">{selectedPatient.fullName}</div>
                      <div className="text-xs text-dark-400">{selectedPatient.patientIdentifier} | {selectedPatient.department}</div>
                    </div>
                    {selectedTimeline.slice(-5).map((event) => (
                      <div key={event.id} className="flex gap-3 rounded-2xl bg-dark-900/70 border border-white/5 p-3">
                        <div className={`mt-1 w-3 h-3 rounded-full ${event.tone === 'critical' ? 'bg-error-400' : event.tone === 'warning' ? 'bg-warning-400' : 'bg-success-400'}`} />
                        <div className="flex-1">
                          <div className="text-sm font-medium">{event.label}</div>
                          <div className="text-xs text-dark-400">{formatDateTime(event.timestamp)}</div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-dark-300">Select a patient to inspect the timeline.</p>
                )}
              </div>
            </div>
          </motion.section>
        )}

        {activeSection === 'operations' && (
          <motion.section initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
            <SectionHeader
              icon={<ClipboardList size={20} />}
              title="Operations"
              subtitle="Billing, shift management, audit logs, notifications, system health, and reports."
            />

            <div className="flex flex-wrap gap-2">
              {operationsTabs.map((tab) => (
                <TabButton key={tab.id} active={activeOperationsTab === tab.id} onClick={() => setActiveOperationsTab(tab.id)} icon={<Sparkles size={14} />} label={tab.label} />
              ))}
            </div>

            {activeOperationsTab === 'billing' && (
              <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                <div className="glass-panel p-5 xl:col-span-2 overflow-hidden">
                  <h3 className="text-lg font-bold mb-4">Financial & Billing</h3>
                  <div className="overflow-x-auto">
                    <table className="min-w-full">
                      <thead>
                        <tr className="border-b border-white/5 text-left text-xs uppercase tracking-[0.2em] text-dark-400">
                          <th className="px-4 py-3">Patient</th>
                          <th className="px-4 py-3">Insurance</th>
                          <th className="px-4 py-3">Total</th>
                          <th className="px-4 py-3">Paid</th>
                          <th className="px-4 py-3">Outstanding</th>
                          <th className="px-4 py-3">Status</th>
                        </tr>
                      </thead>
                      <tbody>
                        {billingRows.map((row) => (
                          <tr key={row.id} className="border-b border-white/5">
                            <td className="px-4 py-3">
                              <div className="font-medium">{row.patientName}</div>
                              <div className="text-xs text-dark-400">{row.patientIdentifier}</div>
                            </td>
                            <td className="px-4 py-3 text-sm">{row.insurance}</td>
                            <td className="px-4 py-3 text-sm">{formatCurrency(row.total)}</td>
                            <td className="px-4 py-3 text-sm">{formatCurrency(row.paid)}</td>
                            <td className="px-4 py-3 text-sm">{formatCurrency(row.outstanding)}</td>
                            <td className="px-4 py-3"><Pill tone={row.status === 'Paid' ? 'success' : row.status === 'Partial' ? 'warning' : 'critical'}>{row.status}</Pill></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
                <div className="glass-panel p-5">
                  <h3 className="text-lg font-bold mb-4">Revenue Snapshot</h3>
                  <div className="space-y-4">
                    <MetricCard icon={<DollarSign size={18} />} label="Total Outstanding" value={formatCurrency(totalOutstanding)} detail="Pending balance" />
                    <MetricCard icon={<Download size={18} />} label="Invoice Generation" value={billingRows.length} detail="Active invoices" />
                    <MetricCard icon={<CalendarDays size={18} />} label="Insurance Coverage" value={`${Math.max(70, 90 - billingRows.filter((row) => row.status !== 'Paid').length * 3)}%`} detail="Projected coverage" />
                  </div>
                  <div className="mt-5 space-y-2">
                    <button
                      onClick={() => {
                        const csv = ['Patient,ID,Insurance,Total,Paid,Outstanding,Status',
                          ...billingRows.map(r => `${r.patientName},${r.patientIdentifier},${r.insurance},${r.total},${r.paid},${r.outstanding},${r.status}`)
                        ].join('\n');
                        const blob = new Blob([csv], { type: 'text/csv' });
                        downloadBlob(blob, 'invoices.csv');
                        appendAuditLog('Generated Invoice Export', 'Billing CSV');
                        pushNotification('Invoice list exported as CSV.', 'info');
                      }}
                      className="w-full btn-primary px-4 py-3 flex items-center gap-2">
                      <FileText size={16} /> Generate Invoice
                    </button>
                    <button
                      onClick={() => {
                        const paid = billingRows.filter(r => r.status === 'Paid').length;
                        const partial = billingRows.filter(r => r.status === 'Partial').length;
                        const pending = billingRows.filter(r => r.status === 'Pending').length;
                        pushNotification(`Payment Summary: ${paid} Paid, ${partial} Partial, ${pending} Pending. Total outstanding: ${formatCurrency(totalOutstanding)}`, 'info');
                        appendAuditLog('Tracked Payments', 'Billing Overview');
                      }}
                      className="w-full btn-secondary px-4 py-3 flex items-center gap-2">
                      <Activity size={16} /> Track Payments
                    </button>
                    <button
                      onClick={() => {
                        const csv = ['Patient,ID,Insurance,Total,Paid,Outstanding,Status',
                          ...billingRows.map(r => `"${r.patientName}","${r.patientIdentifier}","${r.insurance}",${r.total},${r.paid},${r.outstanding},"${r.status}"`)
                        ].join('\n');
                        const blob = new Blob([csv], { type: 'text/csv' });
                        downloadBlob(blob, 'financial-report.csv');
                        appendAuditLog('Exported Financial Report', 'CSV Export');
                        pushNotification('Financial report exported as CSV.', 'info');
                      }}
                      className="w-full px-4 py-3 rounded-xl border border-white/10 bg-dark-800/70 hover:bg-dark-700 text-dark-200 font-semibold flex items-center gap-2 text-sm transition-colors">
                      <Download size={16} /> Export Report
                    </button>
                  </div>
                </div>
              </div>
            )}

            {activeOperationsTab === 'shifts' && (
              <div className="glass-panel p-5 overflow-hidden">
                <div className="flex items-center justify-between mb-4">
                  <div>
                    <h3 className="text-lg font-bold">Shift Management</h3>
                    <p className="text-sm text-dark-300">Adjust the visible shift windows for staff members.</p>
                  </div>
                  <button onClick={() => pushNotification('Shift assignments updated locally.', 'info')} className="btn-secondary px-4 py-3">
                    Save Shift Plan
                  </button>
                </div>
                <div className="overflow-x-auto">
                  <table className="min-w-full">
                    <thead>
                      <tr className="border-b border-white/5 text-left text-xs uppercase tracking-[0.2em] text-dark-400">
                        <th className="px-4 py-3">Staff</th>
                        <th className="px-4 py-3">Role</th>
                        <th className="px-4 py-3">Department</th>
                        <th className="px-4 py-3">Start</th>
                        <th className="px-4 py-3">End</th>
                        <th className="px-4 py-3">Preview</th>
                      </tr>
                    </thead>
                    <tbody>
                      {roster.filter((staff) => staff.role === 'doctor' || staff.role === 'nurse').slice(0, 10).map((staff, index) => {
                        const key = String(staff.id);
                        const shift = getShiftAssignment(key, shiftAssignments, roster, index);
                        return (
                          <tr key={key} className="border-b border-white/5">
                            <td className="px-4 py-3">{staff.fullName}</td>
                            <td className="px-4 py-3">{staff.role}</td>
                            <td className="px-4 py-3">{staff.department}</td>
                            <td className="px-4 py-3">
                              <input className="input-field" value={shift.start} onChange={(event) => setShiftAssignments((current) => ({ ...current, [key]: { ...shift, start: event.target.value } }))} />
                            </td>
                            <td className="px-4 py-3">
                              <input className="input-field" value={shift.end} onChange={(event) => setShiftAssignments((current) => ({ ...current, [key]: { ...shift, end: event.target.value } }))} />
                            </td>
                            <td className="px-4 py-3 text-sm text-dark-300">{shift.start} - {shift.end}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {activeOperationsTab === 'audit' && (
              <div className="glass-panel p-5 overflow-hidden">
                <h3 className="text-lg font-bold mb-4">Audit Logs & Activity Tracking</h3>
                <div className="space-y-3 max-h-[520px] overflow-y-auto pr-1">
                  {auditLogs.map((entry) => (
                    <div key={entry.id} className="rounded-2xl border border-white/5 bg-dark-900/70 p-4 flex items-center justify-between gap-4">
                      <div>
                        <div className="font-medium">{entry.action}</div>
                        <div className="text-xs text-dark-400">{entry.actor} | {entry.target}</div>
                      </div>
                      <div className="text-xs text-dark-400">{formatDateTime(entry.timestamp)}</div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {activeOperationsTab === 'notifications' && (
              <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                <div className="glass-panel p-5 xl:col-span-2">
                  <h3 className="text-lg font-bold mb-4">Notification Center</h3>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-4">
                    <select value={notificationAudience} onChange={(event) => setNotificationAudience(event.target.value as any)} className="input-field">
                      <option value="all">All Staff</option>
                      <option value="doctors">Doctors</option>
                      <option value="department">Department</option>
                    </select>
                    <select value={notificationDepartment} onChange={(event) => setNotificationDepartment(event.target.value)} className="input-field">
                      {staffDepartments.map((department) => (
                        <option key={department} value={department}>
                          {department}
                        </option>
                      ))}
                    </select>
                    <button onClick={handleSendNotification} className="btn-primary px-4 py-3">
                      <Bell size={16} />
                      Send Notification
                    </button>
                  </div>
                  <textarea
                    value={notificationDraft}
                    onChange={(event) => setNotificationDraft(event.target.value)}
                    className="input-field min-h-[140px]"
                    placeholder="Write a critical update, service issue, or emergency broadcast"
                  />
                </div>
                <div className="glass-panel p-5">
                  <h4 className="text-lg font-bold mb-4">Recent Notifications</h4>
                  <div className="space-y-3">
                    {notifications.map((entry) => (
                      <div key={entry.id} className="rounded-2xl bg-dark-900/70 border border-white/5 p-4">
                        <div className="font-medium">{entry.title}</div>
                        <div className="text-xs text-dark-400 mt-1">{entry.detail}</div>
                        <div className="text-xs text-dark-400 mt-2">{formatDateTime(entry.timestamp)}</div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            {activeOperationsTab === 'system' && (
              <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                {systemHealth.map((service) => (
                  <div key={service.name} className="glass-panel p-5">
                    <div className="flex items-center justify-between mb-4">
                      <div>
                        <h4 className="font-bold">{service.name}</h4>
                        <p className="text-xs text-dark-400 mt-1">{service.detail}</p>
                      </div>
                      <Pill tone={service.status === 'Running' ? 'success' : service.status === 'Degraded' ? 'warning' : 'critical'}>{service.status}</Pill>
                    </div>
                    <div className="h-2 rounded-full bg-dark-800 overflow-hidden">
                      <div
                        className={`h-full rounded-full ${service.status === 'Running' ? 'bg-success-400' : service.status === 'Degraded' ? 'bg-warning-400' : 'bg-error-400'}`}
                        style={{ width: service.status === 'Running' ? '100%' : service.status === 'Degraded' ? '64%' : '28%' }}
                      />
                    </div>
                  </div>
                ))}
                <div className="glass-panel p-5 md:col-span-2 xl:col-span-3">
                  <h4 className="font-bold mb-3">Infrastructure Notes</h4>
                  <p className="text-sm text-dark-300">
                    Connected to live services. Use this panel to watch gateway, database, Kafka, and AI service health.
                  </p>
                </div>
              </div>
            )}

            {activeOperationsTab === 'reports' && (
              <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
                <div className="glass-panel p-5 xl:col-span-2 space-y-6">
                  <h3 className="text-lg font-bold">Analytics & Reports</h3>

                  {/* Shared Reports */}
                  <div>
                    <h4 className="text-sm font-semibold text-dark-300 uppercase tracking-wider mb-3">Staff Reports</h4>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <button onClick={() => handleReport('doctor-shift')}
                        className="rounded-2xl border border-white/5 bg-dark-900/80 p-4 text-left hover:border-primary-500/40 transition-colors">
                        <CalendarDays className="text-primary-400 mb-3" />
                        <div className="font-semibold">Doctor Shift Report (PDF)</div>
                        <div className="text-xs text-dark-400 mt-1">Summarizes doctor workload and shift coverage.</div>
                      </button>
                      <button onClick={() => handleReport('nurse-handover')}
                        className="rounded-2xl border border-white/5 bg-dark-900/80 p-4 text-left hover:border-primary-500/40 transition-colors">
                        <ClipboardList className="text-primary-400 mb-3" />
                        <div className="font-semibold">Nurse Handover Report (PDF)</div>
                        <div className="text-xs text-dark-400 mt-1">Quick handover summary for the nursing team.</div>
                      </button>
                    </div>
                  </div>

                  {/* Per-Patient Reports */}
                  <div>
                    <h4 className="text-sm font-semibold text-dark-300 uppercase tracking-wider mb-3">Individual Patient Reports</h4>
                    <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                      {patients.slice(0, 25).map(patient => (
                        <div key={patient.id} className="flex items-center justify-between gap-4 rounded-xl border border-white/5 bg-dark-900/70 px-4 py-3">
                          <div>
                            <div className="font-medium text-sm">{patient.fullName}</div>
                            <div className="text-xs text-dark-400">{patient.patientIdentifier} · {patient.department}</div>
                          </div>
                          <div className="flex gap-2 shrink-0">
                            <button
                              onClick={async () => {
                                try {
                                  const res = await reportApi.downloadPatientReport(patient.id);
                                  downloadBlob(res.data as Blob, `${patient.patientIdentifier}-report.pdf`);
                                  appendAuditLog('Generated PDF report', patient.fullName);
                                  pushNotification(`PDF exported for ${patient.fullName}.`, 'info');
                                } catch {
                                  // Fallback: generate local text report
                                  const mp = seedPatients.find(p => p.id === patient.id);
                                  const content = mp ? generateMedicalReport(patient.id) : `Patient: ${patient.fullName}\nDepartment: ${patient.department}\nCondition: ${patient.condition}`;
                                  const blob = new Blob([content], { type: 'text/plain' });
                                  downloadBlob(blob, `${patient.patientIdentifier}-report.txt`);
                                  appendAuditLog('Generated local report', patient.fullName);
                                  pushNotification(`Report exported for ${patient.fullName}.`, 'info');
                                }
                              }}
                              className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-primary-500/30 bg-primary-500/10 hover:bg-primary-500/20 text-primary-300 transition-colors flex items-center gap-1">
                              <FileText size={12} /> PDF
                            </button>
                            <button
                              onClick={() => {
                                const mp = seedPatients.find(p => p.id === patient.id);
                                const vitals = mp?.vitals || [];
                                const rows = ['Timestamp,HR,Systolic,Diastolic,Temp,SpO2,RecordedBy',
                                  ...vitals.map(v => `${v.timestamp},${v.heartRate},${v.bloodPressure.systolic},${v.bloodPressure.diastolic},${v.temperature},${v.oxygenSaturation},${v.recordedBy}`)
                                ].join('\n');
                                const blob = new Blob([rows], { type: 'text/csv' });
                                downloadBlob(blob, `${patient.patientIdentifier}-vitals.csv`);
                                appendAuditLog('Exported CSV', patient.fullName);
                                pushNotification(`CSV exported for ${patient.fullName}.`, 'info');
                              }}
                              className="px-3 py-1.5 rounded-lg text-xs font-semibold border border-dark-500/30 bg-dark-800/70 hover:bg-dark-700 text-dark-200 transition-colors flex items-center gap-1">
                              <Download size={12} /> CSV
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>

                  <div className="rounded-2xl bg-dark-900/70 border border-white/5 p-4">
                    <h4 className="font-semibold mb-2">Report summary</h4>
                    <div className="text-sm text-dark-300">Total patients: {patients.length}</div>
                    <div className="text-sm text-dark-300">Critical patients: {overviewStats.criticalPatients}</div>
                    <div className="text-sm text-dark-300">Average occupancy: {overviewStats.averageOccupancy}%</div>
                  </div>
                </div>
                <div className="glass-panel p-5">
                  <h4 className="font-bold mb-4">Patient Risk Feed</h4>
                  <div className="space-y-3">
                    {[...patients].sort((l, r) => r.riskScore - l.riskScore).slice(0, 5).map(patient => (
                      <div key={patient.id} className="rounded-2xl bg-dark-900/70 border border-white/5 p-4">
                        <div className="flex items-center justify-between gap-3">
                          <div>
                            <div className="font-medium">{patient.fullName}</div>
                            <div className="text-xs text-dark-400">{patient.department}</div>
                          </div>
                          <Pill tone={patient.riskLevel === 'CRITICAL' ? 'critical' : patient.riskLevel === 'HIGH' ? 'warning' : 'success'}>{patient.riskLevel}</Pill>
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </motion.section>
        )}
      </main>

      {showStaffModal && (
        <Modal title={editingStaff ? 'Edit Staff Member' : `Add ${staffForm.role === 'doctor' ? 'Doctor' : 'Nurse'}`} onClose={() => setShowStaffModal(false)}>
          <form onSubmit={handleStaffSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Field label="Full Name">
                <input className="input-field" value={staffForm.fullName} onChange={(event) => setStaffForm({ ...staffForm, fullName: event.target.value })} required />
              </Field>
              <Field label="Email">
                <input className="input-field" value={staffForm.email} onChange={(event) => setStaffForm({ ...staffForm, email: event.target.value })} required />
              </Field>
              <Field label="Role">
                <select className="input-field" value={staffForm.role} onChange={(event) => setStaffForm({ ...staffForm, role: event.target.value as StaffFormState['role'] })}>
                  <option value="doctor">Doctor</option>
                  <option value="nurse">Nurse</option>
                </select>
              </Field>
              <Field label="Department">
                <select className="input-field" value={staffForm.department} onChange={(event) => setStaffForm({ ...staffForm, department: event.target.value })}>
                  {staffDepartments.map((department) => (
                    <option key={department} value={department}>{department}</option>
                  ))}
                </select>
              </Field>
              <Field label="Specialization">
                <input className="input-field" value={staffForm.specialization} onChange={(event) => setStaffForm({ ...staffForm, specialization: event.target.value })} placeholder="Optional" />
              </Field>
              <Field label="Phone Number">
                <input className="input-field" value={staffForm.phoneNumber} onChange={(event) => setStaffForm({ ...staffForm, phoneNumber: event.target.value })} placeholder="+1 555 000 1234" />
              </Field>
              <Field label="Shift Start">
                <input className="input-field" value={staffForm.shiftStart} onChange={(event) => setStaffForm({ ...staffForm, shiftStart: event.target.value })} placeholder="07:00" />
              </Field>
              <Field label="Shift End">
                <input className="input-field" value={staffForm.shiftEnd} onChange={(event) => setStaffForm({ ...staffForm, shiftEnd: event.target.value })} placeholder="15:00" />
              </Field>
            </div>
            <Field label="License Number">
              <input className="input-field" value={staffForm.licenseNumber} onChange={(event) => setStaffForm({ ...staffForm, licenseNumber: event.target.value })} placeholder="Optional" />
            </Field>
            <label className="flex items-center gap-2 text-sm text-dark-200">
              <input type="checkbox" checked={staffForm.active} onChange={(event) => setStaffForm({ ...staffForm, active: event.target.checked })} />
              Active account
            </label>
            <div className="flex gap-3 pt-2">
              <button type="submit" className="btn-primary px-5 py-3" disabled={savingStaff}>{savingStaff ? 'Saving...' : 'Save Staff Member'}</button>
              <button type="button" onClick={() => setShowStaffModal(false)} className="btn-secondary px-5 py-3">Cancel</button>
            </div>
          </form>
        </Modal>
      )}

      {showPatientModal && (
        <Modal title={editingPatient ? 'Edit Patient' : 'Add Patient'} onClose={() => setShowPatientModal(false)} wide>
          <form onSubmit={handlePatientSubmit} className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Field label="Full Name">
                <input className="input-field" value={patientForm.name} onChange={(event) => setPatientForm({ ...patientForm, name: event.target.value })} required />
              </Field>
              <Field label="Email">
                <input className="input-field" value={patientForm.email} onChange={(event) => setPatientForm({ ...patientForm, email: event.target.value })} required />
              </Field>
              <Field label="Password">
                <input className="input-field" type="password" value={patientForm.password} onChange={(event) => setPatientForm({ ...patientForm, password: event.target.value })} required />
              </Field>
              <Field label="Age">
                <input className="input-field" type="number" value={patientForm.age} onChange={(event) => setPatientForm({ ...patientForm, age: event.target.value })} required />
              </Field>
              <Field label="Gender">
                <select className="input-field" value={patientForm.gender} onChange={(event) => setPatientForm({ ...patientForm, gender: event.target.value })}>
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Other">Other</option>
                </select>
              </Field>
              <Field label="Room Number">
                <input className="input-field" value={patientForm.roomNumber} onChange={(event) => setPatientForm({ ...patientForm, roomNumber: event.target.value })} required />
              </Field>
              <Field label="Condition">
                <select className="input-field" value={patientForm.condition} onChange={(event) => setPatientForm({ ...patientForm, condition: event.target.value })}>
                  <option value="Stable">Stable</option>
                  <option value="Cardiac Monitoring">Cardiac Monitoring</option>
                  <option value="Post-Surgery Recovery">Post-Surgery Recovery</option>
                  <option value="Critical">Critical</option>
                  <option value="Emergency">Emergency</option>
                </select>
              </Field>
              <Field label="Admission Date">
                <input className="input-field" type="date" value={patientForm.admittedDate} onChange={(event) => setPatientForm({ ...patientForm, admittedDate: event.target.value })} />
              </Field>
              <Field label="Doctor">
                <select className="input-field" value={patientForm.doctorId} onChange={(event) => setPatientForm({ ...patientForm, doctorId: event.target.value })}>
                  <option value="">Select doctor</option>
                  {roster.filter((staff) => staff.role === 'doctor').map((doctor) => (
                    <option key={doctor.id} value={doctor.id}>
                      {doctor.fullName}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Nurse">
                <select className="input-field" value={patientForm.nurseId} onChange={(event) => setPatientForm({ ...patientForm, nurseId: event.target.value })}>
                  <option value="">Select nurse</option>
                  {roster.filter((staff) => staff.role === 'nurse').map((nurse) => (
                    <option key={nurse.id} value={nurse.id}>
                      {nurse.fullName}
                    </option>
                  ))}
                </select>
              </Field>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <Field label="Mobile Number">
                <input className="input-field" value={patientForm.mobileNumber} onChange={(event) => setPatientForm({ ...patientForm, mobileNumber: event.target.value })} />
              </Field>
              <Field label="Guardian Name">
                <input className="input-field" value={patientForm.guardianName} onChange={(event) => setPatientForm({ ...patientForm, guardianName: event.target.value })} />
              </Field>
              <Field label="Guardian Mobile">
                <input className="input-field" value={patientForm.guardianMobile} onChange={(event) => setPatientForm({ ...patientForm, guardianMobile: event.target.value })} />
              </Field>
            </div>
            <div className="flex gap-3 pt-2">
              <button type="submit" className="btn-primary px-5 py-3" disabled={savingPatient}>{savingPatient ? 'Saving...' : 'Save Patient'}</button>
              <button type="button" onClick={() => setShowPatientModal(false)} className="btn-secondary px-5 py-3">Cancel</button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}

function buildFallbackRoster(): StaffRecord[] {
  // Use seed users (correct names + department assignments) supplemented by legacy mockUsers
  const combined = [
    ...seedUsers.filter(u => u.role === 'doctor' || u.role === 'nurse' || u.role === 'admin'),
    ...mockUsers.filter(u => !seedUsers.find(s => s.email === u.email)),
  ];
  return combined
    .filter((user) => user.role === 'admin' || user.role === 'doctor' || user.role === 'nurse')
    .map((user) => ({
      id: user.id,
      sourceId: user.id,
      fullName: user.name,
      email: user.email,
      role: user.role as StaffRecord['role'],
      department: user.department ?? null,
      specialization: (user as any).specialization ?? null,
      active: user.isActive,
      createdAt: user.createdAt,
      updatedAt: user.createdAt,
    }));
}

function resolveStaffMember(reference: string | number, roster: StaffRecord[]) {
  const lookup = String(reference);
  return roster.find((staff) => String(staff.id) === lookup || staff.sourceId === lookup) ?? null;
}

function latestPatientVital(patient: RawPatient) {
  return [...patient.vitals].sort((left, right) => new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime())[0] ?? null;
}

function buildPatientSnapshot(patient: RawPatient, roster: StaffRecord[]): PatientSnapshot {
  const doctor = resolveStaffMember(patient.doctorId, roster);
  const nurse = resolveStaffMember(patient.nurseId, roster);
  const latestVital = latestPatientVital(patient);
  const department = doctor?.department || nurse?.department || inferDepartment(patient.condition);
  const riskScore = computeRiskScore(patient.condition, latestVital);
  const { firstName, lastName } = splitName(patient.name);
  const latestVitals = latestVital
    ? {
        heartRate: `${latestVital.heartRate} bpm`,
        bloodPressure: `${latestVital.bloodPressure.systolic}/${latestVital.bloodPressure.diastolic} mmHg`,
        temperature: `${latestVital.temperature.toFixed(1)} F`,
        spo2: `${latestVital.oxygenSaturation}%`,
        respiratoryRate: `${Math.max(12, Math.min(28, Math.round(latestVital.heartRate / 3.5)))} breaths/min`,
      }
    : {
        heartRate: 'N/A',
        bloodPressure: 'N/A',
        temperature: 'N/A',
        spo2: 'N/A',
        respiratoryRate: 'N/A',
      };

  return {
    id: patient.id,
    patientIdentifier: patient.patientIdentifier,
    fullName: patient.name,
    firstName,
    lastName,
    email: patient.email,
    age: patient.age,
    gender: patient.gender,
    roomNumber: patient.roomNumber,
    condition: patient.condition,
    department,
    riskScore,
    riskLevel: riskLevelFromScore(riskScore),
    assignedDoctorId: String(patient.doctorId),
    assignedDoctorName: doctor?.fullName ?? 'Unassigned',
    assignedNurseId: String(patient.nurseId),
    assignedNurseName: nurse?.fullName ?? 'Unassigned',
    admittedDate: patient.admittedDate,
    lastVitalsUpdate: latestVital?.timestamp || patient.admittedDate,
    mobileNumber: patient.mobileNumber,
    guardianName: patient.guardianName,
    guardianMobile: patient.guardianMobile,
    latestVitals,
    timeline: buildTimeline(patient, riskScore),
  };
}

function buildTimeline(patient: RawPatient, riskScore: number): TimelineEvent[] {
  const events: TimelineEvent[] = [...patient.vitals]
    .sort((left, right) => new Date(left.timestamp).getTime() - new Date(right.timestamp).getTime())
    .map((vital, index) => {
      const tone: TimelineEvent['tone'] =
        vital.oxygenSaturation <= 94 || vital.heartRate >= 110 || vital.temperature >= 100.4
          ? 'critical'
          : vital.oxygenSaturation <= 96 || vital.heartRate >= 95
            ? 'warning'
            : 'neutral';
      return {
        id: `${patient.id}-vital-${index}`,
        timestamp: vital.timestamp,
        label: `Vitals recorded: HR ${vital.heartRate}, BP ${vital.bloodPressure.systolic}/${vital.bloodPressure.diastolic}, SpO2 ${vital.oxygenSaturation}%`,
        tone,
      };
    });

  const latestTimestamp = patient.vitals.length > 0 ? patient.vitals[patient.vitals.length - 1].timestamp : patient.admittedDate;
  const baseTime = new Date(latestTimestamp).getTime();
  events.push({
    id: `${patient.id}-ai`,
    timestamp: new Date(baseTime + 5 * 60 * 1000).toISOString(),
    label: `AI risk score updated to ${riskScore}%`,
    tone: riskScore >= 90 ? 'critical' : riskScore >= 70 ? 'warning' : 'neutral',
  });
  if (riskScore >= 70) {
    events.push({
      id: `${patient.id}-alert`,
      timestamp: new Date(baseTime + 10 * 60 * 1000).toISOString(),
      label: riskScore >= 90 ? 'Critical alert escalated' : 'Alert sent to assigned doctor',
      tone: riskScore >= 90 ? 'critical' : 'warning',
    });
  }

  return events.slice(-6);
}

function buildAlerts(patients: PatientSnapshot[]): AlertItem[] {
  return patients
    .filter((patient) => patient.riskScore >= 70)
    .map((patient) => ({
      id: `alert-${patient.id}`,
      patientId: patient.id,
      patientName: patient.fullName,
      patientIdentifier: patient.patientIdentifier,
      department: patient.department,
      severity: (patient.riskLevel === 'CRITICAL' ? 'CRITICAL' : 'HIGH') as AlertSeverity,
      riskScore: patient.riskScore,
      status: 'ACTIVE' as AlertStatus,
      timestamp: patient.lastVitalsUpdate,
      assignedDoctor: patient.assignedDoctorName,
      summary: patient.riskLevel === 'CRITICAL' ? 'Immediate attention required.' : 'Doctor notified and monitoring in progress.',
    }))
    .sort((left, right) => right.riskScore - left.riskScore);
}

function mergeAlertState(current: AlertItem[], derived: AlertItem[]) {
  const currentMap = new Map(current.map((alert) => [alert.id, alert]));
  return derived.map((alert) => ({ ...alert, status: currentMap.get(alert.id)?.status ?? alert.status, summary: currentMap.get(alert.id)?.summary ?? alert.summary }));
}

function buildDepartmentCards(patients: PatientSnapshot[], roster: StaffRecord[]) {
  const departments = new Map<string, { patientCount: number; doctorCount: number; nurseCount: number; criticalCount: number; riskTotal: number }>();

  for (const patient of patients) {
    const current = departments.get(patient.department) ?? { patientCount: 0, doctorCount: 0, nurseCount: 0, criticalCount: 0, riskTotal: 0 };
    current.patientCount += 1;
    current.riskTotal += patient.riskScore;
    if (patient.riskLevel === 'CRITICAL') current.criticalCount += 1;
    departments.set(patient.department, current);
  }

  for (const staff of roster) {
    if (!staff.department) continue;
    const current = departments.get(staff.department) ?? { patientCount: 0, doctorCount: 0, nurseCount: 0, criticalCount: 0, riskTotal: 0 };
    if (staff.role === 'doctor') current.doctorCount += 1;
    if (staff.role === 'nurse') current.nurseCount += 1;
    departments.set(staff.department, current);
  }

  return Array.from(departments.entries()).map(([department, data]) => ({
    department,
    patientCount: data.patientCount,
    doctorCount: data.doctorCount,
    nurseCount: data.nurseCount,
    criticalCount: data.criticalCount,
    averageRisk: data.patientCount === 0 ? 0 : Math.round(data.riskTotal / data.patientCount),
  }));
}

function buildDoctorRows(roster: StaffRecord[], patients: PatientSnapshot[], shiftAssignments: Record<string, ShiftAssignment>) {
  return roster
    .filter((staff) => staff.role === 'doctor')
    .map((staff, index) => {
      const assignedPatients = patients.filter((patient) => patient.assignedDoctorId === String(staff.id) || patient.assignedDoctorId === String(staff.sourceId)).length;
      const criticalPatients = patients.filter(
        (patient) => (patient.assignedDoctorId === String(staff.id) || patient.assignedDoctorId === String(staff.sourceId)) && patient.riskLevel === 'CRITICAL',
      ).length;
      const workload = Math.min(100, assignedPatients * 13 + criticalPatients * 10);
      return {
        id: String(staff.id),
        name: staff.fullName,
        email: staff.email,
        department: staff.department ?? 'General',
        assignedPatients,
        criticalPatients,
        workload,
        active: staff.active !== false,
        shift: getShiftAssignment(String(staff.id), shiftAssignments, roster, index),
      };
    });
}

function buildNurseRows(roster: StaffRecord[], patients: PatientSnapshot[], shiftAssignments: Record<string, ShiftAssignment>) {
  return roster
    .filter((staff) => staff.role === 'nurse')
    .map((staff, index) => {
      const monitoredPatients = patients.filter((patient) => patient.assignedNurseId === String(staff.id) || patient.assignedNurseId === String(staff.sourceId)).length;
      const enteredVitalsCount = seedPatients.reduce((sum, patient) => {
        return sum + patient.vitals.filter((vital) => String(vital.recordedBy) === String(staff.id) || String(vital.recordedBy) === String(staff.sourceId)).length;
      }, 0);
      const lastActive = seedPatients
        .flatMap((patient) => patient.vitals.filter((vital) => String(vital.recordedBy) === String(staff.id) || String(vital.recordedBy) === String(staff.sourceId)))
        .sort((left, right) => new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime())[0]?.timestamp ?? staff.updatedAt ?? staff.createdAt ?? new Date().toISOString();
      return {
        id: String(staff.id),
        name: staff.fullName,
        email: staff.email,
        department: staff.department ?? 'General',
        monitoredPatients,
        enteredVitalsCount,
        lastActive,
        active: staff.active !== false,
        shift: getShiftAssignment(String(staff.id), shiftAssignments, roster, index),
      };
    });
}

function buildDoctorWorkloadRows(roster: StaffRecord[], patients: PatientSnapshot[], shiftAssignments: Record<string, ShiftAssignment>) {
  return buildDoctorRows(roster, patients, shiftAssignments).map((doctor) => ({
    id: doctor.id,
    name: doctor.name,
    department: doctor.department,
    assignedPatients: doctor.assignedPatients,
    criticalPatients: doctor.criticalPatients,
    workload: doctor.workload,
    shift: doctor.shift,
  }));
}

function buildVitalsFeed(patients: PatientSnapshot[]) {
  return [...patients]
    .sort((left, right) => new Date(right.lastVitalsUpdate).getTime() - new Date(left.lastVitalsUpdate).getTime())
    .slice(0, 8)
    .map((patient) => {
      const latestRaw = [...mockPatients].find((item) => item.id === patient.id);
      const latest = latestRaw?.vitals[latestRaw.vitals.length - 1];
      const status: 'NORMAL' | 'HIGH' | 'CRITICAL' = patient.riskLevel === 'CRITICAL' ? 'CRITICAL' : patient.riskLevel === 'HIGH' ? 'HIGH' : 'NORMAL';
      return {
        patientId: patient.id,
        patientName: patient.fullName,
        department: patient.department,
        heartRate: latest ? `${latest.heartRate} bpm` : patient.latestVitals.heartRate,
        bloodPressure: latest ? `${latest.bloodPressure.systolic}/${latest.bloodPressure.diastolic} mmHg` : patient.latestVitals.bloodPressure,
        temperature: latest ? `${latest.temperature.toFixed(1)} F` : patient.latestVitals.temperature,
        spo2: latest ? `${latest.oxygenSaturation}%` : patient.latestVitals.spo2,
        timestamp: patient.lastVitalsUpdate,
        status,
      };
    });
}

function buildAiTrendData(patients: PatientSnapshot[]) {
  const averageRisk = patients.length === 0 ? 0 : patients.reduce((sum, patient) => sum + patient.riskScore, 0) / patients.length;
  return ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((day, index) => ({
    day,
    risk: Math.round(averageRisk + index * 2 + (index % 2 === 0 ? 4 : -3)),
    accuracy: Math.round(92 + Math.min(4, patients.filter((patient) => patient.riskLevel === 'CRITICAL').length + index * 0.4)),
  }));
}

function buildBillingRows(patients: PatientSnapshot[]): BillingEntry[] {
  return patients.slice(0, 8).map((patient, index) => {
    const total = 1800 + patient.age * 25 + patient.riskScore * 18 + index * 120;
    const paid = Math.round(total * (patient.riskLevel === 'CRITICAL' ? 0.35 : patient.riskLevel === 'HIGH' ? 0.55 : 0.78));
    const outstanding = total - paid;
    return {
      id: patient.id,
      patientName: patient.fullName,
      patientIdentifier: patient.patientIdentifier,
      insurance: patient.riskLevel === 'CRITICAL' ? 'Premium' : patient.department === 'ICU' ? 'Corporate' : 'Standard',
      total,
      paid,
      outstanding,
      status: outstanding <= 0 ? 'Paid' : outstanding < total * 0.35 ? 'Partial' : 'Pending',
    };
  });
}

function buildSystemHealth() {
  return [
    { name: 'API Gateway', status: 'Running', detail: 'Routing healthy' },
    { name: 'Patient Service', status: 'Running', detail: 'Database connected' },
    { name: 'Eureka Server', status: 'Running', detail: 'Service discovery online' },
    { name: 'Kafka', status: 'Running', detail: 'Event streaming active' },
    { name: 'Database', status: 'Running', detail: 'MySQL healthy' },
    { name: 'AI Monitoring', status: 'Running', detail: 'Prediction layer available' },
  ] as const;
}

function defaultShiftForStaff(staff: StaffRecord, index: number) {
  const baseShifts = staff.role === 'doctor'
    ? [{ start: '07:00', end: '15:00' }, { start: '15:00', end: '23:00' }]
    : [{ start: '06:00', end: '14:00' }, { start: '14:00', end: '22:00' }, { start: '22:00', end: '06:00' }];
  return baseShifts[index % baseShifts.length];
}

function getShiftAssignment(id: string, shiftAssignments: Record<string, ShiftAssignment>, roster: StaffRecord[], index = 0) {
  return shiftAssignments[id] ?? defaultShiftForStaff(roster.find((staff) => String(staff.id) === id) ?? roster[index] ?? { role: 'doctor' } as StaffRecord, index);
}

function buildSeedAuditLogs(): AuditEntry[] {
  const now = Date.now();
  return [
    { id: 'audit-1', actor: 'Admin', action: 'Viewed patient registry', target: 'Dashboard', timestamp: new Date(now).toISOString() },
    { id: 'audit-2', actor: 'Sarah Johnson', action: 'Recorded vitals', target: 'Patient PT-001', timestamp: new Date(now - 2 * 60 * 60 * 1000).toISOString() },
    { id: 'audit-3', actor: 'Dr. Dipanshu Sharma', action: 'Reviewed emergency alert', target: 'Cardiology', timestamp: new Date(now - 3 * 60 * 60 * 1000).toISOString() },
    { id: 'audit-4', actor: 'Emily Davis', action: 'Recorded vitals', target: 'Patient PT-011', timestamp: new Date(now - 4 * 60 * 60 * 1000).toISOString() },
    { id: 'audit-5', actor: 'Admin', action: 'System login', target: 'Admin Portal', timestamp: new Date(now - 5 * 60 * 60 * 1000).toISOString() },
  ];
}

function buildSeedNotifications(): NotificationEntry[] {
  return [
    { id: 'note-1', title: 'Critical case detected', detail: 'Cardiology requires immediate attention.', tone: 'critical', timestamp: new Date().toISOString() },
    { id: 'note-2', title: 'Service watch', detail: 'Local demo data is active because backend services are unavailable.', tone: 'warning', timestamp: new Date(Date.now() - 30 * 60 * 1000).toISOString() },
  ];
}

function createEmptyPatientForm(): PatientFormState {
  return {
    name: '',
    email: '',
    password: 'password123',
    age: '45',
    gender: 'Male',
    roomNumber: '',
    condition: 'Stable',
    admittedDate: todayString,
    doctorId: '',
    nurseId: '',
    mobileNumber: '',
    guardianName: '',
    guardianMobile: '',
  };
}

function createEmptyStaffForm(role: StaffFormState['role']): StaffFormState {
  return {
    role,
    fullName: '',
    email: '',
    department: 'Cardiology',
    specialization: '',
    phoneNumber: '',
    licenseNumber: '',
    shiftStart: role === 'doctor' ? '07:00' : '06:00',
    shiftEnd: role === 'doctor' ? '15:00' : '14:00',
    active: true,
  };
}

function splitName(value: string) {
  const parts = value.trim().split(/\s+/).filter(Boolean);
  return { firstName: parts[0] ?? '', lastName: parts.slice(1).join(' ') };
}

function inferDepartment(condition: string) {
  const normalized = condition.toLowerCase();
  if (normalized.includes('cardiac')) return 'Cardiology';
  if (normalized.includes('neuro')) return 'Neurology';
  if (normalized.includes('pediatric')) return 'Pediatrics';
  if (normalized.includes('oncology')) return 'Oncology';
  if (normalized.includes('ortho')) return 'Orthopedics';
  if (normalized.includes('emergency')) return 'Emergency';
  return 'General';
}

function computeRiskScore(condition: string, latestVital: RawPatient['vitals'][number] | null) {
  let score = condition.toLowerCase().includes('emergency') ? 95 : condition.toLowerCase().includes('critical') ? 88 : condition.toLowerCase().includes('monitor') ? 72 : condition.toLowerCase().includes('recovery') ? 54 : 34;
  if (latestVital) {
    if (latestVital.heartRate >= 110 || latestVital.heartRate <= 55) score += 8;
    if (latestVital.bloodPressure.systolic >= 140 || latestVital.bloodPressure.diastolic >= 90) score += 8;
    if (latestVital.temperature >= 100.4) score += 6;
    if (latestVital.oxygenSaturation <= 94) score += 8;
    if (latestVital.oxygenSaturation <= 90) score += 6;
  }
  return Math.max(5, Math.min(99, score));
}

function riskLevelFromScore(score: number): RiskLevel {
  if (score >= 90) return 'CRITICAL';
  if (score >= 70) return 'HIGH';
  if (score >= 45) return 'MEDIUM';
  return 'LOW';
}

function matchesText(haystack: string, needle: string) {
  if (!needle.trim()) {
    return true;
  }
  return haystack.toLowerCase().includes(needle.trim().toLowerCase());
}

function formatDate(value: string) {
  return new Date(value).toLocaleDateString();
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString(undefined, {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
    hour12: false,
  });
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }).format(value);
}

function avg(values: number[]) {
  if (values.length === 0) {
    return 0;
  }
  return Math.round(values.reduce((sum, value) => sum + value, 0) / values.length);
}

function shiftLabel(shift: ShiftAssignment) {
  return `${shift.start} - ${shift.end}`;
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

function SectionHeader({ icon, title, subtitle }: { icon: ReactNode; title: string; subtitle: string }) {
  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-3 text-primary-400">
        {icon}
        <span className="text-xs uppercase tracking-[0.3em] text-dark-400">MediTrack AI</span>
      </div>
      <h2 className="text-2xl font-bold">{title}</h2>
      <p className="text-dark-300">{subtitle}</p>
    </div>
  );
}

function TabButton({ active, onClick, icon, label }: { active: boolean; onClick: () => void; icon: ReactNode; label: string }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm transition-colors ${
        active ? 'bg-primary-500 text-white border-primary-500' : 'bg-dark-900/70 text-dark-300 border-white/5 hover:text-white hover:border-white/10'
      }`}
    >
      {icon}
      {label}
    </button>
  );
}

function MetricCard({ icon, label, value, detail }: { icon: ReactNode; label: string; value: ReactNode; detail: string }) {
  return (
    <div className="glass-card p-5">
      <div className="flex items-center justify-between mb-3">
        <div className="w-10 h-10 rounded-xl bg-primary-500/10 text-primary-400 flex items-center justify-center">{icon}</div>
      </div>
      <div className="text-dark-400 text-sm">{label}</div>
      <div className="text-2xl font-bold mt-1">{value}</div>
      <div className="text-xs text-dark-400 mt-2">{detail}</div>
    </div>
  );
}

function LiveStatCard({ icon, label, value }: { icon: ReactNode; label: string; value: ReactNode }) {
  return (
    <div className="glass-panel p-4">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-dark-800/70 border border-white/5 flex items-center justify-center text-primary-400">{icon}</div>
        <div>
          <div className="text-xs text-dark-400 uppercase tracking-[0.2em]">{label}</div>
          <div className="text-xl font-bold">{value}</div>
        </div>
      </div>
    </div>
  );
}

function InfoChip({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="rounded-2xl border border-white/5 bg-dark-900/70 p-4">
      <div className="text-xs uppercase tracking-[0.2em] text-dark-400">{label}</div>
      <div className="text-lg font-semibold mt-1">{value}</div>
    </div>
  );
}

function InfoLine({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="flex items-center justify-between gap-3">
      <span className="text-dark-400">{label}</span>
      <span className="text-white">{value}</span>
    </div>
  );
}

function Pill({ tone, children }: { tone: 'success' | 'warning' | 'critical'; children: ReactNode }) {
  const toneClass =
    tone === 'success'
      ? 'bg-success-500/15 text-success-300 border-success-500/20'
      : tone === 'warning'
        ? 'bg-warning-500/15 text-warning-300 border-warning-500/20'
        : 'bg-error-500/15 text-error-300 border-error-500/20';

  return <span className={`inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold ${toneClass}`}>{children}</span>;
}

function Toast({ message }: { message: string }) {
  return (
    <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 rounded-full border border-primary-500/30 bg-dark-900/95 px-5 py-3 text-sm text-white shadow-2xl shadow-primary-500/10">
      {message}
    </div>
  );
}

function Modal({ title, onClose, children, wide = false }: { title: string; onClose: () => void; children: ReactNode; wide?: boolean }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4">
      <div className={`glass-panel w-full ${wide ? 'max-w-5xl' : 'max-w-3xl'} p-6 max-h-[90vh] overflow-y-auto`}>
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-xl font-bold">{title}</h3>
          <button onClick={onClose} className="text-dark-400 hover:text-white">
            <Trash2 size={16} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="flex flex-col gap-2">
      <span className="text-sm text-dark-300">{label}</span>
      {children}
    </label>
  );
}

type PatientFormState = {
  name: string;
  email: string;
  password: string;
  age: string;
  gender: string;
  roomNumber: string;
  condition: string;
  admittedDate: string;
  doctorId: string;
  nurseId: string;
  mobileNumber: string;
  guardianName: string;
  guardianMobile: string;
};

type StaffFormState = {
  role: 'doctor' | 'nurse';
  fullName: string;
  email: string;
  department: string;
  specialization: string;
  phoneNumber: string;
  licenseNumber: string;
  shiftStart: string;
  shiftEnd: string;
  active: boolean;
};
