import { generateReport, mockPatients, mockUsers, updatePatient } from '../database/mockDatabase'

export type LocalAuthRole = 'admin' | 'doctor' | 'nurse' | 'patient' | 'viewer'

export type LocalAuthResponse = {
  id: string
  name: string
  role: LocalAuthRole
  email: string
  department: string
  bedNumber: string
  patientIdentifier: string
  wardNumber: string
  token: string
}

export type LocalStaffMember = {
  id: number
  sourceId?: string
  fullName: string
  email: string
  role: 'admin' | 'doctor' | 'nurse'
  department: string | null
  specialization: string | null
  phoneNumber: string | null
  licenseNumber: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}

export type LocalPatientRecord = {
  id: string
  patientIdentifier: string
  name: string
  firstName: string
  lastName: string
  fullName: string
  email: string
  condition: string
  clinicalStatus: string
  roomNumber: string
  wardNumber: string
  bedNumber: string
  department: string
  assignedClinicianName: string | null
  assignedClinicianEmail: string | null
  assignedNurseName: string | null
  assignedNurseEmail: string | null
  doctorId: string
  nurseId: string
  admittedDate: string
  createdAt: string
  updatedAt: string
  lastVitalsUpdate: string
  mobileNumber: string | null
  guardianName: string | null
  guardianMobile: string | null
  vitalsCount: number
  riskScore: number
}

export type LocalVitalReading = {
  id: string
  patientId: string
  vitalType: 'HEART_RATE' | 'BLOOD_PRESSURE' | 'TEMPERATURE' | 'SPO2' | 'RESPIRATORY_RATE'
  value: number | null
  unit: string | null
  systolic: number | null
  diastolic: number | null
  readingTimestamp: string
  displayValue: string
  vitalStatus: 'LOW' | 'NORMAL' | 'HIGH' | 'CRITICAL'
  qualityScore: number
}

export type LocalVitalSummary = {
  latestVitals: Array<{
    vitalType: LocalVitalReading['vitalType']
    displayValue: string
    vitalStatus: LocalVitalReading['vitalStatus']
    readingTimestamp: string
    qualityScore: number
  }>
}

const LOCAL_STAFF_STORAGE_KEY = 'meditrack_local_staff_members'

function hasStorage() {
  return typeof window !== 'undefined' && typeof window.localStorage !== 'undefined'
}

function trim(value: string | undefined | null) {
  return (value ?? '').trim()
}

function splitName(value: string) {
  const normalized = trim(value)
  if (!normalized) {
    return { firstName: '', lastName: '' }
  }

  const parts = normalized.split(/\s+/).filter(Boolean)
  return {
    firstName: parts[0] ?? '',
    lastName: parts.slice(1).join(' '),
  }
}

function toIsoDate(value: string | undefined | null) {
  if (!value) {
    return new Date().toISOString()
  }

  if (value.includes('T')) {
    return value
  }

  return `${value}T08:00:00.000Z`
}

function latestTimestamp(values: Array<{ timestamp: string }>) {
  if (values.length === 0) {
    return ''
  }

  return [...values]
    .sort((left, right) => new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime())[0]
    ?.timestamp ?? ''
}

function classifyHeartRate(value: number) {
  if (value >= 130 || value <= 45) {
    return 'CRITICAL'
  }
  if (value >= 110 || value <= 55) {
    return 'HIGH'
  }
  return 'NORMAL'
}

function classifyBloodPressure(systolic: number, diastolic: number) {
  if (systolic >= 180 || diastolic >= 120) {
    return 'CRITICAL'
  }
  if (systolic >= 140 || diastolic >= 90) {
    return 'HIGH'
  }
  if (systolic <= 90 || diastolic <= 60) {
    return 'LOW'
  }
  return 'NORMAL'
}

function classifyTemperature(value: number) {
  if (value >= 103 || value <= 95) {
    return 'CRITICAL'
  }
  if (value >= 100.4 || value <= 97) {
    return 'HIGH'
  }
  return 'NORMAL'
}

