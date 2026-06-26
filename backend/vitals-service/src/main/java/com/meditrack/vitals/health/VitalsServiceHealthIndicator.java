package com.meditrack.vitals.health;

import com.meditrack.vitals.repository.VitalReadingRepository;
import com.meditrack.vitals.service.RedisCacheService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class VitalsServiceHealthIndicator implements HealthIndicator {

    private final VitalReadingRepository vitalReadingRepository;
    private final RedisCacheService redisCacheService;

    public VitalsServiceHealthIndicator(VitalReadingRepository vitalReadingRepository, RedisCacheService redisCacheService) {
        this.vitalReadingRepository = vitalReadingRepository;
        this.redisCacheService = redisCacheService;
    }

    @Override
    public Health health() {
        try {
            long vitalReadings = vitalReadingRepository.count();
            boolean redisHealthy = redisCacheService.isHealthy();

            if (!redisHealthy) {
                return Health.down()
                        .withDetail("timestamp", LocalDateTime.now())
                        .withDetail("redis", "unhealthy")
                        .withDetail("vitalReadings", vitalReadings)
                        .build();
            }

            return Health.up()
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("redis", "healthy")
                    .withDetail("vitalReadings", vitalReadings)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("component", "vitals-service-health")
                    .build();
        }
    }
}
