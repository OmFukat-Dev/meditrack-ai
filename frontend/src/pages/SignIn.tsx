import { useState, type FormEvent, type ReactNode } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Activity, HeartPulse, Shield, User } from 'lucide-react';
import { authApi } from '../services/api';
import { useAuth } from '../context/AuthContext';

type SignInRole = 'admin' | 'doctor' | 'nurse' | 'viewer';

const DEPARTMENTS = ['Cardiology', 'Neurology', 'Orthopedics', 'Pediatrics', 'Oncology'];

export default function SignIn() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [selectedRole, setSelectedRole] = useState<SignInRole>('doctor');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [patientIdentifier, setPatientIdentifier] = useState('');
  const [patientName, setPatientName] = useState('');
  const [bedNumber, setBedNumber] = useState('');
  const [wardNumber, setWardNumber] = useState('');
  const [department, setDepartment] = useState('Neurology');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSignIn = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError('');

    const trimmedEmail = email.trim();
    const payload = buildPayload(selectedRole, {
      email: trimmedEmail,
      password: password.trim(),
      patientIdentifier: patientIdentifier.trim(),
      patientName: patientName.trim(),
      bedNumber: bedNumber.trim(),
      wardNumber: wardNumber.trim(),
      department: department.trim(),
    });

    if ('error' in payload) {
      setError(payload.error ?? '');
      return;
    }

    setSubmitting(true);
    try {
      const response = await authApi.login(payload.body);
      const responseData = response.data as {
        success: boolean;
        message: string;
        token: string;
        sessionToken?: string;
        id?: string;
        email?: string;
        name?: string;
        role?: string;
        department?: string;
        bedNumber?: string;
        patientIdentifier?: string;
        wardNumber?: string;
        user?: {
          id: string;
          email: string;
          firstName: string;
          lastName: string;
          roleName: string;
          departmentName?: string;
          bedNumber?: string;
          patientIdentifier?: string;
          wardNumber?: string;
        };
      };

      // The gateway returns a flat access-principal response, while older auth
      // implementations return the user under `user`. Support both contracts.
      const userDetail = responseData.user;
      const userRole = normalizeRole(userDetail?.roleName || responseData.role || 'viewer');
      const fullName = userDetail
        ? `${userDetail.firstName || ''} ${userDetail.lastName || ''}`.trim() || 'User'
        : responseData.name?.trim() || 'User';
      const token = responseData.token || responseData.sessionToken || '';

      login({
        id: userDetail?.id || responseData.id || '',
        name: fullName,
        role: userRole as 'admin' | 'doctor' | 'nurse' | 'patient' | 'viewer',
        email: userDetail?.email || responseData.email,
        department: userDetail?.departmentName || responseData.department || undefined,
        bedNumber: userDetail?.bedNumber || responseData.bedNumber || undefined,
        patientIdentifier: userDetail?.patientIdentifier || responseData.patientIdentifier || undefined,
        wardNumber: userDetail?.wardNumber || responseData.wardNumber || undefined,
        token: token,
        avatar: avatarForRole(userRole),
      });

      navigate(resolveDashboardPath(userRole), { replace: true });
    } catch (requestError: any) {
      if (!requestError?.response) {
        setError('Unable to reach the backend. Make sure the MediTrack services are running and try again.');
        return;
      }

      const message =
        requestError?.response?.data?.error ||
        requestError?.response?.data?.message ||
        requestError?.message ||
        'Invalid credentials. Only registered staff and patients can sign in.';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-dark-950 overflow-hidden relative">
      <div className="absolute top-1/4 left-1/4 w-[500px] h-[500px] bg-primary-500/10 rounded-full blur-[100px] pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/4 w-[400px] h-[400px] bg-secondary-500/10 rounded-full blur-[100px] pointer-events-none" />

      <motion.div
        initial={{ opacity: 0, scale: 0.95, y: 20 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        transition={{ duration: 0.5, ease: [0.4, 0, 0.2, 1] }}
        className="glass-panel w-full max-w-md p-10 z-10 mx-4"
      >
        <div className="flex flex-col items-center mb-8">
          <div className="w-16 h-16 rounded-2xl bg-dark-800/80 border border-white/5 flex items-center justify-center mb-6 shadow-glow">
            <Activity size={32} className="text-primary-400" />
          </div>
          <h1 className="text-3xl font-bold bg-gradient-primary text-transparent bg-clip-text mb-2">MediTrack AI</h1>
          <p className="text-dark-300 text-sm">Sign in to the central network</p>
        </div>

        <div className="grid grid-cols-4 gap-3 mb-8">
          <RoleSelector
            icon={<Shield size={20} />}
            label="Admin"
            active={selectedRole === 'admin'}
            onClick={() => {
              setSelectedRole('admin');
              setError('');
            }}
          />
          <RoleSelector
            icon={<HeartPulse size={20} />}
            label="Doctor"
            active={selectedRole === 'doctor'}
            onClick={() => {
              setSelectedRole('doctor');
              setError('');
            }}
          />
          <RoleSelector
            icon={<Activity size={20} />}
            label="Nurse"
            active={selectedRole === 'nurse'}
            onClick={() => {
              setSelectedRole('nurse');
              setError('');
            }}
          />
          <RoleSelector
            icon={<User size={20} />}
            label="Viewer"
            active={selectedRole === 'viewer'}
            onClick={() => {
              setSelectedRole('viewer');
              setError('');
            }}
          />
        </div>

        <form onSubmit={handleSignIn} className="flex flex-col gap-5">
          {selectedRole !== 'viewer' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col gap-4">
              <p className="text-xs text-dark-400">
                {selectedRole === 'admin'
                  ? 'Format: name@meditrackadmin.ai'
                  : 'Format: name@meditrack[department].ai (for example, neurology)'}
              </p>
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-dark-300">Hospital Email Address</label>
                <input
                  required
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  className="input-field"
                  placeholder="name@meditrack.ai"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-dark-300">Access Code</label>
                <input
                  required
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  className="input-field"
                  placeholder="••••••••"
                />
              </div>
            </motion.div>
          )}

          {selectedRole === 'viewer' && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col gap-4">
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-dark-300">Patient Email Address</label>
                <input
                  required
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  className="input-field"
                  placeholder="patient@example.com"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-dark-300">Patient Identifier</label>
                <input
                  required
                  type="text"
                  value={patientIdentifier}
                  onChange={(event) => setPatientIdentifier(event.target.value)}
                  className="input-field"
                  placeholder="e.g. PT-100"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-dark-300">Full Name</label>
                <input
                  required
                  type="text"
                  value={patientName}
                  onChange={(event) => setPatientName(event.target.value)}
                  className="input-field"
                  placeholder="John Doe"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-dark-300">Bed Number</label>
                <input
                  required
                  type="text"
                  value={bedNumber}
                  onChange={(event) => setBedNumber(event.target.value)}
                  className="input-field"
                  placeholder="e.g. B-12"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-dark-300">Ward Number</label>
                <input
                  required
                  type="text"
                  value={wardNumber}
                  onChange={(event) => setWardNumber(event.target.value)}
                  className="input-field"
                  placeholder="e.g. W-04"
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="text-sm font-medium text-dark-300">Department</label>
                <select
                  required
                  value={department}
                  onChange={(event) => setDepartment(event.target.value)}
                  className="input-field appearance-none cursor-pointer"
                  style={{
                    backgroundImage: `url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' fill='none' viewBox='0 0 20 20'%3E%3Cpath stroke='%2394a3b8' stroke-linecap='round' stroke-linejoin='round' stroke-width='1.5' d='m6 8 4 4 4-4'/%3E%3C/svg%3E")`,
                    backgroundPosition: 'right 1rem center',
                    backgroundSize: '1.25em 1.25em',
                    backgroundRepeat: 'no-repeat',
                  }}
                >
                  {DEPARTMENTS.map((item) => (
                    <option key={item} value={item} className="bg-dark-900 text-white">
                      {item}
                    </option>
                  ))}
                </select>
              </div>
            </motion.div>
          )}

          {error && (
            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="text-error-400 text-sm text-center bg-error-500/10 py-2 rounded-lg border border-error-500/20"
            >
              {error}
            </motion.p>
          )}

          <button type="submit" className="btn-primary mt-4 py-4 w-full text-base" disabled={submitting}>
            {submitting ? 'Signing in...' : 'Access System'}
          </button>
        </form>
      </motion.div>
    </div>
  );
}

