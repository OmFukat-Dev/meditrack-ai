import { useState, useEffect } from 'react'
import { AuthService } from '../auth/AuthService'
import { UserService } from '../services/userService'
import { User } from '../types/user'

export default function AdminDashboardAPI() {
  const [currentUser, setCurrentUser] = useState<User | null>(null)
  const [users, setUsers] = useState<User[]>([])
  const [userStats, setUserStats] = useState<any>(null)
  const [departments, setDepartments] = useState<any[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState<'dashboard' | 'users' | 'patients'>('dashboard')
  const [showUserModal, setShowUserModal] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)

  const [formData, setFormData] = useState<{
    email: string
    password: string
    firstName: string
    lastName: string
    roleId: number
    departmentId: number
    phone: string
    roleName?: string
  }>({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
    roleId: 2, // Default to doctor
    departmentId: 1, // Default to first department
    phone: '',
    roleName: 'doctor' // Add role name for backend
  })

  // Load data on component mount
  useEffect(() => {
    loadDashboardData()
    const user = AuthService.getCurrentUser()
    setCurrentUser(user)
  }, [])

  const loadDashboardData = async () => {
    setIsLoading(true)
    setError('')
    
    try {
      const [usersData, statsData, departmentsData] = await Promise.all([
        UserService.getAllUsers(),
        UserService.getUserStats(),
        UserService.getDepartments()
      ])
      
      setUsers(usersData)
      setUserStats(statsData)
      setDepartments(departmentsData)
    } catch (error) {
      console.error('Error loading dashboard data:', error)
      setError('Failed to load dashboard data')
    } finally {
      setIsLoading(false)
    }
  }

  const handleUserSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    
    try {
      if (editingUser) {
        await UserService.updateUser(editingUser.id, formData)
        setEditingUser(null)
      } else {
        await UserService.createUser(formData)
      }
      
      setShowUserModal(false)
      setFormData({
        email: '',
        password: '',
        firstName: '',
        lastName: '',
        roleId: 2,
        departmentId: 1,
        phone: ''
      })
      await loadDashboardData() // Refresh data
    } catch (error) {
      console.error('Error saving user:', error)
      setError('Failed to save user')
    }
  }

  const handleEditUser = (user: User) => {
    setEditingUser(user)
    setFormData({
      email: user.email,
      password: '',
      firstName: user.firstName,
      lastName: user.lastName,
      roleId: user.roleId,
      departmentId: user.departmentId || 1,
      phone: ''
    })
    setShowUserModal(true)
  }

  const handleDeleteUser = async (userId: string) => {
    if (window.confirm('Are you sure you want to delete this user?')) {
      try {
        await UserService.deleteUser(userId)
        await loadDashboardData() // Refresh data
      } catch (error) {
        console.error('Error deleting user:', error)
        setError('Failed to delete user')
      }
    }
  }

  const filteredUsers = users.filter(u => u.roleName !== 'admin')

  if (!currentUser) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-white">Loading...</div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-600 to-purple-700 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 mb-6">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-white">Admin Dashboard</h1>
              <p className="text-white/80">Welcome back, {currentUser.firstName} {currentUser.lastName}</p>
            </div>
            <button
              onClick={() => AuthService.logout()}
              className="bg-red-500 text-white px-4 py-2 rounded-lg hover:bg-red-600"
            >
              Sign Out
            </button>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Total Users</h3>
            <p className="text-3xl font-bold text-white">{userStats?.totalCount || 0}</p>
          </div>
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Doctors</h3>
            <p className="text-3xl font-bold text-white">{userStats?.doctorCount || 0}</p>
          </div>
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Nurses</h3>
            <p className="text-3xl font-bold text-white">{userStats?.nurseCount || 0}</p>
          </div>
          <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
            <h3 className="text-white/80 text-sm">Active Users</h3>
            <p className="text-3xl font-bold text-white">{userStats?.activeCount || 0}</p>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="bg-white/10 backdrop-blur-lg rounded-xl p-2 mb-6">
          <div className="flex space-x-2">
            <button
              onClick={() => setActiveTab('dashboard')}
              className={`px-4 py-2 rounded-lg ${activeTab === 'dashboard' ? 'bg-white/20 text-white' : 'text-white/60'}`}
            >
              Dashboard
            </button>
            <button
              onClick={() => setActiveTab('users')}
              className={`px-4 py-2 rounded-lg ${activeTab === 'users' ? 'bg-white/20 text-white' : 'text-white/60'}`}
            >
              Users
            </button>
            <button
              onClick={() => setActiveTab('patients')}
              className={`px-4 py-2 rounded-lg ${activeTab === 'patients' ? 'bg-white/20 text-white' : 'text-white/60'}`}
            >
              Patients
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="bg-white/10 backdrop-blur-lg rounded-xl p-6">
          {isLoading && (
            <div className="bg-white/10 border border-white/20 rounded-lg p-3 mb-4 text-white/80">
              Loading dashboard data...
            </div>
          )}

          {activeTab === 'dashboard' && (
            <div>
              <h2 className="text-2xl font-bold text-white mb-4">Dashboard Overview</h2>
              <div className="text-white/80">
                <p>System is running with {userStats?.totalCount || 0} total users</p>
                <p>Active sessions: {userStats?.activeCount || 0}</p>
              </div>
            </div>
          )}

          {activeTab === 'users' && (
            <div>
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold text-white">Users Management</h2>
                <button
                  onClick={() => setShowUserModal(true)}
                  className="bg-green-500 text-white px-4 py-2 rounded-lg hover:bg-green-600"
                >
                  Add User
                </button>
              </div>

              {error && (
                <div className="bg-red-500/20 border border-red-500/30 rounded-lg p-3 mb-4">
                  <p className="text-white">{error}</p>
                </div>
              )}

              <div className="overflow-x-auto">
                <table className="w-full text-white">
                  <thead>
                    <tr className="border-b border-white/20">
                      <th className="text-left p-3">Name</th>
                      <th className="text-left p-3">Email</th>
                      <th className="text-left p-3">Role</th>
                      <th className="text-left p-3">Department</th>
                      <th className="text-left p-3">Status</th>
                      <th className="text-left p-3">Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredUsers.map((user) => (
                      <tr key={user.id} className="border-b border-white/10">
                        <td className="p-3">{user.firstName} {user.lastName}</td>
                        <td className="p-3">{user.email}</td>
                        <td className="p-3">
                          <span className="bg-blue-500/20 px-2 py-1 rounded text-sm">
                            {user.roleName}
                          </span>
                        </td>
                        <td className="p-3">{user.departmentName || 'N/A'}</td>
                        <td className="p-3">
                          <span className={`px-2 py-1 rounded text-sm ${user.isActive ? 'bg-green-500/20' : 'bg-red-500/20'}`}>
                            {user.isActive ? 'Active' : 'Inactive'}
                          </span>
                        </td>
                        <td className="p-3">
                          <button
                            onClick={() => handleEditUser(user)}
                            className="bg-blue-500 text-white px-3 py-1 rounded mr-2 hover:bg-blue-600"
                          >
                            Edit
                          </button>
                          <button
                            onClick={() => handleDeleteUser(user.id)}
                            className="bg-red-500 text-white px-3 py-1 rounded hover:bg-red-600"
                          >
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === 'patients' && (
            <div>
              <h2 className="text-2xl font-bold text-white mb-4">Patients Management</h2>
              <div className="text-white/80">
                <p>Patient management features will be implemented here.</p>
                <p>This will include patient CRUD operations, medical records, and vitals tracking.</p>
              </div>
            </div>
          )}
        </div>

        {/* User Modal */}
        {showUserModal && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white rounded-xl p-6 w-full max-w-md">
              <h3 className="text-xl font-bold mb-4">
                {editingUser ? 'Edit User' : 'Add New User'}
              </h3>
              
              <form onSubmit={handleUserSubmit}>
                <div className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium mb-1">Email</label>
                    <input
                      type="email"
                      value={formData.email}
                      onChange={(e) => setFormData({...formData, email: e.target.value})}
                      className="w-full px-3 py-2 border rounded-lg"
                      required
                    />
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium mb-1">Password</label>
                    <input
                      type="password"
                      value={formData.password}
                      onChange={(e) => setFormData({...formData, password: e.target.value})}
                      className="w-full px-3 py-2 border rounded-lg"
                      placeholder={editingUser ? 'Leave blank to keep current' : ''}
                    />
                  </div>
                  
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-sm font-medium mb-1">First Name</label>
                      <input
                        type="text"
                        value={formData.firstName}
                        onChange={(e) => setFormData({...formData, firstName: e.target.value})}
                        className="w-full px-3 py-2 border rounded-lg"
                        required
                      />
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium mb-1">Last Name</label>
                      <input
                        type="text"
                        value={formData.lastName}
                        onChange={(e) => setFormData({...formData, lastName: e.target.value})}
                        className="w-full px-3 py-2 border rounded-lg"
                        required
                      />
                    </div>
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium mb-1">Role</label>
                    <select
                      value={formData.roleId}
                      onChange={(e) => setFormData({...formData, roleId: parseInt(e.target.value)})}
                      className="w-full px-3 py-2 border rounded-lg"
                    >
                      <option value={2}>Doctor</option>
                      <option value={3}>Nurse</option>
                      <option value={4}>Viewer</option>
                    </select>
                  </div>
                  
                  <div>
                    <label className="block text-sm font-medium mb-1">Department</label>
                    <select
                      value={formData.departmentId}
                      onChange={(e) => setFormData({...formData, departmentId: parseInt(e.target.value)})}
                      className="w-full px-3 py-2 border rounded-lg"
                    >
                      {departments.map((dept) => (
                        <option key={dept.id} value={dept.id}>
                          {dept.departmentName}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                
                <div className="flex justify-end space-x-3 mt-6">
                  <button
                    type="button"
                    onClick={() => setShowUserModal(false)}
                    className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
                  >
                    {editingUser ? 'Update' : 'Create'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
