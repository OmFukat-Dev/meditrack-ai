package com.meditrack.vitals.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class RedisCacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheService.class);
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Cache keys
    private static final String LATEST_VITAL_KEY = "latest_vital:";
    private static final String PATIENT_SUMMARY_KEY = "vital_summary:";
    private static final String VITAL_TRENDS_KEY = "vital_trends:";
    private static final String VITAL_THRESHOLDS_KEY = "vital_thresholds:";
    private static final String DEVICE_STATUS_KEY = "device_status:";
    private static final String RATE_LIMIT_KEY = "rate_limit:";
    
    // Basic cache operations
    public void set(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
            logger.debug("Cached key: {} with TTL: {} seconds", key, ttlSeconds);
        } catch (Exception e) {
            logger.error("Error caching key: {}", key, e);
        }
    }
    
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                logger.debug("Cache hit for key: {}", key);
                return objectMapper.convertValue(value, clazz);
            }
            logger.debug("Cache miss for key: {}", key);
            return null;
        } catch (Exception e) {
            logger.error("Error retrieving key: {} from cache", key, e);
            return null;
        }
    }
    
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            logger.debug("Deleted key from cache: {}", key);
        } catch (Exception e) {
            logger.error("Error deleting key: {} from cache", key, e);
        }
    }
    
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            logger.error("Error checking existence of key: {}", key, e);
            return false;
        }
    }
    
    public void expire(String key, long ttlSeconds) {
        try {
            redisTemplate.expire(key, Duration.ofSeconds(ttlSeconds));
            logger.debug("Set TTL for key: {} to {} seconds", key, ttlSeconds);
        } catch (Exception e) {
            logger.error("Error setting TTL for key: {}", key, e);
        }
    }
    
    // Latest vital caching
    public void cacheLatestVital(Long patientId, String vitalType, Object vitalReading) {
        String key = LATEST_VITAL_KEY + patientId + ":" + vitalType;
        set(key, vitalReading, 3600); // Cache for 1 hour
    }
    
    public <T> T getLatestVital(Long patientId, String vitalType, Class<T> clazz) {
        String key = LATEST_VITAL_KEY + patientId + ":" + vitalType;
        return get(key, clazz);
    }
    
    public void deleteLatestVital(Long patientId, String vitalType) {
        String key = LATEST_VITAL_KEY + patientId + ":" + vitalType;
        delete(key);
    }
    
    // Patient vital summary caching
    public void cachePatientVitalSummary(Long patientId, Object summary) {
        String key = PATIENT_SUMMARY_KEY + patientId;
        set(key, summary, 1800); // Cache for 30 minutes
    }
    
    public <T> T getPatientVitalSummary(Long patientId, Class<T> clazz) {
        String key = PATIENT_SUMMARY_KEY + patientId;
        return get(key, clazz);
    }
    
    // Vital trends caching
    public void cacheVitalTrends(Long patientId, String vitalType, Object trends) {
        String key = VITAL_TRENDS_KEY + patientId + ":" + vitalType;
        set(key, trends, 7200); // Cache for 2 hours
    }
    
    public <T> T getVitalTrends(Long patientId, String vitalType, Class<T> clazz) {
        String key = VITAL_TRENDS_KEY + patientId + ":" + vitalType;
        return get(key, clazz);
    }
    
    // Vital thresholds caching
    public void cacheVitalThresholds(Long patientId, Object thresholds) {
        String key = VITAL_THRESHOLDS_KEY + patientId;
        set(key, thresholds, 86400); // Cache for 24 hours
    }
    
    public <T> T getVitalThresholds(Long patientId, Class<T> clazz) {
        String key = VITAL_THRESHOLDS_KEY + patientId;
        return get(key, clazz);
    }
    
    // Device status caching
    public void cacheDeviceStatus(String deviceId, Object status) {
        String key = DEVICE_STATUS_KEY + deviceId;
        set(key, status, 300); // Cache for 5 minutes
    }
    
    public <T> T getDeviceStatus(String deviceId, Class<T> clazz) {
        String key = DEVICE_STATUS_KEY + deviceId;
        return get(key, clazz);
    }
    
    // Rate limiting
    public boolean isRateLimited(String identifier, int maxRequests, int timeWindowSeconds) {
        String key = RATE_LIMIT_KEY + identifier;
        try {
            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(timeWindowSeconds));
            }
            
            boolean isLimited = currentCount > maxRequests;
            if (isLimited) {
                logger.warn("Rate limit exceeded for identifier: {} (count: {})", identifier, currentCount);
            }
            
            return isLimited;
        } catch (Exception e) {
            logger.error("Error checking rate limit for identifier: {}", identifier, e);
            return false; // Fail open
        }
    }
    
    public long getRateLimitCount(String identifier) {
        String key = RATE_LIMIT_KEY + identifier;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? ((Number) value).longValue() : 0;
        } catch (Exception e) {
            logger.error("Error getting rate limit count for identifier: {}", identifier, e);
            return 0;
        }
    }
    
    // Hash operations for complex data
    public void setHash(String key, String field, Object value) {
        try {
            redisTemplate.opsForHash().put(key, field, value);
            logger.debug("Set hash field: {} in key: {}", field, key);
        } catch (Exception e) {
            logger.error("Error setting hash field: {} in key: {}", field, key, e);
        }
    }
    
    public <T> T getHash(String key, String field, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForHash().get(key, field);
            if (value != null) {
                return objectMapper.convertValue(value, clazz);
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting hash field: {} from key: {}", field, key, e);
            return null;
        }
    }
    
    public Map<Object, Object> getAllHash(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            logger.error("Error getting all hash entries from key: {}", key, e);
            return Map.of();
        }
    }
    
    public void deleteHash(String key, String field) {
        try {
            redisTemplate.opsForHash().delete(key, field);
            logger.debug("Deleted hash field: {} from key: {}", field, key);
        } catch (Exception e) {
            logger.error("Error deleting hash field: {} from key: {}", field, key, e);
        }
    }
    
    // List operations
    public void pushToList(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            logger.debug("Pushed to list: {}", key);
        } catch (Exception e) {
            logger.error("Error pushing to list: {}", key, e);
        }
    }
    
    public List<Object> getList(String key, int start, int end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            logger.error("Error getting list range: {} from {} to {}", key, start, end, e);
            return List.of();
        }
    }
    
    public long getListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            logger.error("Error getting list size for key: {}", key, e);
            return 0;
        }
    }
    
    // Set operations
    public void addToSet(String key, Object value) {
        try {
            redisTemplate.opsForSet().add(key, value);
            logger.debug("Added to set: {}", key);
        } catch (Exception e) {
            logger.error("Error adding to set: {}", key, e);
        }
    }
    
    public Set<Object> getSet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            logger.error("Error getting set members for key: {}", key, e);
            return Set.of();
        }
    }
    
    public boolean isSetMember(String key, Object value) {
        try {
            Boolean isMember = redisTemplate.opsForSet().isMember(key, value);
            return isMember != null && isMember;
        } catch (Exception e) {
            logger.error("Error checking set membership for key: {}", key, e);
            return false;
        }
    }
    
    // Batch operations
    public void setBatch(Map<String, Object> keyValues) {
        try {
            redisTemplate.opsForValue().multiSet(keyValues);
            logger.debug("Batch cached {} keys", keyValues.size());
        } catch (Exception e) {
            logger.error("Error batch caching keys", e);
        }
    }
    
    public List<Object> getBatch(List<String> keys) {
        try {
            return redisTemplate.opsForValue().multiGet(keys);
        } catch (Exception e) {
            logger.error("Error batch getting keys", e);
            return List.of();
        }
    }
    
    public void deleteBatch(List<String> keys) {
        try {
            redisTemplate.delete(keys);
            logger.debug("Batch deleted {} keys", keys.size());
        } catch (Exception e) {
            logger.error("Error batch deleting keys", e);
        }
    }
    
    // Pattern-based operations
    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.debug("Deleted {} keys matching pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            logger.error("Error deleting keys by pattern: {}", pattern, e);
        }
    }
    
    public Set<String> getKeysByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            logger.debug("Found {} keys matching pattern: {}", keys != null ? keys.size() : 0, pattern);
            return keys;
        } catch (Exception e) {
            logger.error("Error getting keys by pattern: {}", pattern, e);
            return Set.of();
        }
    }
    
    // Cache statistics
    public Map<String, Object> getCacheStats() {
        try {
            // Get Redis info
            Object info = redisTemplate.execute(connection -> {
                return connection.info();
            });
            
            return Map.of(
                "redis_info", info,
                "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            logger.error("Error getting cache stats", e);
            return Map.of("error", e.getMessage());
        }
    }
    
    // Cache warming
    public void warmCacheForPatient(Long patientId) {
        try {
            logger.info("Warming cache for patient: {}", patientId);
            
            // Pre-load common patterns
            String[] vitalTypes = {"HEART_RATE", "BLOOD_PRESSURE", "TEMPERATURE", "SPO2", "RESPIRATORY_RATE"};
            
            for (String vitalType : vitalTypes) {
                String key = LATEST_VITAL_KEY + patientId + ":" + vitalType;
                // This would typically load from database and cache
                // For now, we'll just check if key exists
                if (!exists(key)) {
                    logger.debug("Cache miss for patient {} vital type {}", patientId, vitalType);
                }
            }
            
            logger.info("Cache warming completed for patient: {}", patientId);
        } catch (Exception e) {
            logger.error("Error warming cache for patient: {}", patientId, e);
        }
    }
    
    // Cache invalidation
    public void invalidatePatientCache(Long patientId) {
        try {
            String pattern = "*:" + patientId + ":*";
            Set<String> keys = getKeysByPattern(pattern);
            
            if (!keys.isEmpty()) {
                deleteBatch(keys);
                logger.info("Invalidated {} cache keys for patient: {}", keys.size(), patientId);
            }
        } catch (Exception e) {
            logger.error("Error invalidating cache for patient: {}", patientId, e);
        }
    }
    
    public void invalidateVitalTypeCache(String vitalType) {
        try {
            String pattern = LATEST_VITAL_KEY + "*:" + vitalType;
            Set<String> keys = getKeysByPattern(pattern);
            
            if (!keys.isEmpty()) {
                deleteBatch(keys);
                logger.info("Invalidated {} cache keys for vital type: {}", keys.size(), vitalType);
            }
        } catch (Exception e) {
            logger.error("Error invalidating cache for vital type: {}", vitalType, e);
        }
    }
    
    // Health check
    public boolean isHealthy() {
        try {
            // Test Redis connection
            redisTemplate.opsForValue().set("health_check", "ok", 10);
            String result = (String) redisTemplate.opsForValue().get("health_check");
            redisTemplate.delete("health_check");
            
            return "ok".equals(result);
        } catch (Exception e) {
            logger.error("Redis health check failed", e);
            return false;
        }
    }
}
