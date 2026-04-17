package com.meditrack.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "meditrack.ai-prediction")
public class AiPredictionConfig {
    
    // Weka configuration
    private WekaConfig weka = new WekaConfig();
    
    // NEWS scoring configuration
    private NewsConfig news = new NewsConfig();
    
    // Feature engineering configuration
    private FeatureEngineeringConfig featureEngineering = new FeatureEngineeringConfig();
    
    // Model performance tracking configuration
    private PerformanceTrackingConfig performanceTracking = new PerformanceTrackingConfig();
    
    // General AI configuration
    private GeneralConfig general = new GeneralConfig();
    
    // Getters and setters
    public WekaConfig getWeka() { return weka; }
    public void setWeka(WekaConfig weka) { this.weka = weka; }
    
    public NewsConfig getNews() { return news; }
    public void setNews(NewsConfig news) { this.news = news; }
    
    public FeatureEngineeringConfig getFeatureEngineering() { return featureEngineering; }
    public void setFeatureEngineering(FeatureEngineeringConfig featureEngineering) { this.featureEngineering = featureEngineering; }
    
    public PerformanceTrackingConfig getPerformanceTracking() { return performanceTracking; }
    public void setPerformanceTracking(PerformanceTrackingConfig performanceTracking) { this.performanceTracking = performanceTracking; }
    
    public GeneralConfig getGeneral() { return general; }
    public void setGeneral(GeneralConfig general) { this.general = general; }
    
    // Nested configuration classes
    public static class WekaConfig {
        private String modelStoragePath = "./models";
        private int maxModelsInMemory = 10;
        private boolean enableModelPersistence = true;
        private boolean enableModelVersioning = true;
        private int maxModelVersions = 5;
        private boolean enableAutoRetraining = false;
        private int retrainingThreshold = 1000; // predictions before retraining
        private Map<String, Object> defaultJ48Params = Map.of(
            "confidenceFactor", 0.25f,
            "minNumObj", 2,
            "binarySplits", false,
            "unpruned", false,
            "reducedErrorPruning", true,
            "numFolds", 3
        );
        
        // Getters and setters
        public String getModelStoragePath() { return modelStoragePath; }
        public void setModelStoragePath(String modelStoragePath) { this.modelStoragePath = modelStoragePath; }
        
        public int getMaxModelsInMemory() { return maxModelsInMemory; }
        public void setMaxModelsInMemory(int maxModelsInMemory) { this.maxModelsInMemory = maxModelsInMemory; }
        
        public boolean isEnableModelPersistence() { return enableModelPersistence; }
        public void setEnableModelPersistence(boolean enableModelPersistence) { this.enableModelPersistence = enableModelPersistence; }
        
        public boolean isEnableModelVersioning() { return enableModelVersioning; }
        public void setEnableModelVersioning(boolean enableModelVersioning) { this.enableModelVersioning = enableModelVersioning; }
        
        public int getMaxModelVersions() { return maxModelVersions; }
        public void setMaxModelVersions(int maxModelVersions) { this.maxModelVersions = maxModelVersions; }
        
        public boolean isEnableAutoRetraining() { return enableAutoRetraining; }
        public void setEnableAutoRetraining(boolean enableAutoRetraining) { this.enableAutoRetraining = enableAutoRetraining; }
        
        public int getRetrainingThreshold() { return retrainingThreshold; }
        public void setRetrainingThreshold(int retrainingThreshold) { this.retrainingThreshold = retrainingThreshold; }
        
        public Map<String, Object> getDefaultJ48Params() { return defaultJ48Params; }
        public void setDefaultJ48Params(Map<String, Object> defaultJ48Params) { this.defaultJ48Params = defaultJ48Params; }
    }
    
    public static class NewsConfig {
        private boolean enableTrendAnalysis = true;
        private boolean enableRiskLevelCalculation = true;
        private Map<String, Map<String, Integer>> customScoring = Map.of();
        private boolean enableHistoricalComparison = true;
        private int historicalComparisonHours = 24;
        private boolean enableAlerts = true;
        private double alertThreshold = 7.0; // NEWS score threshold for alerts
        
        // Getters and setters
        public boolean isEnableTrendAnalysis() { return enableTrendAnalysis; }
        public void setEnableTrendAnalysis(boolean enableTrendAnalysis) { this.enableTrendAnalysis = enableTrendAnalysis; }
        
        public boolean isEnableRiskLevelCalculation() { return enableRiskLevelCalculation; }
        public void setEnableRiskLevelCalculation(boolean enableRiskLevelCalculation) { this.enableRiskLevelCalculation = enableRiskLevelCalculation; }
        
        public Map<String, Map<String, Integer>> getCustomScoring() { return customScoring; }
        public void setCustomScoring(Map<String, Map<String, Integer>> customScoring) { this.customScoring = customScoring; }
        
        public boolean isEnableHistoricalComparison() { return enableHistoricalComparison; }
        public void setEnableHistoricalComparison(boolean enableHistoricalComparison) { this.enableHistoricalComparison = enableHistoricalComparison; }
        