function classifySpo2(value: number) {
  if (value <= 88) {
    return 'CRITICAL'
  }
  if (value <= 94) {
    return 'HIGH'
  }
  return 'NORMAL'
}

function classifyRespiratoryRate(value: number) {
  if (value >= 30 || value <= 8) {
    return 'CRITICAL'
  }
  if (value >= 24 || value <= 12) {
    return 'HIGH'
  }
  return 'NORMAL'
}

function riskScoreForCondition(condition: string) {
  const normalized = trim(condition).toLowerCase()
  if (normalized.includes('emergency')) return 95
  if (normalized.includes('critical')) return 88
  if (normalized.includes('cardiac')) return 72
  if (normalized.includes('observation')) return 58
  if (normalized.includes('recovery')) return 45
  return 30
}

function resolveStaffMember(reference: string | number | undefined | null) {
  const lookup = trim(reference == null ? '' : String(reference))
  if (!lookup) {
    return null
  }

  return readStaffMembers().find((member) => String(member.id) === lookup || member.sourceId === lookup) ?? null
}

function clonePatientVitals(vitals: Array<{ id: string; timestamp: string; heartRate: number; bloodPressure: { systolic: number; diastolic: number }; temperature: number; oxygenSaturation: number; recordedBy: string }>) {
  return [...vitals]
}

function buildLocalPatientRecord(patient: (typeof mockPatients)[number]): LocalPatientRecord {
  const doctor = resolveStaffMember(patient.doctorId)
  const nurse = resolveStaffMember(patient.nurseId)
  const { firstName, lastName } = splitName(patient.name)
  const lastVitalsUpdate = latestTimestamp(clonePatientVitals(patient.vitals)) || toIsoDate(patient.admittedDate)
  const department = doctor?.department ?? nurse?.department ?? 'General'
  const riskScore = riskScoreForCondition(patient.condition)

  return {
    id: patient.id,
    patientIdentifier: patient.patientIdentifier,
    name: patient.name,
    firstName,
    lastName,
    fullName: patient.name,
    email: patient.email,
    condition: patient.condition,
    clinicalStatus: patient.condition,
    roomNumber: patient.roomNumber,
    wardNumber: patient.roomNumber,
    bedNumber: patient.roomNumber,
    department,
    assignedClinicianName: doctor?.fullName ?? null,
    assignedClinicianEmail: doctor?.email ?? null,
    assignedNurseName: nurse?.fullName ?? null,
    assignedNurseEmail: nurse?.email ?? null,
    doctorId: patient.doctorId,
    nurseId: patient.nurseId,
    admittedDate: patient.admittedDate,
    createdAt: toIsoDate(patient.admittedDate),
    updatedAt: lastVitalsUpdate,
    lastVitalsUpdate,
    mobileNumber: patient.mobileNumber ?? null,
    guardianName: patient.guardianName ?? null,
    guardianMobile: patient.guardianMobile ?? null,
    vitalsCount: patient.vitals.length,
    riskScore,
  }
}

function buildLocalVitalReading(
  patientId: string,
  vitalType: LocalVitalReading['vitalType'],
  value: number,
  timestamp: string,
  payload: Record<string, string | number | null | undefined>,
): LocalVitalReading {
  const unit = typeof payload.unit === 'string' ? payload.unit : null
  const systolic = typeof payload.systolic === 'number' ? payload.systolic : null
  const diastolic = typeof payload.diastolic === 'number' ? payload.diastolic : null

  let displayValue = `${value}${unit ? ` ${unit}` : ''}`.trim()
  let status: LocalVitalReading['vitalStatus'] = 'NORMAL'

  switch (vitalType) {
    case 'HEART_RATE':
      status = classifyHeartRate(value)
      displayValue = `${value} bpm`
      break
    case 'BLOOD_PRESSURE':
      status = classifyBloodPressure(systolic ?? value, diastolic ?? value)
      displayValue = `${systolic ?? value}/${diastolic ?? value} mmHg`
      break
    case 'TEMPERATURE':
      status = classifyTemperature(value)
      displayValue = `${value} F`
      break
    case 'SPO2':
      status = classifySpo2(value)
      displayValue = `${value}%`
      break
    case 'RESPIRATORY_RATE':
      status = classifyRespiratoryRate(value)
      displayValue = `${value} breaths/min`
      break
    default:
      displayValue = `${value}`
  }

  return {
    id: `${patientId}-${vitalType}-${Date.now()}`,
    patientId,
    vitalType,
    value,
    unit,
    systolic,
    diastolic,
    readingTimestamp: timestamp,
    displayValue,
    vitalStatus: status,
    qualityScore: typeof payload.qualityScore === 'number' ? payload.qualityScore : 0.95,
  }
}

