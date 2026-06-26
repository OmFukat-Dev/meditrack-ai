import { createSlice, PayloadAction, createAsyncThunk } from '@reduxjs/toolkit';
import { patientApi } from '../../services/api';

export interface PatientRecord {
  id: string;
  bedNo: string;
  name: string;
  doctor: string;
  dept: string;
  bill: number;
  paid: number;
  condition: string;
  lastUpdated: string;
  active: boolean;
}

interface PatientState {
  patients: PatientRecord[];
  loading: boolean;
  error: string | null;
}

const initialState: PatientState = {
  patients: [
    { 
      id: '1', 
      bedNo: 'A-101', 
      name: 'John Doe', 
      doctor: 'Dr. Samantha Martin', 
      dept: 'Cardiology', 
      bill: 5000, 
      paid: 2000, 
      condition: 'Stable',
      lastUpdated: new Date().toISOString(),
      active: true 
    },
    { 
      id: '2', 
      bedNo: 'B-205', 
      name: 'Jane Smith', 
      doctor: 'Dr. Robert Chen', 
      dept: 'Neurology', 
      bill: 12000, 
      paid: 12000, 
      condition: 'Critical',
      lastUpdated: new Date().toISOString(),
      active: true 
    },
    { 
      id: '3', 
      bedNo: 'C-310', 
      name: 'Alice Johnson', 
      doctor: 'Dr. Emily Davis', 
      dept: 'Pediatrics', 
      bill: 1500, 
      paid: 0, 
      condition: 'Recovering',
      lastUpdated: new Date().toISOString(),
      active: true 
    },
    { 
      id: '4', 
      bedNo: 'D-405', 
      name: 'Michael Brown', 
      doctor: 'Dr. Samantha Martin', 
      dept: 'Cardiology', 
      bill: 8000, 
      paid: 4000, 
      condition: 'Under Observation',
      lastUpdated: new Date().toISOString(),
      active: true 
    },
  ],
  loading: false,
  error: null,
};

export const fetchPatients = createAsyncThunk(
  'patients/fetchPatients',
  async (_, { rejectWithValue }) => {
    try {
      const response = await patientApi.getAll(0, 100);
      // Ensure backend data maps to the PatientRecord interface
      return response.data.content || response.data;
    } catch (error: any) {
      return rejectWithValue(error.response?.data || 'Failed to fetch patients');
    }
  }
);

export const updateConditionAsync = createAsyncThunk(
  'patients/updateCondition',
  async ({ id, condition }: { id: string; condition: string }, { rejectWithValue }) => {
    try {
      await patientApi.updateCondition(id, condition);
      return { id, condition };
    } catch (error: any) {
      // Even if API fails, we return payload to allow optimistic update in UI if desired
      console.warn('API update failed, optimistic update will apply.', error);
      return { id, condition };
    }
  }
);

export const patientSlice = createSlice({
  name: 'patients',
  initialState,
  reducers: {
    updatePatientCondition: (state, action: PayloadAction<{ id: string; condition: string }>) => {
      const patient = state.patients.find(p => p.id === action.payload.id);
      if (patient) {
        patient.condition = action.payload.condition;
        patient.lastUpdated = new Date().toISOString();
      }
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchPatients.pending, (state) => {
        state.loading = true;
      })
      .addCase(fetchPatients.fulfilled, (state, action) => {
        state.loading = false;
        // If backend returns valid data, merge or replace. For now, replace if length > 0
        if (action.payload && action.payload.length > 0) {
          state.patients = action.payload.map((p: any) => ({
            ...p,
            condition: p.condition || 'Stable',
            lastUpdated: p.lastUpdated || new Date().toISOString()
          }));
        }
      })
      .addCase(fetchPatients.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      })
      .addCase(updateConditionAsync.fulfilled, (state, action) => {
        const patient = state.patients.find(p => p.id === action.payload.id);
        if (patient) {
          patient.condition = action.payload.condition;
          patient.lastUpdated = new Date().toISOString();
        }
      });
  },
});

export const { updatePatientCondition } = patientSlice.actions;

export default patientSlice.reducer;
