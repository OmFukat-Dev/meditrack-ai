// Mock Database from Seed Data - Matches the actual database structure
// This file contains the same data as the database seed files

export interface User {
  id: string;
  email: string;
  password: string;
  name: string;
  role: 'admin' | 'doctor' | 'nurse' | 'viewer';
  department: string;
  isActive: boolean;
  createdAt: string;
}

export interface Patient {
  id: string;
  patientIdentifier: string;
  email: string;
  password: string;
  name: string;
  age: number;
  gender: 'Male' | 'Female';
  roomNumber: string;
  condition: 'STABLE' | 'CRITICAL' | 'EMERGENCY';
  admittedDate: string;
  doctorId: string;
  nurseId: string;
  mobileNumber: string;
  guardianName: string;
  guardianMobile: string;
  vitals: VitalSign[];
  reports: MedicalReport[];
}

export interface VitalSign {
  id: string;
  patientId: string;
  timestamp: string;
  heartRate: number;
  bloodPressure: {
    systolic: number;
    diastolic: number;
  };
  temperature: number;
  oxygenSaturation: number;
  recordedBy: string;
}

export interface MedicalReport {
  id: string;
  patientId: string;
  doctorId: string;
  title: string;
  content: string;
  createdAt: string;
}

// Users from database seed
export const mockUsers: User[] = [
  { id: 'admin-om', email: 'om@meditrackadmin.ai', password: 'password123', name: 'Om Fukat', role: 'admin', department: 'Administration', isActive: true, createdAt: '2024-01-01' },
  { id: 'doc-dipanshu', email: 'dipanshu@meditrackcardiology.ai', password: 'password123', name: 'Dipanshu Sharma', role: 'doctor', department: 'Cardiology', isActive: true, createdAt: '2024-01-01' },
  { id: 'nurse-sarah', email: 'sarah@meditrackcardiology.ai', password: 'password123', name: 'Sarah Johnson', role: 'nurse', department: 'Cardiology', isActive: true, createdAt: '2024-01-01' }
];

// ─────────────────────────────────────────────────────────────────────────────
// DEPARTMENT ↔ STAFF MAPPING  (each department has 1 doctor + 1 nurse)
// Cardiology   : doc-dipanshu  / nurse-sarah
// Pediatrics   : doc-tanmay    / nurse-jessica
// Neurology    : doc-ayush     / nurse-emily
// Oncology     : doc-chetan    / nurse-monalisa
// Orthopedics  : doc-monir     / nurse-lana
//
// PATIENT DISTRIBUTION (5 patients per department, IDs patient-1 … patient-25)
// Cardiology   : patient-1  patient-2  patient-3  patient-4  patient-5
// Pediatrics   : patient-6  patient-7  patient-8  patient-9  patient-10
// Neurology    : patient-11 patient-12 patient-13 patient-14 patient-15
// Oncology     : patient-16 patient-17 patient-18 patient-19 patient-20
// Orthopedics  : patient-21 patient-22 patient-23 patient-24 patient-25
// ─────────────────────────────────────────────────────────────────────────────