function buildLatestSummary(readings: LocalVitalReading[]): LocalVitalSummary {
  const latestByType = new Map<LocalVitalReading['vitalType'], LocalVitalReading>()

  for (const reading of [...readings].sort((left, right) => new Date(right.readingTimestamp).getTime() - new Date(left.readingTimestamp).getTime())) {
    if (!latestByType.has(reading.vitalType)) {
      latestByType.set(reading.vitalType, reading)
    }
  }

  return {
    latestVitals: Array.from(latestByType.values()).map((reading) => ({
      vitalType: reading.vitalType,
      displayValue: reading.displayValue,
      vitalStatus: reading.vitalStatus,
      readingTimestamp: reading.readingTimestamp,
      qualityScore: reading.qualityScore,
    })),
  }
}

function buildReportBlob(title: string, content: string) {
  return new Blob([`${title}\n\n${content}`], { type: 'application/pdf' })
}

function normalizeRole(value: string | undefined | null): LocalAuthRole {
  const role = trim(value).toLowerCase()
  if (role === 'admin' || role === 'doctor' || role === 'nurse' || role === 'patient' || role === 'viewer') {
    return role
  }

  return 'patient'
}

function seedStaffMembers(): LocalStaffMember[] {
  return mockUsers
    .filter((user) => {
      const role = normalizeRole(user.role)
      return role === 'admin' || role === 'doctor' || role === 'nurse'
    })
    .map((user, index) => ({
      id: index + 1,
      sourceId: user.id,
      fullName: user.name,
      email: user.email,
      role: normalizeRole(user.role) as LocalStaffMember['role'],
      department: user.department ?? null,
      specialization: user.specialization ?? null,
      phoneNumber: null,
      licenseNumber: null,
      active: user.isActive,
      createdAt: user.createdAt,
      updatedAt: user.createdAt,
    }))
}

function staffMemberKey(member: Pick<LocalStaffMember, 'sourceId' | 'email' | 'role'>) {
  const sourceId = trim(member.sourceId ?? '').toLowerCase()
  if (sourceId) {
    return sourceId
  }

  return `${trim(member.email).toLowerCase()}|${member.role}`
}

function normalizeStoredStaffMember(member: any): LocalStaffMember | null {
  const fullName = trim(String(member?.fullName ?? ''))
  const email = trim(String(member?.email ?? ''))

  if (!fullName || !email) {
    return null
  }

  const role = normalizeRole(member?.role)
  if (role !== 'admin' && role !== 'doctor' && role !== 'nurse') {
    return null
  }

  const createdAt = typeof member?.createdAt === 'string' && member.createdAt ? member.createdAt : new Date().toISOString()
  const updatedAt = typeof member?.updatedAt === 'string' && member.updatedAt ? member.updatedAt : createdAt

  return {
    id: Number.isFinite(Number(member?.id)) ? Number(member.id) : 0,
    sourceId: typeof member?.sourceId === 'string' && member.sourceId ? member.sourceId : undefined,
    fullName,
    email,
    role: role as LocalStaffMember['role'],
    department: typeof member?.department === 'string' ? member.department : null,
    specialization: typeof member?.specialization === 'string' ? member.specialization : null,
    phoneNumber: typeof member?.phoneNumber === 'string' ? member.phoneNumber : null,
    licenseNumber: typeof member?.licenseNumber === 'string' ? member.licenseNumber : null,
    active: member?.active !== false,
    createdAt,
    updatedAt,
  }
}

