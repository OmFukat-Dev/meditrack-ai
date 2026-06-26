import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import SignIn from './pages/SignIn';
import AdminDashboardPro from './pages/AdminDashboardPro';
import DoctorDashboardRealTime from './pages/DoctorDashboardRealTime';
import NurseDashboardAPI from './pages/NurseDashboardAPI';
import NurseDashboardRealTime from './pages/NurseDashboardRealTime';
import ViewerDashboard from './pages/ViewerDashboard';
import { ProtectedRoute } from './components/ProtectedRoute';

function LoadingScreen() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-950">
      <div className="text-center">
        <div className="w-14 h-14 rounded-full border-4 border-primary-500/30 border-t-primary-400 animate-spin mx-auto mb-4" />
        <p className="text-white font-medium">Loading MediTrack session...</p>
      </div>
    </div>
  );
}

function RootRedirect() {
  const { user, isAuthenticated } = useAuth();

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  switch (user.role) {
    case 'admin':
      return <Navigate to="/admin" replace />;
    case 'doctor':
      return <Navigate to="/doctor" replace />;
    case 'nurse':
      return <Navigate to="/nurse" replace />;
    case 'patient':
    case 'viewer':
      return <Navigate to="/patient" replace />;
    default:
      return <Navigate to="/login" replace />;
  }
}

function LegacyViewerRedirect() {
  return <Navigate to="/patient" replace />;
}

export default function App() {
  const { isLoading } = useAuth();

  if (isLoading) {
    return <LoadingScreen />;
  }

  return (
    <Routes>
      <Route path="/login" element={<SignIn />} />
      <Route path="/" element={<RootRedirect />} />
      <Route path="/viewer" element={<LegacyViewerRedirect />} />

      <Route element={<ProtectedRoute allowedRoles={['admin']} />}>
        <Route path="/admin" element={<AdminDashboardPro />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['doctor']} />}>
        <Route path="/doctor" element={<DoctorDashboardRealTime />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['nurse']} />}>
        <Route path="/nurse" element={<NurseDashboardRealTime />} />
      </Route>

      <Route element={<ProtectedRoute allowedRoles={['patient', 'viewer']} />}>
        <Route path="/patient" element={<ViewerDashboard />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
