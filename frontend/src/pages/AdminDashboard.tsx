import { useEffect, useMemo, useState, type FormEvent, type ReactNode } from 'react';
import { useAuth } from '../context/AuthContext';
import { staffApi } from '../services/api';
import QuickCallMenu from '../components/QuickCallMenu';
import { LogOut, Shield, UserRoundCheck, UserRoundCog, PencilLine, Trash2, Plus, RefreshCw } from 'lucide-react';

type StaffTab = 'doctors' | 'nurses';

type StaffMember = {
  id: number;
  fullName: string;
  email: string;
  role: 'admin' | 'doctor' | 'nurse';
  department?: string | null;
  specialization?: string | null;
  phoneNumber?: string | null;
  licenseNumber?: string | null;
  active?: boolean;
};

type StaffFormState = {
  fullName: string;
  email: string;
  department: string;
  specialization: string;
  phoneNumber: string;
  licenseNumber: string;
  active: boolean;
};

const departments = ['Cardiology', 'Neurology', 'Orthopedics', 'Pediatrics', 'Oncology'];

const emptyForm = (tab: StaffTab): StaffFormState => ({
  fullName: '',
  email: '',
  department: tab === 'doctors' ? 'Cardiology' : 'Cardiology',
  specialization: '',
  phoneNumber: '',
  licenseNumber: '',
  active: true,
});

