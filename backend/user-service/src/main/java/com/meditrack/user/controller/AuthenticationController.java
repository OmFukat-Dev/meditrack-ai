package com.meditrack.user.controller;

import com.meditrack.user.dto.LoginRequest;
import com.meditrack.user.dto.LoginResponse;
import com.meditrack.user.dto.UserResponse;
import com.meditrack.user.service.AuthenticationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AuthenticationController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    
    @Autowired
    private AuthenticationService authenticationService;
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest loginRequest, 
                                            HttpServletRequest httpRequest) {
        try {
            // Add IP address and user agent from request
            loginRequest.setIpAddress(getClientIpAddress(httpRequest));
            loginRequest.setUserAgent(httpRequest.getHeader("User-Agent"));
            
            LoginResponse response = authenticationService.authenticate(loginRequest);
            
            if (response.isSuccess()) {
                logger.info("Login successful for user: {}", loginRequest.getEmail());
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Login failed for user: {}", loginRequest.getEmail());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Login error for user {}: {}", loginRequest.getEmail(), e.getMessage(), e);
            LoginResponse errorResponse = LoginResponse.builder()
                .success(false)
                .message("Authentication failed due to system error")
                .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String sessionToken) {
        try {
            String token = sessionToken.replace("Bearer ", "");
            authenticationService.logout(token);
            logger.info("Logout successful for token: {}", token.substring(0, 8) + "...");
            return ResponseEntity.ok().body("Logout successful");
        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Logout failed");
        }
    }
    
    @GetMapping("/validate")
    public ResponseEntity<?> validateSession(@RequestHeader("Authorization") String sessionToken) {
        try {
            String token = sessionToken.replace("Bearer ", "");
            boolean isValid = authenticationService.validateSession(token);
            
            if (isValid) {
                return ResponseEntity.ok().body("Session is valid");
            } else {
                return ResponseEntity.badRequest().body("Session is invalid or expired");
            }
        } catch (Exception e) {
            logger.error("Session validation error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Session validation failed");
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(@RequestHeader("Authorization") String sessionToken) {
        try {
            String token = sessionToken.replace("Bearer ", "");
            UserResponse user = authenticationService.getCurrentUser(token);
            
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            logger.error("Get current user error: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestHeader("Authorization") String oldToken) {
        try {
            String token = oldToken.replace("Bearer ", "");
            LoginResponse response = authenticationService.refreshToken(token);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            logger.error("Token refresh error: {}", e.getMessage(), e);
            LoginResponse errorResponse = LoginResponse.builder()
                .success(false)
                .message("Token refresh failed due to system error")
                .build();
            return ResponseEntity.internalServerError().body(errorResponse);
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
