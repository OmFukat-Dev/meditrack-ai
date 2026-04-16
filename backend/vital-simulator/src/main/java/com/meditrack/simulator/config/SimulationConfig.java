package com.meditrack.simulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "meditrack.simulator")
public class SimulationConfig {
    
    // General simulation settings
    private boolean enabled = true;
    private int patientCount = 10;
    private int simulationIntervalSeconds = 30;
    private int maxConcurrentSimulations = 5;
    private boolean autoStart = true;
    
    // Vital generation settings
    private VitalGeneration vitalGeneration = new VitalGeneration();
    
    // Patient profiles
    private List<PatientProfile> patientProfiles = List.of();
    
    // Simulation scenarios
    private List<SimulationScenario> scenarios = List.of();
    
    // Kafka settings
    private KafkaProducer kafkaProducer = new KafkaProducer();
    
    // Anomaly settings
    private AnomalyGeneration anomalyGeneration = new AnomalyGeneration();
    
    // Performance settings
    private Performance performance = new Performance();
    
    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public int getPatientCount() { return patientCount; }
    public void setPatientCount(int patientCount) { this.patientCount = patientCount; }
    
    public int getSimulationIntervalSeconds() { return simulationIntervalSeconds; }
    public void setSimulationIntervalSeconds(int simulationIntervalSeconds) { this.simulationIntervalSeconds = simulationIntervalSeconds; }
    
    public int getMaxConcurrentSimulations() { return maxConcurrentSimulations; }
    public void setMaxConcurrentSimulations(int maxConcurrentSimulations) { this.maxConcurrentSimulations = maxConcurrentSimulations; }
    
