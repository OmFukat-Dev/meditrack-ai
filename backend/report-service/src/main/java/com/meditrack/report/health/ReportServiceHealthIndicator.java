package com.meditrack.report.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@Component
public class ReportServiceHealthIndicator implements HealthIndicator {

    private final String reportsPath;

    public ReportServiceHealthIndicator(@Value("${meditrack.report.storage.reports-path:./reports/}") String reportsPath) {
        this.reportsPath = reportsPath;
    }

    @Override
    public Health health() {
        try {
            Path reportDirectory = Paths.get(reportsPath).toAbsolutePath().normalize();
            Files.createDirectories(reportDirectory);

            long reportFiles;
            try (Stream<Path> paths = Files.list(reportDirectory)) {
                reportFiles = paths.filter(Files::isRegularFile).count();
            }

            boolean writable = Files.isWritable(reportDirectory);
            if (!writable) {
                return Health.down()
                        .withDetail("timestamp", LocalDateTime.now())
                        .withDetail("reportsPath", reportDirectory.toString())
                        .withDetail("writable", false)
                        .build();
            }

            return Health.up()
                    .withDetail("timestamp", LocalDateTime.now())
                    .withDetail("reportsPath", reportDirectory.toString())
                    .withDetail("writable", true)
                    .withDetail("reportFiles", reportFiles)
                    .build();
        } catch (IOException ex) {
            return Health.down(ex)
                    .withDetail("component", "report-service-health")
                    .withDetail("reportsPath", reportsPath)
                    .build();
        }
    }
}
