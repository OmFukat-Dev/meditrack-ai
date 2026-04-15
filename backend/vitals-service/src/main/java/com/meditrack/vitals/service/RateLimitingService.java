package com.meditrack.vitals.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RateLimitingService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingService.class);
    
    @Autowired
    private RedisCacheService redisCacheService;
    
    // In-memory rate limiting for high-frequency checks
    private final ConcurrentHashMap<String, AtomicInteger> inMemoryCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> lastResetTime = new ConcurrentHashMap<>();
    
    // Rate limit configurations
    private static final int DEFAULT_MAX_REQUESTS_PER_MINUTE = 100;
    private static final int DEFAULT_MAX_REQUESTS_PER_HOUR = 1000;
    private static final int DEFAULT_MAX_REQUESTS_PER_DAY = 10000;
    private static final int DEVICE_MAX_REQUESTS_PER_MINUTE = 50;
    private static final int PATIENT_MAX_REQUESTS_PER_MINUTE = 200;
    
    // Rate limit result class
    public static class RateLimitResult {
        private boolean allowed;
        private int remainingRequests;
        private long resetTime;
        private String limitType;
        
        public RateLimitResult(boolean allowed, int remainingRequests, long resetTime, String limitType) {
            this.allowed = allowed;
            this.remainingRequests = remainingRequests;
            this.resetTime = resetTime;
            this.limitType = limitType;
        }
        
        public boolean isAllowed() { return allowed; }
        public int getRemainingRequests() { return remainingRequests; }
        public long getResetTime() { return resetTime; }
        public String getLimitType() { return limitType; }
    }
    
    // Rate limiting methods
    public RateLimitResult checkRateLimit(String identifier, String limitType) {
        switch (limitType.toLowerCase()) {
            case "minute":
                return checkMinuteRateLimit(identifier, DEFAULT_MAX_REQUESTS_PER_MINUTE);
            case "hour":
                return checkHourRateLimit(identifier, DEFAULT_MAX_REQUESTS_PER_HOUR);
            case "day":
                return checkDayRateLimit(identifier, DEFAULT_MAX_REQUESTS_PER_DAY);
            case "device":
                return checkDeviceRateLimit(identifier);
            case "patient":
                return checkPatientRateLimit(identifier);
            default:
                return checkMinuteRateLimit(identifier, DEFAULT_MAX_REQUESTS_PER_MINUTE);
        }
    }
    
    public RateLimitResult checkMinuteRateLimit(String identifier, int maxRequests) {
        return checkRateLimitInternal(identifier, maxRequests, 60, "minute");
    }
    
    public RateLimitResult checkHourRateLimit(String identifier, int maxRequests) {
        return checkRateLimitInternal(identifier, maxRequests, 3600, "hour");
    }
    
    public RateLimitResult checkDayRateLimit(String identifier, int maxRequests) {
        return checkRateLimitInternal(identifier, maxRequests, 86400, "day");
    }
    
    public RateLimitResult checkDeviceRateLimit(String deviceId) {
        return checkRateLimitInternal("device:" + deviceId, DEVICE_MAX_REQUESTS_PER_MINUTE, 60, "device_minute");
    }
    
    public RateLimitResult checkPatientRateLimit(String patientId) {
        return checkRateLimitInternal("patient:" + patientId, PATIENT_MAX_REQUESTS_PER_MINUTE, 60, "patient_minute");
    }
    
    // Internal rate limiting implementation
    private RateLimitResult checkRateLimitInternal(String identifier, int maxRequests, int timeWindowSeconds, String limitType) {
        try {
            // Try Redis first
            RateLimitResult redisResult = checkRedisRateLimit(identifier, maxRequests, timeWindowSeconds, limitType);
            if (redisResult != null) {
                return redisResult;
            }
            
            // Fallback to in-memory rate limiting
            return checkInMemoryRateLimit(identifier, maxRequests, timeWindowSeconds, limitType);
            
        } catch (Exception e) {
            logger.error("Error checking rate limit for identifier: {}", identifier, e);
            // Fail open - allow the request
            return new RateLimitResult(true, maxRequests, System.currentTimeMillis() + timeWindowSeconds * 1000, limitType);
        }
    }
    
    private RateLimitResult checkRedisRateLimit(String identifier, int maxRequests, int timeWindowSeconds, String limitType) {
        try {
            String rateLimitKey = "rate_limit:" + identifier + ":" + limitType;
            
            // Check if rate limited
            boolean isLimited = redisCacheService.isRateLimited(rateLimitKey, maxRequests, timeWindowSeconds);
            
            if (isLimited) {
                long currentCount = redisCacheService.getRateLimitCount(rateLimitKey);
                long resetTime = System.currentTimeMillis() + timeWindowSeconds * 1000;
                
                logger.warn("Rate limit exceeded for identifier: {} (count: {}, max: {})", 
                           identifier, currentCount, maxRequests);
                
                return new RateLimitResult(false, 0, resetTime, limitType);
            }
            
            // Get current count
            long currentCount = redisCacheService.getRateLimitCount(rateLimitKey);
            int remaining = Math.max(0, maxRequests - (int) currentCount);
            long resetTime = System.currentTimeMillis() + timeWindowSeconds * 1000;
            
            return new RateLimitResult(true, remaining, resetTime, limitType);
            
        } catch (Exception e) {
            logger.error("Error checking Redis rate limit for identifier: {}", identifier, e);
            return null; // Fallback to in-memory
        }
    }
    
    private RateLimitResult checkInMemoryRateLimit(String identifier, int maxRequests, int timeWindowSeconds, String limitType) {
        try {
            AtomicInteger counter = inMemoryCounters.computeIfAbsent(identifier, k -> new AtomicInteger(0));
            AtomicLong lastReset = lastResetTime.computeIfAbsent(identifier, k -> new AtomicLong(System.currentTimeMillis()));
            
            long currentTime = System.currentTimeMillis();
            long lastResetTimeMs = lastReset.get();
            
            // Reset if time window has passed
            if (currentTime - lastResetTimeMs > timeWindowSeconds * 1000) {
                counter.set(0);
                lastReset.set(currentTime);
            }
            
            int currentCount = counter.incrementAndGet();
            
            if (currentCount > maxRequests) {
                logger.warn("In-memory rate limit exceeded for identifier: {} (count: {}, max: {})", 
                           identifier, currentCount, maxRequests);
                
                return new RateLimitResult(false, 0, 
                                       lastReset.get() + timeWindowSeconds * 1000, limitType);
            }
            
            int remaining = Math.max(0, maxRequests - currentCount);
            long resetTime = lastReset.get() + timeWindowSeconds * 1000;
            
            return new RateLimitResult(true, remaining, resetTime, limitType);
            
        } catch (Exception e) {
            logger.error("Error checking in-memory rate limit for identifier: {}", identifier, e);
            // Fail open
            return new RateLimitResult(true, maxRequests, 
                                   System.currentTimeMillis() + timeWindowSeconds * 1000, limitType);
        }
    }
    
    // Advanced rate limiting methods
    public RateLimitResult checkAdaptiveRateLimit(String identifier, int baseMaxRequests, String limitType) {
        try {
            // Get historical usage to adjust rate limit
            String usageKey = "usage_history:" + identifier + ":" + limitType;
            String usageJson = redisCacheService.get(usageKey, String.class);
            
            int adjustedMaxRequests = baseMaxRequests;
            
            if (usageJson != null) {
                // Simple adaptive logic - reduce limit if high usage detected
                // In production, this would be more sophisticated
                adjustedMaxRequests = calculateAdaptiveLimit(usageJson, baseMaxRequests);
            }
            
            return checkRateLimitInternal(identifier, adjustedMaxRequests, 60, limitType);
            
        } catch (Exception e) {
            logger.error("Error checking adaptive rate limit for identifier: {}", identifier, e);
            return checkRateLimitInternal(identifier, baseMaxRequests, 60, limitType);
        }
    }
    
    private int calculateAdaptiveLimit(String usageJson, int baseLimit) {
        try {
            // Simple adaptive logic - reduce limit by 25% if high usage detected
            // In production, this would use more sophisticated algorithms
            if (usageJson.contains("high_usage")) {
                return (int) (baseLimit * 0.75);
            }
            return baseLimit;
        } catch (Exception e) {
            logger.error("Error calculating adaptive limit", e);
            return baseLimit;
        }
    }
    
    // Rate limit for specific operations
    public RateLimitResult checkVitalIngestionRateLimit(String patientId, String vitalType) {
        String identifier = "vital_ingestion:" + patientId + ":" + vitalType;
        
        // Different limits for different vital types
        int maxRequests = switch (vitalType.toLowerCase()) {
            case "heart_rate" -> 60; // 1 per second
            case "blood_pressure" -> 30; // 1 per 2 seconds
            case "temperature" -> 20; // 1 per 3 seconds
            case "spo2" -> 60; // 1 per second
            case "respiratory_rate" -> 30; // 1 per 2 seconds
            default -> 30;
        };
        
        return checkRateLimitInternal(identifier, maxRequests, 60, "vital_ingestion_minute");
    }
    
    public RateLimitResult checkAlertRateLimit(String patientId) {
        String identifier = "alerts:" + patientId;
        return checkRateLimitInternal(identifier, 10, 60, "alerts_minute"); // 10 alerts per minute
    }
    
    public RateLimitResult checkApiRateLimit(String apiKey, String endpoint) {
        String identifier = "api:" + apiKey + ":" + endpoint;
        return checkRateLimitInternal(identifier, 1000, 3600, "api_hour"); // 1000 requests per hour per endpoint
    }
    
    // Rate limit management
    public void resetRateLimit(String identifier, String limitType) {
        try {
            String rateLimitKey = "rate_limit:" + identifier + ":" + limitType;
            
            // Reset Redis
            redisCacheService.delete(rateLimitKey);
            
            // Reset in-memory
            String memoryKey = identifier + ":" + limitType;
            inMemoryCounters.remove(memoryKey);
            lastResetTime.remove(memoryKey);
            
            logger.info("Reset rate limit for identifier: {} limitType: {}", identifier, limitType);
            
        } catch (Exception e) {
            logger.error("Error resetting rate limit for identifier: {} limitType: {}", identifier, limitType, e);
        }
    }
    
    public void resetAllRateLimits(String identifier) {
        try {
            // Reset all rate limits for identifier
            String pattern = "rate_limit:" + identifier + ":*";
            redisCacheService.deleteByPattern(pattern);
            
            // Reset in-memory counters
            inMemoryCounters.keySet().removeIf(key -> key.startsWith(identifier + ":"));
            lastResetTime.keySet().removeIf(key -> key.startsWith(identifier + ":"));
            
            logger.info("Reset all rate limits for identifier: {}", identifier);
            
        } catch (Exception e) {
            logger.error("Error resetting all rate limits for identifier: {}", identifier, e);
        }
    }
    
    // Rate limit statistics
    public RateLimitStats getRateLimitStats(String identifier) {
        try {
            String statsKey = "rate_limit_stats:" + identifier;
            String statsJson = redisCacheService.get(statsKey, String.class);
            
            if (statsJson != null) {
                return parseRateLimitStats(statsJson);
            }
            
            return new RateLimitStats();
            
        } catch (Exception e) {
            logger.error("Error getting rate limit stats for identifier: {}", identifier, e);
            return new RateLimitStats();
        }
    }
    
    public void updateRateLimitStats(String identifier, String limitType, boolean wasLimited) {
        try {
            String statsKey = "rate_limit_stats:" + identifier;
            RateLimitStats stats = getRateLimitStats(identifier);
            
            stats.incrementTotalRequests();
            if (wasLimited) {
                stats.incrementRateLimitHits(limitType);
            }
            
            // Cache stats for 1 hour
            redisCacheService.set(statsKey, stats.toJson(), 3600);
            
        } catch (Exception e) {
            logger.error("Error updating rate limit stats for identifier: {}", identifier, e);
        }
    }
    
    // Cleanup methods
    public void cleanupExpiredCounters() {
        try {
            long currentTime = System.currentTimeMillis();
            
            // Clean up in-memory counters
            lastResetTime.entrySet().removeIf(entry -> {
                long timeDiff = currentTime - entry.getValue().get();
                return timeDiff > 86400000; // Remove entries older than 24 hours
            });
            
            inMemoryCounters.entrySet().removeIf(entry -> {
                String key = entry.getKey();
                return !lastResetTime.containsKey(key); // Remove if no corresponding reset time
            });
            
            logger.debug("Cleaned up expired rate limit counters");
            
        } catch (Exception e) {
            logger.error("Error cleaning up expired rate limit counters", e);
        }
    }
    
    // Utility methods
    private RateLimitStats parseRateLimitStats(String statsJson) {
        // Simple implementation - in production, use proper JSON parsing
        RateLimitStats stats = new RateLimitStats();
        
        if (statsJson.contains("total_requests")) {
            stats.setTotalRequests(100); // Placeholder
        }
        
        return stats;
    }
    
    // Rate limit stats class
    public static class RateLimitStats {
        private long totalRequests;
        private long rateLimitHits;
        private java.util.Map<String, Long> limitTypeHits;
        
        public RateLimitStats() {
            this.totalRequests = 0;
            this.rateLimitHits = 0;
            this.limitTypeHits = new java.util.concurrent.ConcurrentHashMap<>();
        }
        
        public void incrementTotalRequests() {
            totalRequests++;
        }
        
        public void incrementRateLimitHits(String limitType) {
            rateLimitHits++;
            limitTypeHits.merge(limitType, 1L, Long::sum);
        }
        
        // Getters and setters
        public long getTotalRequests() { return totalRequests; }
        public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }
        
        public long getRateLimitHits() { return rateLimitHits; }
        public void setRateLimitHits(long rateLimitHits) { this.rateLimitHits = rateLimitHits; }
        
        public java.util.Map<String, Long> getLimitTypeHits() { return limitTypeHits; }
        public void setLimitTypeHits(java.util.Map<String, Long> limitTypeHits) { this.limitTypeHits = limitTypeHits; }
        
        public String toJson() {
            return String.format("{\"total_requests\":%d,\"rate_limit_hits\":%d,\"limit_type_hits\":%s}", 
                               totalRequests, rateLimitHits, limitTypeHits);
        }
    }
}
