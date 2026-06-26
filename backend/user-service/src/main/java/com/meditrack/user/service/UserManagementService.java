package com.meditrack.user.service;

import com.meditrack.user.dto.UserRequest;
import com.meditrack.user.dto.UserResponse;
import com.meditrack.user.dto.RoleCredentials;
import com.meditrack.user.dto.DepartmentResponse;
import com.meditrack.user.dto.UserStats;
import com.meditrack.user.entity.User;
import com.meditrack.user.entity.Role;
import com.meditrack.user.entity.Department;
import com.meditrack.user.repository.UserRepository;
import com.meditrack.user.repository.RoleRepository;
import com.meditrack.user.repository.DepartmentRepository;
import com.meditrack.user.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private UserSessionRepository userSessionRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private SessionService sessionService;
    
    public RoleCredentials getLoginCredentials() {
        try {
            RoleCredentials credentials = new RoleCredentials();
            credentials.setAdmin(new ArrayList<>());
            credentials.setDoctor(new ArrayList<>());
            credentials.setNurse(new ArrayList<>());
            return credentials;
            
        } catch (Exception e) {
            logger.error("Error getting login credentials: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get login credentials", e);
        }
    }
    
    public List<UserResponse> getAllUsers(String sessionToken) {
        try {
            // Validate session
            if (!sessionService.isSessionValid(sessionToken)) {
                throw new SecurityException("Invalid session");
            }
            
            List<User> users = userRepository.findAll();
            return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error getting all users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get users", e);
        }
    }
    
    public List<UserResponse> getUsersByRole(String role, String sessionToken) {
        try {
            // Validate session
            if (!sessionService.isSessionValid(sessionToken)) {
                throw new SecurityException("Invalid session");
            }
            
            Optional<Role> roleOpt = roleRepository.findByRoleName(role.toUpperCase());
            if (roleOpt.isEmpty()) {
                return List.of();
            }
            
            List<User> users = userRepository.findByRoleId(roleOpt.get().getId());
            return users.stream()
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error getting users by role {}: {}", role, e.getMessage(), e);
            throw new RuntimeException("Failed to get users by role", e);
        }
    }
    
    public UserResponse getUserById(String userId, String sessionToken) {
        try {
            // Validate session
            if (!sessionService.isSessionValid(sessionToken)) {
                throw new SecurityException("Invalid session");
            }
            
            Optional<User> userOpt = userRepository.findById(userId);
            return userOpt.map(this::convertToUserResponse).orElse(null);
            
        } catch (Exception e) {
            logger.error("Error getting user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get user", e);
        }
    }
    
    public UserResponse createUser(UserRequest userRequest, String sessionToken, String ipAddress, String userAgent) {
        try {
            // Validate session and check admin role
            String currentUserId = verifyAdminSession(sessionToken);
            
            // Check if email already exists
            if (userRepository.existsByEmail(userRequest.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }
            
            // Get role
            Optional<Role> roleOpt = roleRepository.findById(userRequest.getRoleId());
            if (roleOpt.isEmpty()) {
                throw new IllegalArgumentException("Invalid role");
            }
            
            // Get department if provided
            Department department = null;
            if (userRequest.getDepartmentId() != null) {
                Optional<Department> deptOpt = departmentRepository.findById(userRequest.getDepartmentId());
                department = deptOpt.orElse(null);
            }
            
            // Create user
            User user = new User();
            user.setId(generateUserId(userRequest.getRoleName()));
            user.setEmail(userRequest.getEmail());
            user.setPasswordHash(passwordEncoder.encode(userRequest.getPassword()));
            user.setFirstName(userRequest.getFirstName());
            user.setLastName(userRequest.getLastName());
            user.setRoleId(userRequest.getRoleId());
            user.setDepartmentId(userRequest.getDepartmentId());
            user.setPhone(userRequest.getPhone());
            user.setActive(true);
            user.setCreatedBy(currentUserId);
            user.setUpdatedBy(currentUserId);
            
            User savedUser = userRepository.save(user);
            
            // Log the action
            auditService.logUserCreation(savedUser.getId(), currentUserId, userRequest);
            
            logger.info("User created successfully: {} ({})", savedUser.getEmail(), savedUser.getRole().getRoleName());
            
            return convertToUserResponse(savedUser);
            
        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user", e);
        }
    }
    
    public UserResponse updateUser(String userId, UserRequest userRequest, String sessionToken, String ipAddress, String userAgent) {
        try {
            // Validate session and check admin role
            String currentUserId = verifyAdminSession(sessionToken);
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return null;
            }
            
            User user = userOpt.get();
            
            // Store old values for audit
            Object oldValues = convertToUserResponse(user);
            
            // Update user details
            if (userRequest.getEmail() != null && !userRequest.getEmail().equals(user.getEmail())) {
                if (userRepository.existsByEmail(userRequest.getEmail())) {
                    throw new IllegalArgumentException("Email already exists");
                }
                user.setEmail(userRequest.getEmail());
            }
            
            if (userRequest.getFirstName() != null) {
                user.setFirstName(userRequest.getFirstName());
            }
            
            if (userRequest.getLastName() != null) {
                user.setLastName(userRequest.getLastName());
            }
            
            if (userRequest.getRoleId() != null) {
                user.setRoleId(userRequest.getRoleId());
            }
            
            if (userRequest.getDepartmentId() != null) {
                user.setDepartmentId(userRequest.getDepartmentId());
            }
            
            if (userRequest.getPhone() != null) {
                user.setPhone(userRequest.getPhone());
            }
            
            if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
                user.setPasswordHash(passwordEncoder.encode(userRequest.getPassword()));
            }
            
            user.setUpdatedBy(currentUserId);
            
            User savedUser = userRepository.save(user);
            
            // Log the action
            Object newValues = convertToUserResponse(savedUser);
            auditService.logUserUpdate(userId, currentUserId, oldValues, newValues);
            
            logger.info("User updated successfully: {}", savedUser.getEmail());
            
            return convertToUserResponse(savedUser);
            
        } catch (Exception e) {
            logger.error("Error updating user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to update user", e);
        }
    }
    
    public boolean deleteUser(String userId, String sessionToken, String ipAddress, String userAgent) {
        try {
            // Validate session and check admin role
            String currentUserId = verifyAdminSession(sessionToken);
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return false;
            }
            
            User user = userOpt.get();
            
            // Log the action before deletion
            auditService.logUserDeletion(userId, currentUserId, convertToUserResponse(user));
            
            // Invalidate all sessions for this user
            userSessionRepository.deleteByUserId(userId);
            
            // Delete user
            userRepository.delete(user);
            
            logger.info("User deleted successfully: {}", user.getEmail());
            
            return true;
            
        } catch (Exception e) {
            logger.error("Error deleting user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to delete user", e);
        }
    }
    
    public List<DepartmentResponse> getDepartments(String sessionToken) {
        try {
            // Validate session
            if (!sessionService.isSessionValid(sessionToken)) {
                throw new SecurityException("Invalid session");
            }
            
            List<Department> departments = departmentRepository.findByIsActive(true);
            return departments.stream()
                .map(this::convertToDepartmentResponse)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.error("Error getting departments: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get departments", e);
        }
    }
    
    public UserStats getUserStats(String sessionToken) {
        try {
            // Validate session
            if (!sessionService.isSessionValid(sessionToken)) {
                throw new SecurityException("Invalid session");
            }
            
            List<User> allUsers = userRepository.findAll();
            
            long adminCount = allUsers.stream()
                .filter(u -> u.getRole().getRoleName().equalsIgnoreCase("admin"))
                .count();
            
            long doctorCount = allUsers.stream()
                .filter(u -> u.getRole().getRoleName().equalsIgnoreCase("doctor"))
                .count();
            
            long nurseCount = allUsers.stream()
                .filter(u -> u.getRole().getRoleName().equalsIgnoreCase("nurse"))
                .count();
            
            long activeCount = allUsers.stream()
                .filter(User::isActive)
                .count();
            
            return new UserStats(adminCount, doctorCount, nurseCount, activeCount, allUsers.size());
            
        } catch (Exception e) {
            logger.error("Error getting user stats: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get user stats", e);
        }
    }
    
    private UserResponse convertToUserResponse(User user) {
        return UserResponse.builder()
            .id(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .roleId(user.getRoleId())
            .departmentId(user.getDepartmentId())
            .roleName(user.getRole().getRoleName())
            .departmentName(user.getDepartment() != null ? user.getDepartment().getDepartmentName() : null)
            .isActive(user.isActive())
            .lastLogin(user.getLastLogin())
            .build();
    }
    
    private DepartmentResponse convertToDepartmentResponse(Department department) {
        return DepartmentResponse.builder()
            .id(department.getId())
            .departmentName(department.getDepartmentName())
            .description(department.getDescription())
            .isActive(department.isActive())
            .build();
    }
    
    private String verifyAdminSession(String sessionToken) {
        String currentUserId = sessionService.getUserIdByToken(sessionToken);
        if (currentUserId == null) {
            throw new SecurityException("Invalid session");
        }
        Optional<User> currentUserOpt = userRepository.findById(currentUserId);
        if (currentUserOpt.isEmpty() || !currentUserOpt.get().getRole().getRoleName().equalsIgnoreCase("ADMIN")) {
            throw new SecurityException("Access Denied: Only administrators are authorized to perform this operation");
        }
        return currentUserId;
    }

    private String generateUserId(String roleName) {
        String prefix = roleName.toLowerCase().substring(0, 3);
        long timestamp = System.currentTimeMillis();
        return prefix + "-" + timestamp;
    }
}
