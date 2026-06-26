import { configureStore } from '@reduxjs/toolkit';
import alertReducer from './slices/alertSlice';
import vitalReducer from './slices/vitalSlice';
import patientReducer from './slices/patientSlice';

export const store = configureStore({
  reducer: {
    alerts: alertReducer,
    vitals: vitalReducer,
    patients: patientReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