    public boolean isAutoStart() { return autoStart; }
    public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }
    
    public VitalGeneration getVitalGeneration() { return vitalGeneration; }
    public void setVitalGeneration(VitalGeneration vitalGeneration) { this.vitalGeneration = vitalGeneration; }
    
    public List<PatientProfile> getPatientProfiles() { return patientProfiles; }
    public void setPatientProfiles(List<PatientProfile> patientProfiles) { this.patientProfiles = patientProfiles; }
    
    public List<SimulationScenario> getScenarios() { return scenarios; }
    public void setScenarios(List<SimulationScenario> scenarios) { this.scenarios = scenarios; }
    
    public KafkaProducer getKafkaProducer() { return kafkaProducer; }
    public void setKafkaProducer(KafkaProducer kafkaProducer) { this.kafkaProducer = kafkaProducer; }
    
    public AnomalyGeneration getAnomalyGeneration() { return anomalyGeneration; }
    public void setAnomalyGeneration(AnomalyGeneration anomalyGeneration) { this.anomalyGeneration = anomalyGeneration; }
    
    public Performance getPerformance() { return performance; }
    public void setPerformance(Performance performance) { this.performance = performance; }
    
    // Nested classes
    public static class VitalGeneration {
        private List<String> enabledVitals = List.of("HEART_RATE", "BLOOD_PRESSURE", "TEMPERATURE", "SPO2", "RESPIRATORY_RATE");
        private Map<String, VitalConfig> vitalConfigs = Map.of();
        private boolean generateRealisticPatterns = true;
        private boolean includeNoise = true;
        private double noiseLevel = 0.1;
        private boolean includeCircadianRhythm = true;
        private boolean includeActivityVariations = true;
        
        // Getters and setters
        public List<String> getEnabledVitals() { return enabledVitals; }
        public void setEnabledVitals(List<String> enabledVitals) { this.enabledVitals = enabledVitals; }
        
        public Map<String, VitalConfig> getVitalConfigs() { return vitalConfigs; }
        public void setVitalConfigs(Map<String, VitalConfig> vitalConfigs) { this.vitalConfigs = vitalConfigs; }
        
        public boolean isGenerateRealisticPatterns() { return generateRealisticPatterns; }
        public void setGenerateRealisticPatterns(boolean generateRealisticPatterns) { this.generateRealisticPatterns = generateRealisticPatterns; }
        
        public boolean isIncludeNoise() { return includeNoise; }
        public void setIncludeNoise(boolean includeNoise) { this.includeNoise = includeNoise; }
        
        public double getNoiseLevel() { return noiseLevel; }
        public void setNoiseLevel(double noiseLevel) { this.noiseLevel = noiseLevel; }
        
        public boolean isIncludeCircadianRhythm() { return includeCircadianRhythm; }
        public void setIncludeCircadianRhythm(boolean includeCircadianRhythm) { this.includeCircadianRhythm = includeCircadianRhythm; }
        
        public boolean isIncludeActivityVariations() { return includeActivityVariations; }
        public void setIncludeActivityVariations(boolean includeActivityVariations) { this.includeActivityVariations = includeActivityVariations; }
    }
    
    public static class VitalConfig {
        private String vitalType;
        private double minValue;
        private double maxValue;
        private double baseValue;
        private double standardDeviation;
        private int updateIntervalSeconds;
        private boolean enabled;
        private Map<String, Object> customParameters = Map.of();
        
        // Getters and setters
        public String getVitalType() { return vitalType; }
        public void setVitalType(String vitalType) { this.vitalType = vitalType; }
        
        public double getMinValue() { return minValue; }
        public void setMinValue(double minValue) { this.minValue = minValue; }
        
        public double getMaxValue() { return maxValue; }
        public void setMaxValue(double maxValue) { this.maxValue = maxValue; }
        
        public double getBaseValue() { return baseValue; }
        public void setBaseValue(double baseValue) { this.baseValue = baseValue; }
        
        public double getStandardDeviation() { return standardDeviation; }
        public void setStandardDeviation(double standardDeviation) { this.standardDeviation = standardDeviation; }
        
        public int getUpdateIntervalSeconds() { return updateIntervalSeconds; }
        public void setUpdateIntervalSeconds(int updateIntervalSeconds) { this.updateIntervalSeconds = updateIntervalSeconds; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public Map<String, Object> getCustomParameters() { return customParameters; }
        public void setCustomParameters(Map<String, Object> customParameters) { this.customParameters = customParameters; }
    }
    
    public static class PatientProfile {
        private String patientId;
        private int age;
        private String gender;
        private Map<String, Double> baseVitals = Map.of();
        private String activityLevel = "RESTING";
        private List<String> conditions = List.of();
        private Map<String, Object> customParameters = Map.of();
        
        // Getters and setters
        public String getPatientId() { return patientId; }
        public void setPatientId(String patientId) { this.patientId = patientId; }
        
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        
        public Map<String, Double> getBaseVitals() { return baseVitals; }
        public void setBaseVitals(Map<String, Double> baseVitals) { this.baseVitals = baseVitals; }
        
        public String getActivityLevel() { return activityLevel; }
        public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }
        
        public List<String> getConditions() { return conditions; }
        public void setConditions(List<String> conditions) { this.conditions = conditions; }
        
        public Map<String, Object> getCustomParameters() { return customParameters; }
        public void setCustomParameters(Map<String, Object> customParameters) { this.customParameters = customParameters; }
    }
    
    public static class SimulationScenario {
        private String name;
        private String description;
        private boolean enabled;
        private List<String> patientIds = List.of();
        private Map<String, Object> parameters = Map.of();
        private Map<String, Object> triggers = Map.of();
        private int durationMinutes;
        private boolean repeatable;
        private int repeatIntervalMinutes;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public List<String> getPatientIds() { return patientIds; }
        public void setPatientIds(List<String> patientIds) { this.patientIds = patientIds; }
        
        public Map<String, Object> getParameters() { return parameters; }
        public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
        
        public Map<String, Object> getTriggers() { return triggers; }
        public void setTriggers(Map<String, Object> triggers) { this.triggers = triggers; }
        
        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
        
        public boolean isRepeatable() { return repeatable; }
        public void setRepeatable(boolean repeatable) { this.repeatable = repeatable; }
        
        public int getRepeatIntervalMinutes() { return repeatIntervalMinutes; }
        public void setRepeatIntervalMinutes(int repeatIntervalMinutes) { this.repeatIntervalMinutes = repeatIntervalMinutes; }
    }
    
    public static class KafkaProducer {
        private String topic = "vital-readings";
        private int batchSize = 100;
        private int lingerMs = 10;
        private int compressionType = 1; // gzip
        private int retries = 3;
        private boolean enableIdempotence = true;
        private Map<String, Object> producerProperties = Map.of();
        
        // Getters and setters
        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }
        
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        
        public int getLingerMs() { return lingerMs; }
        public void setLingerMs(int lingerMs) { this.lingerMs = lingerMs; }
        
        public int getCompressionType() { return compressionType; }
        public void setCompressionType(int compressionType) { this.compressionType = compressionType; }
        
        public int getRetries() { return retries; }
        public void setRetries(int retries) { this.retries = retries; }
        
        public boolean isEnableIdempotence() { return enableIdempotence; }
        public void setEnableIdempotence(boolean enableIdempotence) { this.enableIdempotence = enableIdempotence; }
        
        public Map<String, Object> getProducerProperties() { return producerProperties; }
        public void setProducerProperties(Map<String, Object> producerProperties) { this.producerProperties = producerProperties; }
    }
    
    public static class AnomalyGeneration {
        private boolean enabled = false;
        private double probability = 0.05; // 5% chance
        private List<String> anomalyTypes = List.of("HIGH", "LOW", "CRITICAL_HIGH", "CRITICAL_LOW");
        private Map<String, Double> anomalyProbabilities = Map.of();
        private boolean scheduleAnomalies = false;
        private List<String> scheduledAnomalyTimes = List.of();
        private int maxAnomaliesPerHour = 10;
        private boolean generateTrends = false;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public double getProbability() { return probability; }
        public void setProbability(double probability) { this.probability = probability; }
        
        public List<String> getAnomalyTypes() { return anomalyTypes; }
        public void setAnomalyTypes(List<String> anomalyTypes) { this.anomalyTypes = anomalyTypes; }
        
        public Map<String, Double> getAnomalyProbabilities() { return anomalyProbabilities; }
        public void setAnomalyProbabilities(Map<String, Double> anomalyProbabilities) { this.anomalyProbabilities = anomalyProbabilities; }
        
        public boolean isScheduleAnomalies() { return scheduleAnomalies; }
        public void setScheduleAnomalies(boolean scheduleAnomalies) { this.scheduleAnomalies = scheduleAnomalies; }
        
        public List<String> getScheduledAnomalyTimes() { return scheduledAnomalyTimes; }
        public void setScheduledAnomalyTimes(List<String> scheduledAnomalyTimes) { this.scheduledAnomalyTimes = scheduledAnomalyTimes; }
        
        public int getMaxAnomaliesPerHour() { return maxAnomaliesPerHour; }
        public void setMaxAnomaliesPerHour(int maxAnomaliesPerHour) { this.maxAnomaliesPerHour = maxAnomaliesPerHour; }
        
        public boolean isGenerateTrends() { return generateTrends; }
        public void setGenerateTrends(boolean generateTrends) { this.generateTrends = generateTrends; }
    }
    
    public static class Performance {
        private int threadPoolSize = 10;
        private int queueCapacity = 1000;
        private boolean enableMetrics = true;
        private int metricsReportingIntervalSeconds = 60;
        private boolean enableBatching = true;
        private int batchSize = 50;
        private int batchTimeoutMs = 1000;
        private boolean enableCompression = true;
        private int maxMemoryUsageMB = 512;
        
        // Getters and setters
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        
        public boolean isEnableMetrics() { return enableMetrics; }
        public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
        
        public int getMetricsReportingIntervalSeconds() { return metricsReportingIntervalSeconds; }
        public void setMetricsReportingIntervalSeconds(int metricsReportingIntervalSeconds) { this.metricsReportingIntervalSeconds = metricsReportingIntervalSeconds; }
        
        public boolean isEnableBatching() { return enableBatching; }
        public void setEnableBatching(boolean enableBatching) { this.enableBatching = enableBatching; }
        
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        
        public int getBatchTimeoutMs() { return batchTimeoutMs; }
        public void setBatchTimeoutMs(int batchTimeoutMs) { this.batchTimeoutMs = batchTimeoutMs; }
        
        public boolean isEnableCompression() { return enableCompression; }
        public void setEnableCompression(boolean enableCompression) { this.enableCompression = enableCompression; }
        
        public int getMaxMemoryUsageMB() { return maxMemoryUsageMB; }
        public void setMaxMemoryUsageMB(int maxMemoryUsageMB) { this.maxMemoryUsageMB = maxMemoryUsageMB; }
    }
}