export default function AdminDashboard() {
  const { logout } = useAuth();
  const [staffMembers, setStaffMembers] = useState<StaffMember[]>([]);
  const [activeTab, setActiveTab] = useState<StaffTab>('doctors');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [editingStaff, setEditingStaff] = useState<StaffMember | null>(null);
  const [formState, setFormState] = useState<StaffFormState>(emptyForm('doctors'));
  const [error, setError] = useState('');

  useEffect(() => {
    void loadStaff();
  }, []);

  const doctors = useMemo(() => staffMembers.filter((member) => member.role === 'doctor'), [staffMembers]);
  const nurses = useMemo(() => staffMembers.filter((member) => member.role === 'nurse'), [staffMembers]);
  const admins = useMemo(() => staffMembers.filter((member) => member.role === 'admin'), [staffMembers]);

  async function loadStaff() {
    setLoading(true);
    setError('');
    try {
      const response = await staffApi.getAll();
      setStaffMembers(Array.isArray(response.data) ? response.data : []);
    } catch (requestError: any) {
      console.error(requestError);
      setError('Unable to load staff registry. Check the backend service.');
    } finally {
      setLoading(false);
    }
  }

  function startAdd(tab: StaffTab) {
    setActiveTab(tab);
    setEditingStaff(null);
    setFormState(emptyForm(tab));
    setShowForm(true);
    setError('');
  }

  function startEdit(staff: StaffMember) {
    setActiveTab(staff.role === 'doctor' ? 'doctors' : 'nurses');
    setEditingStaff(staff);
    setFormState({
      fullName: staff.fullName || '',
      email: staff.email || '',
      department: staff.department || departments[0],
      specialization: staff.specialization || '',
      phoneNumber: staff.phoneNumber || '',
      licenseNumber: staff.licenseNumber || '',
      active: staff.active ?? true,
    });
    setShowForm(true);
    setError('');
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError('');

    const payload = {
      fullName: formState.fullName.trim(),
      email: formState.email.trim(),
      role: activeTab === 'doctors' ? 'doctor' : 'nurse',
      department: formState.department.trim(),
      specialization: formState.specialization.trim(),
      phoneNumber: formState.phoneNumber.trim(),
      licenseNumber: formState.licenseNumber.trim(),
      active: formState.active,
    };

    try {
      if (editingStaff) {
        await staffApi.update(editingStaff.id, payload);
      } else {
        await staffApi.create(payload);
      }
      setShowForm(false);
      setEditingStaff(null);
      await loadStaff();
    } catch (requestError: any) {
      console.error(requestError);
      setError(requestError?.response?.data?.message || 'Failed to save staff member.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDeactivate(staffId: number) {
    if (!window.confirm('Deactivate this staff member?')) {
      return;
    }

    try {
      await staffApi.remove(staffId);
      await loadStaff();
    } catch (requestError) {
      console.error(requestError);
      setError('Failed to deactivate staff member.');
    }
  }

  const activeStaff = activeTab === 'doctors' ? doctors : nurses;

  return (
    <div className="min-h-screen bg-dark-950 text-white relative overflow-hidden">
      <div className="absolute top-0 right-0 w-[700px] h-[700px] bg-primary-500/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-0 left-0 w-[600px] h-[600px] bg-secondary-500/10 rounded-full blur-[140px] pointer-events-none" />

      <header className="relative z-10 px-6 py-5 border-b border-white/5 bg-dark-950/80 backdrop-blur-xl flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-dark-400">Administration Console</p>
          <h1 className="text-2xl font-bold mt-1">MediTrack Registry Control</h1>
        </div>

        <div className="flex items-center gap-3">
          <QuickCallMenu />
          <button
            onClick={loadStaff}
            className="w-10 h-10 rounded-full bg-dark-800/60 border border-white/5 flex items-center justify-center text-dark-300 hover:text-white hover:bg-dark-700 transition-colors"
            title="Refresh registry"
          >
            <RefreshCw size={16} />
          </button>
          <button
            onClick={logout}
            className="w-10 h-10 rounded-full bg-error-500/10 border border-error-500/20 flex items-center justify-center text-error-400 hover:text-white hover:bg-error-500 transition-colors"
            title="Sign out"
          >
            <LogOut size={16} />
          </button>
        </div>
      </header>

      <main className="relative z-10 max-w-7xl mx-auto px-6 py-8 space-y-8">
        <section className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <SummaryCard icon={<Shield size={18} />} label="Admins" value={admins.length} />
          <SummaryCard icon={<UserRoundCheck size={18} />} label="Active Doctors" value={doctors.filter((member) => member.active !== false).length} />
          <SummaryCard icon={<UserRoundCog size={18} />} label="Active Nurses" value={nurses.filter((member) => member.active !== false).length} />
          <SummaryCard icon={<RefreshCw size={18} />} label="Total Staff" value={staffMembers.length} />
        </section>

        {error && (
          <div className="glass-panel border-error-500/30 bg-error-500/10 text-error-100 px-4 py-3">
            {error}
          </div>
        )}

        <section className="glass-panel overflow-hidden">
          <div className="flex items-center justify-between border-b border-white/5 px-6 py-4">
            <div>
              <h2 className="text-xl font-bold">Staff Registry</h2>
              <p className="text-sm text-dark-300">Only registered doctors and nurses can sign in. Admins are seeded and controlled here.</p>
            </div>
            <button
              onClick={() => startAdd(activeTab)}
              className="btn-primary px-4 py-2 text-sm"
            >
              <Plus size={16} />
              Add {activeTab === 'doctors' ? 'Doctor' : 'Nurse'}
            </button>
          </div>

          <div className="px-6 pt-4">
            <div className="inline-flex rounded-full bg-dark-900/80 border border-white/5 p-1">
              <button
                onClick={() => { setActiveTab('doctors'); setShowForm(false); setEditingStaff(null); }}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${activeTab === 'doctors' ? 'bg-primary-500 text-white' : 'text-dark-300 hover:text-white'}`}
              >
                Doctors ({doctors.length})
              </button>
              <button
                onClick={() => { setActiveTab('nurses'); setShowForm(false); setEditingStaff(null); }}
                className={`px-4 py-2 rounded-full text-sm font-medium transition-colors ${activeTab === 'nurses' ? 'bg-primary-500 text-white' : 'text-dark-300 hover:text-white'}`}
              >
                Nurses ({nurses.length})
              </button>
            </div>
          </div>

          {showForm && (
            <form onSubmit={handleSubmit} className="px-6 py-6 grid grid-cols-1 md:grid-cols-2 gap-4 border-b border-white/5">
              <Field label="Full Name">
                <input
                  value={formState.fullName}
                  onChange={(event) => setFormState({ ...formState, fullName: event.target.value })}
                  className="input-field"
                  required
                />
              </Field>
              <Field label="Email">
                <input
                  type="email"
                  value={formState.email}
                  onChange={(event) => setFormState({ ...formState, email: event.target.value })}
                  className="input-field"
                  required
                />
              </Field>
              <Field label="Department">
                <select
                  value={formState.department}
                  onChange={(event) => setFormState({ ...formState, department: event.target.value })}
                  className="input-field"
                  required
                >
                  {departments.map((department) => (
                    <option key={department} value={department}>
                      {department}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Specialization">
                <input
                  value={formState.specialization}
                  onChange={(event) => setFormState({ ...formState, specialization: event.target.value })}
                  className="input-field"
                  placeholder={activeTab === 'doctors' ? 'Cardiology, Neurology...' : 'Optional'}
                />
              </Field>
              <Field label="Phone Number">
                <input
                  value={formState.phoneNumber}
                  onChange={(event) => setFormState({ ...formState, phoneNumber: event.target.value })}
                  className="input-field"
                  placeholder="+1 555 000 1234"
                />
              </Field>
              <Field label="License Number">
                <input
                  value={formState.licenseNumber}
                  onChange={(event) => setFormState({ ...formState, licenseNumber: event.target.value })}
                  className="input-field"
                  placeholder="Optional"
                />
              </Field>
              <div className="flex items-center gap-3 pt-5">
                <input
                  id="active"
                  type="checkbox"
                  checked={formState.active}
                  onChange={(event) => setFormState({ ...formState, active: event.target.checked })}
                  className="w-4 h-4"
                />
                <label htmlFor="active" className="text-sm text-dark-200">Active account</label>
              </div>

              <div className="md:col-span-2 flex items-center gap-3 pt-2">
                <button type="submit" className="btn-primary px-5 py-3" disabled={saving}>
                  {saving ? 'Saving...' : editingStaff ? 'Update Staff' : `Add ${activeTab === 'doctors' ? 'Doctor' : 'Nurse'}`}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowForm(false);
                    setEditingStaff(null);
                  }}
                  className="btn-secondary px-5 py-3"
                >
                  Cancel
                </button>
              </div>
            </form>
          )}

          <div className="overflow-x-auto">
            <table className="min-w-full">
              <thead>
                <tr className="border-b border-white/5 text-left text-xs uppercase tracking-[0.2em] text-dark-400">
                  <th className="px-6 py-4">Name</th>
                  <th className="px-6 py-4">Email</th>
                  <th className="px-6 py-4">Department</th>
                  {activeTab === 'doctors' && <th className="px-6 py-4">Specialization</th>}
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {!loading && activeStaff.map((member) => (
                  <tr key={member.id} className="border-b border-white/5 hover:bg-white/3 transition-colors">
                    <td className="px-6 py-4 font-medium">{member.fullName}</td>
                    <td className="px-6 py-4 text-dark-300">{member.email}</td>
                    <td className="px-6 py-4 text-dark-300">{member.department || '-'}</td>
                    {activeTab === 'doctors' && (
                      <td className="px-6 py-4 text-dark-300">{member.specialization || '-'}</td>
                    )}
                    <td className="px-6 py-4">
                      <span className={`inline-flex px-3 py-1 rounded-full text-xs font-semibold ${member.active === false ? 'bg-error-500/15 text-error-300' : 'bg-success-500/15 text-success-300'}`}>
                        {member.active === false ? 'Inactive' : 'Active'}
                      </span>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => startEdit(member)}
                          className="w-9 h-9 rounded-full bg-dark-800/80 border border-white/5 flex items-center justify-center text-dark-300 hover:text-white hover:bg-dark-700 transition-colors"
                          title="Edit"
                        >
                          <PencilLine size={14} />
                        </button>
                        <button
                          onClick={() => handleDeactivate(member.id)}
                          className="w-9 h-9 rounded-full bg-error-500/10 border border-error-500/20 flex items-center justify-center text-error-300 hover:text-white hover:bg-error-500 transition-colors"
                          title="Deactivate"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
                {loading && (
                  <tr>
                    <td colSpan={activeTab === 'doctors' ? 6 : 5} className="px-6 py-10 text-center text-dark-400">
                      Loading staff registry...
                    </td>
                  </tr>
                )}
                {!loading && activeStaff.length === 0 && (
                  <tr>
                    <td colSpan={activeTab === 'doctors' ? 6 : 5} className="px-6 py-10 text-center text-dark-400">
                      No {activeTab} registered yet.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  );
}

function SummaryCard({ icon, label, value }: { icon: ReactNode; label: string; value: number }) {
  return (
    <div className="glass-card p-5">
      <div className="flex items-center justify-between mb-4">
        <div className="w-10 h-10 rounded-xl bg-primary-500/10 text-primary-400 flex items-center justify-center">
          {icon}
        </div>
      </div>
      <div className="text-dark-400 text-sm">{label}</div>
      <div className="text-2xl font-bold mt-1">{value}</div>
    </div>
  );
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="flex flex-col gap-2">
      <span className="text-sm text-dark-300">{label}</span>
      {children}
    </label>
  );
}
