package com.meditrack.ai.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureEngineeringServiceTest {

    private final FeatureEngineeringService service = new FeatureEngineeringService();

    @Test
    void engineerFeaturesCalculatesVitalAndCrossVitalStatistics() {
        LocalDateTime base = LocalDateTime.of(2026, 4, 23, 12, 0);
        List<FeatureEngineeringService.VitalReading> readings = List.of(
            reading("HEART_RATE", 70, base.minusHours(3), 0.90),
            reading("HEART_RATE", 72, base.minusHours(2), 0.85),
            reading("HEART_RATE", 74, base.minusHours(1), 0.80),
            reading("BLOOD_PRESSURE", 120, base.minusHours(3), 0.95),
            reading("BLOOD_PRESSURE", 122, base.minusHours(2), 0.92),
            reading("BLOOD_PRESSURE", 124, base.minusHours(1), 0.88)
        );

        FeatureEngineeringService.EngineeredFeatures features = service.engineerFeatures(readings, "patient-1");

        assertEquals("patient-1", features.getPatientId());
        assertEquals(6, features.getOriginalReadingCount());
        assertEquals(2, features.getVitalFeatures().size());

        FeatureEngineeringService.VitalFeatures heartRate = features.getVitalFeatures().get("HEART_RATE");
        FeatureEngineeringService.VitalFeatures bloodPressure = features.getVitalFeatures().get("BLOOD_PRESSURE");

        assertNotNull(heartRate);
        assertNotNull(bloodPressure);
        assertEquals(3, heartRate.getReadingCount());
        assertEquals(74.0, heartRate.getCurrentValue(), 0.0001);
        assertEquals(72.0, heartRate.getMean(), 0.0001);
        assertEquals("INCREASING", heartRate.getTrendDirection());
        assertEquals(124.0, bloodPressure.getCurrentValue(), 0.0001);

        assertNotNull(features.getCrossVitalFeatures());
        assertTrue(features.getCrossVitalFeatures().getHrBpCorrelation() > 0.99);
        assertEquals(74.0 / 124.0, features.getCrossVitalFeatures().getShockIndex(), 0.0001);

        assertNotNull(features.getTemporalFeatures());
        assertNotNull(features.getStatisticalFeatures());
        assertEquals(97.0, features.getStatisticalFeatures().getOverallMean(), 0.0001);
        assertTrue(features.getStatisticalFeatures().getAverageQualityScore() > 0.85);
    }

    @Test
    void engineerFeaturesHandlesEmptyInput() {
        FeatureEngineeringService.EngineeredFeatures features = service.engineerFeatures(List.of(), "patient-1");

        assertEquals("patient-1", features.getPatientId());
        assertEquals(0, features.getOriginalReadingCount());
        assertTrue(features.getVitalFeatures().isEmpty());
        assertNotNull(features.getCrossVitalFeatures());
        assertNotNull(features.getTemporalFeatures());
        assertNotNull(features.getStatisticalFeatures());
    }

    private FeatureEngineeringService.VitalReading reading(String vitalType, double value, LocalDateTime timestamp, double qualityScore) {
        FeatureEngineeringService.VitalReading reading = new FeatureEngineeringService.VitalReading();
        reading.setVitalType(vitalType);
        reading.setValue(BigDecimal.valueOf(value));
        reading.setTimestamp(timestamp);
        reading.setQualityScore(BigDecimal.valueOf(qualityScore));
        return reading;
    }
}