// Vital signs — 2-3 readings per patient, recorded by the department nurse
export const mockVitalSigns: VitalSign[] = [
  // ── Cardiology (nurse-sarah) ──────────────────────────────────────────────
  { id: 'vs-c1a', patientId: 'patient-1', timestamp: '2024-05-20T08:00:00Z', heartRate: 72, bloodPressure: { systolic: 120, diastolic: 80 }, temperature: 98.6, oxygenSaturation: 98, recordedBy: 'nurse-sarah' },
  { id: 'vs-c1b', patientId: 'patient-1', timestamp: '2024-05-20T14:00:00Z', heartRate: 75, bloodPressure: { systolic: 118, diastolic: 78 }, temperature: 98.4, oxygenSaturation: 97, recordedBy: 'nurse-sarah' },
  { id: 'vs-c1c', patientId: 'patient-1', timestamp: '2024-05-20T20:00:00Z', heartRate: 70, bloodPressure: { systolic: 122, diastolic: 82 }, temperature: 98.8, oxygenSaturation: 98, recordedBy: 'nurse-sarah' },
  { id: 'vs-c2a', patientId: 'patient-2', timestamp: '2024-05-20T09:00:00Z', heartRate: 145, bloodPressure: { systolic: 160, diastolic: 100 }, temperature: 101.2, oxygenSaturation: 88, recordedBy: 'nurse-sarah' },
  { id: 'vs-c2b', patientId: 'patient-2', timestamp: '2024-05-20T15:00:00Z', heartRate: 138, bloodPressure: { systolic: 155, diastolic: 95 }, temperature: 100.8, oxygenSaturation: 90, recordedBy: 'nurse-sarah' },
  { id: 'vs-c3a', patientId: 'patient-3', timestamp: '2024-05-20T07:30:00Z', heartRate: 82, bloodPressure: { systolic: 130, diastolic: 85 }, temperature: 99.1, oxygenSaturation: 96, recordedBy: 'nurse-sarah' },
  { id: 'vs-c3b', patientId: 'patient-3', timestamp: '2024-05-20T13:30:00Z', heartRate: 79, bloodPressure: { systolic: 128, diastolic: 84 }, temperature: 98.9, oxygenSaturation: 97, recordedBy: 'nurse-sarah' },
  { id: 'vs-c4a', patientId: 'patient-4', timestamp: '2024-05-20T08:45:00Z', heartRate: 110, bloodPressure: { systolic: 148, diastolic: 95 }, temperature: 100.5, oxygenSaturation: 92, recordedBy: 'nurse-sarah' },
  { id: 'vs-c4b', patientId: 'patient-4', timestamp: '2024-05-20T16:45:00Z', heartRate: 105, bloodPressure: { systolic: 144, diastolic: 92 }, temperature: 100.1, oxygenSaturation: 93, recordedBy: 'nurse-sarah' },
  { id: 'vs-c5a', patientId: 'patient-5', timestamp: '2024-05-20T09:30:00Z', heartRate: 98, bloodPressure: { systolic: 142, diastolic: 90 }, temperature: 99.5, oxygenSaturation: 95, recordedBy: 'nurse-sarah' },
  { id: 'vs-c5b', patientId: 'patient-5', timestamp: '2024-05-20T17:30:00Z', heartRate: 94, bloodPressure: { systolic: 138, diastolic: 88 }, temperature: 99.2, oxygenSaturation: 96, recordedBy: 'nurse-sarah' },
  // ── Pediatrics (nurse-jessica) ────────────────────────────────────────────
  { id: 'vs-p6a', patientId: 'patient-6', timestamp: '2024-05-20T08:00:00Z', heartRate: 68, bloodPressure: { systolic: 115, diastolic: 75 }, temperature: 98.2, oxygenSaturation: 99, recordedBy: 'nurse-jessica' },
  { id: 'vs-p6b', patientId: 'patient-6', timestamp: '2024-05-20T16:00:00Z', heartRate: 71, bloodPressure: { systolic: 118, diastolic: 76 }, temperature: 98.4, oxygenSaturation: 99, recordedBy: 'nurse-jessica' },
  { id: 'vs-p7a', patientId: 'patient-7', timestamp: '2024-05-20T09:00:00Z', heartRate: 85, bloodPressure: { systolic: 125, diastolic: 82 }, temperature: 99.0, oxygenSaturation: 97, recordedBy: 'nurse-jessica' },
  { id: 'vs-p7b', patientId: 'patient-7', timestamp: '2024-05-20T17:00:00Z', heartRate: 88, bloodPressure: { systolic: 128, diastolic: 84 }, temperature: 99.2, oxygenSaturation: 96, recordedBy: 'nurse-jessica' },
  { id: 'vs-p8a', patientId: 'patient-8', timestamp: '2024-05-20T10:00:00Z', heartRate: 70, bloodPressure: { systolic: 118, diastolic: 78 }, temperature: 98.6, oxygenSaturation: 98, recordedBy: 'nurse-jessica' },
  { id: 'vs-p8b', patientId: 'patient-8', timestamp: '2024-05-20T18:00:00Z', heartRate: 72, bloodPressure: { systolic: 120, diastolic: 80 }, temperature: 98.8, oxygenSaturation: 98, recordedBy: 'nurse-jessica' },
  { id: 'vs-p9a', patientId: 'patient-9', timestamp: '2024-05-20T11:00:00Z', heartRate: 118, bloodPressure: { systolic: 152, diastolic: 98 }, temperature: 100.5, oxygenSaturation: 91, recordedBy: 'nurse-jessica' },
  { id: 'vs-p9b', patientId: 'patient-9', timestamp: '2024-05-20T19:00:00Z', heartRate: 112, bloodPressure: { systolic: 148, diastolic: 95 }, temperature: 100.1, oxygenSaturation: 92, recordedBy: 'nurse-jessica' },
  { id: 'vs-p10a', patientId: 'patient-10', timestamp: '2024-05-20T12:00:00Z', heartRate: 74, bloodPressure: { systolic: 120, diastolic: 78 }, temperature: 98.6, oxygenSaturation: 98, recordedBy: 'nurse-jessica' },
  { id: 'vs-p10b', patientId: 'patient-10', timestamp: '2024-05-20T20:00:00Z', heartRate: 76, bloodPressure: { systolic: 122, diastolic: 80 }, temperature: 98.8, oxygenSaturation: 98, recordedBy: 'nurse-jessica' },
  // ── Neurology (nurse-emily) ───────────────────────────────────────────────
  { id: 'vs-n11a', patientId: 'patient-11', timestamp: '2024-05-20T07:00:00Z', heartRate: 92, bloodPressure: { systolic: 140, diastolic: 90 }, temperature: 99.5, oxygenSaturation: 85, recordedBy: 'nurse-emily' },
  { id: 'vs-n11b', patientId: 'patient-11', timestamp: '2024-05-20T15:00:00Z', heartRate: 86, bloodPressure: { systolic: 136, diastolic: 88 }, temperature: 99.2, oxygenSaturation: 87, recordedBy: 'nurse-emily' },
  { id: 'vs-n12a', patientId: 'patient-12', timestamp: '2024-05-20T08:00:00Z', heartRate: 78, bloodPressure: { systolic: 125, diastolic: 82 }, temperature: 98.4, oxygenSaturation: 96, recordedBy: 'nurse-emily' },
  { id: 'vs-n12b', patientId: 'patient-12', timestamp: '2024-05-20T16:00:00Z', heartRate: 80, bloodPressure: { systolic: 128, diastolic: 84 }, temperature: 98.6, oxygenSaturation: 95, recordedBy: 'nurse-emily' },
  { id: 'vs-n13a', patientId: 'patient-13', timestamp: '2024-05-20T09:00:00Z', heartRate: 76, bloodPressure: { systolic: 118, diastolic: 76 }, temperature: 98.4, oxygenSaturation: 98, recordedBy: 'nurse-emily' },
  { id: 'vs-n13b', patientId: 'patient-13', timestamp: '2024-05-20T17:00:00Z', heartRate: 74, bloodPressure: { systolic: 116, diastolic: 74 }, temperature: 98.2, oxygenSaturation: 99, recordedBy: 'nurse-emily' },
  { id: 'vs-n14a', patientId: 'patient-14', timestamp: '2024-05-20T10:00:00Z', heartRate: 72, bloodPressure: { systolic: 120, diastolic: 80 }, temperature: 98.6, oxygenSaturation: 99, recordedBy: 'nurse-emily' },
  { id: 'vs-n14b', patientId: 'patient-14', timestamp: '2024-05-20T18:00:00Z', heartRate: 75, bloodPressure: { systolic: 122, diastolic: 82 }, temperature: 98.8, oxygenSaturation: 99, recordedBy: 'nurse-emily' },
  { id: 'vs-n15a', patientId: 'patient-15', timestamp: '2024-05-20T11:00:00Z', heartRate: 88, bloodPressure: { systolic: 135, diastolic: 85 }, temperature: 99.0, oxygenSaturation: 94, recordedBy: 'nurse-emily' },
  { id: 'vs-n15b', patientId: 'patient-15', timestamp: '2024-05-20T19:00:00Z', heartRate: 84, bloodPressure: { systolic: 132, diastolic: 83 }, temperature: 98.8, oxygenSaturation: 95, recordedBy: 'nurse-emily' },
  // ── Oncology (nurse-monalisa) ─────────────────────────────────────────────
  { id: 'vs-o16a', patientId: 'patient-16', timestamp: '2024-05-20T08:00:00Z', heartRate: 88, bloodPressure: { systolic: 145, diastolic: 92 }, temperature: 99.8, oxygenSaturation: 91, recordedBy: 'nurse-monalisa' },
  { id: 'vs-o16b', patientId: 'patient-16', timestamp: '2024-05-20T16:00:00Z', heartRate: 85, bloodPressure: { systolic: 142, diastolic: 90 }, temperature: 99.5, oxygenSaturation: 92, recordedBy: 'nurse-monalisa' },
  { id: 'vs-o17a', patientId: 'patient-17', timestamp: '2024-05-20T09:00:00Z', heartRate: 112, bloodPressure: { systolic: 148, diastolic: 96 }, temperature: 100.2, oxygenSaturation: 92, recordedBy: 'nurse-monalisa' },
  { id: 'vs-o17b', patientId: 'patient-17', timestamp: '2024-05-20T17:00:00Z', heartRate: 108, bloodPressure: { systolic: 145, diastolic: 93 }, temperature: 99.9, oxygenSaturation: 93, recordedBy: 'nurse-monalisa' },
  { id: 'vs-o18a', patientId: 'patient-18', timestamp: '2024-05-20T10:00:00Z', heartRate: 72, bloodPressure: { systolic: 120, diastolic: 80 }, temperature: 98.6, oxygenSaturation: 99, recordedBy: 'nurse-monalisa' },
  { id: 'vs-o18b', patientId: 'patient-18', timestamp: '2024-05-20T18:00:00Z', heartRate: 74, bloodPressure: { systolic: 122, diastolic: 80 }, temperature: 98.8, oxygenSaturation: 99, recordedBy: 'nurse-monalisa' },
  { id: 'vs-o19a', patientId: 'patient-19', timestamp: '2024-05-20T11:00:00Z', heartRate: 96, bloodPressure: { systolic: 140, diastolic: 92 }, temperature: 99.6, oxygenSaturation: 88, recordedBy: 'nurse-monalisa' },
  { id: 'vs-o19b', patientId: 'patient-19', timestamp: '2024-05-20T19:00:00Z', heartRate: 90, bloodPressure: { systolic: 136, diastolic: 89 }, temperature: 99.3, oxygenSaturation: 90, recordedBy: 'nurse-monalisa' },
  { id: 'vs-o20a', patientId: 'patient-20', timestamp: '2024-05-20T12:00:00Z', heartRate: 75, bloodPressure: { systolic: 118, diastolic: 76 }, temperature: 98.4, oxygenSaturation: 99, recordedBy: 'nurse-monalisa' },
  { id: 'vs-o20b', patientId: 'patient-20', timestamp: '2024-05-20T20:00:00Z', heartRate: 78, bloodPressure: { systolic: 120, diastolic: 78 }, temperature: 98.6, oxygenSaturation: 98, recordedBy: 'nurse-monalisa' },
  // ── Orthopedics (nurse-lana) ──────────────────────────────────────────────
  { id: 'vs-r21a', patientId: 'patient-21', timestamp: '2024-05-20T08:00:00Z', heartRate: 65, bloodPressure: { systolic: 110, diastolic: 70 }, temperature: 98.0, oxygenSaturation: 100, recordedBy: 'nurse-lana' },
  { id: 'vs-r21b', patientId: 'patient-21', timestamp: '2024-05-20T16:00:00Z', heartRate: 67, bloodPressure: { systolic: 112, diastolic: 72 }, temperature: 98.2, oxygenSaturation: 100, recordedBy: 'nurse-lana' },
  { id: 'vs-r22a', patientId: 'patient-22', timestamp: '2024-05-20T09:00:00Z', heartRate: 78, bloodPressure: { systolic: 125, diastolic: 82 }, temperature: 98.4, oxygenSaturation: 96, recordedBy: 'nurse-lana' },
  { id: 'vs-r22b', patientId: 'patient-22', timestamp: '2024-05-20T17:00:00Z', heartRate: 80, bloodPressure: { systolic: 127, diastolic: 83 }, temperature: 98.6, oxygenSaturation: 97, recordedBy: 'nurse-lana' },
  { id: 'vs-r23a', patientId: 'patient-23', timestamp: '2024-05-20T10:00:00Z', heartRate: 88, bloodPressure: { systolic: 135, diastolic: 85 }, temperature: 99.0, oxygenSaturation: 94, recordedBy: 'nurse-lana' },
  { id: 'vs-r23b', patientId: 'patient-23', timestamp: '2024-05-20T18:00:00Z', heartRate: 84, bloodPressure: { systolic: 132, diastolic: 83 }, temperature: 98.8, oxygenSaturation: 95, recordedBy: 'nurse-lana' },
  { id: 'vs-r24a', patientId: 'patient-24', timestamp: '2024-05-20T11:00:00Z', heartRate: 94, bloodPressure: { systolic: 138, diastolic: 88 }, temperature: 99.4, oxygenSaturation: 90, recordedBy: 'nurse-lana' },
  { id: 'vs-r24b', patientId: 'patient-24', timestamp: '2024-05-20T19:00:00Z', heartRate: 90, bloodPressure: { systolic: 135, diastolic: 86 }, temperature: 99.1, oxygenSaturation: 91, recordedBy: 'nurse-lana' },
  { id: 'vs-r25a', patientId: 'patient-25', timestamp: '2024-05-20T12:00:00Z', heartRate: 70, bloodPressure: { systolic: 118, diastolic: 76 }, temperature: 98.6, oxygenSaturation: 98, recordedBy: 'nurse-lana' },
  { id: 'vs-r25b', patientId: 'patient-25', timestamp: '2024-05-20T20:00:00Z', heartRate: 72, bloodPressure: { systolic: 120, diastolic: 78 }, temperature: 98.8, oxygenSaturation: 98, recordedBy: 'nurse-lana' },
];