function reconcileStaffMembers(storedMembers: LocalStaffMember[]) {
  const merged = [...storedMembers]
  const indexByKey = new Map<string, number>()

  merged.forEach((member, index) => {
    indexByKey.set(staffMemberKey(member), index)
  })

  let changed = false

  for (const seed of seedStaffMembers()) {
    const key = staffMemberKey(seed)
    const existingIndex = indexByKey.get(key)

    if (existingIndex === undefined) {
      const newMember = {
        ...seed,
        id: nextStaffId(merged),
      }

      merged.push(newMember)
      indexByKey.set(key, merged.length - 1)
      changed = true
      continue
    }

    const current = merged[existingIndex]
    const repaired: LocalStaffMember = {
      ...current,
      sourceId: seed.sourceId ?? current.sourceId,
      fullName: current.fullName || seed.fullName,
      email: seed.email,
      role: seed.role,
      department: current.department ?? seed.department,
      specialization: current.specialization ?? seed.specialization,
      phoneNumber: current.phoneNumber ?? null,
      licenseNumber: current.licenseNumber ?? null,
      active: seed.active,
      createdAt: current.createdAt || seed.createdAt,
      updatedAt: current.updatedAt || seed.updatedAt,
    }

    if (JSON.stringify(repaired) !== JSON.stringify(current)) {
      merged[existingIndex] = repaired
      changed = true
    }
  }

  return { members: merged, changed }
}

function readStaffMembers(): LocalStaffMember[] {
  if (!hasStorage()) {
    return seedStaffMembers()
  }

  const raw = localStorage.getItem(LOCAL_STAFF_STORAGE_KEY)
  if (!raw) {
    const seeded = seedStaffMembers()
    localStorage.setItem(LOCAL_STAFF_STORAGE_KEY, JSON.stringify(seeded))
    return seeded
  }

  try {
    const parsed = JSON.parse(raw)
    if (Array.isArray(parsed)) {
      const normalized = parsed
        .map((member) => normalizeStoredStaffMember(member))
        .filter((member): member is LocalStaffMember => member !== null)

      const reconciled = reconcileStaffMembers(normalized)
      if (reconciled.changed) {
        localStorage.setItem(LOCAL_STAFF_STORAGE_KEY, JSON.stringify(reconciled.members))
      }
      return reconciled.members
    }
  } catch (error) {
    console.warn('Unable to parse local staff cache', error)
  }

  const seeded = seedStaffMembers()
  localStorage.setItem(LOCAL_STAFF_STORAGE_KEY, JSON.stringify(seeded))
  return seeded
}

function writeStaffMembers(members: LocalStaffMember[]) {
  if (!hasStorage()) {
    return
  }

  localStorage.setItem(LOCAL_STAFF_STORAGE_KEY, JSON.stringify(members))
}

function nextStaffId(members: LocalStaffMember[]) {
  return members.reduce((max, member) => Math.max(max, Number(member.id) || 0), 0) + 1
}

function toStaffMemberView(member: LocalStaffMember) {
  return {
    id: member.id,
    sourceId: member.sourceId,
    fullName: member.fullName,
    email: member.email,
    role: member.role,
    department: member.department,
    specialization: member.specialization,
    phoneNumber: member.phoneNumber,
    licenseNumber: member.licenseNumber,
    active: member.active,
    createdAt: member.createdAt,
    updatedAt: member.updatedAt,
  }
}

