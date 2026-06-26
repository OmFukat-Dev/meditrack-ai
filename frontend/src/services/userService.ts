import { User, RoleCredentials } from '../types/user'

export class UserService {
  private static readonly API_BASE_URL = `${import.meta.env.VITE_API_BASE_URL ?? '/api'}/users`

  static async getLoginCredentials(): Promise<RoleCredentials> {
    try {
      const response = await fetch(`${this.API_BASE_URL}/credentials`)
      if (!response.ok) {
        if (response.status >= 500) {
          throw new Error(`Server error: ${response.status}`);
        }
        throw new Error('Failed to fetch login credentials')
      }
      return await response.json()
    } catch (error) {
      console.warn('Backend server offline or returned error. Loading local mock credentials...');
      try {
        const { mockUsers } = await import('../database/mockDatabaseFromSeed');
        const adminCredentials = mockUsers.filter(u => u.role === 'admin').map(u => ({ email: u.email, password: u.password, name: u.name }));
        const doctorCredentials = mockUsers.filter(u => u.role === 'doctor').map(u => ({ email: u.email, password: u.password, name: u.name }));
        const nurseCredentials = mockUsers.filter(u => u.role === 'nurse').map(u => ({ email: u.email, password: u.password, name: u.name }));
        return {
          admin: adminCredentials,
          doctor: doctorCredentials,
          nurse: nurseCredentials
        };
      } catch (mockError) {
        return { admin: [], doctor: [], nurse: [] };
      }
    }
  }

  static async getUsersByRole(role: string): Promise<User[]> {
    try {
      const response = await fetch(`${this.API_BASE_URL}/role/${role}`)
      if (!response.ok) {
        if (response.status >= 500) {
          throw new Error(`Server error: ${response.status}`);
        }
        throw new Error(`Failed to fetch users for role: ${role}`)
      }
      return await response.json()
    } catch (error) {
      console.warn(`Backend server offline or returned error. Loading local mock ${role} users...`);
      try {
        const { mockUsers } = await import('../database/mockDatabaseFromSeed');
        return mockUsers
          .filter(u => u.role === role)
          .map(user => ({
            id: user.id,
            email: user.email,
            firstName: user.name.split(' ')[0],
            lastName: user.name.split(' ')[1] || '',
            roleId: user.role === 'admin' ? 1 : user.role === 'doctor' ? 2 : user.role === 'nurse' ? 3 : 4,
            roleName: user.role,
            departmentName: user.department,
            isActive: user.isActive
          }));
      } catch (mockError) {
        console.error('Mock users load failed:', mockError);
        return [];
      }
    }
  }

  static async getAllUsers(): Promise<User[]> {
    try {
      const response = await fetch(`${this.API_BASE_URL}`)
      if (!response.ok) {
        if (response.status >= 500) {
          throw new Error(`Server error: ${response.status}`);
        }
        throw new Error('Failed to fetch users')
      }
      return await response.json()
    } catch (error) {
      console.warn('Backend server offline or returned error. Loading local mock users...');
      try {
        const { mockUsers } = await import('../database/mockDatabaseFromSeed');
        return mockUsers.map(user => ({
          id: user.id,
          email: user.email,
          firstName: user.name.split(' ')[0],
          lastName: user.name.split(' ')[1] || '',
          roleId: user.role === 'admin' ? 1 : user.role === 'doctor' ? 2 : user.role === 'nurse' ? 3 : 4,
          roleName: user.role,
          departmentName: user.department,
          isActive: user.isActive
        }));
      } catch (mockError) {
        console.error('Mock users load failed:', mockError);
        return [];
      }
    }
  }

