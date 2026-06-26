package com.meditrack.ai.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewsScoringServiceTest {

    private final NewsScoringService service = new NewsScoringService();

    @Test
    void calculateNewsScoreReturnsCriticalRiskForSevereVitals() {
        NewsScoringService.PatientVitals vitals = new NewsScoringService.PatientVitals();
        vitals.setPatientId("patient-1");
        vitals.setRespiratoryRate(new BigDecimal("30"));
        vitals.setOxygenSaturation(new BigDecimal("90"));
        vitals.setSupplementalOxygen(false);
        vitals.setTemperature(new BigDecimal("39.2"));
        vitals.setSystolicBp(new BigDecimal("240"));
        vitals.setHeartRate(new BigDecimal("120"));
        vitals.setConsciousnessLevel("unresponsive");

        NewsScoringService.NewsScore score = service.calculateNewsScore(vitals);

        assertEquals("patient-1", score.getPatientId());
        assertEquals(15, score.getTotalScore());
        assertEquals("CRITICAL", score.getRiskLevel());
        assertEquals(2, score.getRespiratoryRateScore().getScore());
        assertEquals(3, score.getOxygenSaturationScore().getScore());
        assertEquals(2, score.getTemperatureScore().getScore());
        assertEquals(2, score.getSystolicBpScore().getScore());
        assertEquals(3, score.getHeartRateScore().getScore());
        assertEquals(3, score.getConsciousnessScore().getScore());
        assertNotNull(score.getTimestamp());
    }

    @Test
    void getNewsScoringParametersIncludesExpectedBuckets() {
        Map<String, Map<String, Integer>> parameters = service.getNewsScoringParameters();

        assertTrue(parameters.containsKey("heart_rate"));
        assertTrue(parameters.containsKey("respiratory_rate"));
        assertEquals(0, parameters.get("heart_rate").get("normal"));
        assertEquals(3, parameters.get("heart_rate").get("high"));
    }
}