export function loginLocally(credentials: Record<string, string>) {
  const role = normalizeRole(credentials.role)
  const email = trim(credentials.email).toLowerCase()
  const password = trim(credentials.password)

  if (!email) {
    return null
  }

  if (role === 'admin' || role === 'doctor' || role === 'nurse') {
    if (password && password !== 'password123') {
      return null
    }

    const member = readStaffMembers().find(
      (staff) => staff.role === role && staff.email.trim().toLowerCase() === email && staff.active,
    )

    if (!member) {
      return null
    }

    return {
      id: String(member.id),
      name: member.fullName,
      role: member.role,
      email: member.email,
      department: member.department ?? '',
      bedNumber: '',
      patientIdentifier: '',
      wardNumber: '',
      token: `local-dev-${member.role}-${member.id}`,
    } satisfies LocalAuthResponse
  }

  const patientIdentifier = trim(credentials.patientIdentifier).toLowerCase()
  const requestedName = trim(credentials.name).toLowerCase()
  const patient = mockPatients.find((record) => {
    const matchesEmail = record.email.trim().toLowerCase() === email
    const matchesIdentifier = !patientIdentifier || record.patientIdentifier.trim().toLowerCase() === patientIdentifier
    const matchesName = !requestedName || record.name.trim().toLowerCase() === requestedName
    return matchesEmail && matchesIdentifier && matchesName
  })

  if (!patient) {
    return null
  }

  return {
    id: patient.id,
    name: patient.name,
    role: 'patient',
    email: patient.email,
    department: 'General',
    bedNumber: patient.roomNumber,
    patientIdentifier: patient.patientIdentifier,
    wardNumber: '',
    token: `local-dev-patient-${patient.id}`,
  } satisfies LocalAuthResponse
}

export function getLocalStaffMembers(roleFilter?: string) {
  const normalizedRole = trim(roleFilter).toLowerCase()
  return readStaffMembers()
    .filter((member) => !normalizedRole || member.role === normalizedRole)
    .sort((left, right) => left.fullName.localeCompare(right.fullName))
    .map(toStaffMemberView)
}

export function createLocalStaffMember(payload: Record<string, string | number | boolean | undefined>) {
  const members = readStaffMembers()
  const now = new Date().toISOString()
  const role = normalizeRole(String(payload.role ?? 'doctor'))

  if (role !== 'admin' && role !== 'doctor' && role !== 'nurse') {
    throw new Error(`Unsupported staff role: ${payload.role}`)
  }

  const newMember: LocalStaffMember = {
    id: nextStaffId(members),
    fullName: trim(String(payload.fullName ?? '')),
    email: trim(String(payload.email ?? '')),
    role,
    department: trim(String(payload.department ?? '')) || null,
    specialization: trim(String(payload.specialization ?? '')) || null,
    phoneNumber: trim(String(payload.phoneNumber ?? '')) || null,
    licenseNumber: trim(String(payload.licenseNumber ?? '')) || null,
    active: payload.active === false ? false : true,
    createdAt: now,
    updatedAt: now,
  }

  members.push(newMember)
  writeStaffMembers(members)
  return toStaffMemberView(newMember)
}

export function updateLocalStaffMember(
  id: number | string,
  payload: Record<string, string | number | boolean | undefined>,
) {
  const members = readStaffMembers()
  const index = members.findIndex((member) => String(member.id) === String(id))
  if (index === -1) {
    throw new Error(`Staff member not found: ${id}`)
  }

  const current = members[index]
  const updated: LocalStaffMember = {
    ...current,
    fullName: trim(String(payload.fullName ?? current.fullName)),
    email: trim(String(payload.email ?? current.email)),
    role: normalizeRole(String(payload.role ?? current.role)) as LocalStaffMember['role'],
    department: trim(String(payload.department ?? current.department ?? '')) || null,
    specialization: trim(String(payload.specialization ?? current.specialization ?? '')) || null,
    phoneNumber: trim(String(payload.phoneNumber ?? current.phoneNumber ?? '')) || null,
    licenseNumber: trim(String(payload.licenseNumber ?? current.licenseNumber ?? '')) || null,
    active: typeof payload.active === 'boolean' ? payload.active : current.active,
    updatedAt: new Date().toISOString(),
  }

  members[index] = updated
  writeStaffMembers(members)
  return toStaffMemberView(updated)
}

export function deactivateLocalStaffMember(id: number | string) {
  return updateLocalStaffMember(id, { active: false })
}

export function getLocalPatients() {
  return mockPatients.map((patient) => buildLocalPatientRecord(patient))
}

export function getLocalPatientById(id: string | number) {
  return getLocalPatients().find((patient) => String(patient.id) === String(id)) ?? null
}

