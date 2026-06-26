package com.meditrack.user.controller;

import com.meditrack.user.dto.UserRequest;
import com.meditrack.user.dto.UserResponse;
import com.meditrack.user.dto.RoleCredentials;
import com.meditrack.user.dto.DepartmentResponse;
import com.meditrack.user.dto.UserStats;
import com.meditrack.user.service.UserManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserManagementController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);
    
    @Autowired
    private UserManagementService userManagementService;
    
    @GetMapping("/credentials")
    public ResponseEntity<RoleCredentials> getLoginCredentials() {
        try {
            RoleCredentials credentials = userManagementService.getLoginCredentials();
            return ResponseEntity.ok(credentials);
        } catch (Exception e) {
            logger.error("Error fetching login credentials: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(@RequestHeader("Authorization") String sessionToken) {
        try {
            List<UserResponse> users = userManagementService.getAllUsers(sessionToken);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error fetching all users: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable String role, 
                                                          @RequestHeader("Authorization") String sessionToken) {
        try {
            List<UserResponse> users = userManagementService.getUsersByRole(role, sessionToken);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            logger.error("Error fetching users by role {}: {}", role, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String userId, 
                                                  @RequestHeader("Authorization") String sessionToken) {
        try {
            UserResponse user = userManagementService.getUserById(userId, sessionToken);
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error fetching user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserRequest userRequest, 
                                                @RequestHeader("Authorization") String sessionToken,
                                                HttpServletRequest httpRequest) {
        try {
            UserResponse user = userManagementService.createUser(userRequest, sessionToken, 
                getClientIpAddress(httpRequest), httpRequest.getHeader("User-Agent"));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable String userId, 
                                                @RequestBody UserRequest userRequest,
                                                @RequestHeader("Authorization") String sessionToken,
                                                HttpServletRequest httpRequest) {
        try {
            UserResponse user = userManagementService.updateUser(userId, userRequest, sessionToken,
                getClientIpAddress(httpRequest), httpRequest.getHeader("User-Agent"));
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error updating user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId, 
                                          @RequestHeader("Authorization") String sessionToken,
                                          HttpServletRequest httpRequest) {
        try {
            boolean deleted = userManagementService.deleteUser(userId, sessionToken,
                getClientIpAddress(httpRequest), httpRequest.getHeader("User-Agent"));
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentResponse>> getDepartments(@RequestHeader("Authorization") String sessionToken) {
        try {
            List<DepartmentResponse> departments = userManagementService.getDepartments(sessionToken);
            return ResponseEntity.ok(departments);
        } catch (Exception e) {
            logger.error("Error fetching departments: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<UserStats> getUserStats(@RequestHeader("Authorization") String sessionToken) {
        try {
            UserStats stats = userManagementService.getUserStats(sessionToken);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching user stats: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
