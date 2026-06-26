package com.meditrack.user.service;

import com.meditrack.user.config.JwtUtil;
import com.meditrack.user.dto.LoginRequest;
import com.meditrack.user.dto.LoginResponse;
import com.meditrack.user.dto.UserResponse;
import com.meditrack.user.entity.User;
import com.meditrack.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class AuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private AuditService auditService;
    
    @Autowired
    private SessionService sessionService;

    @Autowired
    private JwtUtil jwtUtil;
    
    public LoginResponse authenticate(LoginRequest loginRequest) {
        try {
            logger.info("Authentication attempt for email: {}", loginRequest.getEmail());
            
            // Find user by email
            Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());
            
            if (userOpt.isEmpty()) {
                logger.warn("Authentication failed: User not found for email: {}", loginRequest.getEmail());
                return LoginResponse.builder()
                    .success(false)
                    .message("Invalid email or password")
                    .build();
            }
            
            User user = userOpt.get();
            
            // Check if user is active
            if (!user.isActive()) {
                logger.warn("Authentication failed: User is inactive for email: {}", loginRequest.getEmail());
                return LoginResponse.builder()
                    .success(false)
                    .message("Account is inactive")
                    .build();
            }
            
            // Verify password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                logger.warn("Authentication failed: Invalid password for email: {}", loginRequest.getEmail());
                return LoginResponse.builder()
                    .success(false)
                    .message("Invalid email or password")
                    .build();
            }
            
            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            // Create session token (JWT)
            String displayName = user.getFirstName() + " " + user.getLastName();
            String sessionToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().getRoleName(),
                user.getDepartment() != null ? user.getDepartment().getDepartmentName() : null,
                user.getId(),
                displayName
            );
            sessionService.createSession(user.getId(), sessionToken, loginRequest.getIpAddress(), loginRequest.getUserAgent());
            
            // Log the successful login
            auditService.logAction(user.getId(), "LOGIN", "user", user.getId(), 
                null, null, loginRequest.getIpAddress(), loginRequest.getUserAgent());
            
            // Build user response
            UserResponse userResponse = UserResponse.builder()
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
            
            logger.info("Authentication successful for user: {} ({})", user.getEmail(), user.getRole().getRoleName());
            
            return LoginResponse.builder()
                .success(true)
                .message("Authentication successful")
                .user(userResponse)
                .sessionToken(sessionToken)
                .token(sessionToken)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
                
        } catch (Exception e) {
            logger.error("Authentication error: {}", e.getMessage(), e);
            return LoginResponse.builder()
                .success(false)
                .message("Authentication failed due to system error")
                .build();
        }
    }
    
    public void logout(String sessionToken) {
        try {
            String userId = sessionService.getUserIdByToken(sessionToken);
            if (userId != null) {
                sessionService.invalidateSession(sessionToken);
                auditService.logAction(userId, "LOGOUT", "user", userId, 
                    null, null, null, null);
                logger.info("User logged out: {}", userId);
            }
        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage(), e);
        }
    }
    
    public boolean validateSession(String sessionToken) {
        try {
            return sessionService.isSessionValid(sessionToken);
        } catch (Exception e) {
            logger.error("Session validation error: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public UserResponse getCurrentUser(String sessionToken) {
        try {
            String userId = sessionService.getUserIdByToken(sessionToken);
            if (userId == null) {
                return null;
            }
            
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || !userOpt.get().isActive()) {
                return null;
            }
            
            User user = userOpt.get();
            
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
                
        } catch (Exception e) {
            logger.error("Get current user error: {}", e.getMessage(), e);
            return null;
        }
    }
    
    public LoginResponse refreshToken(String oldToken) {
        try {
            if (!sessionService.isSessionValid(oldToken)) {
                return LoginResponse.builder()
                    .success(false)
                    .message("Invalid session token")
                    .build();
            }
            
            String userId = sessionService.getUserIdByToken(oldToken);
            Optional<User> userOpt = userRepository.findById(userId);
            
            if (userOpt.isEmpty() || !userOpt.get().isActive()) {
                return LoginResponse.builder()
                    .success(false)
                    .message("User not found or inactive")
                    .build();
            }
            
            // Invalidate old session and create new one
            sessionService.invalidateSession(oldToken);
            User user = userOpt.get();
            String displayName = user.getFirstName() + " " + user.getLastName();
            String newToken = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().getRoleName(),
                user.getDepartment() != null ? user.getDepartment().getDepartmentName() : null,
                user.getId(),
                displayName
            );
            sessionService.createSession(userId, newToken, null, null);
            
            UserResponse userResponse = UserResponse.builder()
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
            
            return LoginResponse.builder()
                .success(true)
                .message("Token refreshed successfully")
                .user(userResponse)
                .sessionToken(newToken)
                .token(newToken)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
                
        } catch (Exception e) {
            logger.error("Token refresh error: {}", e.getMessage(), e);
            return LoginResponse.builder()
                .success(false)
                .message("Token refresh failed")
                .build();
        }
    }
}
