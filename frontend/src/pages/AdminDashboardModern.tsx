import { useState, useEffect } from 'react'
import { motion } from 'framer-motion'
import { AuthService } from '../auth/AuthService'
import { mockUsers, addNewUser, updateUser, User } from '../database/mockDatabase'

export default function AdminDashboard() {
  const [currentUser, setCurrentUser] = useState<User | null>(null)
  const [users, setUsers] = useState<User[]>([])
  const [activeTab, setActiveTab] = useState<'doctors' | 'nurses'>('doctors')
  const [showAddModal, setShowAddModal] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    department: '',
    specialization: ''
  })

  useEffect(() => {
    const user = AuthService.getCurrentUser()
    setCurrentUser(user)
    if (user) {
      const allUsers = mockUsers.filter(u => u.role !== 'admin')
      setUsers(allUsers)
    }
  }, [])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    
    if (editingUser) {
      // Update existing user
      const updatedUser = updateUser(editingUser.id, {
        ...formData,
        role: activeTab === 'doctors' ? 'doctor' : 'nurse',
        isActive: true
      })
      if (updatedUser) {
        setUsers(users.map(u => u.id === updatedUser.id ? updatedUser : u))
        setEditingUser(null)
      }
    } else {
      // Add new user
      const newUser = addNewUser({
        ...formData,
        role: activeTab === 'doctors' ? 'doctor' : 'nurse',
        isActive: true
      })
      setUsers([...users, newUser])
    }
    
    setFormData({ name: '', email: '', password: '', department: '', specialization: '' })
    setShowAddModal(false)
  }

  const handleEdit = (user: User) => {
    setEditingUser(user)
    setFormData({
      name: user.name,
      email: user.email,
      password: '',
      department: user.department || '',
      specialization: user.specialization || ''
    })
    setShowAddModal(true)
  }

  const handleDeactivate = (userId: string) => {
    const updatedUser = updateUser(userId, { isActive: false })
    if (updatedUser) {
      setUsers(users.map(u => u.id === updatedUser.id ? updatedUser : u))
    }
  }

  const filteredUsers = users.filter(u => u.role === activeTab)

  const stats = {
    totalDoctors: users.filter(u => u.role === 'doctor' && u.isActive).length,
    totalNurses: users.filter(u => u.role === 'nurse' && u.isActive).length,
    inactiveUsers: users.filter(u => !u.isActive).length,
    totalUsers: users.length
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-violet-900">
      {/* Animated Background */}
      <div className="absolute inset-0">
        <div className="absolute top-20 left-20 w-72 h-72 bg-purple-500 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-blob"></div>
        <div className="absolute top-40 right-20 w-72 h-72 bg-indigo-500 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-blob animation-delay-2000"></div>
        <div className="absolute -bottom-8 left-40 w-72 h-72 bg-pink-500 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-blob animation-delay-4000"></div>
      </div>

      {/* Header */}
      <motion.header
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative z-10 bg-white/10 backdrop-blur-lg border-b border-white/20"
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-2xl font-bold text-white">Admin Dashboard</h1>
            </div>
            <div className="flex items-center space-x-4">
              <span className="text-white/80">Welcome, {currentUser?.name}</span>
              <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center">
                <span className="text-white font-medium">{currentUser?.name.charAt(0)}</span>
              </div>
              <button
                onClick={() => AuthService.logout()}
                className="px-4 py-2 bg-red-500/20 text-red-300 rounded-lg hover:bg-red-500/30 transition-all"
              >
                Sign Out
              </button>
            </div>
          </div>
        </div>
      </motion.header>

      {/* Main Content */}
      <main className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Stats Cards */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8"
        >
          <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-white/60 text-sm">Active Doctors</p>
                <p className="text-3xl font-bold text-white">{stats.totalDoctors}</p>
              </div>
              <div className="w-12 h-12 bg-blue-500/20 rounded-full flex items-center justify-center">
                <span className="text-2xl">👨‍⚕️</span>
              </div>
            </div>
          </div>

          <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-white/60 text-sm">Active Nurses</p>
                <p className="text-3xl font-bold text-white">{stats.totalNurses}</p>
              </div>
              <div className="w-12 h-12 bg-green-500/20 rounded-full flex items-center justify-center">
                <span className="text-2xl">👩‍⚕️</span>
              </div>
            </div>
          </div>

          <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-white/60 text-sm">Inactive Users</p>
                <p className="text-3xl font-bold text-white">{stats.inactiveUsers}</p>
              </div>
              <div className="w-12 h-12 bg-yellow-500/20 rounded-full flex items-center justify-center">
                <span className="text-2xl">⚠️</span>
              </div>
            </div>
          </div>

          <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-white/60 text-sm">Total Users</p>
                <p className="text-3xl font-bold text-white">{stats.totalUsers}</p>
              </div>
              <div className="w-12 h-12 bg-purple-500/20 rounded-full flex items-center justify-center">
                <span className="text-2xl">👥</span>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Staff Management */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20"
        >
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-bold text-white">Staff Management</h2>
            <button
              onClick={() => setShowAddModal(true)}
              className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-all"
            >
              Add New {activeTab === 'doctors' ? 'Doctor' : 'Nurse'}
            </button>
          </div>

          {/* Tabs */}
          <div className="flex space-x-4 mb-6">
            <button
              onClick={() => setActiveTab('doctors')}
              className={`px-4 py-2 rounded-lg transition-all ${
                activeTab === 'doctors'
                  ? 'bg-blue-500 text-white'
                  : 'bg-white/10 text-white/60 hover:bg-white/20'
              }`}
            >
              Doctors ({users.filter(u => u.role === 'doctor').length})
            </button>
            <button
              onClick={() => setActiveTab('nurses')}
              className={`px-4 py-2 rounded-lg transition-all ${
                activeTab === 'nurses'
                  ? 'bg-blue-500 text-white'
                  : 'bg-white/10 text-white/60 hover:bg-white/20'
              }`}
            >
              Nurses ({users.filter(u => u.role === 'nurse').length})
            </button>
          </div>

          {/* Users Table */}
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-white/20">
                  <th className="text-left py-3 px-4 text-white/60">Name</th>
                  <th className="text-left py-3 px-4 text-white/60">Email</th>
                  <th className="text-left py-3 px-4 text-white/60">Department</th>
                  <th className="text-left py-3 px-4 text-white/60">Specialization</th>
                  <th className="text-left py-3 px-4 text-white/60">Status</th>
                  <th className="text-left py-3 px-4 text-white/60">Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user, index) => (
                  <motion.tr
                    key={user.id}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.3 + index * 0.05 }}
                    className="border-b border-white/10 hover:bg-white/5"
                  >
                    <td className="py-3 px-4 text-white">{user.name}</td>
                    <td className="py-3 px-4 text-white/80">{user.email}</td>
                    <td className="py-3 px-4 text-white/80">{user.department || '-'}</td>
                    <td className="py-3 px-4 text-white/80">{user.specialization || '-'}</td>
                    <td className="py-3 px-4">
                      <span className={`px-2 py-1 rounded-full text-xs ${
                        user.isActive
                          ? 'bg-green-500/20 text-green-300'
                          : 'bg-red-500/20 text-red-300'
                      }`}>
                        {user.isActive ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex space-x-2">
                        <button
                          onClick={() => handleEdit(user)}
                          className="px-3 py-1 bg-blue-500/20 text-blue-300 rounded hover:bg-blue-500/30 transition-all"
                        >
                          Edit
                        </button>
                        <button
                          onClick={() => handleDeactivate(user.id)}
                          className="px-3 py-1 bg-red-500/20 text-red-300 rounded hover:bg-red-500/30 transition-all"
                        >
                          {user.isActive ? 'Deactivate' : 'Activate'}
                        </button>
                      </div>
                    </td>
                  </motion.tr>
                ))}
              </tbody>
            </table>
          </div>
        </motion.div>
      </main>

      {/* Add/Edit Modal */}
      {showAddModal && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed inset-0 bg-black/50 flex items-center justify-center z-50"
        >
          <motion.div
            initial={{ scale: 0.9, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 w-full max-w-md border border-white/20"
          >
            <h3 className="text-xl font-bold text-white mb-4">
              {editingUser ? 'Edit User' : `Add New ${activeTab === 'doctors' ? 'Doctor' : 'Nurse'}`}
            </h3>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-white/60 text-sm mb-1">Name</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                  placeholder="Enter name"
                  required
                />
              </div>
              <div>
                <label className="block text-white/60 text-sm mb-1">Email</label>
                <input
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                  placeholder="Enter email"
                  required
                />
              </div>
              {!editingUser && (
                <div>
                  <label className="block text-white/60 text-sm mb-1">Password</label>
                  <input
                    type="password"
                    value={formData.password}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                    className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                    placeholder="Enter password"
                    required
                  />
                </div>
              )}
              <div>
                <label className="block text-white/60 text-sm mb-1">Department</label>
                <input
                  type="text"
                  value={formData.department}
                  onChange={(e) => setFormData({ ...formData, department: e.target.value })}
                  className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                  placeholder={activeTab === 'doctors' ? 'Cardiology, Neurology...' : 'Optional'}
                />
              </div>
              {activeTab === 'doctors' && (
                <div>
                  <label className="block text-white/60 text-sm mb-1">Specialization</label>
                  <input
                    type="text"
                    value={formData.specialization}
                    onChange={(e) => setFormData({ ...formData, specialization: e.target.value })}
                    className="w-full px-3 py-2 bg-white/20 border border-white/30 rounded-lg text-white placeholder-white/50 focus:outline-none focus:ring-2 focus:ring-white/50"
                    placeholder="e.g., Interventional Cardiology"
                  />
                </div>
              )}
              <div className="flex space-x-3 pt-4">
                <button
                  type="submit"
                  className="flex-1 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-all"
                >
                  {editingUser ? 'Update' : 'Add'}
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setShowAddModal(false)
                    setEditingUser(null)
                    setFormData({ name: '', email: '', password: '', department: '', specialization: '' })
                  }}
                  className="flex-1 py-2 bg-white/20 text-white rounded-lg hover:bg-white/30 transition-all"
                >
                  Cancel
                </button>
              </div>
            </form>
          </motion.div>
        </motion.div>
      )}

      <style jsx>{`
        @keyframes blob {
          0% { transform: translate(0px, 0px) scale(1); }
          33% { transform: translate(30px, -50px) scale(1.1); }
          66% { transform: translate(-20px, 20px) scale(0.9); }
          100% { transform: translate(0px, 0px) scale(1); }
        }
        .animate-blob {
          animation: blob 7s infinite;
        }
        .animation-delay-2000 {
          animation-delay: 2s;
        }
        .animation-delay-4000 {
          animation-delay: 4s;
        }
      `}</style>
    </div>
  )
}