function buildPayload(
  role: SignInRole,
    values: {
      email: string;
      password: string;
      patientIdentifier: string;
      patientName: string;
      bedNumber: string;
      wardNumber: string;
    department: string;
  }
): { body: Record<string, string>; error?: never } | { error: string } {
  if (role === 'admin') {
    if (!values.email.endsWith('@meditrackadmin.ai')) {
      return { error: 'Admin must use a @meditrackadmin.ai email address.' };
    }

    return {
      body: {
        role,
        email: values.email,
        name: values.email.split('@')[0].replace(/[._-]+/g, ' '),
        password: values.password,
      },
    };
  }

  if (role === 'doctor' || role === 'nurse') {
    if (!values.email.includes('@meditrack')) {
      return { error: 'Hospital email is required.' };
    }

    const domain = values.email.split('@')[1]?.toLowerCase();
    if (domain === 'meditrackadmin.ai') {
      return { error: `${role === 'doctor' ? 'Doctors' : 'Nurses'} cannot use the admin domain.` };
    }

    const departmentMatch = domain?.match(/^meditrack([a-z]+)\.ai$/);
    if (!departmentMatch?.[1]) {
      return { error: 'Use a registered department email address.' };
    }

    const derivedDepartment = capitalize(departmentMatch[1]);

    return {
      body: {
        role,
        email: values.email,
        department: derivedDepartment,
        name: values.email.split('@')[0].replace(/[._-]+/g, ' '),
        password: values.password,
      },
    };
  }

  if (!values.email || !values.patientIdentifier || !values.patientName || !values.bedNumber || !values.wardNumber || !values.department) {
    return { error: 'All viewer fields are required.' };
  }

  return {
    body: {
      role: 'viewer',
      email: values.email,
      password: values.password,
      patientIdentifier: values.patientIdentifier,
      name: values.patientName,
      bedNumber: values.bedNumber,
      wardNumber: values.wardNumber,
      department: values.department,
    },
  };
}

