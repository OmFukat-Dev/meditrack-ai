package com.meditrack.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.ai.dto.NewsScoreResult;
import com.meditrack.ai.dto.VitalReadingEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

@Service
public class KafkaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventListener.class);
    private static final String ALERTS_TOPIC = "patient-alerts";
    private static final String LEGACY_ALERTS_TOPIC = "vital-alerts";
    private static final String PREDICTIONS_TOPIC = "patient-predictions";
    private static final String MODEL_NAME = "patient-deterioration";
    private static final int MAX_HISTORY_ENTRIES = 24;
    private static final Set<String> HIGH_RISK_LEVELS = Set.of("HIGH", "CRITICAL");
    private static final List<String> RISK_ORDER = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    @Autowired
    private NewsScoringService newsScoringService;

    @Autowired
    private WekaService wekaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private final ConcurrentMap<String, ConcurrentLinkedDeque<VitalReadingEvent>> patientHistory = new ConcurrentHashMap<>();

    @PostConstruct
    public void bootstrapDefaultModel() {
        try {
            if (wekaService.getModelInfo(MODEL_NAME) == null) {
                logger.info("Bootstrapping default J48 model for patient deterioration risk");
                trainDefaultRiskModel();
            }
        } catch (Exception e) {
            logger.warn("Unable to bootstrap default risk model: {}", e.getMessage());
        }
    }

    /**
     * Listen for vital reading events and trigger AI analysis.
     */
    @KafkaListener(topics = "patient-vitals", groupId = "ai-prediction-group")
    public void handleVitalReading(String vitalJson) {
        try {
            VitalReadingEvent event = objectMapper.readValue(vitalJson, VitalReadingEvent.class);
            if (event.getPatientId() == null || event.getPatientId().isBlank()) {
                logger.warn("Ignoring vital event without patientId");
                return;
            }
            if (event.getDepartment() == null || event.getDepartment().isBlank()) {
                logger.warn("Ignoring vital event without department metadata: patientId={}", event.getPatientId());
                return;
            }
            if (event.getCreatedBy() == null || event.getCreatedBy().isBlank()) {
                logger.warn("Ignoring vital event without createdBy metadata: patientId={}", event.getPatientId());
                return;
            }

            recordVitalEvent(event);
            analyzeAndPublish(event.getPatientId(), event, false);
        } catch (Exception e) {
            logger.error("Failed to process vital reading: {}", e.getMessage(), e);
        }
    }

    /**
     * Listen for prediction requests.
     */
    @KafkaListener(topics = "patient-predictions", groupId = "ai-prediction-group")
    public void handlePredictionRequest(String requestJson) {
        try {
            VitalReadingEvent request = objectMapper.readValue(requestJson, VitalReadingEvent.class);
            if (!"PREDICTION_REQUESTED".equals(request.getEventType()) || request.getPatientId() == null) {
                return;
            }

            logger.info("Processing prediction request for patient: {}", request.getPatientId());
            analyzeAndPublish(request.getPatientId(), request, true);
        } catch (Exception e) {
            logger.error("Failed to process prediction request: {}", e.getMessage(), e);
        }
    }

    private void analyzeAndPublish(String patientId, VitalReadingEvent triggerEvent, boolean requested) {
        PatientVitalsSnapshot snapshot = buildPatientVitalsSnapshot(patientId);

        NewsScoringService.NewsScore newsScore = newsScoringService.calculateNewsScore(snapshot.current);
        Map<String, Object> predictionFeatures = buildPredictionFeatures(snapshot, newsScore, triggerEvent);
        WekaService.PredictionResult modelPrediction = predictRisk(predictionFeatures);

        String trendRisk = newsScore.getRiskLevelWithTrend() != null ? newsScore.getRiskLevelWithTrend() : newsScore.getRiskLevel();
        String modelRisk = modelPrediction != null && modelPrediction.getPredictedClass() != null
            ? modelPrediction.getPredictedClass().toUpperCase()
            : trendRisk;
        String riskLevel = highestRisk(modelRisk, trendRisk);

        int newsScoreValue = newsScore.getTotalScoreWithTrend() > 0 ? newsScore.getTotalScoreWithTrend() : newsScore.getTotalScore();
        int trendScoreValue = newsScore.getTrendScore() != null ? newsScore.getTrendScore().getTotalTrendScore() : 0;
        int confidencePercent = calculateConfidencePercent(modelPrediction, newsScore, riskLevel);
        List<String> recommendations = buildRecommendations(snapshot, newsScore, riskLevel, requested);

        NewsScoreResult result = new NewsScoreResult();
        result.setPatientId(patientId);
        result.setNewsScore(newsScoreValue);
        result.setRiskLevel(riskLevel);
        result.setConfidence(String.valueOf(confidencePercent));
        result.setTimestamp(LocalDateTime.now());
        result.setRecommendations(recommendations);
        result.setSeverity(riskLevel);
        result.setMessage(buildPredictionMessage(patientId, riskLevel, newsScoreValue, trendScoreValue, requested));

        publishPrediction(triggerEvent, result, snapshot.latestEvent);

        if (HIGH_RISK_LEVELS.contains(riskLevel)) {
            publishAlert(triggerEvent, result, snapshot.latestEvent);
        }
    }

    private void recordVitalEvent(VitalReadingEvent event) {
        ConcurrentLinkedDeque<VitalReadingEvent> history = patientHistory.computeIfAbsent(
            event.getPatientId(),
            key -> new ConcurrentLinkedDeque<>()
        );

        synchronized (history) {
            history.addFirst(event);
            while (history.size() > MAX_HISTORY_ENTRIES) {
                history.pollLast();
            }
        }
    }

    private PatientVitalsSnapshot buildPatientVitalsSnapshot(String patientId) {
        ConcurrentLinkedDeque<VitalReadingEvent> history = patientHistory.get(patientId);
        NewsScoringService.PatientVitals current = new NewsScoringService.PatientVitals();
        current.setPatientId(patientId);

        if (history == null || history.isEmpty()) {
            current.setConsciousnessLevel("alert");
            return new PatientVitalsSnapshot(current, null, null, null, null);
        }

        Map<String, VitalReadingEvent> latestByType = new LinkedHashMap<>();
        Map<String, VitalReadingEvent> previousByType = new LinkedHashMap<>();
        VitalReadingEvent latestEvent = null;

        synchronized (history) {
            for (VitalReadingEvent event : history) {
                if (event.getVitalType() == null) {
                    continue;
                }
                latestEvent = latestEvent == null ? event : latestEvent;
                if (!latestByType.containsKey(event.getVitalType())) {
                    latestByType.put(event.getVitalType(), event);
                } else if (!previousByType.containsKey(event.getVitalType())) {
                    previousByType.put(event.getVitalType(), event);
                }
            }
        }

        applyVitalSnapshot(current, latestByType);
        current.setConsciousnessLevel(deriveConsciousnessLevel(latestEvent));
        BigDecimal currentDiastolic = resolveDiastolic(latestByType.get("BLOOD_PRESSURE"));

        NewsScoringService.PatientVitals previous = null;
        BigDecimal previousDiastolic = null;
        if (!previousByType.isEmpty()) {
            previous = new NewsScoringService.PatientVitals();
            previous.setPatientId(patientId);
            applyVitalSnapshot(previous, previousByType);
            previous.setConsciousnessLevel(deriveConsciousnessLevel(previousByType.values().stream().findFirst().orElse(null)));
            previousDiastolic = resolveDiastolic(previousByType.get("BLOOD_PRESSURE"));
        }

        if (previous != null) {
            current.setPreviousVitals(List.of(previous));
        }

        return new PatientVitalsSnapshot(current, previous, latestEvent, currentDiastolic, previousDiastolic);
    }

    private void applyVitalSnapshot(NewsScoringService.PatientVitals target, Map<String, VitalReadingEvent> readingsByType) {
        target.setRespiratoryRate(resolveNumber(readingsByType.get("RESPIRATORY_RATE")));
        target.setOxygenSaturation(resolveNumber(readingsByType.get("SPO2")));
        target.setTemperature(resolveNumber(readingsByType.get("TEMPERATURE")));
        target.setSystolicBp(resolveBloodPressure(readingsByType.get("BLOOD_PRESSURE")));
        target.setHeartRate(resolveNumber(readingsByType.get("HEART_RATE")));
        target.setSupplementalOxygen(false);
    }

    private BigDecimal resolveNumber(VitalReadingEvent event) {
        if (event == null) {
            return null;
        }
        return toBigDecimal(event.getValue());
    }

    private BigDecimal resolveBloodPressure(VitalReadingEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getSystolic() != null) {
            return toBigDecimal(event.getSystolic());
        }
        return toBigDecimal(event.getValue());
    }

    private BigDecimal resolveDiastolic(VitalReadingEvent event) {
        if (event == null) {
            return null;
        }
        if (event.getDiastolic() != null) {
            return toBigDecimal(event.getDiastolic());
        }
        return null;
    }

    private String deriveConsciousnessLevel(VitalReadingEvent event) {
        if (event == null) {
            return "alert";
        }
        String message = event.getMessage() == null ? "" : event.getMessage().toLowerCase();
        if (message.contains("unresponsive")) {
            return "unresponsive";
        }
        if (message.contains("pain")) {
            return "pain";
        }
        if (message.contains("voice")) {
            return "voice";
        }
        return "alert";
    }

    private Map<String, Object> buildPredictionFeatures(
        PatientVitalsSnapshot snapshot,
        NewsScoringService.NewsScore newsScore,
        VitalReadingEvent triggerEvent
    ) {
        Map<String, Object> features = new HashMap<>();
        features.put("heartRate", numberOrZero(snapshot.current.getHeartRate()));
        features.put("respiratoryRate", numberOrZero(snapshot.current.getRespiratoryRate()));
        features.put("oxygenSaturation", numberOrZero(snapshot.current.getOxygenSaturation()));
        features.put("temperature", numberOrZero(snapshot.current.getTemperature()));
        features.put("systolicBp", numberOrZero(snapshot.current.getSystolicBp()));
        features.put("diastolicBp", numberOrZero(snapshot.diastolicBp));
        features.put("newsScore", (double) (newsScore.getTotalScoreWithTrend() > 0 ? newsScore.getTotalScoreWithTrend() : newsScore.getTotalScore()));
        features.put("trendScore", (double) (newsScore.getTrendScore() != null ? newsScore.getTrendScore().getTotalTrendScore() : 0));
        return features;
    }

    private double numberOrZero(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private WekaService.PredictionResult predictRisk(Map<String, Object> features) {
        try {
            return wekaService.makePrediction(MODEL_NAME, features);
        } catch (Exception e) {
            logger.debug("J48 model prediction unavailable, falling back to NEWS scoring: {}", e.getMessage());
            return null;
        }
    }

    private int calculateConfidencePercent(WekaService.PredictionResult modelPrediction, NewsScoringService.NewsScore newsScore, String riskLevel) {
        int confidence = 55;
        if (modelPrediction != null) {
            confidence = (int) Math.round(modelPrediction.getConfidence() * 100);
        }

        if (newsScore.getTrendScore() != null) {
            confidence += Math.min(15, Math.abs(newsScore.getTrendScore().getTotalTrendScore()) * 2);
        }

        if ("CRITICAL".equals(riskLevel)) {
            confidence += 5;
        } else if ("HIGH".equals(riskLevel)) {
            confidence += 3;
        }

        return Math.max(50, Math.min(99, confidence));
    }

    private String highestRisk(String left, String right) {
        String normalizedLeft = normalizeRiskLevel(left);
        String normalizedRight = normalizeRiskLevel(right);

        if (severityRank(normalizedLeft) >= severityRank(normalizedRight)) {
            return normalizedLeft;
        }
        return normalizedRight;
    }

    private String normalizeRiskLevel(String value) {
        if (value == null || value.isBlank()) {
            return "LOW";
        }
        String normalized = value.trim().toUpperCase();
        if (!RISK_ORDER.contains(normalized)) {
            return "LOW";
        }
        return normalized;
    }

    private int severityRank(String riskLevel) {
        int index = RISK_ORDER.indexOf(normalizeRiskLevel(riskLevel));
        return index < 0 ? 0 : index;
    }

    private List<String> buildRecommendations(
        PatientVitalsSnapshot snapshot,
        NewsScoringService.NewsScore newsScore,
        String riskLevel,
        boolean requested
    ) {
        List<String> recommendations = new ArrayList<>();
        BigDecimal heartRate = snapshot.current.getHeartRate();
        BigDecimal oxygen = snapshot.current.getOxygenSaturation();
        BigDecimal temperature = snapshot.current.getTemperature();
        BigDecimal systolic = snapshot.current.getSystolicBp();

        if ("CRITICAL".equals(riskLevel)) {
            recommendations.add("Escalate to the emergency response team immediately");
            recommendations.add("Notify the patient department care team and admin on duty");
        } else if ("HIGH".equals(riskLevel)) {
            recommendations.add("Notify the patient department care team immediately");
        } else if ("MEDIUM".equals(riskLevel)) {
            recommendations.add("Increase bedside monitoring frequency");
        } else {
            recommendations.add("Continue standard monitoring");
        }

        if (oxygen != null && oxygen.doubleValue() < 94.0) {
            recommendations.add("Review oxygenation and respiratory support");
        }
        if (temperature != null && temperature.doubleValue() > 38.0) {
            recommendations.add("Assess for infection or fever escalation");
        }
        if (heartRate != null && heartRate.doubleValue() > 100.0) {
            recommendations.add("Review pain, fluids, and cardiac status");
        }
        if (systolic != null && systolic.doubleValue() < 90.0) {
            recommendations.add("Assess perfusion and blood pressure trend");
        }
        if (newsScore.getTrendScore() != null && newsScore.getTrendScore().getTotalTrendScore() > 4) {
            recommendations.add("Trend pattern indicates deterioration risk");
        }
        if (requested) {
            recommendations.add("Manual request acknowledged by AI service");
        }

        return recommendations.stream().distinct().toList();
    }

    private String buildPredictionMessage(String patientId, String riskLevel, int newsScore, int trendScore, boolean requested) {
        return String.format(
            "Patient %s risk assessment: %s risk, NEWS %d, trend score %d%s",
            patientId,
            riskLevel,
            newsScore,
            trendScore,
            requested ? " (manual request)" : ""
        );
    }

    private void publishPrediction(VitalReadingEvent triggerEvent, NewsScoreResult result, VitalReadingEvent sourceEvent) {
        try {
            VitalReadingEvent predictionEvent = new VitalReadingEvent();
            predictionEvent.setEventId(java.util.UUID.randomUUID().toString());
            predictionEvent.setEventType("PREDICTION_GENERATED");
            predictionEvent.setTimestamp(result.getTimestamp());
            predictionEvent.setPatientId(result.getPatientId());
            predictionEvent.setVitalType(sourceEvent != null ? sourceEvent.getVitalType() : triggerEvent != null ? triggerEvent.getVitalType() : null);
            predictionEvent.setSeverity(result.getSeverity());
            predictionEvent.setMessage(result.getMessage());
            predictionEvent.setConfidence(result.getConfidence());
            predictionEvent.setRiskLevel(result.getRiskLevel());
            predictionEvent.setNewsScore(result.getNewsScore());
            predictionEvent.setRecommendations(result.getRecommendations());
            predictionEvent.setNurseId(sourceEvent != null ? sourceEvent.getNurseId() : triggerEvent != null ? triggerEvent.getNurseId() : null);
            predictionEvent.setDepartment(sourceEvent != null ? sourceEvent.getDepartment() : triggerEvent != null ? triggerEvent.getDepartment() : null);
            predictionEvent.setCreatedBy(sourceEvent != null ? sourceEvent.getCreatedBy() : triggerEvent != null ? triggerEvent.getCreatedBy() : null);
            predictionEvent.setRole(sourceEvent != null ? sourceEvent.getRole() : triggerEvent != null ? triggerEvent.getRole() : null);

            String predictionJson = objectMapper.writeValueAsString(predictionEvent);
            kafkaTemplate.send(PREDICTIONS_TOPIC, result.getPatientId(), predictionJson);

            logger.info(
                "Published prediction: Patient={}, Risk={}, Confidence={}%",
                result.getPatientId(),
                result.getRiskLevel(),
                result.getConfidence()
            );
        } catch (Exception e) {
            logger.error("Failed to publish prediction: {}", e.getMessage(), e);
        }
    }

    private void publishAlert(VitalReadingEvent triggerEvent, NewsScoreResult result, VitalReadingEvent sourceEvent) {
        try {
            VitalReadingEvent alertEvent = new VitalReadingEvent();
            alertEvent.setEventId(java.util.UUID.randomUUID().toString());
            alertEvent.setEventType("ALERT_GENERATED");
            alertEvent.setTimestamp(result.getTimestamp());
            alertEvent.setPatientId(result.getPatientId());
            alertEvent.setVitalType(sourceEvent != null ? sourceEvent.getVitalType() : triggerEvent != null ? triggerEvent.getVitalType() : null);
            alertEvent.setSeverity(result.getRiskLevel());
            alertEvent.setMessage(result.getMessage());
            alertEvent.setConfidence(result.getConfidence());
            alertEvent.setRiskLevel(result.getRiskLevel());
            alertEvent.setNewsScore(result.getNewsScore());
            alertEvent.setRecommendations(result.getRecommendations());
            alertEvent.setNurseId(sourceEvent != null ? sourceEvent.getNurseId() : triggerEvent != null ? triggerEvent.getNurseId() : null);
            alertEvent.setDepartment(sourceEvent != null ? sourceEvent.getDepartment() : triggerEvent != null ? triggerEvent.getDepartment() : null);
            alertEvent.setCreatedBy(sourceEvent != null ? sourceEvent.getCreatedBy() : triggerEvent != null ? triggerEvent.getCreatedBy() : null);
            alertEvent.setRole(sourceEvent != null ? sourceEvent.getRole() : triggerEvent != null ? triggerEvent.getRole() : "SYSTEM");

            String alertJson = objectMapper.writeValueAsString(alertEvent);
            kafkaTemplate.send(ALERTS_TOPIC, result.getPatientId(), alertJson);
            kafkaTemplate.send(LEGACY_ALERTS_TOPIC, result.getPatientId(), alertJson);

            logger.warn(
                "ALERT PUBLISHED: Patient={}, Severity={}, Message={}",
                result.getPatientId(),
                result.getRiskLevel(),
                result.getMessage()
            );
        } catch (Exception e) {
            logger.error("Failed to publish alert: {}", e.getMessage(), e);
        }
    }

    private void trainDefaultRiskModel() {
        try {
            List<Map<String, Object>> trainingData = new ArrayList<>();
            trainingData.addAll(buildSyntheticTrainingSet("LOW", 60, 16, 98, 36.8, 120, 80, 1, 1));
            trainingData.addAll(buildSyntheticTrainingSet("MEDIUM", 92, 20, 95, 37.8, 138, 86, 4, 4));
            trainingData.addAll(buildSyntheticTrainingSet("HIGH", 118, 28, 91, 38.7, 160, 95, 8, 8));
            trainingData.addAll(buildSyntheticTrainingSet("CRITICAL", 140, 34, 84, 39.6, 185, 110, 12, 12));

            Map<String, Object> params = Map.of(
                "confidenceFactor", 0.25f,
                "minNumObj", 2,
                "reducedErrorPruning", true,
                "numFolds", 3
            );

            wekaService.trainJ48Model(MODEL_NAME, trainingData, "riskLevel", params);
        } catch (Exception e) {
            logger.warn("Default risk model bootstrap failed: {}", e.getMessage(), e);
        }
    }

    private List<Map<String, Object>> buildSyntheticTrainingSet(
        String riskLevel,
        int heartRate,
        int respiratoryRate,
        int oxygenSaturation,
        double temperature,
        int systolicBp,
        int diastolicBp,
        int newsScore,
        int trendScore
    ) {
        List<Map<String, Object>> samples = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            Map<String, Object> sample = new HashMap<>();
            sample.put("heartRate", heartRate + (i % 3) - 1);
            sample.put("respiratoryRate", respiratoryRate + (i % 2));
            sample.put("oxygenSaturation", oxygenSaturation - (i % 4));
            sample.put("temperature", round(temperature + (i % 2) * 0.1));
            sample.put("systolicBp", systolicBp + (i % 3));
            sample.put("diastolicBp", diastolicBp + (i % 2));
            sample.put("newsScore", newsScore + (i % 2));
            sample.put("trendScore", trendScore + (i % 3));
            sample.put("riskLevel", riskLevel);
            samples.add(sample);
        }
        return samples;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private static class PatientVitalsSnapshot {
        private final NewsScoringService.PatientVitals current;
        private final NewsScoringService.PatientVitals previous;
        private final VitalReadingEvent latestEvent;
        private final BigDecimal diastolicBp;
        private final BigDecimal previousDiastolicBp;

        private PatientVitalsSnapshot(
            NewsScoringService.PatientVitals current,
            NewsScoringService.PatientVitals previous,
            VitalReadingEvent latestEvent,
            BigDecimal diastolicBp,
            BigDecimal previousDiastolicBp
        ) {
            this.current = current;
            this.previous = previous;
            this.latestEvent = latestEvent;
            this.diastolicBp = diastolicBp;
            this.previousDiastolicBp = previousDiastolicBp;
        }
    }
}
