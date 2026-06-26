package com.meditrack.monitoring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PerformanceMonitoringService {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // In-memory metrics storage (fallback if Redis is not available)
    private final Map<String, AtomicLong> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> responseTimes = new ConcurrentHashMap<>();
    private final Map<String, ServiceMetrics> serviceMetrics = new ConcurrentHashMap<>();
    
    /**
     * Record API request
     */
    public void recordRequest(String serviceName, String endpoint, long responseTime, boolean success) {
        String key = serviceName + ":" + endpoint;
        
        // Increment request count
        requestCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        
        // Record response time
        responseTimes.computeIfAbsent(key, k -> new ArrayList<>()).add(responseTime);
        
        // Keep only last 100 response times
        List<Long> times = responseTimes.get(key);
        if (times.size() > 100) {
            times.remove(0);
        }
        
        // Increment error count if not successful
        if (!success) {
            errorCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }
        
        // Update service metrics
        ServiceMetrics metrics = serviceMetrics.computeIfAbsent(serviceName, s -> new ServiceMetrics());
        metrics.incrementRequests();
        if (success) {
            metrics.incrementSuccess();
        } else {
            metrics.incrementErrors();
        }
        metrics.addResponseTime(responseTime);
        
        // Store in Redis if available
        if (redisTemplate != null) {
            try {
                String redisKey = "metrics:" + key;
                redisTemplate.opsForHash().increment(redisKey, "requests", 1);
                if (!success) {
                    redisTemplate.opsForHash().increment(redisKey, "errors", 1);
                }
                redisTemplate.expire(redisKey, Duration.ofHours(24));
            } catch (Exception e) {
                logger.error("Failed to store metrics in Redis: {}", e.getMessage());
            }
        }
        
        logger.debug("Recorded request: Service={}, Endpoint={}, Time={}ms, Success={}", 
            serviceName, endpoint, responseTime, success);
    }
    
    /**
     * Get service metrics
     */
    public ServiceMetrics getServiceMetrics(String serviceName) {
        return serviceMetrics.getOrDefault(serviceName, new ServiceMetrics());
    }
    
    /**
     * Get endpoint metrics
     */
    public EndpointMetrics getEndpointMetrics(String serviceName, String endpoint) {
        String key = serviceName + ":" + endpoint;
        
        EndpointMetrics metrics = new EndpointMetrics();
        metrics.setServiceName(serviceName);
        metrics.setEndpoint(endpoint);
        metrics.setRequestCount(requestCounts.getOrDefault(key, new AtomicLong(0)).get());
        metrics.setErrorCount(errorCounts.getOrDefault(key, new AtomicLong(0)).get());
        
        List<Long> times = responseTimes.getOrDefault(key, Collections.emptyList());
        if (!times.isEmpty()) {
            metrics.setAverageResponseTime(times.stream().mapToLong(Long::longValue).average().orElse(0));
            metrics.setMinResponseTime(Collections.min(times));
            metrics.setMaxResponseTime(Collections.max(times));
            metrics.setP95ResponseTime(calculatePercentile(times, 95));
            metrics.setP99ResponseTime(calculatePercentile(times, 99));
        }
        
        return metrics;
    }
    
    /**
     * Get all service metrics
     */
    public Map<String, ServiceMetrics> getAllServiceMetrics() {
        return new HashMap<>(serviceMetrics);
    }
    
    /**
     * Calculate percentile
     */
    private long calculatePercentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0;
        
        List<Long> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        
        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
    
    /**
     * Listen for performance events from Kafka
     */
    @KafkaListener(topics = "performance-events", groupId = "monitoring-group")
    public void handlePerformanceEvent(String eventJson) {
        try {
            // Parse and process performance event
            logger.info("Performance event received: {}", eventJson);
            
            // In production, parse JSON and update metrics accordingly
            
        } catch (Exception e) {
            logger.error("Failed to process performance event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Reset metrics for a service
     */
    public void resetMetrics(String serviceName) {
        serviceMetrics.remove(serviceName);
        logger.info("Reset metrics for service: {}", serviceName);
    }
    
    /**
     * Reset all metrics
     */
    public void resetAllMetrics() {
        requestCounts.clear();
        errorCounts.clear();
        responseTimes.clear();
        serviceMetrics.clear();
        logger.info("Reset all metrics");
    }
    
    /**
     * Service metrics DTO
     */
    public static class ServiceMetrics {
        private long totalRequests;
        private long successfulRequests;
        private long failedRequests;
        private double averageResponseTime;
        private double minResponseTime;
        private double maxResponseTime;
        private double p95ResponseTime;
        private double p99ResponseTime;
        private LocalDateTime lastUpdated;
        private final List<Long> responseTimes = new ArrayList<>();
        
        public void incrementRequests() {
            totalRequests++;
            lastUpdated = LocalDateTime.now();
        }
        
        public void incrementSuccess() {
            successfulRequests++;
        }
        
        public void incrementErrors() {
            failedRequests++;
        }
        
        public void addResponseTime(long time) {
            responseTimes.add(time);
            if (responseTimes.size() > 1000) {
                responseTimes.remove(0);
            }
            updateResponseTimeMetrics();
        }
        
        private void updateResponseTimeMetrics() {
            if (!responseTimes.isEmpty()) {
                averageResponseTime = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);
                minResponseTime = Collections.min(responseTimes);
                maxResponseTime = Collections.max(responseTimes);
                
                List<Long> sorted = new ArrayList<>(responseTimes);
                Collections.sort(sorted);
                
                int p95Index = (int) Math.ceil(0.95 * sorted.size()) - 1;
                p95ResponseTime = sorted.get(Math.max(0, Math.min(p95Index, sorted.size() - 1)));
                
                int p99Index = (int) Math.ceil(0.99 * sorted.size()) - 1;
                p99ResponseTime = sorted.get(Math.max(0, Math.min(p99Index, sorted.size() - 1)));
            }
        }
        
        // Getters
        public long getTotalRequests() { return totalRequests; }
        public long getSuccessfulRequests() { return successfulRequests; }
        public long getFailedRequests() { return failedRequests; }
        public double getAverageResponseTime() { return averageResponseTime; }
        public double getMinResponseTime() { return minResponseTime; }
        public double getMaxResponseTime() { return maxResponseTime; }
        public double getP95ResponseTime() { return p95ResponseTime; }
        public double getP99ResponseTime() { return p99ResponseTime; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
        
        public double getSuccessRate() {
            return totalRequests > 0 ? (successfulRequests * 100.0 / totalRequests) : 0;
        }
        
        public double getErrorRate() {
            return totalRequests > 0 ? (failedRequests * 100.0 / totalRequests) : 0;
        }
    }
    
    /**
     * Endpoint metrics DTO
     */
    public static class EndpointMetrics {
        private String serviceName;
        private String endpoint;
        private long requestCount;
        private long errorCount;
        private double averageResponseTime;
        private long minResponseTime;
        private long maxResponseTime;
        private long p95ResponseTime;
        private long p99ResponseTime;
        
        // Getters and Setters
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        
        public long getRequestCount() { return requestCount; }
        public void setRequestCount(long requestCount) { this.requestCount = requestCount; }
        
        public long getErrorCount() { return errorCount; }
        public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
        
        public double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public long getMinResponseTime() { return minResponseTime; }
        public void setMinResponseTime(long minResponseTime) { this.minResponseTime = minResponseTime; }
        
        public long getMaxResponseTime() { return maxResponseTime; }
        public void setMaxResponseTime(long maxResponseTime) { this.maxResponseTime = maxResponseTime; }
        
        public long getP95ResponseTime() { return p95ResponseTime; }
        public void setP95ResponseTime(long p95ResponseTime) { this.p95ResponseTime = p95ResponseTime; }
        
        public long getP99ResponseTime() { return p99ResponseTime; }
        public void setP99ResponseTime(long p99ResponseTime) { this.p99ResponseTime = p99ResponseTime; }
        
        public double getErrorRate() {
            return requestCount > 0 ? (errorCount * 100.0 / requestCount) : 0;
        }
    }
}