// Patients — 5 per department, each assigned to the correct department doctor + nurse
// Cardiology: doc-dipanshu + nurse-sarah     → patient-1  … patient-5
// Pediatrics: doc-tanmay   + nurse-jessica   → patient-6  … patient-10
// Neurology:  doc-ayush    + nurse-emily     → patient-11 … patient-15
// Oncology:   doc-chetan   + nurse-monalisa  → patient-16 … patient-20
// Orthopedics:doc-monir    + nurse-lana      → patient-21 … patient-25
export const mockPatients: Patient[] = [
  // ── Cardiology ────────────────────────────────────────────────────────────
  { id: 'patient-1', patientIdentifier: 'PT-001', email: 'john.doe@email.com', password: 'password123', name: 'John Doe', age: 45, gender: 'Male', roomNumber: 'A-101', condition: 'STABLE', admittedDate: '2024-04-15', doctorId: 'doc-dipanshu', nurseId: 'nurse-sarah', mobileNumber: '+1-555-0101', guardianName: 'Jane Doe', guardianMobile: '+1-555-0102', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-1'), reports: [] },
  { id: 'patient-2', patientIdentifier: 'PT-002', email: 'jane.smith@email.com', password: 'password123', name: 'Jane Smith', age: 32, gender: 'Female', roomNumber: 'A-102', condition: 'CRITICAL', admittedDate: '2024-04-18', doctorId: 'doc-dipanshu', nurseId: 'nurse-sarah', mobileNumber: '+1-555-0103', guardianName: 'Robert Smith', guardianMobile: '+1-555-0104', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-2'), reports: [] },
  { id: 'patient-3', patientIdentifier: 'PT-003', email: 'alice.cooper@email.com', password: 'password123', name: 'Alice Cooper', age: 58, gender: 'Female', roomNumber: 'A-103', condition: 'STABLE', admittedDate: '2024-04-20', doctorId: 'doc-dipanshu', nurseId: 'nurse-sarah', mobileNumber: '+1-555-0105', guardianName: 'Bob Cooper', guardianMobile: '+1-555-0106', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-3'), reports: [] },
  { id: 'patient-4', patientIdentifier: 'PT-004', email: 'henry.ford@email.com', password: 'password123', name: 'Henry Ford', age: 62, gender: 'Male', roomNumber: 'A-104', condition: 'EMERGENCY', admittedDate: '2024-04-22', doctorId: 'doc-dipanshu', nurseId: 'nurse-sarah', mobileNumber: '+1-555-0107', guardianName: 'Mary Ford', guardianMobile: '+1-555-0108', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-4'), reports: [] },
  { id: 'patient-5', patientIdentifier: 'PT-005', email: 'linda.evans@email.com', password: 'password123', name: 'Linda Evans', age: 49, gender: 'Female', roomNumber: 'A-105', condition: 'STABLE', admittedDate: '2024-04-25', doctorId: 'doc-dipanshu', nurseId: 'nurse-sarah', mobileNumber: '+1-555-0109', guardianName: 'Tom Evans', guardianMobile: '+1-555-0110', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-5'), reports: [] },
  // ── Pediatrics ────────────────────────────────────────────────────────────
  { id: 'patient-6', patientIdentifier: 'PT-006', email: 'emily.brown@email.com', password: 'password123', name: 'Emily Brown', age: 8, gender: 'Female', roomNumber: 'B-101', condition: 'STABLE', admittedDate: '2024-04-27', doctorId: 'doc-tanmay', nurseId: 'nurse-jessica', mobileNumber: '+1-555-0111', guardianName: 'David Brown', guardianMobile: '+1-555-0112', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-6'), reports: [] },
  { id: 'patient-7', patientIdentifier: 'PT-007', email: 'liam.jones@email.com', password: 'password123', name: 'Liam Jones', age: 12, gender: 'Male', roomNumber: 'B-102', condition: 'CRITICAL', admittedDate: '2024-04-29', doctorId: 'doc-tanmay', nurseId: 'nurse-jessica', mobileNumber: '+1-555-0113', guardianName: 'Sarah Jones', guardianMobile: '+1-555-0114', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-7'), reports: [] },
  { id: 'patient-8', patientIdentifier: 'PT-008', email: 'mia.clark@email.com', password: 'password123', name: 'Mia Clark', age: 6, gender: 'Female', roomNumber: 'B-103', condition: 'STABLE', admittedDate: '2024-05-01', doctorId: 'doc-tanmay', nurseId: 'nurse-jessica', mobileNumber: '+1-555-0115', guardianName: 'Thomas Clark', guardianMobile: '+1-555-0116', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-8'), reports: [] },
  { id: 'patient-9', patientIdentifier: 'PT-009', email: 'noah.white@email.com', password: 'password123', name: 'Noah White', age: 10, gender: 'Male', roomNumber: 'B-104', condition: 'EMERGENCY', admittedDate: '2024-05-03', doctorId: 'doc-tanmay', nurseId: 'nurse-jessica', mobileNumber: '+1-555-0117', guardianName: 'Anna White', guardianMobile: '+1-555-0118', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-9'), reports: [] },
  { id: 'patient-10', patientIdentifier: 'PT-010', email: 'sofia.harris@email.com', password: 'password123', name: 'Sofia Harris', age: 14, gender: 'Female', roomNumber: 'B-105', condition: 'STABLE', admittedDate: '2024-05-05', doctorId: 'doc-tanmay', nurseId: 'nurse-jessica', mobileNumber: '+1-555-0119', guardianName: 'Eric Harris', guardianMobile: '+1-555-0120', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-10'), reports: [] },
  // ── Neurology ─────────────────────────────────────────────────────────────
  { id: 'patient-11', patientIdentifier: 'PT-011', email: 'robert.johnson@email.com', password: 'password123', name: 'Robert Johnson', age: 55, gender: 'Male', roomNumber: 'C-101', condition: 'EMERGENCY', admittedDate: '2024-04-20', doctorId: 'doc-ayush', nurseId: 'nurse-emily', mobileNumber: '+1-555-0121', guardianName: 'Mary Johnson', guardianMobile: '+1-555-0122', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-11'), reports: [] },
  { id: 'patient-12', patientIdentifier: 'PT-012', email: 'patricia.thomas@email.com', password: 'password123', name: 'Patricia Thomas', age: 61, gender: 'Female', roomNumber: 'C-102', condition: 'STABLE', admittedDate: '2024-05-05', doctorId: 'doc-ayush', nurseId: 'nurse-emily', mobileNumber: '+1-555-0123', guardianName: 'Robert Thomas', guardianMobile: '+1-555-0124', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-12'), reports: [] },
  { id: 'patient-13', patientIdentifier: 'PT-013', email: 'paul.walker@email.com', password: 'password123', name: 'Paul Walker', age: 43, gender: 'Male', roomNumber: 'C-103', condition: 'CRITICAL', admittedDate: '2024-05-10', doctorId: 'doc-ayush', nurseId: 'nurse-emily', mobileNumber: '+1-555-0125', guardianName: 'Linda Walker', guardianMobile: '+1-555-0126', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-13'), reports: [] },
  { id: 'patient-14', patientIdentifier: 'PT-014', email: 'donna.king@email.com', password: 'password123', name: 'Donna King', age: 38, gender: 'Female', roomNumber: 'C-104', condition: 'STABLE', admittedDate: '2024-05-15', doctorId: 'doc-ayush', nurseId: 'nurse-emily', mobileNumber: '+1-555-0127', guardianName: 'Richard King', guardianMobile: '+1-555-0128', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-14'), reports: [] },
  { id: 'patient-15', patientIdentifier: 'PT-015', email: 'james.taylor@email.com', password: 'password123', name: 'James Taylor', age: 47, gender: 'Male', roomNumber: 'C-105', condition: 'EMERGENCY', admittedDate: '2024-05-18', doctorId: 'doc-ayush', nurseId: 'nurse-emily', mobileNumber: '+1-555-0129', guardianName: 'Patricia Taylor', guardianMobile: '+1-555-0130', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-15'), reports: [] },
  // ── Oncology ──────────────────────────────────────────────────────────────
  { id: 'patient-16', patientIdentifier: 'PT-016', email: 'sarah.davis@email.com', password: 'password123', name: 'Sarah Davis', age: 52, gender: 'Female', roomNumber: 'D-101', condition: 'CRITICAL', admittedDate: '2024-04-27', doctorId: 'doc-chetan', nurseId: 'nurse-monalisa', mobileNumber: '+1-555-0131', guardianName: 'James Davis', guardianMobile: '+1-555-0132', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-16'), reports: [] },
  { id: 'patient-17', patientIdentifier: 'PT-017', email: 'mark.allen@email.com', password: 'password123', name: 'Mark Allen', age: 60, gender: 'Male', roomNumber: 'D-102', condition: 'EMERGENCY', admittedDate: '2024-05-05', doctorId: 'doc-chetan', nurseId: 'nurse-monalisa', mobileNumber: '+1-555-0133', guardianName: 'Diane Allen', guardianMobile: '+1-555-0134', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-17'), reports: [] },
  { id: 'patient-18', patientIdentifier: 'PT-018', email: 'sandra.young@email.com', password: 'password123', name: 'Sandra Young', age: 44, gender: 'Female', roomNumber: 'D-103', condition: 'STABLE', admittedDate: '2024-05-10', doctorId: 'doc-chetan', nurseId: 'nurse-monalisa', mobileNumber: '+1-555-0135', guardianName: 'Robert Young', guardianMobile: '+1-555-0136', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-18'), reports: [] },
  { id: 'patient-19', patientIdentifier: 'PT-019', email: 'kevin.hernandez@email.com', password: 'password123', name: 'Kevin Hernandez', age: 56, gender: 'Male', roomNumber: 'D-104', condition: 'CRITICAL', admittedDate: '2024-05-15', doctorId: 'doc-chetan', nurseId: 'nurse-monalisa', mobileNumber: '+1-555-0137', guardianName: 'Maria Hernandez', guardianMobile: '+1-555-0138', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-19'), reports: [] },
  { id: 'patient-20', patientIdentifier: 'PT-020', email: 'jennifer.robinson@email.com', password: 'password123', name: 'Jennifer Robinson', age: 48, gender: 'Female', roomNumber: 'D-105', condition: 'STABLE', admittedDate: '2024-05-20', doctorId: 'doc-chetan', nurseId: 'nurse-monalisa', mobileNumber: '+1-555-0139', guardianName: 'William Robinson', guardianMobile: '+1-555-0140', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-20'), reports: [] },
  // ── Orthopedics ───────────────────────────────────────────────────────────
  { id: 'patient-21', patientIdentifier: 'PT-021', email: 'david.miller@email.com', password: 'password123', name: 'David Miller', age: 50, gender: 'Male', roomNumber: 'E-101', condition: 'STABLE', admittedDate: '2024-04-29', doctorId: 'doc-monir', nurseId: 'nurse-lana', mobileNumber: '+1-555-0141', guardianName: 'Susan Miller', guardianMobile: '+1-555-0142', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-21'), reports: [] },
  { id: 'patient-22', patientIdentifier: 'PT-022', email: 'karen.hall@email.com', password: 'password123', name: 'Karen Hall', age: 40, gender: 'Female', roomNumber: 'E-102', condition: 'STABLE', admittedDate: '2024-05-03', doctorId: 'doc-monir', nurseId: 'nurse-lana', mobileNumber: '+1-555-0143', guardianName: 'Steven Hall', guardianMobile: '+1-555-0144', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-22'), reports: [] },
  { id: 'patient-23', patientIdentifier: 'PT-023', email: 'christopher.martinez@email.com', password: 'password123', name: 'Christopher Martinez', age: 65, gender: 'Male', roomNumber: 'E-103', condition: 'EMERGENCY', admittedDate: '2024-05-07', doctorId: 'doc-monir', nurseId: 'nurse-lana', mobileNumber: '+1-555-0145', guardianName: 'Maria Martinez', guardianMobile: '+1-555-0146', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-23'), reports: [] },
  { id: 'patient-24', patientIdentifier: 'PT-024', email: 'nancy.lewis@email.com', password: 'password123', name: 'Nancy Lewis', age: 57, gender: 'Female', roomNumber: 'E-104', condition: 'CRITICAL', admittedDate: '2024-05-12', doctorId: 'doc-monir', nurseId: 'nurse-lana', mobileNumber: '+1-555-0147', guardianName: 'George Lewis', guardianMobile: '+1-555-0148', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-24'), reports: [] },
  { id: 'patient-25', patientIdentifier: 'PT-025', email: 'lisa.anderson@email.com', password: 'password123', name: 'Lisa Anderson', age: 35, gender: 'Female', roomNumber: 'E-105', condition: 'STABLE', admittedDate: '2024-05-17', doctorId: 'doc-monir', nurseId: 'nurse-lana', mobileNumber: '+1-555-0149', guardianName: 'Thomas Anderson', guardianMobile: '+1-555-0150', vitals: mockVitalSigns.filter(v => v.patientId === 'patient-25'), reports: [] },
];

// Medical reports - Initially empty
export const mockMedicalReports: MedicalReport[] = [];

// Authentication function
export const authenticateUser = (email: string, password: string): User | null => {
  const user = mockUsers.find(u => u.email === email && u.password === password);
  return user || null;
};


// Database management functions
export const addNewUser = (userData: Partial<User>) => {
  const newUser: User = {
    id: `user-${Date.now()}`,
    email: userData.email || '',
    password: userData.password || 'password123',
    name: userData.name || '',
    role: userData.role || 'viewer',
    department: userData.department || '',
    isActive: true,
    createdAt: new Date().toISOString().split('T')[0]
  };
  mockUsers.push(newUser);
  return newUser;
};

export const updateUser = (userId: string, updates: Partial<User>) => {
  const index = mockUsers.findIndex(u => u.id === userId);
  if (index !== -1) {
    mockUsers[index] = { ...mockUsers[index], ...updates };
    return mockUsers[index];
  }
  throw new Error('User not found');
};

export const addNewPatient = (patientData: Partial<Patient>) => {
  const newPatient: Patient = {
    id: `patient-${Date.now()}`,
    patientIdentifier: `PT-${String(mockPatients.length + 1).padStart(3, '0')}`,
    email: patientData.email || '',
    password: patientData.password || 'password123',
    name: patientData.name || '',
    age: patientData.age || 0,
    gender: patientData.gender || 'Male',
    roomNumber: patientData.roomNumber || '',
    condition: patientData.condition || 'STABLE',
    admittedDate: patientData.admittedDate || new Date().toISOString().split('T')[0],
    doctorId: patientData.doctorId || '',
    nurseId: patientData.nurseId || '',
    mobileNumber: patientData.mobileNumber || '',
    guardianName: patientData.guardianName || '',
    guardianMobile: patientData.guardianMobile || '',
    vitals: [],
    reports: []
  };
  mockPatients.push(newPatient);
  return newPatient;
};

export const updatePatient = (patientId: string, updates: Partial<Patient>) => {
  const index = mockPatients.findIndex(p => p.id === patientId);
  if (index !== -1) {
    mockPatients[index] = { ...mockPatients[index], ...updates };
    return mockPatients[index];
  }
  throw new Error('Patient not found');
};

export const getPatientsByDoctor = (doctorId: string) => {
  return mockPatients.filter(p => p.doctorId === doctorId);
};

export const getPatientsByNurse = (nurseId: string) => {
  return mockPatients.filter(p => p.nurseId === nurseId);
};

export const getVitalSignsByPatient = (patientId: string) => {
  return mockVitalSigns.filter(v => v.patientId === patientId);
};

export const addVitalSign = (vitalData: Partial<VitalSign>) => {
  const newVital: VitalSign = {
    id: `vital-${Date.now()}`,
    patientId: vitalData.patientId || '',
    timestamp: vitalData.timestamp || new Date().toISOString(),
    heartRate: Number(vitalData.heartRate) || 0,
    bloodPressure: vitalData.bloodPressure || { systolic: 0, diastolic: 0 },
    temperature: Number(vitalData.temperature) || 0,
    oxygenSaturation: Number(vitalData.oxygenSaturation) || 0,
    recordedBy: vitalData.recordedBy || ''
  };
  mockVitalSigns.push(newVital);
  
  // Link to patient
  const patient = mockPatients.find(p => p.id === newVital.patientId);
  if (patient) {
    if (!patient.vitals) {
      patient.vitals = [];
    }
    patient.vitals.push(newVital);
  }
  
  return newVital;
};

export const generateMedicalReport = (patientId: string) => {
  const patient = mockPatients.find(p => p.id === patientId);
  const vitals = mockVitalSigns.filter(v => v.patientId === patientId);
  const doctor = mockUsers.find(u => u.id === patient?.doctorId);
  
  if (!patient || !doctor) return '';
  
  const report = `
MEDICAL REPORT
==============

PATIENT INFORMATION
------------------
Name: ${patient.name}
Age: ${patient.age}
Gender: ${patient.gender}
Room: ${patient.roomNumber}
Condition: ${patient.condition}
Admitted: ${patient.admittedDate}

ATTENDING PHYSICIAN
------------------
${doctor.name}
${doctor.department}

VITAL SIGNS ANALYSIS
--------------------
${vitals.map(v => `
Date: ${new Date(v.timestamp).toLocaleString()}
Heart Rate: ${v.heartRate} bpm
Blood Pressure: ${v.bloodPressure.systolic}/${v.bloodPressure.diastolic} mmHg
Temperature: ${v.temperature}°F
Oxygen Saturation: ${v.oxygenSaturation}%
Recorded by: ${v.recordedBy}
`).join('\n')}

ASSESSMENT
-----------
Patient condition: ${patient.condition}
Overall status: ${patient.condition === 'CRITICAL' ? 'Requires immediate attention' : 
                   patient.condition === 'EMERGENCY' ? 'Urgent care needed' : 
                   'Stable condition'}

TREATMENT PLAN
---------------
Continue current treatment protocol
Monitor vital signs regularly
${patient.condition === 'CRITICAL' ? 'Intensive care monitoring required' : ''}

PROGNOSIS
----------
${patient.condition === 'STABLE' ? 'Good' : 
  patient.condition === 'CRITICAL' ? 'Guarded' : 
  'Fair'}

--
Report generated on ${new Date().toLocaleString()}
Attending Physician: ${doctor.name}
Department: ${doctor.department}
  `.trim();
  
  return report;
};
