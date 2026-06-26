export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  roleId: number;
  departmentId?: number;
  roleName: string;
  departmentName?: string;
  isActive: boolean;
  lastLogin?: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
  name: string;
}

export interface RoleCredentials {
  admin: LoginCredentials[];
  doctor: LoginCredentials[];
  nurse: LoginCredentials[];
}