        public int getHistoricalComparisonHours() { return historicalComparisonHours; }
        public void setHistoricalComparisonHours(int historicalComparisonHours) { this.historicalComparisonHours = historicalComparisonHours; }
        
        public boolean isEnableAlerts() { return enableAlerts; }
        public void setEnableAlerts(boolean enableAlerts) { this.enableAlerts = enableAlerts; }
        
        public double getAlertThreshold() { return alertThreshold; }
        public void setAlertThreshold(double alertThreshold) { this.alertThreshold = alertThreshold; }
    }
    
    public static class FeatureEngineeringConfig {
        private int shortTermWindow = 6;
        private int mediumTermWindow = 12;
        private int longTermWindow = 24;
        private boolean enableCrossVitalFeatures = true;
        private boolean enableTemporalFeatures = true;
        private boolean enableStatisticalFeatures = true;
        private boolean enableOutlierDetection = true;
        private double outlierThreshold = 1.5; // IQR multiplier
        private boolean enableTrendAnalysis = true;
        private boolean enableVolatilityCalculation = true;
        private int maxFeaturesPerVital = 50;
        private boolean enableFeatureSelection = false;
        private double featureSelectionThreshold = 0.05;
        
        // Getters and setters
        public int getShortTermWindow() { return shortTermWindow; }
        public void setShortTermWindow(int shortTermWindow) { this.shortTermWindow = shortTermWindow; }
        
        public int getMediumTermWindow() { return mediumTermWindow; }
        public void setMediumTermWindow(int mediumTermWindow) { this.mediumTermWindow = mediumTermWindow; }
        
        public int getLongTermWindow() { return longTermWindow; }
        public void setLongTermWindow(int longTermWindow) { this.longTermWindow = longTermWindow; }
        
        public boolean isEnableCrossVitalFeatures() { return enableCrossVitalFeatures; }
        public void setEnableCrossVitalFeatures(boolean enableCrossVitalFeatures) { this.enableCrossVitalFeatures = enableCrossVitalFeatures; }
        
        public boolean isEnableTemporalFeatures() { return enableTemporalFeatures; }
        public void setEnableTemporalFeatures(boolean enableTemporalFeatures) { this.enableTemporalFeatures = enableTemporalFeatures; }
        
        public boolean isEnableStatisticalFeatures() { return enableStatisticalFeatures; }
        public void setEnableStatisticalFeatures(boolean enableStatisticalFeatures) { this.enableStatisticalFeatures = enableStatisticalFeatures; }
        
        public boolean isEnableOutlierDetection() { return enableOutlierDetection; }
        public void setEnableOutlierDetection(boolean enableOutlierDetection) { this.enableOutlierDetection = enableOutlierDetection; }
        
        public double getOutlierThreshold() { return outlierThreshold; }
        public void setOutlierThreshold(double outlierThreshold) { this.outlierThreshold = outlierThreshold; }
        
        public boolean isEnableTrendAnalysis() { return enableTrendAnalysis; }
        public void setEnableTrendAnalysis(boolean enableTrendAnalysis) { this.enableTrendAnalysis = enableTrendAnalysis; }
        
        public boolean isEnableVolatilityCalculation() { return enableVolatilityCalculation; }
        public void setEnableVolatilityCalculation(boolean enableVolatilityCalculation) { this.enableVolatilityCalculation = enableVolatilityCalculation; }
        
        public int getMaxFeaturesPerVital() { return maxFeaturesPerVital; }
        public void setMaxFeaturesPerVital(int maxFeaturesPerVital) { this.maxFeaturesPerVital = maxFeaturesPerVital; }
        
        public boolean isEnableFeatureSelection() { return enableFeatureSelection; }
        public void setEnableFeatureSelection(boolean enableFeatureSelection) { this.enableFeatureSelection = enableFeatureSelection; }
        
        public double getFeatureSelectionThreshold() { return featureSelectionThreshold; }
        public void setFeatureSelectionThreshold(double featureSelectionThreshold) { this.featureSelectionThreshold = featureSelectionThreshold; }
    }
    
    public static class PerformanceTrackingConfig {
        private boolean enableTracking = true;
        private int maxPredictionHistory = 10000;
        private int maxDriftHistory = 1000;
        private double driftDetectionThreshold = 0.1; // 10% accuracy drop
        private int driftDetectionWindowSize = 100;
        private boolean enableRealTimeMonitoring = true;
        private int monitoringIntervalSeconds = 60;
        private boolean enablePerformanceAlerts = true;
        private double performanceAlertThreshold = 0.8; // 80% accuracy threshold
        private boolean enableModelComparison = true;
        private String modelComparisonMetric = "accuracy";
        
        // Getters and setters
        public boolean isEnableTracking() { return enableTracking; }
        public void setEnableTracking(boolean enableTracking) { this.enableTracking = enableTracking; }
        
        public int getMaxPredictionHistory() { return maxPredictionHistory; }
        public void setMaxPredictionHistory(int maxPredictionHistory) { this.maxPredictionHistory = maxPredictionHistory; }
        
        public int getMaxDriftHistory() { return maxDriftHistory; }
        public void setMaxDriftHistory(int maxDriftHistory) { this.maxDriftHistory = maxDriftHistory; }
        
