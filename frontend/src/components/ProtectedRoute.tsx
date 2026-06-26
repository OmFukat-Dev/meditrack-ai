import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface ProtectedRouteProps {
  allowedRoles?: Array<'admin' | 'doctor' | 'nurse' | 'patient' | 'viewer'>;
}

export function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const { user, isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return null;
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && !allowedRoles.includes(user.role)) {
    // Redirect to appropriate dashboard based on user's role
    switch (user.role) {
      case 'admin':
        return <Navigate to="/admin" replace />;
      case 'doctor':
        return <Navigate to="/doctor" replace />;
      case 'nurse':
        return <Navigate to="/nurse" replace />;
      case 'viewer':
      case 'patient':
        return <Navigate to="/patient" replace />;
      default:
        return <Navigate to="/login" replace />;
    }
  }

  return <Outlet />;
}
