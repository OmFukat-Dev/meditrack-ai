import { User } from '../types/user'
import { patientApi } from '../services/api'

const STORAGE_KEY = 'meditrack_auth'

export class AuthService {
  private static currentUser: User | null = null
  private static sessionToken: string | null = null
  private static readonly API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api'

  static async login(email: string, password: string): Promise<{ success: boolean; user?: User; error?: string }> {
    try {
      const response = await fetch(`${this.API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          email,
          password,
          ipAddress: '127.0.0.1', // In production, this would be detected
          userAgent: navigator.userAgent
        })
      })

      const data = await response.json()

      if (!response.ok || !data.success || !data.user) {
        if (response.status >= 500) {
          throw new Error(`Server returned error status: ${response.status}`);
        }
        return {
          success: false,
          error: data.message || 'Invalid email or password'
        }
      }

      this.currentUser = this.mapUserResponse(data.user)
      this.sessionToken = typeof data.sessionToken === 'string' && data.sessionToken.length > 0
        ? data.sessionToken
        : null

      localStorage.setItem('currentUser', JSON.stringify(this.currentUser))
      localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...this.currentUser, token: this.sessionToken }))

      if (this.sessionToken) {
        localStorage.setItem('sessionToken', this.sessionToken)
        localStorage.setItem('token', this.sessionToken)
      } else {
        localStorage.removeItem('sessionToken')
        localStorage.removeItem('token')
      }

      return {
        success: true,
        user: this.currentUser
      }
    } catch (error) {
      console.warn('Backend authentication server is offline. Falling back to local mock database...');
      
      try {
        const { authenticateUser } = await import('../database/mockDatabaseFromSeed');
        const mockUser = authenticateUser(email, password);
        
        if (mockUser) {
          const mappedUser: User = {
            id: mockUser.id,
            email: mockUser.email,
            firstName: mockUser.name.split(' ')[0],
            lastName: mockUser.name.split(' ')[1] || '',
            roleId: mockUser.role === 'admin' ? 1 : mockUser.role === 'doctor' ? 2 : mockUser.role === 'nurse' ? 3 : 4,
            roleName: mockUser.role,
            departmentName: mockUser.department,
            isActive: mockUser.isActive
          };
          
          this.currentUser = mappedUser;
          this.sessionToken = 'mock-session-token-' + Date.now();
          
          localStorage.setItem('currentUser', JSON.stringify(this.currentUser));
          localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...this.currentUser, token: this.sessionToken }));
          localStorage.setItem('sessionToken', this.sessionToken);
          localStorage.setItem('token', this.sessionToken);

          return {
            success: true,
            user: this.currentUser
          };
        }
      } catch (mockError) {
        console.error('Mock database fallback failed:', mockError);
      }
      
      return {
        success: false,
        error: 'Invalid email or password. (Mock Mode Active)'
      };
    }
  }

  static logout(): void {
    const token = this.sessionToken ?? localStorage.getItem('sessionToken') ?? localStorage.getItem('token')

    if (token) {
      fetch(`${this.API_BASE_URL}/auth/logout`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        }
      }).catch(error => console.error('Logout error:', error))
    }
    
    this.currentUser = null
    this.sessionToken = null
    localStorage.removeItem('currentUser')
    localStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem('sessionToken')
    localStorage.removeItem('token')
    // Force redirect to login page
    window.location.href = '/login'
  }

  static getCurrentUser(): User | null {
    if (this.currentUser) {
      return this.currentUser
    }

    const storedUser = localStorage.getItem('currentUser')
    if (storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser)
        this.currentUser = parsedUser
        this.sessionToken = parsedUser.token ?? localStorage.getItem('sessionToken') ?? localStorage.getItem('token')
        return this.currentUser
      } catch {
        localStorage.removeItem('currentUser')
      }
    }

    const storedAuth = localStorage.getItem(STORAGE_KEY)
    if (storedAuth) {
      try {
        const parsed = JSON.parse(storedAuth) as any
        this.currentUser = {
          id: parsed.id,
          email: parsed.email,
          firstName: parsed.firstName,
          lastName: parsed.lastName,
          roleId: parsed.roleId,
          departmentId: parsed.departmentId,
          roleName: parsed.roleName,
          departmentName: parsed.departmentName,
          isActive: parsed.isActive,
          lastLogin: parsed.lastLogin,
        }
        this.sessionToken = parsed.token ?? localStorage.getItem('sessionToken') ?? localStorage.getItem('token')
        return this.currentUser
      } catch {
        localStorage.removeItem(STORAGE_KEY)
      }
    }

    return null
  }

  static isAuthenticated(): boolean {
    return this.getCurrentUser() !== null
  }

  static getUserRole(): string | null {
    const user = this.getCurrentUser()
    return user ? user.roleName : null
  }

  static hasRole(role: string): boolean {
    const user = this.getCurrentUser()
    return user ? user.roleName === role : false
  }

  
  static initializeAuth(): void {
    const storedUser = localStorage.getItem('currentUser')
    const storedAuth = localStorage.getItem(STORAGE_KEY)
    const storedToken = localStorage.getItem('sessionToken') ?? localStorage.getItem('token')
    
    if (storedUser) {
      try {
        const parsedUser = JSON.parse(storedUser)
        this.currentUser = parsedUser
        this.sessionToken = parsedUser.token ?? storedToken
      } catch (error) {
        localStorage.removeItem('currentUser')
        localStorage.removeItem('sessionToken')
      }
    } else if (storedAuth) {
      try {
        const parsed = JSON.parse(storedAuth) as any
        this.currentUser = {
          id: parsed.id,
          email: parsed.email,
          firstName: parsed.firstName,
          lastName: parsed.lastName,
          roleId: parsed.roleId,
          departmentId: parsed.departmentId,
          roleName: parsed.roleName,
          departmentName: parsed.departmentName,
          isActive: parsed.isActive,
          lastLogin: parsed.lastLogin,
        }
        this.sessionToken = parsed.token ?? storedToken
      } catch (error) {
        localStorage.removeItem(STORAGE_KEY)
        localStorage.removeItem('sessionToken')
      }
    }
  }

  private static mapUserResponse(userResponse: any): User {
    const firstName = typeof userResponse.firstName === 'string' ? userResponse.firstName : ''
    const lastName = typeof userResponse.lastName === 'string' ? userResponse.lastName : ''
    const roleName = typeof userResponse.roleName === 'string' ? userResponse.roleName.trim().toLowerCase() : ''

    return {
      id: userResponse.id,
      email: userResponse.email,
      firstName,
      lastName,
      roleId: userResponse.roleId,
      departmentId: userResponse.departmentId,
      roleName,
      departmentName: typeof userResponse.departmentName === 'string' ? userResponse.departmentName : undefined,
      isActive: Boolean(userResponse.isActive),
      lastLogin: typeof userResponse.lastLogin === 'string' ? userResponse.lastLogin : undefined
    }
  }

  static async getPatientsForCurrentUser(): Promise<any[]> {
    const user = this.getCurrentUser()
    if (!user) {
      throw new Error('No authenticated user is available')
    }

    const extractPatients = (payload: any): any[] => {
      if (!payload) return []
      if (Array.isArray(payload.content)) return payload.content
      if (Array.isArray(payload)) return payload
      if (Array.isArray(payload.items)) return payload.items
      return []
    }

    try {
      const response = await patientApi.getAll(0, 200)
      const patients = extractPatients(response.data)
      const normalized = patients.map((patient: any) => ({
        ...patient,
        department: patient.department || patient.departmentName || 'General',
      }))

      if (user.roleName === 'nurse' && user.departmentName) {
        return normalized.filter((patient: any) =>
          String(patient.department).toLowerCase() === String(user.departmentName).toLowerCase(),
        )
      }

      if (user.roleName === 'doctor' && user.email) {
        return normalized.filter((patient: any) =>
          String(patient.assignedClinicianEmail).toLowerCase() === String(user.email).toLowerCase() ||
          String(patient.assignedClinicianName || '').toLowerCase().includes(String(user.email).toLowerCase()),
        )
      }

      if (user.roleName === 'patient') {
        return normalized.filter((patient: any) =>
          String(patient.id) === String(user.id) ||
          String(patient.patientIdentifier) === String(user.id) ||
          String(patient.email).toLowerCase() === String(user.email).toLowerCase(),
        )
      }

      return normalized
    } catch (error) {
      console.warn('Unable to load patient list for current user:', error)
      return []
    }
  }
}