        public double getDriftDetectionThreshold() { return driftDetectionThreshold; }
        public void setDriftDetectionThreshold(double driftDetectionThreshold) { this.driftDetectionThreshold = driftDetectionThreshold; }
        
        public int getDriftDetectionWindowSize() { return driftDetectionWindowSize; }
        public void setDriftDetectionWindowSize(int driftDetectionWindowSize) { this.driftDetectionWindowSize = driftDetectionWindowSize; }
        
        public boolean isEnableRealTimeMonitoring() { return enableRealTimeMonitoring; }
        public void setEnableRealTimeMonitoring(boolean enableRealTimeMonitoring) { this.enableRealTimeMonitoring = enableRealTimeMonitoring; }
        
        public int getMonitoringIntervalSeconds() { return monitoringIntervalSeconds; }
        public void setMonitoringIntervalSeconds(int monitoringIntervalSeconds) { this.monitoringIntervalSeconds = monitoringIntervalSeconds; }
        
        public boolean isEnablePerformanceAlerts() { return enablePerformanceAlerts; }
        public void setEnablePerformanceAlerts(boolean enablePerformanceAlerts) { this.enablePerformanceAlerts = enablePerformanceAlerts; }
        
        public double getPerformanceAlertThreshold() { return performanceAlertThreshold; }
        public void setPerformanceAlertThreshold(double performanceAlertThreshold) { this.performanceAlertThreshold = performanceAlertThreshold; }
        
        public boolean isEnableModelComparison() { return enableModelComparison; }
        public void setEnableModelComparison(boolean enableModelComparison) { this.enableModelComparison = enableModelComparison; }
        
        public String getModelComparisonMetric() { return modelComparisonMetric; }
        public void setModelComparisonMetric(String modelComparisonMetric) { this.modelComparisonMetric = modelComparisonMetric; }
    }
    
    public static class GeneralConfig {
        private boolean enableAiServices = true;
        private boolean enableCaching = true;
        private int cacheExpirationMinutes = 30;
        private boolean enableMetrics = true;
        private int metricsReportingIntervalSeconds = 300;
        private boolean enableLogging = true;
        private String logLevel = "INFO";
        private boolean enableAsyncProcessing = true;
        private int threadPoolSize = 10;
        private int queueCapacity = 1000;
        private boolean enableRateLimiting = true;
        private int maxRequestsPerMinute = 100;
        private boolean enableCircuitBreaker = true;
        private double circuitBreakerFailureThreshold = 0.5;
        private int circuitBreakerTimeoutSeconds = 30;
        
        // Getters and setters
        public boolean isEnableAiServices() { return enableAiServices; }
        public void setEnableAiServices(boolean enableAiServices) { this.enableAiServices = enableAiServices; }
        
        public boolean isEnableCaching() { return enableCaching; }
        public void setEnableCaching(boolean enableCaching) { this.enableCaching = enableCaching; }
        
        public int getCacheExpirationMinutes() { return cacheExpirationMinutes; }
        public void setCacheExpirationMinutes(int cacheExpirationMinutes) { this.cacheExpirationMinutes = cacheExpirationMinutes; }
        
        public boolean isEnableMetrics() { return enableMetrics; }
        public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
        
        public int getMetricsReportingIntervalSeconds() { return metricsReportingIntervalSeconds; }
        public void setMetricsReportingIntervalSeconds(int metricsReportingIntervalSeconds) { this.metricsReportingIntervalSeconds = metricsReportingIntervalSeconds; }
        
        public boolean isEnableLogging() { return enableLogging; }
        public void setEnableLogging(boolean enableLogging) { this.enableLogging = enableLogging; }
        
        public String getLogLevel() { return logLevel; }
        public void setLogLevel(String logLevel) { this.logLevel = logLevel; }
        
        public boolean isEnableAsyncProcessing() { return enableAsyncProcessing; }
        public void setEnableAsyncProcessing(boolean enableAsyncProcessing) { this.enableAsyncProcessing = enableAsyncProcessing; }
        
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        
        public boolean isEnableRateLimiting() { return enableRateLimiting; }
        public void setEnableRateLimiting(boolean enableRateLimiting) { this.enableRateLimiting = enableRateLimiting; }
        
        public int getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
        public void setMaxRequestsPerMinute(int maxRequestsPerMinute) { this.maxRequestsPerMinute = maxRequestsPerMinute; }
        
        public boolean isEnableCircuitBreaker() { return enableCircuitBreaker; }
        public void setEnableCircuitBreaker(boolean enableCircuitBreaker) { this.enableCircuitBreaker = enableCircuitBreaker; }
        
        public double getCircuitBreakerFailureThreshold() { return circuitBreakerFailureThreshold; }
        public void setCircuitBreakerFailureThreshold(double circuitBreakerFailureThreshold) { this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold; }
        
        public int getCircuitBreakerTimeoutSeconds() { return circuitBreakerTimeoutSeconds; }
        public void setCircuitBreakerTimeoutSeconds(int circuitBreakerTimeoutSeconds) { this.circuitBreakerTimeoutSeconds = circuitBreakerTimeoutSeconds; }
    }
}