function avatarForRole(role: string) {
  switch (normalizeRole(role)) {
    case 'admin':
      return 'https://images.unsplash.com/photo-1560250097-0b93528c311a?w=150&q=80';
    case 'doctor':
      return 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=150&q=80';
    case 'nurse':
      return 'https://images.unsplash.com/photo-1582750433449-648ed127bb54?w=150&q=80';
    case 'patient':
    case 'viewer':
      return 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150&q=80';
    default:
      return '/avatar1.png';
  }
}

function normalizeRole(role: string) {
  return role.trim().toLowerCase();
}

function capitalize(value: string) {
  if (!value) {
    return value;
  }

  return value.charAt(0).toUpperCase() + value.slice(1).toLowerCase();
}

function resolveDashboardPath(role: string) {
  switch (normalizeRole(role)) {
    case 'admin':
      return '/admin';
    case 'doctor':
      return '/doctor';
    case 'nurse':
      return '/nurse';
    case 'patient':
    case 'viewer':
      return '/patient';
    default:
      return '/patient';
  }
}

function RoleSelector({
  icon,
  label,
  active,
  onClick,
}: {
  icon: ReactNode;
  label: string;
  active: boolean;
  onClick: () => void;
}) {
  return (
    <div
      onClick={onClick}
      className={`flex flex-col items-center gap-2 p-3 rounded-xl cursor-pointer transition-all duration-300 border
        ${active ? 'bg-dark-800/80 border-primary-500/50 shadow-inner-glow' : 'bg-dark-800/20 border-white/5 hover:border-white/10 hover:bg-dark-800/40'}
      `}
    >
      <div className={active ? 'text-primary-400' : 'text-dark-400'}>{icon}</div>
      <span className={`text-sm ${active ? 'font-semibold text-white' : 'font-medium text-dark-300'}`}>{label}</span>
    </div>
  );
}