export function getLocalPatientByIdentifier(patientIdentifier: string | number) {
  const lookup = trim(String(patientIdentifier)).toLowerCase()
  return getLocalPatients().find((patient) => patient.patientIdentifier.trim().toLowerCase() === lookup) ?? null
}

export function getLocalPatientStats() {
  const patients = getLocalPatients()
  const criticalPatients = patients.filter((patient) => patient.riskScore >= 90).length
  const highRiskPatients = patients.filter((patient) => patient.riskScore >= 70 && patient.riskScore < 90).length

  return {
    totalPatients: patients.length,
    criticalPatients,
    highRiskPatients,
    activePatients: patients.length,
    averageRiskScore: patients.length === 0 ? 0 : Math.round(patients.reduce((sum, patient) => sum + patient.riskScore, 0) / patients.length),
  }
}

export function updateLocalPatientCondition(id: string | number, condition: string) {
  const patient = mockPatients.find((record) => String(record.id) === String(id) || record.patientIdentifier === String(id))
  if (!patient) {
    throw new Error(`Patient not found: ${id}`)
  }

  const updated = updatePatient(patient.id, { condition })
  return updated ? buildLocalPatientRecord(updated) : buildLocalPatientRecord(patient)
}

export function getLocalVitalsReadings(patientId: string | number) {
  const patient = mockPatients.find((record) => String(record.id) === String(patientId) || record.patientIdentifier === String(patientId))
  if (!patient) {
    return []
  }

  const readings: LocalVitalReading[] = []
  const timestamp = (value: string) => value || new Date().toISOString()

  for (const vital of [...patient.vitals].sort((left, right) => new Date(left.timestamp).getTime() - new Date(right.timestamp).getTime())) {
    readings.push(buildLocalVitalReading(patient.id, 'HEART_RATE', vital.heartRate, timestamp(vital.timestamp), { unit: 'bpm' }))
    readings.push(
      buildLocalVitalReading(patient.id, 'BLOOD_PRESSURE', vital.bloodPressure.systolic, timestamp(vital.timestamp), {
        systolic: vital.bloodPressure.systolic,
        diastolic: vital.bloodPressure.diastolic,
        unit: 'mmHg',
      }),
    )
    readings.push(buildLocalVitalReading(patient.id, 'TEMPERATURE', vital.temperature, timestamp(vital.timestamp), { unit: 'F' }))
    readings.push(buildLocalVitalReading(patient.id, 'SPO2', vital.oxygenSaturation, timestamp(vital.timestamp), { unit: '%' }))
    const respiratoryRate = Math.max(12, Math.min(28, Math.round(vital.heartRate / 3.5)))
    readings.push(buildLocalVitalReading(patient.id, 'RESPIRATORY_RATE', respiratoryRate, timestamp(vital.timestamp), { unit: 'breaths/min' }))
  }

  return readings
}

export function getLocalVitalsSummary(patientId: string | number) {
  return buildLatestSummary(getLocalVitalsReadings(patientId))
}

