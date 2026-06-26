import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export interface Alert {
  id: string;
  patientId: string;
  vitalType: string;
  message: string;
  severity: 'WARNING' | 'CRITICAL';
  timestamp: string;
  read: boolean;
}

interface AlertState {
  activeAlerts: Alert[];
  unreadCount: number;
}

const initialState: AlertState = {
  activeAlerts: [],
  unreadCount: 0,
};

const alertSlice = createSlice({
  name: 'alerts',
  initialState,
  reducers: {
    addAlert: (state, action: PayloadAction<Omit<Alert, 'id' | 'timestamp' | 'read'>>) => {
      const newAlert: Alert = {
        ...action.payload,
        id: Math.random().toString(36).substr(2, 9),
        timestamp: new Date().toISOString(),
        read: false,
      };
      state.activeAlerts.unshift(newAlert);
      state.unreadCount += 1;
    },
    markAsRead: (state, action: PayloadAction<string>) => {
      const alert = state.activeAlerts.find(a => a.id === action.payload);
      if (alert && !alert.read) {
        alert.read = true;
        state.unreadCount -= 1;
      }
    },
    markAllAsRead: (state) => {
      state.activeAlerts.forEach(a => a.read = true);
      state.unreadCount = 0;
    },
    clearAlerts: (state) => {
      state.activeAlerts = [];
      state.unreadCount = 0;
    }
  },
});

export const { addAlert, markAsRead, markAllAsRead, clearAlerts } = alertSlice.actions;
export default alertSlice.reducer;
