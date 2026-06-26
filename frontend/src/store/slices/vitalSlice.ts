import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export interface VitalReading {
  timestamp: string;
  heartRate: number;
  bloodPressureSys: number;
  bloodPressureDia: number;
  glucose: number;
}

interface VitalState {
  currentVitals: {
    hr: string;
    bp: string;
    glucose: string;
  };
  history: VitalReading[];
}

const initialState: VitalState = {
  currentVitals: {
    hr: '120',
    bp: '112/75',
    glucose: '162'
  },
  history: Array.from({ length: 20 }).map((_, i) => ({
    timestamp: new Date(Date.now() - (20 - i) * 60000).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    heartRate: 70 + Math.floor(Math.random() * 30),
    bloodPressureSys: 110 + Math.floor(Math.random() * 20),
    bloodPressureDia: 70 + Math.floor(Math.random() * 15),
    glucose: 90 + Math.floor(Math.random() * 40)
  })), // Pre-populate with some mock historical data for the chart
};

const vitalSlice = createSlice({
  name: 'vitals',
  initialState,
  reducers: {
    updateCurrentVitals: (state, action: PayloadAction<Partial<VitalState['currentVitals']>>) => {
      state.currentVitals = { ...state.currentVitals, ...action.payload };
    },
    addHistoricalReading: (state, action: PayloadAction<VitalReading>) => {
      state.history.push(action.payload);
      if (state.history.length > 50) {
        state.history.shift(); // Keep only last 50 readings
      }
    }
  },
});

export const { updateCurrentVitals, addHistoricalReading } = vitalSlice.actions;
export default vitalSlice.reducer;
