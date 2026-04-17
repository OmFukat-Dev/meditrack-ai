package com.meditrack.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeatureEngineeringService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureEngineeringService.class);
    
    // Feature engineering window sizes
    private static final int SHORT_TERM_WINDOW = 6;    // 6 readings (3 hours for 30-min intervals)
    private static final int MEDIUM_TERM_WINDOW = 12;   // 12 readings (6 hours)
    private static final int LONG_TERM_WINDOW = 24;     // 24 readings (12 hours)
    
    // Engineer features from vital readings
    public EngineeredFeatures engineerFeatures(List<VitalReading> vitalReadings, String patientId) {
        try {
            logger.debug("Engineering features for patient: {} with {} readings", patientId, vitalReadings.size());
            
            EngineeredFeatures features = new EngineeredFeatures();
            features.setPatientId(patientId);
            features.setTimestamp(LocalDateTime.now());
            features.setOriginalReadingCount(vitalReadings.size());
            
            // Sort readings by timestamp
            List<VitalReading> sortedReadings = vitalReadings.stream()
                .sorted(Comparator.comparing(VitalReading::getTimestamp))
                .collect(Collectors.toList());
            
            // Group by vital type
            Map<String, List<VitalReading>> readingsByType = sortedReadings.stream()
                .collect(Collectors.groupingBy(VitalReading::getVitalType));
            
            // Engineer features for each vital type
            Map<String, VitalFeatures> vitalFeatures = new HashMap<>();
            
            for (Map.Entry<String, List<VitalReading>> entry : readingsByType.entrySet()) {
                String vitalType = entry.getKey();
                List<VitalReading> typeReadings = entry.getValue();
                
                VitalFeatures typeFeatures = engineerVitalFeatures(vitalType, typeReadings);
                vitalFeatures.put(vitalType, typeFeatures);
            }
            
            features.setVitalFeatures(vitalFeatures);
            
            // Engineer cross-vital features
            features.setCrossVitalFeatures(engineerCrossVitalFeatures(vitalFeatures));
            
            // Engineer temporal features
            features.setTemporalFeatures(engineerTemporalFeatures(sortedReadings));
            
            // Engineer statistical features
            features.setStatisticalFeatures(engineerStatisticalFeatures(sortedReadings));
            
            logger.info("Engineered {} features for patient: {}", 
                       features.getTotalFeatureCount(), patientId);
            
            return features;
            
        } catch (Exception e) {
            logger.error("Error engineering features for patient: {}", patientId, e);
            throw new RuntimeException("Failed to engineer features", e);
        }
    }
    
    // Engineer features for a specific vital type
    private VitalFeatures engineerVitalFeatures(String vitalType, List<VitalReading> readings) {
        VitalFeatures features = new VitalFeatures();
        features.setVitalType(vitalType);
        features.setReadingCount(readings.size());
        
        if (readings.isEmpty()) {
            return features;
        }
        
        // Extract values
        List<Double> values = readings.stream()
            .map(r -> r.getValue().doubleValue())
            .collect(Collectors.toList());
        
        // Basic statistical features
        features.setCurrentValue(values.get(values.size() - 1));
        features.setMean(calculateMean(values));
        features.setMedian(calculateMedian(values));
        features.setStandardDeviation(calculateStandardDeviation(values));
        features.setVariance(calculateVariance(values));
        features.setMin(Collections.min(values));
        features.setMax(Collections.max(values));
        features.setRange(features.getMax() - features.getMin());
        
        // Rate of change features
        features.setRateOfChange(calculateRateOfChange(values));
        features.setRateOfChangePercentage(calculateRateOfChangePercentage(values));
        features.setAcceleration(calculateAcceleration(values));
        
        // Trend features
        features.setTrendSlope(calculateTrendSlope(values));
        features.setTrendDirection(determineTrendDirection(values));
        features.setTrendStrength(calculateTrendStrength(values));
        
        // Window-based features
        features.setShortTermMean(calculateWindowMean(values, SHORT_TERM_WINDOW));
        features.setMediumTermMean(calculateWindowMean(values, MEDIUM_TERM_WINDOW));
        features.setLongTermMean(calculateWindowMean(values, LONG_TERM_WINDOW));
        
        features.setShortTermStdDev(calculateWindowStdDev(values, SHORT_TERM_WINDOW));
        features.setMediumTermStdDev(calculateWindowStdDev(values, MEDIUM_TERM_WINDOW));
        features.setLongTermStdDev(calculateWindowStdDev(values, LONG_TERM_WINDOW));
        
        // Volatility features
        features.setVolatility(calculateVolatility(values));
        features.setVolatilityShortTerm(calculateVolatilityWindow(values, SHORT_TERM_WINDOW));
        features.setVolatilityMediumTerm(calculateVolatilityWindow(values, MEDIUM_TERM_WINDOW));
        features.setVolatilityLongTerm(calculateVolatilityWindow(values, LONG_TERM_WINDOW));
        
        // Percentile features
        features.setPercentile25(calculatePercentile(values, 25));
        features.setPercentile75(calculatePercentile(values, 75));
        features.setPercentile90(calculatePercentile(values, 90));
        features.setPercentile95(calculatePercentile(values, 95));
        
        // Outlier detection features
        features.setOutlierCount(calculateOutlierCount(values));
        features.setOutlierPercentage(features.getOutlierCount() / (double) values.size());
        
        // Time-based features
        features.setFirstReadingTime(readings.get(0).getTimestamp());
        features.setLastReadingTime(readings.get(readings.size() - 1).getTimestamp());
        features.setTimeSpanMinutes(calculateTimeSpan(readings));
        features.setReadingFrequency(calculateReadingFrequency(readings));
        
        return features;
    }
    
    // Engineer cross-vital features
    private CrossVitalFeatures engineerCrossVitalFeatures(Map<String, VitalFeatures> vitalFeatures) {
        CrossVitalFeatures features = new CrossVitalFeatures();
        
        // Get heart rate and blood pressure features
        VitalFeatures heartRate = vitalFeatures.get("HEART_RATE");
        VitalFeatures bloodPressure = vitalFeatures.get("BLOOD_PRESSURE");
        VitalFeatures temperature = vitalFeatures.get("TEMPERATURE");
        VitalFeatures spo2 = vitalFeatures.get("SPO2");
        VitalFeatures respiratoryRate = vitalFeatures.get("RESPIRATORY_RATE");
        
        // Heart rate and blood pressure correlation
        if (heartRate != null && bloodPressure != null) {
            features.setHrBpCorrelation(calculateCorrelation(
                getRecentValues(heartRate), getRecentValues(bloodPressure)));
            features.setShockIndex(calculateShockIndex(heartRate.getCurrentValue(), bloodPressure.getCurrentValue()));
        }
        
        // Temperature and heart rate relationship
        if (temperature != null && heartRate != null) {
            features.setTempHrCorrelation(calculateCorrelation(
                getRecentValues(temperature), getRecentValues(heartRate)));
        }
        
        // SpO2 and respiratory rate relationship
        if (spo2 != null && respiratoryRate != null) {
            features.setSpo2RrCorrelation(calculateCorrelation(
                getRecentValues(spo2), getRecentValues(respiratoryRate)));
        }
        
        // Calculate composite scores
        features.setVitalStabilityScore(calculateVitalStabilityScore(vitalFeatures));
        features.setOverallTrendScore(calculateOverallTrendScore(vitalFeatures));
        features.setAbnormalityScore(calculateAbnormalityScore(vitalFeatures));
        
        return features;
    }
    
    // Engineer temporal features
    private TemporalFeatures engineerTemporalFeatures(List<VitalReading> readings) {
        TemporalFeatures features = new TemporalFeatures();
        
        if (readings.isEmpty()) {
            return features;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstReading = readings.get(0).getTimestamp();
        LocalDateTime lastReading = readings.get(readings.size() - 1).getTimestamp();
        
        // Time-based features
        features.setHourOfDay(now.getHour());
        features.setDayOfWeek(now.getDayOfWeek().getValue());
        features.setDayOfMonth(now.getDayOfMonth());
        features.setMonthOfYear(now.getMonthValue());
        
        features.setTimeSinceLastReading(ChronoUnit.MINUTES.between(lastReading, now));
        features.setTimeSinceFirstReading(ChronoUnit.MINUTES.between(firstReading, now));
        
        // Reading pattern features
        features.setReadingRegularity(calculateReadingRegularity(readings));
        features.setReadingDensity(calculateReadingDensity(readings));
        
        // Circadian features
        features.setIsNightTime(isNightTime(now));
        features.setIsWeekend(isWeekend(now));
        
        return features;
    }
    
    // Engineer statistical features
    private StatisticalFeatures engineerStatisticalFeatures(List<VitalReading> readings) {
        StatisticalFeatures features = new StatisticalFeatures();
        
        if (readings.isEmpty()) {
            return features;
        }
        
        // Overall statistics
        List<Double> allValues = readings.stream()
            .map(r -> r.getValue().doubleValue())
            .collect(Collectors.toList());
        
        features.setOverallMean(calculateMean(allValues));
        features.setOverallStdDev(calculateStandardDeviation(allValues));
        features.setOverallSkewness(calculateSkewness(allValues));
        features.setOverallKurtosis(calculateKurtosis(allValues));
        
        // Distribution features
        features.setEntropy(calculateEntropy(allValues));
        features.setCoefficientOfVariation(calculateCoefficientOfVariation(allValues));
        
        // Quality features
        features.setAverageQualityScore(calculateAverageQualityScore(readings));
        features.setQualityVariance(calculateQualityVariance(readings));
        
        return features;
    }
    
    // Statistical calculation methods
    private double calculateMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }
    
    private double calculateMedian(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        
        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }
    
    private double calculateStandardDeviation(List<Double> values) {
        double mean = calculateMean(values);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
        return Math.sqrt(variance);
    }
    
    private double calculateVariance(List<Double> values) {
        double mean = calculateMean(values);
        return values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
    }
    
    private double calculateRateOfChange(List<Double> values) {
        if (values.size() < 2) return 0.0;
        
        double current = values.get(values.size() - 1);
        double previous = values.get(values.size() - 2);
        
        return current - previous;
    }
    
    private double calculateRateOfChangePercentage(List<Double> values) {
        if (values.size() < 2) return 0.0;
        
        double current = values.get(values.size() - 1);
        double previous = values.get(values.size() - 2);
        
        if (previous == 0) return 0.0;
        return ((current - previous) / previous) * 100.0;
    }
    
    private double calculateAcceleration(List<Double> values) {
        if (values.size() < 3) return 0.0;
        
        double current = values.get(values.size() - 1);
        double previous = values.get(values.size() - 2);
        double beforePrevious = values.get(values.size() - 3);
        
        return (current - previous) - (previous - beforePrevious);
    }
    
    private double calculateTrendSlope(List<Double> values) {
        if (values.size() < 2) return 0.0;
        
        int n = values.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += i;
            sumY += values.get(i);
            sumXY += i * values.get(i);
            sumX2 += i * i;
        }
        
        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) return 0.0;
        
        return (n * sumXY - sumX * sumY) / denominator;
    }
    
    private String determineTrendDirection(List<Double> values) {
        if (values.size() < 2) return "STABLE";
        
        double slope = calculateTrendSlope(values);
        double threshold = 0.01; // Minimum slope threshold
        
        if (slope > threshold) return "INCREASING";
        else if (slope < -threshold) return "DECREASING";
        else return "STABLE";
    }
    
    private double calculateTrendStrength(List<Double> values) {
        if (values.size() < 2) return 0.0;
        
        double slope = calculateTrendSlope(values);
        double stdDev = calculateStandardDeviation(values);
        
        if (stdDev == 0) return 0.0;
        return Math.abs(slope / stdDev);
    }
    
    private double calculateWindowMean(List<Double> values, int windowSize) {
        if (values.size() < windowSize) {
            return calculateMean(values);
        }
        
        List<Double> window = values.subList(values.size() - windowSize, values.size());
        return calculateMean(window);
    }
    
    private double calculateWindowStdDev(List<Double> values, int windowSize) {
        if (values.size() < windowSize) {
            return calculateStandardDeviation(values);
        }
        
        List<Double> window = values.subList(values.size() - windowSize, values.size());
        return calculateStandardDeviation(window);
    }
    
    private double calculateVolatility(List<Double> values) {
        if (values.size() < 2) return 0.0;
        
        double sumSquaredReturns = 0.0;
        for (int i = 1; i < values.size(); i++) {
            double returnValue = (values.get(i) - values.get(i - 1)) / values.get(i - 1);
            sumSquaredReturns += returnValue * returnValue;
        }
        
        return Math.sqrt(sumSquaredReturns / (values.size() - 1));
    }
    
    private double calculateVolatilityWindow(List<Double> values, int windowSize) {
        if (values.size() < windowSize) {
            return calculateVolatility(values);
        }
        
        List<Double> window = values.subList(values.size() - windowSize, values.size());
        return calculateVolatility(window);
    }
    
    private double calculatePercentile(List<Double> values, int percentile) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        
        int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        
        return sorted.get(index);
    }
    
    private int calculateOutlierCount(List<Double> values) {
        if (values.size() < 4) return 0;
        
        double q1 = calculatePercentile(values, 25);
        double q3 = calculatePercentile(values, 75);
        double iqr = q3 - q1;
        double lowerBound = q1 - 1.5 * iqr;
        double upperBound = q3 + 1.5 * iqr;
        
        return (int) values.stream()
            .filter(v -> v < lowerBound || v > upperBound)
            .count();
    }
    
    private long calculateTimeSpan(List<VitalReading> readings) {
        if (readings.size() < 2) return 0;
        
        LocalDateTime first = readings.get(0).getTimestamp();
        LocalDateTime last = readings.get(readings.size() - 1).getTimestamp();
        
        return ChronoUnit.MINUTES.between(first, last);
    }
    
    private double calculateReadingFrequency(List<VitalReading> readings) {
        if (readings.size() < 2) return 0.0;
        
        long timeSpan = calculateTimeSpan(readings);
        if (timeSpan == 0) return 0.0;
        
        return (double) readings.size() / (timeSpan / 60.0); // readings per hour
    }
    
    private double calculateCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size() || x.size() < 2) return 0.0;
        
        double meanX = calculateMean(x);
        double meanY = calculateMean(y);
        
        double numerator = 0.0, denominatorX = 0.0, denominatorY = 0.0;
        
        for (int i = 0; i < x.size(); i++) {
            double diffX = x.get(i) - meanX;
            double diffY = y.get(i) - meanY;
            
            numerator += diffX * diffY;
            denominatorX += diffX * diffX;
            denominatorY += diffY * diffY;
        }
        
        double denominator = Math.sqrt(denominatorX * denominatorY);
        return denominator == 0 ? 0.0 : numerator / denominator;
    }
    
    private double calculateShockIndex(double heartRate, double bloodPressure) {
        if (bloodPressure == 0) return 0.0;
        return heartRate / bloodPressure;
    }
    
    private List<Double> getRecentValues(VitalFeatures features) {
        return Arrays.asList(
            features.getCurrentValue(),
            features.getShortTermMean(),
            features.getMediumTermMean()
        );
    }
    
    private double calculateVitalStabilityScore(Map<String, VitalFeatures> vitalFeatures) {
        double totalStability = 0.0;
        int count = 0;
        
        for (VitalFeatures features : vitalFeatures.values()) {
            double stability = 1.0 / (1.0 + features.getVolatility());
            totalStability += stability;
            count++;
        }
        
        return count > 0 ? totalStability / count : 0.0;
    }
    
    private double calculateOverallTrendScore(Map<String, VitalFeatures> vitalFeatures) {
        double totalTrend = 0.0;
        int count = 0;
        
        for (VitalFeatures features : vitalFeatures.values()) {
            totalTrend += features.getTrendStrength();
            count++;
        }
        
        return count > 0 ? totalTrend / count : 0.0;
    }
    
    private double calculateAbnormalityScore(Map<String, VitalFeatures> vitalFeatures) {
        double totalAbnormality = 0.0;
        int count = 0;
        
        for (VitalFeatures features : vitalFeatures.values()) {
            double abnormality = features.getOutlierPercentage();
            totalAbnormality += abnormality;
            count++;
        }
        
        return count > 0 ? totalAbnormality / count : 0.0;
    }
    
    private double calculateSkewness(List<Double> values) {
        if (values.size() < 3) return 0.0;
        
        double mean = calculateMean(values);
        double stdDev = calculateStandardDeviation(values);
        
        if (stdDev == 0) return 0.0;
        
        double skewness = values.stream()
            .mapToDouble(v -> Math.pow((v - mean) / stdDev, 3))
            .average().orElse(0.0);
        
        return skewness;
    }
    
    private double calculateKurtosis(List<Double> values) {
        if (values.size() < 4) return 0.0;
        
        double mean = calculateMean(values);
        double stdDev = calculateStandardDeviation(values);
        
        if (stdDev == 0) return 0.0;
        
        double kurtosis = values.stream()
            .mapToDouble(v -> Math.pow((v - mean) / stdDev, 4))
            .average().orElse(0.0);
        
        return kurtosis - 3.0; // Excess kurtosis
    }
    
    private double calculateEntropy(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        
        Map<Double, Long> frequencyMap = values.stream()
            .collect(Collectors.groupingBy(v -> Math.round(v * 100.0) / 100.0, Collectors.counting()));
        
        double entropy = 0.0;
        double total = values.size();
        
        for (long count : frequencyMap.values()) {
            double probability = count / total;
            entropy -= probability * Math.log(probability) / Math.log(2);
        }
        
        return entropy;
    }
    
    private double calculateCoefficientOfVariation(List<Double> values) {
        double mean = calculateMean(values);
        double stdDev = calculateStandardDeviation(values);
        
        return mean == 0 ? 0.0 : stdDev / mean;
    }
    
    private double calculateAverageQualityScore(List<VitalReading> readings) {
        return readings.stream()
            .mapToDouble(r -> r.getQualityScore().doubleValue())
            .average().orElse(0.0);
    }
    
    private double calculateQualityVariance(List<VitalReading> readings) {
        double mean = calculateAverageQualityScore(readings);
        
        return readings.stream()
            .mapToDouble(r -> Math.pow(r.getQualityScore().doubleValue() - mean, 2))
            .average().orElse(0.0);
    }
    
    private double calculateReadingRegularity(List<VitalReading> readings) {
        if (readings.size() < 3) return 0.0;
        
        List<Long> intervals = new ArrayList<>();
        for (int i = 1; i < readings.size(); i++) {
            long interval = ChronoUnit.MINUTES.between(
                readings.get(i - 1).getTimestamp(),
                readings.get(i).getTimestamp()
            );
            intervals.add((long) interval);
        }
        
        double meanInterval = intervals.stream().mapToDouble(Long::doubleValue).average().orElse(0.0);
        double variance = intervals.stream()
            .mapToDouble(i -> Math.pow(i - meanInterval, 2))
            .average().orElse(0.0);
        
        return meanInterval == 0 ? 0.0 : 1.0 / (1.0 + Math.sqrt(variance) / meanInterval);
    }
    
    private double calculateReadingDensity(List<VitalReading> readings) {
        if (readings.size() < 2) return 0.0;
        
        long timeSpan = calculateTimeSpan(readings);
        return timeSpan > 0 ? (double) readings.size() / (timeSpan / 60.0) : 0.0;
    }
    
    private boolean isNightTime(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        return hour < 6 || hour >= 22;
    }
    
    private boolean isWeekend(LocalDateTime dateTime) {
        int dayOfWeek = dateTime.getDayOfWeek().getValue();
        return dayOfWeek == 6 || dayOfWeek == 7; // Saturday or Sunday
    }
    
    // Inner classes
    public static class EngineeredFeatures {
        private String patientId;
        private LocalDateTime timestamp;
        private int originalReadingCount;
        private Map<String, VitalFeatures> vitalFeatures;
        private CrossVitalFeatures crossVitalFeatures;
        private TemporalFeatures temporalFeatures;
        private StatisticalFeatures statisticalFeatures;
        
        // Getters and setters
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public int getOriginalReadingCount() { return originalReadingCount; }
        public void setOriginalReadingCount(int originalReadingCount) { this.originalReadingCount = originalReadingCount; }
        
        public Map<String, VitalFeatures> getVitalFeatures() { return vitalFeatures; }
        public void setVitalFeatures(Map<String, VitalFeatures> vitalFeatures) { this.vitalFeatures = vitalFeatures; }
        
        public CrossVitalFeatures getCrossVitalFeatures() { return crossVitalFeatures; }
        public void setCrossVitalFeatures(CrossVitalFeatures crossVitalFeatures) { this.crossVitalFeatures = crossVitalFeatures; }
        
        public TemporalFeatures getTemporalFeatures() { return temporalFeatures; }
        public void setTemporalFeatures(TemporalFeatures temporalFeatures) { this.temporalFeatures = temporalFeatures; }
        
        public StatisticalFeatures getStatisticalFeatures() { return statisticalFeatures; }
        public void setStatisticalFeatures(StatisticalFeatures statisticalFeatures) { this.statisticalFeatures = statisticalFeatures; }
        
        public int getTotalFeatureCount() {
            int count = 0;
            if (vitalFeatures != null) count += vitalFeatures.size() * 20; // Approximate
            if (crossVitalFeatures != null) count += 10;
            if (temporalFeatures != null) count += 10;
            if (statisticalFeatures != null) count += 10;
            return count;
        }
    }
    
    public static class VitalFeatures {
        private String vitalType;
        private int readingCount;
        
        // Basic statistics
        private double currentValue;
        private double mean;
        private double median;
        private double standardDeviation;
        private double variance;
        private double min;
        private double max;
        private double range;
        
        // Rate of change
        private double rateOfChange;
        private double rateOfChangePercentage;
        private double acceleration;
        
        // Trend
        private double trendSlope;
        private String trendDirection;
        private double trendStrength;
        
        // Window features
        private double shortTermMean;
        private double mediumTermMean;
        private double longTermMean;
        private double shortTermStdDev;
        private double mediumTermStdDev;
        private double longTermStdDev;
        
        // Volatility
        private double volatility;
        private double volatilityShortTerm;
        private double volatilityMediumTerm;
        private double volatilityLongTerm;
        
        // Percentiles
        private double percentile25;
        private double percentile75;
        private double percentile90;
        private double percentile95;
        
        // Outliers
        private int outlierCount;
        private double outlierPercentage;
        
        // Time-based
        private LocalDateTime firstReadingTime;
        private LocalDateTime lastReadingTime;
        private long timeSpanMinutes;
        private double readingFrequency;
        
        // Getters and setters
        public String getVitalType() { return vitalType; }
        public void setVitalType(String vitalType) { this.vitalType = vitalType; }
        
        public int getReadingCount() { return readingCount; }
        public void setReadingCount(int readingCount) { this.readingCount = readingCount; }
        
        public double getCurrentValue() { return currentValue; }
        public void setCurrentValue(double currentValue) { this.currentValue = currentValue; }
        
        public double getMean() { return mean; }
        public void setMean(double mean) { this.mean = mean; }
        
        public double getMedian() { return median; }
        public void setMedian(double median) { this.median = median; }
        
        public double getStandardDeviation() { return standardDeviation; }
        public void setStandardDeviation(double standardDeviation) { this.standardDeviation = standardDeviation; }
        
        public double getVariance() { return variance; }
        public void setVariance(double variance) { this.variance = variance; }
        
        public double getMin() { return min; }
        public void setMin(double min) { this.min = min; }
        
        public double getMax() { return max; }
        public void setMax(double max) { this.max = max; }
        
        public double getRange() { return range; }
        public void setRange(double range) { this.range = range; }
        
        public double getRateOfChange() { return rateOfChange; }
        public void setRateOfChange(double rateOfChange) { this.rateOfChange = rateOfChange; }
        
        public double getRateOfChangePercentage() { return rateOfChangePercentage; }
        public void setRateOfChangePercentage(double rateOfChangePercentage) { this.rateOfChangePercentage = rateOfChangePercentage; }
        
        public double getAcceleration() { return acceleration; }
        public void setAcceleration(double acceleration) { this.acceleration = acceleration; }
        
        public double getTrendSlope() { return trendSlope; }
        public void setTrendSlope(double trendSlope) { this.trendSlope = trendSlope; }
        
        public String getTrendDirection() { return trendDirection; }
        public void setTrendDirection(String trendDirection) { this.trendDirection = trendDirection; }
        
        public double getTrendStrength() { return trendStrength; }
        public void setTrendStrength(double trendStrength) { this.trendStrength = trendStrength; }
        
        public double getShortTermMean() { return shortTermMean; }
        public void setShortTermMean(double shortTermMean) { this.shortTermMean = shortTermMean; }
        
        public double getMediumTermMean() { return mediumTermMean; }
        public void setMediumTermMean(double mediumTermMean) { this.mediumTermMean = mediumTermMean; }
        
        public double getLongTermMean() { return longTermMean; }
        public void setLongTermMean(double longTermMean) { this.longTermMean = longTermMean; }
        
        public double getShortTermStdDev() { return shortTermStdDev; }
        public void setShortTermStdDev(double shortTermStdDev) { this.shortTermStdDev = shortTermStdDev; }
        
        public double getMediumTermStdDev() { return mediumTermStdDev; }
        public void setMediumTermStdDev(double mediumTermStdDev) { this.mediumTermStdDev = mediumTermStdDev; }
        
        public double getLongTermStdDev() { return longTermStdDev; }
        public void setLongTermStdDev(double longTermStdDev) { this.longTermStdDev = longTermStdDev; }
        
        public double getVolatility() { return volatility; }
        public void setVolatility(double volatility) { this.volatility = volatility; }
        
        public double getVolatilityShortTerm() { return volatilityShortTerm; }
        public void setVolatilityShortTerm(double volatilityShortTerm) { this.volatilityShortTerm = volatilityShortTerm; }
        
        public double getVolatilityMediumTerm() { return volatilityMediumTerm; }
        public void setVolatilityMediumTerm(double volatilityMediumTerm) { this.volatilityMediumTerm = volatilityMediumTerm; }
        
        public double getVolatilityLongTerm() { return volatilityLongTerm; }
        public void setVolatilityLongTerm(double volatilityLongTerm) { this.volatilityLongTerm = volatilityLongTerm; }
        
        public double getPercentile25() { return percentile25; }
        public void setPercentile25(double percentile25) { this.percentile25 = percentile25; }
        
        public double getPercentile75() { return percentile75; }
        public void setPercentile75(double percentile75) { this.percentile75 = percentile75; }
        
        public double getPercentile90() { return percentile90; }
        public void setPercentile90(double percentile90) { this.percentile90 = percentile90; }
        
        public double getPercentile95() { return percentile95; }
        public void setPercentile95(double percentile95) { this.percentile95 = percentile95; }
        
        public int getOutlierCount() { return outlierCount; }
        public void setOutlierCount(int outlierCount) { this.outlierCount = outlierCount; }
        
        public double getOutlierPercentage() { return outlierPercentage; }
        public void setOutlierPercentage(double outlierPercentage) { this.outlierPercentage = outlierPercentage; }
        
        public LocalDateTime getFirstReadingTime() { return firstReadingTime; }
        public void setFirstReadingTime(LocalDateTime firstReadingTime) { this.firstReadingTime = firstReadingTime; }
        
        public LocalDateTime getLastReadingTime() { return lastReadingTime; }
        public void setLastReadingTime(LocalDateTime lastReadingTime) { this.lastReadingTime = lastReadingTime; }
        
        public long getTimeSpanMinutes() { return timeSpanMinutes; }
        public void setTimeSpanMinutes(long timeSpanMinutes) { this.timeSpanMinutes = timeSpanMinutes; }
        
        public double getReadingFrequency() { return readingFrequency; }
        public void setReadingFrequency(double readingFrequency) { this.readingFrequency = readingFrequency; }
    }
    
    public static class CrossVitalFeatures {
        private double hrBpCorrelation;
        private double tempHrCorrelation;
        private double spo2RrCorrelation;
        private double shockIndex;
        private double vitalStabilityScore;
        private double overallTrendScore;
        private double abnormalityScore;
        
        // Getters and setters
        public double getHrBpCorrelation() { return hrBpCorrelation; }
        public void setHrBpCorrelation(double hrBpCorrelation) { this.hrBpCorrelation = hrBpCorrelation; }
        
        public double getTempHrCorrelation() { return tempHrCorrelation; }
        public void setTempHrCorrelation(double tempHrCorrelation) { this.tempHrCorrelation = tempHrCorrelation; }
        
        public double getSpo2RrCorrelation() { return spo2RrCorrelation; }
        public void setSpo2RrCorrelation(double spo2RrCorrelation) { this.spo2RrCorrelation = spo2RrCorrelation; }
        
        public double getShockIndex() { return shockIndex; }
        public void setShockIndex(double shockIndex) { this.shockIndex = shockIndex; }
        
        public double getVitalStabilityScore() { return vitalStabilityScore; }
        public void setVitalStabilityScore(double vitalStabilityScore) { this.vitalStabilityScore = vitalStabilityScore; }
        
        public double getOverallTrendScore() { return overallTrendScore; }
        public void setOverallTrendScore(double overallTrendScore) { this.overallTrendScore = overallTrendScore; }
        
        public double getAbnormalityScore() { return abnormalityScore; }
        public void setAbnormalityScore(double abnormalityScore) { this.abnormalityScore = abnormalityScore; }
    }
    
    public static class TemporalFeatures {
        private int hourOfDay;
        private int dayOfWeek;
        private int dayOfMonth;
        private int monthOfYear;
        private long timeSinceLastReading;
        private long timeSinceFirstReading;
        private double readingRegularity;
        private double readingDensity;
        private boolean isNightTime;
        private boolean isWeekend;
        
        // Getters and setters
        public int getHourOfDay() { return hourOfDay; }
        public void setHourOfDay(int hourOfDay) { this.hourOfDay = hourOfDay; }
        
        public int getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
        
        public int getDayOfMonth() { return dayOfMonth; }
        public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }
        
        public int getMonthOfYear() { return monthOfYear; }
        public void setMonthOfYear(int monthOfYear) { this.monthOfYear = monthOfYear; }
        
        public long getTimeSinceLastReading() { return timeSinceLastReading; }
        public void setTimeSinceLastReading(long timeSinceLastReading) { this.timeSinceLastReading = timeSinceLastReading; }
        
        public long getTimeSinceFirstReading() { return timeSinceFirstReading; }
        public void setTimeSinceFirstReading(long timeSinceFirstReading) { this.timeSinceFirstReading = timeSinceFirstReading; }
        
        public double getReadingRegularity() { return readingRegularity; }
        public void setReadingRegularity(double readingRegularity) { this.readingRegularity = readingRegularity; }
        
        public double getReadingDensity() { return readingDensity; }
        public void setReadingDensity(double readingDensity) { this.readingDensity = readingDensity; }
        
        public boolean isNightTime() { return isNightTime; }
        public void setNightTime(boolean nightTime) { isNightTime = nightTime; }
        
        public boolean isWeekend() { return isWeekend; }
        public void setWeekend(boolean weekend) { isWeekend = weekend; }
    }
    
    public static class StatisticalFeatures {
        private double overallMean;
        private double overallStdDev;
        private double overallSkewness;
        private double overallKurtosis;
        private double entropy;
        private double coefficientOfVariation;
        private double averageQualityScore;
        private double qualityVariance;
        
        // Getters and setters
        public double getOverallMean() { return overallMean; }
        public void setOverallMean(double overallMean) { this.overallMean = overallMean; }
        
        public double getOverallStdDev() { return overallStdDev; }
        public void setOverallStdDev(double overallStdDev) { this.overallStdDev = overallStdDev; }
        
        public double getOverallSkewness() { return overallSkewness; }
        public void setOverallSkewness(double overallSkewness) { this.overallSkewness = overallSkewness; }
        
        public double getOverallKurtosis() { return overallKurtosis; }
        public void setOverallKurtosis(double overallKurtosis) { this.overallKurtosis = overallKurtosis; }
        
        public double getEntropy() { return entropy; }
        public void setEntropy(double entropy) { this.entropy = entropy; }
        
        public double getCoefficientOfVariation() { return coefficientOfVariation; }
        public void setCoefficientOfVariation(double coefficientOfVariation) { this.coefficientOfVariation = coefficientOfVariation; }
        
        public double getAverageQualityScore() { return averageQualityScore; }
        public void setAverageQualityScore(double averageQualityScore) { this.averageQualityScore = averageQualityScore; }
        
        public double getQualityVariance() { return qualityVariance; }
        public void setQualityVariance(double qualityVariance) { this.qualityVariance = qualityVariance; }
    }
    
    public static class VitalReading {
        private String vitalType;
        private BigDecimal value;
        private LocalDateTime timestamp;
        private BigDecimal qualityScore;
        
        // Getters and setters
        public String getVitalType() { return vitalType; }
        public void setVitalType(String vitalType) { this.vitalType = vitalType; }
        
        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
        
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        public BigDecimal getQualityScore() { return qualityScore; }
        public void setQualityScore(BigDecimal qualityScore) { this.qualityScore = qualityScore; }
    }
}
