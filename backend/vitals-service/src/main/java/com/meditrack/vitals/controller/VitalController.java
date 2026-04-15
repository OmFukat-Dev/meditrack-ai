package com.meditrack.vitals.controller;

import com.meditrack.vitals.dto.VitalReadingMessage;
import com.meditrack.vitals.entity.VitalReading;
import com.meditrack.vitals.service.ErrorHandlerService;
import com.meditrack.vitals.service.RateLimitingService;
import com.meditrack.vitals.service.VitalService;
import com.meditrack.vitals.service.RateLimitingService.RateLimitResult;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vitals")
@CrossOrigin(origins = "*")
public class VitalController {
    
    private static final Logger logger = LoggerFactory.getLogger(VitalController.class);
    
    @Autowired
    private VitalService vitalService;
    
    @Autowired
    private RateLimitingService rateLimitingService;
    
    @Autowired
    private ErrorHandlerService errorHandlerService;
    
    // Create vital reading
    @PostMapping
    @Counted(value = "vital.api.created", description = "Number of vitals created via API")
    @Timed(value = "vital.api.create.time", description = "Time taken to create vital via API")
    public ResponseEntity<?> createVitalReading(@Valid @RequestBody VitalReadingMessage vitalMessage,
                                              @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            // Rate limiting check
            RateLimitResult rateLimitResult = rateLimitingService.checkVitalIngestionRateLimit(
                vitalMessage.getPatientIdentifier(), vitalMessage.getVitalType());
            
            if (!rateLimitResult.isAllowed()) {
                logger.warn("Rate limit exceeded for patient: {}, vitalType: {}", 
                           vitalMessage.getPatientIdentifier(), vitalMessage.getVitalType());
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "error", "Rate limit exceeded",
                        "message", rateLimitResult.getUserMessage(),
                        "resetTime", rateLimitResult.getResetTime()
                    ));
            }
            
            // Process vital reading
            VitalReading savedReading = vitalService.processVitalReading(vitalMessage);
            
            logger.info("Created vital reading: id={}, patientId={}, vitalType={}", 
                       savedReading.getId(), savedReading.getPatient().getId(), savedReading.getVitalType());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedReading);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid vital reading: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Validation failed", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating vital reading: {}", e.getMessage(), e);
            
            ErrorHandlerService.ErrorResult errorResult = errorHandlerService.handleError(
                e, "CREATE_VITAL", Map.of("userId", userId, "vitalMessage", vitalMessage));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", errorResult.getErrorCategory(),
                    "message", errorResult.getUserMessage(),
                    "shouldRetry", errorResult.shouldRetry()
                ));
        }
    }
    
    // Get vital readings for patient
    @GetMapping("/patient/{patientId}")
    @Counted(value = "vital.api.read.patient", description = "Number of patient vitals read via API")
    @Timed(value = "vital.api.read.patient.time", description = "Time taken to read patient vitals via API")
    public ResponseEntity<?> getVitalReadings(
            @PathVariable Long patientId,
            @RequestParam(required = false) String vitalType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            // Rate limiting check
            RateLimitResult rateLimitResult = rateLimitingService.checkPatientRateLimit(String.valueOf(patientId));
            
            if (!rateLimitResult.isAllowed()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "error", "Rate limit exceeded",
                        "message", rateLimitResult.getUserMessage(),
                        "resetTime", rateLimitResult.getResetTime()
                    ));
            }
            
            Page<VitalReading> readings = vitalService.getVitalReadings(
                patientId, vitalType, startTime, endTime, page, size);
            
            return ResponseEntity.ok(readings);
            
        } catch (Exception e) {
            logger.error("Error getting vital readings for patient {}: {}", patientId, e.getMessage(), e);
            
            ErrorHandlerService.ErrorResult errorResult = errorHandlerService.handleError(
                e, "READ_PATIENT_VITALS", Map.of("patientId", patientId, "vitalType", vitalType));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", errorResult.getErrorCategory(),
                    "message", errorResult.getUserMessage(),
                    "shouldRetry", errorResult.shouldRetry()
                ));
        }
    }
    
    // Get latest vitals for patient
    @GetMapping("/patient/{patientId}/latest")
    @Counted(value = "vital.api.read.latest", description = "Number of latest vitals read via API")
    @Timed(value = "vital.api.read.latest.time", description = "Time taken to read latest vitals via API")
    public ResponseEntity<?> getLatestVitals(@PathVariable Long patientId) {
        try {
            // Rate limiting check
            RateLimitResult rateLimitResult = rateLimitingService.checkPatientRateLimit(String.valueOf(patientId));
            
            if (!rateLimitResult.isAllowed()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "error", "Rate limit exceeded",
                        "message", rateLimitResult.getUserMessage(),
                        "resetTime", rateLimitResult.getResetTime()
                    ));
            }
            
            List<VitalReading> latestVitals = vitalService.getLatestVitals(patientId);
            
            return ResponseEntity.ok(latestVitals);
            
        } catch (Exception e) {
            logger.error("Error getting latest vitals for patient {}: {}", patientId, e.getMessage(), e);
            
            ErrorHandlerService.ErrorResult errorResult = errorHandlerService.handleError(
                e, "READ_LATEST_VITALS", Map.of("patientId", patientId));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", errorResult.getErrorCategory(),
                    "message", errorResult.getUserMessage(),
                    "shouldRetry", errorResult.shouldRetry()
                ));
        }
    }
    
    // Get abnormal readings for patient
    @GetMapping("/patient/{patientId}/abnormal")
    @Counted(value = "vital.api.read.abnormal", description = "Number of abnormal vitals read via API")
    @Timed(value = "vital.api.read.abnormal.time", description = "Time taken to read abnormal vitals via API")
    public ResponseEntity<?> getAbnormalReadings(
            @PathVariable Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        
        try {
            if (since == null) {
                since = LocalDateTime.now().minusHours(24); // Default to last 24 hours
            }
            
            // Rate limiting check
            RateLimitResult rateLimitResult = rateLimitingService.checkPatientRateLimit(String.valueOf(patientId));
            
            if (!rateLimitResult.isAllowed()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "error", "Rate limit exceeded",
                        "message", rateLimitResult.getUserMessage(),
                        "resetTime", rateLimitResult.getResetTime()
                    ));
            }
            
            List<VitalReading> abnormalReadings = vitalService.getAbnormalReadings(patientId, since);
            
            return ResponseEntity.ok(abnormalReadings);
            
        } catch (Exception e) {
            logger.error("Error getting abnormal readings for patient {}: {}", patientId, e.getMessage(), e);
            
            ErrorHandlerService.ErrorResult errorResult = errorHandlerService.handleError(
                e, "READ_ABNORMAL_VITALS", Map.of("patientId", patientId, "since", since));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", errorResult.getErrorCategory(),
                    "message", errorResult.getUserMessage(),
                    "shouldRetry", errorResult.shouldRetry()
                ));
        }
    }
    
    // Get critical readings for patient
    @GetMapping("/patient/{patientId}/critical")
    @Counted(value = "vital.api.read.critical", description = "Number of critical vitals read via API")
    @Timed(value = "vital.api.read.critical.time", description = "Time taken to read critical vitals via API")
    public ResponseEntity<?> getCriticalReadings(
            @PathVariable Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        
        try {
            if (since == null) {
                since = LocalDateTime.now().minusHours(24); // Default to last 24 hours
            }
            
            // Rate limiting check
            RateLimitResult rateLimitResult = rateLimitingService.checkPatientRateLimit(String.valueOf(patientId));
            
            if (!rateLimitResult.isAllowed()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "error", "Rate limit exceeded",
                        "message", rateLimitResult.getUserMessage(),
                        "resetTime", rateLimitResult.getResetTime()
                    ));
            }
            
            List<VitalReading> criticalReadings = vitalService.getCriticalReadings(patientId, since);
            
            return ResponseEntity.ok(criticalReadings);
            
        } catch (Exception e) {
            logger.error("Error getting critical readings for patient {}: {}", patientId, e.getMessage(), e);
            
            ErrorHandlerService.ErrorResult errorResult = errorHandlerService.handleError(
                e, "READ_CRITICAL_VITALS", Map.of("patientId", patientId, "since", since));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", errorResult.getErrorCategory(),
                    "message", errorResult.getUserMessage(),
                    "shouldRetry", errorResult.shouldRetry()
                ));
        }
    }
    
    // Get vital summary for patient
    @GetMapping("/patient/{patientId}/summary")
    @Counted(value = "vital.api.read.summary", description = "Number of vital summaries read via API")
    @Timed(value = "vital.api.read.summary.time", description = "Time taken to read vital summary via API")
    public ResponseEntity<?> getVitalSummary(@PathVariable Long patientId) {
        try {
            // Rate limiting check
            RateLimitResult rateLimitResult = rateLimitingService.checkPatientRateLimit(String.valueOf(patientId));
            
            if (!rateLimitResult.isAllowed()) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "error", "Rate limit exceeded",
                        "message", rateLimitResult.getUserMessage(),
                        "resetTime", rateLimitResult.getResetTime()
                    ));
            }
            
            List<VitalReading> latestVitals = vitalService.getLatestVitals(patientId);
            
            // Create summary
            Map<String, Object> summary = Map.of(
                "patientId", patientId,
                "latestVitals", latestVitals,
                "timestamp", LocalDateTime.now(),
                "vitalCount", latestVitals.size()
            );
            
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            logger.error("Error getting vital summary for patient {}: {}", patientId, e.getMessage(), e);
            
            ErrorHandlerService.ErrorResult errorResult = errorHandlerService.handleError(
                e, "READ_VITAL_SUMMARY", Map.of("patientId", patientId));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", errorResult.getErrorCategory(),
                    "message", errorResult.getUserMessage(),
                    "shouldRetry", errorResult.shouldRetry()
                ));
        }
    }
    
    // Health check endpoint
    @GetMapping("/health")
    @Counted(value = "vital.api.health", description = "Number of health checks via API")
    @Timed(value = "vital.api.health.time", description = "Time taken for health check via API")
    public ResponseEntity<?> healthCheck() {
        try {
            boolean isHealthy = vitalService.isHealthy();
            
            Map<String, Object> health = Map.of(
                "status", isHealthy ? "UP" : "DOWN",
                "timestamp", LocalDateTime.now(),
                "service", "vitals-service"
            );
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            logger.error("Error in health check: {}", e.getMessage(), e);
            
            Map<String, Object> health = Map.of(
                "status", "DOWN",
                "timestamp", LocalDateTime.now(),
                "service", "vitals-service",
                "error", e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
    
    // Service statistics
    @GetMapping("/stats")
    @Counted(value = "vital.api.stats", description = "Number of stats requests via API")
    @Timed(value = "vital.api.stats.time", description = "Time taken for stats request via API")
    public ResponseEntity<?> getServiceStats() {
        try {
            Map<String, Object> stats = Map.of(
                "timestamp", LocalDateTime.now(),
                "service", "vitals-service",
                "rateLimitStats", rateLimitingService.getRateLimitStats("global"),
                "errorStats", errorHandlerService.getErrorStats(),
                "cacheStats", vitalService.getCacheStats()
            );
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            logger.error("Error getting service stats: {}", e.getMessage(), e);
            
            ErrorHandlerService.ErrorResult errorResult = errorHandlerService.handleError(
                e, "GET_SERVICE_STATS", Map.of());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", errorResult.getErrorCategory(),
                    "message", errorResult.getUserMessage(),
                    "shouldRetry", errorResult.shouldRetry()
                ));
        }
    }
    
    // Exception handler
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGlobalException(Exception e) {
        logger.error("Unhandled exception: {}", e.getMessage(), e);
        
        ErrorHandlerService.ErrorResult errorResult = errorHandlerService.handleError(
            e, "GLOBAL_EXCEPTION", Map.of());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", errorResult.getErrorCategory(),
                "message", errorResult.getUserMessage(),
                "shouldRetry", errorResult.shouldRetry(),
                "timestamp", LocalDateTime.now()
            ));
    }
}