export function createLocalVitalReading(payload: Record<string, string | number | null | undefined>) {
  const patientIdentifier = trim(
    typeof payload.patientIdentifier === 'string'
      ? payload.patientIdentifier
      : typeof payload.patientId === 'string'
        ? payload.patientId
        : '',
  )

  const patient = mockPatients.find(
    (record) =>
      String(record.id) === patientIdentifier ||
      record.patientIdentifier.trim().toLowerCase() === patientIdentifier.toLowerCase(),
  )

  if (!patient) {
    throw new Error(`Patient not found: ${patientIdentifier}`)
  }

  const latest = [...patient.vitals].sort((left, right) => new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime())[0]
  const timestamp = typeof payload.readingTimestamp === 'string' && payload.readingTimestamp ? payload.readingTimestamp : new Date().toISOString()
  const vitalType = String(payload.vitalType ?? 'HEART_RATE') as LocalVitalReading['vitalType']
  const baseHeartRate = latest?.heartRate ?? 72
  const baseBloodPressure = latest?.bloodPressure ?? { systolic: 120, diastolic: 80 }
  const baseTemperature = latest?.temperature ?? 98.6
  const baseOxygen = latest?.oxygenSaturation ?? 98

  const nextVital = {
    id: `vital-${Date.now()}`,
    patientId: patient.id,
    timestamp,
    heartRate: baseHeartRate,
    bloodPressure: baseBloodPressure,
    temperature: baseTemperature,
    oxygenSaturation: baseOxygen,
    recordedBy: trim(String(payload.recordedBy ?? payload.nurseId ?? 'local-demo')) || 'local-demo',
  }

  const numericValue = typeof payload.value === 'number' ? payload.value : Number(payload.value ?? 0)
  switch (vitalType) {
    case 'HEART_RATE':
      nextVital.heartRate = Number.isFinite(numericValue) ? numericValue : baseHeartRate
      break
    case 'BLOOD_PRESSURE':
      nextVital.bloodPressure = {
        systolic: typeof payload.systolic === 'number' ? payload.systolic : baseBloodPressure.systolic,
        diastolic: typeof payload.diastolic === 'number' ? payload.diastolic : baseBloodPressure.diastolic,
      }
      break
    case 'TEMPERATURE':
      nextVital.temperature = Number.isFinite(numericValue) ? numericValue : baseTemperature
      break
    case 'SPO2':
      nextVital.oxygenSaturation = Number.isFinite(numericValue) ? numericValue : baseOxygen
      break
    case 'RESPIRATORY_RATE':
      nextVital.heartRate = Math.max(50, Math.round((Number.isFinite(numericValue) ? numericValue : 16) * 3.5))
      break
    default:
      break
  }

  updatePatient(patient.id, { vitals: [...patient.vitals, nextVital] })

  return buildLocalVitalReading(patient.id, vitalType, numericValue, timestamp, payload)
}

export function getLocalReportBlob(
  reportKind: 'doctor-shift' | 'nurse-handover' | 'patient',
  patientId?: string | number,
) {
  if (reportKind === 'patient' && patientId != null) {
    const patient = getLocalPatientById(patientId)
    if (!patient) {
      throw new Error(`Patient not found: ${patientId}`)
    }

    const readingSummary = getLocalVitalsSummary(patientId)
    const content = [
      `Patient: ${patient.fullName}`,
      `Identifier: ${patient.patientIdentifier}`,
      `Department: ${patient.department}`,
      `Condition: ${patient.condition}`,
      `Risk Score: ${patient.riskScore}`,
      '',
      'Latest Vitals:',
      ...readingSummary.latestVitals.map((vital) => `- ${vital.vitalType}: ${vital.displayValue} (${vital.vitalStatus})`),
    ].join('\n')

    generateReport(patient.id, {
      patientId: patient.id,
      type: 'medical',
      title: `${patient.fullName} Clinical Summary`,
      content,
      generatedBy: 'MediTrack AI',
    })

    return buildReportBlob(`${patient.fullName} Clinical Summary`, content)
  }

  if (reportKind === 'doctor-shift') {
    const patients = getLocalPatients()
    const criticalPatients = patients.filter((patient) => patient.riskScore >= 90).length
    const content = [
      'Doctor Shift Summary',
      `Total patients: ${patients.length}`,
      `Critical patients: ${criticalPatients}`,
      `High-risk patients: ${patients.filter((patient) => patient.riskScore >= 70 && patient.riskScore < 90).length}`,
      '',
      'Assigned workloads are managed locally while the backend is unavailable.',
    ].join('\n')

    return buildReportBlob('Doctor Shift Summary', content)
  }

  const nurses = readStaffMembers().filter((member) => member.role === 'nurse')
  const content = [
    'Nurse Handover Summary',
    `Active nurses: ${nurses.filter((nurse) => nurse.active).length}`,
    `Total nurses: ${nurses.length}`,
    `Patients under observation: ${getLocalPatients().filter((patient) => patient.riskScore >= 45).length}`,
  ].join('\n')

  return buildReportBlob('Nurse Handover Summary', content)
}

export function isLocalDevSession() {
  if (!hasStorage()) {
    return false
  }

  const token = localStorage.getItem('token') ?? ''
  return token.startsWith('local-dev-')
}