  static async createUser(userData: Partial<User>): Promise<User> {
    try {
      const response = await fetch(`${this.API_BASE_URL}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('sessionToken')}`
        },
        body: JSON.stringify(userData)
      })
      if (!response.ok) {
        if (response.status >= 500) {
          throw new Error(`Server error: ${response.status}`);
        }
        throw new Error('Failed to create user')
      }
      return await response.json()
    } catch (error) {
      console.warn('Backend server offline. Creating user locally in mock database...');
      try {
        const { addNewUser } = await import('../database/mockDatabaseFromSeed');
        const mockUser = addNewUser({
          name: `${userData.firstName} ${userData.lastName}`,
          email: userData.email,
          role: userData.roleName as any,
          department: userData.departmentName,
          isActive: true
        });
        return {
          id: mockUser.id,
          email: mockUser.email,
          firstName: mockUser.name.split(' ')[0],
          lastName: mockUser.name.split(' ')[1] || '',
          roleId: mockUser.role === 'admin' ? 1 : mockUser.role === 'doctor' ? 2 : mockUser.role === 'nurse' ? 3 : 4,
          roleName: mockUser.role,
          departmentName: mockUser.department,
          isActive: mockUser.isActive
        };
      } catch (mockError) {
        console.error('Mock create user failed:', mockError);
        throw error;
      }
    }
  }

  static async updateUser(userId: string, userData: Partial<User>): Promise<User> {
    try {
      const response = await fetch(`${this.API_BASE_URL}/${userId}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('sessionToken')}`
        },
        body: JSON.stringify(userData)
      })
      if (!response.ok) {
        if (response.status >= 500) {
          throw new Error(`Server error: ${response.status}`);
        }
        throw new Error('Failed to update user')
      }
      return await response.json()
    } catch (error) {
      console.warn('Backend server offline. Updating user locally in mock database...');
      try {
        const { updateUser } = await import('../database/mockDatabaseFromSeed');
        const updates: any = {};
        if (userData.firstName || userData.lastName) {
          updates.name = `${userData.firstName || ''} ${userData.lastName || ''}`.trim();
        }
        if (userData.email) updates.email = userData.email;
        if (userData.roleName) updates.role = userData.roleName;
        if (userData.departmentName) updates.department = userData.departmentName;
        if (userData.isActive !== undefined) updates.isActive = userData.isActive;

        const mockUser = updateUser(userId, updates);
        return {
          id: mockUser.id,
          email: mockUser.email,
          firstName: mockUser.name.split(' ')[0],
          lastName: mockUser.name.split(' ')[1] || '',
          roleId: mockUser.role === 'admin' ? 1 : mockUser.role === 'doctor' ? 2 : mockUser.role === 'nurse' ? 3 : 4,
          roleName: mockUser.role,
          departmentName: mockUser.department,
          isActive: mockUser.isActive
        };
      } catch (mockError) {
        console.error('Mock update user failed:', mockError);
        throw error;
      }
    }
  }

  static async deleteUser(userId: string): Promise<void> {
    try {
      const response = await fetch(`${this.API_BASE_URL}/${userId}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('sessionToken')}`
        }
      })
      if (!response.ok) {
        if (response.status >= 500) {
          throw new Error(`Server error: ${response.status}`);
        }
        throw new Error('Failed to delete user')
      }
    } catch (error) {
      console.warn('Backend server offline. Deleting user locally in mock database...');
      try {
        const { mockUsers } = await import('../database/mockDatabaseFromSeed');
        const index = mockUsers.findIndex(u => u.id === userId);
        if (index !== -1) {
          mockUsers.splice(index, 1);
        }
      } catch (mockError) {
        console.error('Mock delete user failed:', mockError);
        throw error;
      }
    }
  }

  static async getUserStats(): Promise<any> {
    try {
      const response = await fetch(`${this.API_BASE_URL}/stats`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('sessionToken')}`
        }
      })
      if (!response.ok) {
        if (response.status >= 500) {
          throw new Error(`Server error: ${response.status}`);
        }
        throw new Error('Failed to fetch user stats')
      }
      return await response.json()
    } catch (error) {
      console.warn('Backend server offline or returned error. Loading local mock user stats...');
      try {
        const { mockUsers } = await import('../database/mockDatabaseFromSeed');
        const activeUsers = mockUsers.filter(u => u.isActive).length;
        const doctorsCount = mockUsers.filter(u => u.role === 'doctor').length;
        const nursesCount = mockUsers.filter(u => u.role === 'nurse').length;
        const adminsCount = mockUsers.filter(u => u.role === 'admin').length;
        
        return {
          totalUsers: mockUsers.length,
          activeUsers,
          doctorsCount,
          nursesCount,
          adminsCount,
          departmentsCount: 8
        };
      } catch (mockError) {
        return { totalUsers: 0, activeUsers: 0, doctorsCount: 0, nursesCount: 0, adminsCount: 0, departmentsCount: 0 };
      }
    }
  }

  static async getDepartments(): Promise<any[]> {
    try {
      const response = await fetch(`${this.API_BASE_URL}/departments`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('sessionToken')}`
        }
      })
      if (!response.ok) {
        if (response.status >= 500) {
          throw new Error(`Server error: ${response.status}`);
        }
        throw new Error('Failed to fetch departments')
      }
      return await response.json()
    } catch (error) {
      console.warn('Backend server offline or returned error. Loading local mock departments...');
      return [
        { id: 1, departmentName: 'Cardiology', description: 'Heart and cardiovascular system', isActive: true },
        { id: 2, departmentName: 'Neurology', description: 'Brain and nervous system', isActive: true },
        { id: 3, departmentName: 'Oncology', description: 'Cancer diagnosis and treatment', isActive: true },
        { id: 4, departmentName: 'Emergency', description: 'Emergency medical care', isActive: true },
        { id: 5, departmentName: 'ICU', description: 'Intensive Care Unit', isActive: true },
        { id: 6, departmentName: 'General Medicine', description: 'General medical care', isActive: true },
        { id: 7, departmentName: 'Pediatrics', description: 'Medical care for children', isActive: true },
        { id: 8, departmentName: 'Orthopedics', description: 'Bone and joint treatment', isActive: true }
      ];
    }
  }
}
