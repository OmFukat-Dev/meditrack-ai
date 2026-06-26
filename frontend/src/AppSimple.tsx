import { useState, useEffect } from 'react'
import { AuthService } from './auth/AuthService'
import Login from './pages/LoginPage'
import AdminDashboard from './pages/AdminDashboardComplete'
import DoctorDashboardSimple from './pages/DoctorDashboardSimple'
import NurseDashboard from './pages/NurseDashboard'

function App() {
  const [currentUser, setCurrentUser] = useState<any>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    AuthService.initializeAuth()
    const user = AuthService.getCurrentUser()
    setCurrentUser(user)
    setLoading(false)
  }, [])

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-purple-600 to-blue-600">
        <div className="text-white text-xl">Loading...</div>
      </div>
    )
  }

  if (!currentUser) {
    return <Login />
  }

  // Route to appropriate dashboard based on the persisted role name
  const currentRole = String(currentUser.roleName ?? currentUser.role ?? '').toLowerCase()

  switch (currentRole) {
    case 'admin':
      return <AdminDashboard />
    case 'doctor':
      return <DoctorDashboardSimple />
    case 'nurse':
      return <NurseDashboard />
    case 'patient':
      return <div className="min-h-screen bg-gray-100 flex items-center justify-center">
        <div className="text-center">
          <h1 className="text-2xl font-bold text-gray-900 mb-4">Patient Dashboard</h1>
          <p className="text-gray-600">Patient dashboard coming soon...</p>
          <button 
            onClick={() => AuthService.logout()}
            className="mt-4 px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
          >
            Logout
          </button>
        </div>
      </div>
    default:
      return <Login />
  }
}

export default App
