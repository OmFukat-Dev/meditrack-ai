import React from 'react';
import ReactDOM from 'react-dom/client';
import { ThemeProvider, CssBaseline } from '@mui/material';
import { BrowserRouter } from 'react-router-dom';
import { darkTheme } from './theme/theme';
import App from './App';
import { AuthProvider } from './context/AuthContext';
import './index.css';

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <ThemeProvider theme={darkTheme}>
          <CssBaseline />
          <App />
        </ThemeProvider>
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
