package com.meditrack.ai.health;

import com.meditrack.ai.service.WekaService;
import com.meditrack.ai.service.WekaService.ModelInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AiPredictionHealthIndicator implements HealthIndicator {

    private final WekaService wekaService;
    private final String modelStoragePath;

    public AiPredictionHealthIndicator(
            WekaService wekaService,
            @Value("${meditrack.ai-prediction.weka.model-storage-path:./models}") String modelStoragePath) {
        this.wekaService = wekaService;
        this.modelStoragePath = modelStoragePath;
    }

    @Override
    public Health health() {
        try {
            Path storagePath = Paths.get(modelStoragePath).toAbsolutePath().normalize();
            Files.createDirectories(storagePath);

            List<ModelInfo> models = wekaService.listAllModels();
            List<String> modelNames = models.stream()
                    .map(ModelInfo::getModelName)
                    .collect(Collectors.toList());

            return Health.up()
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("modelStoragePath", storagePath.toString())
                    .withDetail("trainedModels", models.size())
                    .withDetail("modelNames", modelNames)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex)
                    .withDetail("component", "ai-prediction-health")
                    .withDetail("modelStoragePath", modelStoragePath)
                    .build();
        }
    }
}
