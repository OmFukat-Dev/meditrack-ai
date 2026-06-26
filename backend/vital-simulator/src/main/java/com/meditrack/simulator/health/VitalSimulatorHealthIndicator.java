package com.meditrack.simulator.health;

import com.meditrack.simulator.kafka.VitalKafkaProducer;
import com.meditrack.simulator.service.VitalSimulationService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class VitalSimulatorHealthIndicator implements HealthIndicator {

    private final VitalSimulationService vitalSimulationService;
    private final VitalKafkaProducer vitalKafkaProducer;

    public VitalSimulatorHealthIndicator(VitalSimulationService vitalSimulationService, VitalKafkaProducer vitalKafkaProducer) {
        this.vitalSimulationService = vitalSimulationService;
        this.vitalKafkaProducer = vitalKafkaProducer;
    }

    @Override
    public Health health() {
        try {
            boolean simulationHealthy = vitalSimulationService.isHealthy();
            boolean kafkaHealthy = vitalKafkaProducer.isHealthy();
            Map<String, Object> stats = vitalSimulationService.getSimulationStats();

            if (!simulationHealthy || !kafkaHealthy) {
                return Health.down()
                        .withDetail("timestamp", LocalDateTime.now())
                        .withDetail("simulationEngine", simulationHealthy ? "healthy" : "unhealthy")
                        .withDetail("kafkaProducer", kafkaHealthy ? "healthy" : "unhealthy")
                        .withDetail("simulationStats", stats)
                        .build();
            }

            return Health.up()
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("simulationEngine", "healthy")
                    .withDetail("kafkaProducer", "healthy")
                    .withDetail("simulationStats", stats)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("component", "vital-simulator-health")
                    .build();
        }
    }
}
