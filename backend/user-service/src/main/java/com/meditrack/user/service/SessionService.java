package com.meditrack.user.service;

import com.meditrack.user.entity.UserSession;
import com.meditrack.user.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class SessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    
    @Autowired
    private UserSessionRepository userSessionRepository;
    
    public void createSession(String userId, String sessionToken, String ipAddress, String userAgent) {
        try {
            UserSession session = new UserSession();
            session.setUserId(userId);
            session.setSessionToken(sessionToken);
            session.setIpAddress(ipAddress);
            session.setUserAgent(userAgent);
            session.setExpiresAt(LocalDateTime.now().plusHours(24));
            
            userSessionRepository.save(session);
            logger.info("Session created for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Error creating session for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to create session", e);
        }
    }
    
    public boolean isSessionValid(String sessionToken) {
        try {
            Optional<UserSession> sessionOpt = userSessionRepository.findBySessionToken(sessionToken);
            if (sessionOpt.isEmpty()) {
                return false;
            }
            
            UserSession session = sessionOpt.get();
            return session.getExpiresAt().isAfter(LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("Error validating session: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public String getUserIdByToken(String sessionToken) {
        try {
            Optional<UserSession> sessionOpt = userSessionRepository.findBySessionToken(sessionToken);
            if (sessionOpt.isEmpty()) {
                return null;
            }
            
            UserSession session = sessionOpt.get();
            if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                return null;
            }
            
            return session.getUserId();
            
        } catch (Exception e) {
            logger.error("Error getting user ID by token: {}", e.getMessage(), e);
            return null;
        }
    }
    
    public void invalidateSession(String sessionToken) {
        try {
            Optional<UserSession> sessionOpt = userSessionRepository.findBySessionToken(sessionToken);
            if (sessionOpt.isPresent()) {
                userSessionRepository.delete(sessionOpt.get());
                logger.info("Session invalidated for token: {}", sessionToken.substring(0, 8) + "...");
            }
        } catch (Exception e) {
            logger.error("Error invalidating session: {}", e.getMessage(), e);
        }
    }
    
    public void invalidateAllUserSessions(String userId) {
        try {
            userSessionRepository.deleteByUserId(userId);
            logger.info("All sessions invalidated for user: {}", userId);
        } catch (Exception e) {
            logger.error("Error invalidating all sessions for user {}: {}", userId, e.getMessage(), e);
        }
    }
    
    public void cleanupExpiredSessions() {
        try {
            int deletedCount = userSessionRepository.deleteExpiredSessions(LocalDateTime.now());
            if (deletedCount > 0) {
                logger.info("Cleaned up {} expired sessions", deletedCount);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up expired sessions: {}", e.getMessage(), e);
        }
    }
}
