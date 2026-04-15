package com.meditrack.vitals.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ErrorHandlerService {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlerService.class);
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Error tracking
    private final Map<String, ErrorStats> errorStats = new ConcurrentHashMap<>();
    private final Map<String, Long> lastErrorTime = new ConcurrentHashMap<>();
    
    // Error result class
    public static class ErrorResult {
        private boolean shouldRetry;
        private int retryDelaySeconds;
        private String errorCategory;
        private String userMessage;
        
        public ErrorResult(boolean shouldRetry, int retryDelaySeconds, String errorCategory, String userMessage) {
            this.shouldRetry = shouldRetry;
            this.retryDelaySeconds = retryDelaySeconds;
            this.errorCategory = errorCategory;
            this.userMessage = userMessage;
        }
        
        public boolean shouldRetry() { return shouldRetry; }
        public int getRetryDelaySeconds() { return retryDelaySeconds; }
        public String getErrorCategory() { return errorCategory; }
        public String getUserMessage() { return userMessage; }
    }
    
    // Error statistics class
    public static class ErrorStats {
        private long totalCount;
        private long lastHourCount;
        private long lastDayCount;
        private LocalDateTime firstOccurrence;
        private LocalDateTime lastOccurrence;
        private String severity;
        
        public ErrorStats() {
            this.totalCount = 0;
            this.lastHourCount = 0;
            this.lastDayCount = 0;
            this.firstOccurrence = LocalDateTime.now();
            this.severity = "MEDIUM";
        }
        
        public void increment() {
            totalCount++;
            lastHourCount++;
            lastDayCount++;
            lastOccurrence = LocalDateTime.now();
        }
        
        // Getters and setters
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        
        public long getLastHourCount() { return lastHourCount; }
        public void setLastHourCount(long lastHourCount) { this.lastHourCount = lastHourCount; }
        
        public long getLastDayCount() { return lastDayCount; }
        public void setLastDayCount(long lastDayCount) { this.lastDayCount = lastDayCount; }
        
        public LocalDateTime getFirstOccurrence() { return firstOccurrence; }
        public void setFirstOccurrence(LocalDateTime firstOccurrence) { this.firstOccurrence = firstOccurrence; }
        
        public LocalDateTime getLastOccurrence() { return lastOccurrence; }
        public void setLastOccurrence(LocalDateTime lastOccurrence) { this.lastOccurrence = lastOccurrence; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }
    
    // Main error handling method
    @Counted(value = "error.handled", description = "Number of errors handled")
    @Timed(value = "error.handle.time", description = "Time taken to handle error")
    public ErrorResult handleError(Exception exception, String context, Map<String, Object> additionalData) {
        try {
            String errorKey = generateErrorKey(exception, context);
            ErrorStats stats = errorStats.computeIfAbsent(errorKey, k -> new ErrorStats());
            
            // Update statistics
            stats.increment();
            lastErrorTime.put(errorKey, System.currentTimeMillis());
            
            // Determine error category and retry strategy
            ErrorResult result = categorizeAndDetermineRetry(exception, context, additionalData);
            
            // Log error
            logError(exception, context, additionalData, result);
            
            // Publish error event
            publishErrorEvent(exception, context, additionalData, result);
            
            // Check for error escalation
            checkForEscalation(errorKey, stats);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error in error handler: {}", e.getMessage(), e);
            return new ErrorResult(false, 0, "SYSTEM_ERROR", "Internal error occurred");
        }
    }
    
    // Error categorization
    private ErrorResult categorizeAndDetermineRetry(Exception exception, String context, Map<String, Object> additionalData) {
        String errorMessage = exception.getMessage();
        String exceptionClass = exception.getClass().getSimpleName();
        
        // Validation errors
        if (isValidationError(exception)) {
            return new ErrorResult(false, 0, "VALIDATION_ERROR", 
                                  "Invalid data provided: " + getValidationErrorMessage(exception));
        }
        
        // Network/Connection errors
        if (isNetworkError(exception)) {
            return new ErrorResult(true, 5, "NETWORK_ERROR", 
                                  "Network connectivity issue, retrying...");
        }
        
        // Database errors
        if (isDatabaseError(exception)) {
            if (isTransientDatabaseError(exception)) {
                return new ErrorResult(true, 2, "DATABASE_ERROR", 
                                      "Temporary database issue, retrying...");
            } else {
                return new ErrorResult(false, 0, "DATABASE_ERROR", 
                                      "Database error occurred");
            }
        }
        
        // Kafka errors
        if (isKafkaError(exception)) {
            if (isTransientKafkaError(exception)) {
                return new ErrorResult(true, 3, "KAFKA_ERROR", 
                                      "Message queue issue, retrying...");
            } else {
                return new ErrorResult(false, 0, "KAFKA_ERROR", 
                                      "Message queue error occurred");
            }
        }
        
        // Redis errors
        if (isRedisError(exception)) {
            if (isTransientRedisError(exception)) {
                return new ErrorResult(true, 1, "REDIS_ERROR", 
                                      "Cache service issue, retrying...");
            } else {
                return new ErrorResult(false, 0, "REDIS_ERROR", 
                                      "Cache service error occurred");
            }
        }
        
        // Rate limiting errors
        if (isRateLimitError(exception)) {
            return new ErrorResult(true, 60, "RATE_LIMIT_ERROR", 
                                  "Rate limit exceeded, please try again later");
        }
        
        // Business logic errors
        if (isBusinessLogicError(exception)) {
            return new ErrorResult(false, 0, "BUSINESS_ERROR", 
                                  exception.getMessage());
        }
        
        // System errors
        if (isSystemError(exception)) {
            return new ErrorResult(true, 10, "SYSTEM_ERROR", 
                                  "System error occurred, retrying...");
        }
        
        // Unknown errors
        return new ErrorResult(true, 5, "UNKNOWN_ERROR", 
                              "An unexpected error occurred, retrying...");
    }
    
    // Error type detection methods
    private boolean isValidationError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        
        return className.contains("Validation") || 
               className.contains("Constraint") || 
               className.contains("Invalid") ||
               (message != null && (message.contains("Invalid") || 
                                   message.contains("Required") || 
                                   message.contains("not allowed")));
    }
    
    private boolean isNetworkError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        
        return className.contains("Connect") || 
               className.contains("Timeout") || 
               className.contains("Socket") ||
               (message != null && (message.contains("connection") || 
                                   message.contains("timeout") || 
                                   message.contains("network")));
    }
    
    private boolean isDatabaseError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        
        return className.contains("SQL") || 
               className.contains("Database") || 
               className.contains("JPA") ||
               (message != null && (message.contains("database") || 
                                   message.contains("connection") || 
                                   message.contains("constraint")));
    }
    
    private boolean isTransientDatabaseError(Exception exception) {
        String message = exception.getMessage();
        
        return message != null && (
            message.contains("connection") ||
            message.contains("timeout") ||
            message.contains("deadlock") ||
            message.contains("lock") ||
            message.contains("temporary")
        );
    }
    
    private boolean isKafkaError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        
        return className.contains("Kafka") || 
               className.contains("Producer") || 
               className.contains("Consumer") ||
               (message != null && (message.contains("kafka") || 
                                   message.contains("broker") || 
                                   message.contains("topic")));
    }
    
    private boolean isTransientKafkaError(Exception exception) {
        String message = exception.getMessage();
        
        return message != null && (
            message.contains("timeout") ||
            message.contains("not available") ||
            message.contains("rebalance") ||
            message.contains("temporary")
        );
    }
    
    private boolean isRedisError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        
        return className.contains("Redis") || 
               className.contains("Jedis") || 
               className.contains("Lettuce") ||
               (message != null && (message.contains("redis") || 
                                   message.contains("cache")));
    }
    
    private boolean isTransientRedisError(Exception exception) {
        String message = exception.getMessage();
        
        return message != null && (
            message.contains("connection") ||
            message.contains("timeout") ||
            message.contains("temporary")
        );
    }
    
    private boolean isRateLimitError(Exception exception) {
        String message = exception.getMessage();
        
        return message != null && (
            message.contains("rate limit") ||
            message.contains("too many requests") ||
            message.contains("throttled")
        );
    }
    
    private boolean isBusinessLogicError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        
        return className.contains("Business") || 
               className.contains("Service") ||
               exception instanceof IllegalArgumentException ||
               exception instanceof IllegalStateException;
    }
    
    private boolean isSystemError(Exception exception) {
        String className = exception.getClass().getSimpleName();
        
        return className.contains("System") || 
               className.contains("Runtime") ||
               className.contains("Internal") ||
               exception instanceof RuntimeException;
    }
    
    // Error logging and monitoring
    private void logError(Exception exception, String context, Map<String, Object> additionalData, ErrorResult result) {
        try {
            String logMessage = String.format("Error in context: %s, category: %s, retry: %s", 
                                          context, result.getErrorCategory(), result.shouldRetry());
            
            if (result.getErrorCategory().equals("CRITICAL")) {
                logger.error(logMessage, exception);
            } else if (result.getErrorCategory().equals("HIGH")) {
                logger.error(logMessage, exception);
            } else if (result.getErrorCategory().equals("MEDIUM")) {
                logger.warn(logMessage, exception);
            } else {
                logger.info(logMessage, exception);
            }
            
            // Log additional data
            if (additionalData != null && !additionalData.isEmpty()) {
                logger.debug("Additional error data: {}", additionalData);
            }
            
        } catch (Exception e) {
            logger.error("Error logging error: {}", e.getMessage(), e);
        }
    }
    
    private void publishErrorEvent(Exception exception, String context, Map<String, Object> additionalData, ErrorResult result) {
        try {
            ErrorEvent event = new ErrorEvent(
                exception.getClass().getSimpleName(),
                exception.getMessage(),
                context,
                result.getErrorCategory(),
                result.shouldRetry(),
                result.getRetryDelaySeconds(),
                LocalDateTime.now(),
                additionalData
            );
            
            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("error-events", eventJson);
            
            logger.debug("Published error event: {}", eventJson);
            
        } catch (Exception e) {
            logger.error("Error publishing error event: {}", e.getMessage(), e);
        }
    }
    
    // Error escalation
    private void checkForEscalation(String errorKey, ErrorStats stats) {
        try {
            // Check for high error rate
            if (stats.getLastHourCount() > 100) {
                triggerEscalation(errorKey, "HIGH_ERROR_RATE", 
                                "High error rate detected: " + stats.getLastHourCount() + " errors in last hour");
            }
            
            // Check for sustained errors
            if (stats.getLastDayCount() > 1000) {
                triggerEscalation(errorKey, "SUSTAINED_ERRORS", 
                                "Sustained high error rate: " + stats.getLastDayCount() + " errors in last day");
            }
            
            // Check for critical errors
            if ("CRITICAL".equals(stats.getSeverity())) {
                triggerEscalation(errorKey, "CRITICAL_ERROR", 
                                "Critical error occurred: " + errorKey);
            }
            
        } catch (Exception e) {
            logger.error("Error checking for escalation: {}", e.getMessage(), e);
        }
    }
    
    private void triggerEscalation(String errorKey, String escalationType, String message) {
        try {
            EscalationEvent escalation = new EscalationEvent(
                errorKey,
                escalationType,
                message,
                LocalDateTime.now()
            );
            
            String escalationJson = objectMapper.writeValueAsString(escalation);
            kafkaTemplate.send("escalation-events", escalationJson);
            
            logger.warn("Triggered escalation: {} - {}", escalationType, message);
            
        } catch (Exception e) {
            logger.error("Error triggering escalation: {}", e.getMessage(), e);
        }
    }
    
    // Utility methods
    private String generateErrorKey(Exception exception, String context) {
        String className = exception.getClass().getSimpleName();
        String message = exception.getMessage();
        
        if (message != null && message.length() > 50) {
            message = message.substring(0, 50);
        }
        
        return context + ":" + className + ":" + (message != null ? message : "no_message");
    }
    
    private String getValidationErrorMessage(Exception exception) {
        String message = exception.getMessage();
        
        if (message == null) {
            return "Validation error occurred";
        }
        
        // Extract meaningful validation message
        if (message.contains("required")) {
            return "Required field is missing";
        } else if (message.contains("invalid")) {
            return "Invalid data format";
        } else if (message.contains("range")) {
            return "Value out of valid range";
        } else {
            return message;
        }
    }
    
    // Error statistics
    public Map<String, ErrorStats> getErrorStats() {
        return new ConcurrentHashMap<>(errorStats);
    }
    
    public ErrorStats getErrorStats(String errorKey) {
        return errorStats.get(errorKey);
    }
    
    public void resetErrorStats() {
        errorStats.clear();
        lastErrorTime.clear();
        logger.info("Reset error statistics");
    }
    
    // Health check
    public boolean isHealthy() {
        try {
            // Check if error rate is acceptable
            long totalErrors = errorStats.values().stream()
                .mapToLong(ErrorStats::getLastHourCount)
                .sum();
            
            // Consider unhealthy if more than 100 errors in last hour
            return totalErrors < 100;
            
        } catch (Exception e) {
            logger.error("Error checking health status: {}", e.getMessage(), e);
            return false;
        }
    }
    
    // Event classes
    public static class ErrorEvent {
        private String exceptionType;
        private String errorMessage;
        private String context;
        private String errorCategory;
        private boolean shouldRetry;
        private int retryDelaySeconds;
        private LocalDateTime timestamp;
        private Map<String, Object> additionalData;
        
        public ErrorEvent(String exceptionType, String errorMessage, String context, 
                       String errorCategory, boolean shouldRetry, int retryDelaySeconds, 
                       LocalDateTime timestamp, Map<String, Object> additionalData) {
            this.exceptionType = exceptionType;
            this.errorMessage = errorMessage;
            this.context = context;
            this.errorCategory = errorCategory;
            this.shouldRetry = shouldRetry;
            this.retryDelaySeconds = retryDelaySeconds;
            this.timestamp = timestamp;
            this.additionalData = additionalData;
        }
        
        // Getters
        public String getExceptionType() { return exceptionType; }
        public String getErrorMessage() { return errorMessage; }
        public String getContext() { return context; }
        public String getErrorCategory() { return errorCategory; }
        public boolean isShouldRetry() { return shouldRetry; }
        public int getRetryDelaySeconds() { return retryDelaySeconds; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Object> getAdditionalData() { return additionalData; }
    }
    
    public static class EscalationEvent {
        private String errorKey;
        private String escalationType;
        private String message;
        private LocalDateTime timestamp;
        
        public EscalationEvent(String errorKey, String escalationType, String message, LocalDateTime timestamp) {
            this.errorKey = errorKey;
            this.escalationType = escalationType;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getErrorKey() { return errorKey; }
        public String getEscalationType() { return escalationType; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
