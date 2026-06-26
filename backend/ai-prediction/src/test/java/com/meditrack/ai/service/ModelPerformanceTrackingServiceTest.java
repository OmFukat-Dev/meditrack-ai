package com.meditrack.ai.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelPerformanceTrackingServiceTest {

    private final ModelPerformanceTrackingService service = new ModelPerformanceTrackingService();

    @Test
    void trackPredictionUpdatesMetricsHistoryAndGlobalStatistics() throws InterruptedException {
        service.trackPrediction("model-a", "POS", "POS", 0.91, Map.of("heartRate", 80));
        Thread.sleep(5);
        service.trackPrediction("model-a", "NEG", "POS", 0.62, Map.of("heartRate", 120));
        Thread.sleep(5);
        service.trackPrediction("model-a", "POS", "POS", 0.88, Map.of("heartRate", 78));

        ModelPerformanceTrackingService.ModelPerformanceMetrics metrics = service.getModelMetrics("model-a");
        assertEquals(3, metrics.getTotalPredictions());
        assertEquals(2, metrics.getCorrectPredictions());
        assertEquals(2.0 / 3.0, metrics.getOverallAccuracy(), 0.0001);

        List<ModelPerformanceTrackingService.PredictionRecord> history = service.getPredictionHistory("model-a", 10);
        assertEquals(3, history.size());
        assertEquals("POS", history.get(0).getPredictedClass());
        assertTrue(history.get(0).getTimestamp().isAfter(history.get(1).getTimestamp())
            || history.get(0).getTimestamp().isEqual(history.get(1).getTimestamp()));

        ModelPerformanceTrackingService.ModelPerformanceSummary summary = service.calculatePerformanceSummary(
            "model-a",
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1)
        );
        assertEquals(3, summary.getTotalPredictions());
        assertEquals(2.0 / 3.0, summary.getAccuracy(), 0.0001);
        assertTrue(summary.getClassMetrics().containsKey("POS"));
        assertTrue(summary.getClassMetrics().containsKey("NEG"));

        ModelPerformanceTrackingService.GlobalStatistics stats = service.getGlobalStatistics();
        assertEquals(1, stats.getTotalModels());
        assertEquals(3, stats.getTotalPredictions());
        assertEquals(2.0 / 3.0, stats.getAvgAccuracy(), 0.0001);
    }
}
