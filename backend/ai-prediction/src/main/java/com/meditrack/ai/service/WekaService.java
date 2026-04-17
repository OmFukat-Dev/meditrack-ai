package com.meditrack.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import weka.classifiers.trees.J48;
import weka.core.*;
import weka.core.converters.CSVLoader;
import weka.core.converters.CSVSaver;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.Standardize;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class WekaService {
    
    private static final Logger logger = LoggerFactory.getLogger(WekaService.class);
    
    // Model storage
    private final Map<String, J48> trainedModels = new HashMap<>();
    private final Map<String, Instances> trainingData = new HashMap<>();
    private final Map<String, ModelPerformance> modelPerformance = new HashMap<>();
    
    // Train J48 Decision Tree model
    public ModelTrainingResult trainJ48Model(String modelName, List<Map<String, Object>> trainingDataList, 
                                           String targetAttribute, Map<String, Object> trainingParams) {
        try {
            logger.info("Training J48 model: {} with {} samples", modelName, trainingDataList.size());
            
            // Convert training data to Weka Instances
            Instances instances = convertToWekaInstances(trainingDataList, targetAttribute);
            
            // Set class index (target attribute)
            instances.setClassIndex(instances.numAttributes() - 1);
            
            // Create and configure J48 classifier
            J48 j48 = new J48();
            configureJ48Classifier(j48, trainingParams);
            
            // Train the model
            long startTime = System.currentTimeMillis();
            j48.buildClassifier(instances);
            long trainingTime = System.currentTimeMillis() - startTime;
            
            // Store model and data
            trainedModels.put(modelName, j48);
            this.trainingData.put(modelName, instances);
            
            // Calculate model performance
            ModelPerformance performance = evaluateModel(j48, instances);
            modelPerformance.put(modelName, performance);
            
            logger.info("Successfully trained J48 model: {} in {}ms", modelName, trainingTime);
            
            return new ModelTrainingResult(
                modelName,
                true,
                "Model trained successfully",
                trainingTime,
                performance,
                j48.toString()
            );
            
        } catch (Exception e) {
            logger.error("Error training J48 model: {}", e.getMessage(), e);
            return new ModelTrainingResult(
                modelName,
                false,
                "Failed to train model: " + e.getMessage(),
                0,
                null,
                null
            );
        }
    }
    
    // Make prediction using trained model
    public PredictionResult makePrediction(String modelName, Map<String, Object> inputFeatures) {
        try {
            J48 model = trainedModels.get(modelName);
            if (model == null) {
                throw new IllegalArgumentException("Model not found: " + modelName);
            }
            
            Instances instances = trainingData.get(modelName);
            if (instances == null) {
                throw new IllegalArgumentException("Training data not found for model: " + modelName);
            }
            
            // Create instance for prediction
            Instance instance = createInstanceForPrediction(inputFeatures, instances);
            
            // Make prediction
            double prediction = model.classifyInstance(instance);
            double[] distribution = model.distributionForInstance(instance);
            
            // Get class label
            String predictedClass = instances.classAttribute().value((int) prediction);
            
            // Calculate confidence
            double confidence = Arrays.stream(distribution).max().orElse(0.0);
            
            logger.debug("Prediction for model {}: {} (confidence: {})", 
                       modelName, predictedClass, confidence);
            
            return new PredictionResult(
                modelName,
                predictedClass,
                confidence,
                distribution,
                inputFeatures,
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error making prediction with model {}: {}", modelName, e.getMessage(), e);
            return new PredictionResult(
                modelName,
                null,
                0.0,
                null,
                inputFeatures,
                LocalDateTime.now(),
                "Prediction failed: " + e.getMessage()
            );
        }
    }
    
    // Batch prediction
    public List<PredictionResult> makeBatchPrediction(String modelName, List<Map<String, Object>> inputFeaturesList) {
        List<PredictionResult> results = new ArrayList<>();
        
        for (Map<String, Object> inputFeatures : inputFeaturesList) {
            results.add(makePrediction(modelName, inputFeatures));
        }
        
        return results;
    }
    
    // Get model information
    public ModelInfo getModelInfo(String modelName) {
        try {
            J48 model = trainedModels.get(modelName);
            Instances instances = trainingData.get(modelName);
            ModelPerformance performance = modelPerformance.get(modelName);
            
            if (model == null || instances == null) {
                throw new IllegalArgumentException("Model not found: " + modelName);
            }
            
            return new ModelInfo(
                modelName,
                model.getClass().getSimpleName(),
                instances.numAttributes(),
                instances.numInstances(),
                performance,
                model.toString(),
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error getting model info for {}: {}", modelName, e.getMessage(), e);
            return null;
        }
    }
    
    // List all trained models
    public List<ModelInfo> listAllModels() {
        List<ModelInfo> models = new ArrayList<>();
        
        for (String modelName : trainedModels.keySet()) {
            ModelInfo info = getModelInfo(modelName);
            if (info != null) {
                models.add(info);
            }
        }
        
        return models;
    }
    
    // Save model to file
    public boolean saveModel(String modelName, String filePath) {
        try {
            J48 model = trainedModels.get(modelName);
            Instances instances = trainingData.get(modelName);
            
            if (model == null || instances == null) {
                throw new IllegalArgumentException("Model not found: " + modelName);
            }
            
            // Save model
            weka.core.SerializationHelper.write(filePath + "_model.model", model);
            
            // Save training data
            CSVSaver saver = new CSVSaver();
            saver.setInstances(instances);
            saver.setFile(new File(filePath + "_data.csv"));
            saver.writeBatch();
            
            logger.info("Successfully saved model: {} to {}", modelName, filePath);
            return true;
            
        } catch (Exception e) {
            logger.error("Error saving model {}: {}", modelName, e.getMessage(), e);
            return false;
        }
    }
    
    // Load model from file
    public boolean loadModel(String modelName, String modelFilePath, String dataFilePath) {
        try {
            // Load model
            J48 model = (J48) weka.core.SerializationHelper.read(modelFilePath);
            
            // Load training data
            CSVLoader loader = new CSVLoader();
            loader.setSource(new File(dataFilePath));
            Instances instances = loader.getDataSet();
            instances.setClassIndex(instances.numAttributes() - 1);
            
            // Store model and data
            trainedModels.put(modelName, model);
            trainingData.put(modelName, instances);
            
            // Calculate performance
            ModelPerformance performance = evaluateModel(model, instances);
            modelPerformance.put(modelName, performance);
            
            logger.info("Successfully loaded model: {} from {}", modelName, modelFilePath);
            return true;
            
        } catch (Exception e) {
            logger.error("Error loading model {}: {}", modelName, e.getMessage(), e);
            return false;
        }
    }
    
    // Delete model
    public boolean deleteModel(String modelName) {
        if (trainedModels.containsKey(modelName)) {
            trainedModels.remove(modelName);
            trainingData.remove(modelName);
            modelPerformance.remove(modelName);
            
            logger.info("Successfully deleted model: {}", modelName);
            return true;
        }
        return false;
    }
    
    // Get model performance metrics
    public ModelPerformance getModelPerformance(String modelName) {
        return modelPerformance.get(modelName);
    }
    
    // Utility methods
    private Instances convertToWekaInstances(List<Map<String, Object>> data, String targetAttribute) {
        try {
            // Create attribute list
            ArrayList<Attribute> attributes = new ArrayList<>();
            
            if (!data.isEmpty()) {
                Map<String, Object> firstRow = data.get(0);
                
                // Create attributes for each feature
                for (String key : firstRow.keySet()) {
                    if (key.equals(targetAttribute)) {
                        // Target attribute (nominal)
                        Set<Object> uniqueValues = new HashSet<>();
                        for (Map<String, Object> row : data) {
                            uniqueValues.add(row.get(key));
                        }
                        attributes.add(new Attribute(key, new ArrayList<>(uniqueValues)));
                    } else {
                        // Numeric attribute
                        attributes.add(new Attribute(key));
                    }
                }
            }
            
            // Create instances
            Instances instances = new Instances("TrainingData", attributes, data.size());
            
            // Add instances
            for (Map<String, Object> row : data) {
                double[] values = new double[attributes.size()];
                
                for (int i = 0; i < attributes.size(); i++) {
                    Attribute attr = attributes.get(i);
                    Object value = row.get(attr.name());
                    
                    if (value == null) {
                        values[i] = Utils.missingValue();
                    } else if (attr.isNominal()) {
                        values[i] = attr.indexOfValue(value.toString());
                    } else {
                        values[i] = ((Number) value).doubleValue();
                    }
                }
                
                instances.add(new DenseInstance(1.0, values));
            }
            
            return instances;
            
        } catch (Exception e) {
            logger.error("Error converting data to Weka Instances: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to convert data to Weka format", e);
        }
    }
    
    private void configureJ48Classifier(J48 j48, Map<String, Object> params) {
        try {
            // Set J48 parameters
            if (params.containsKey("confidenceFactor")) {
                j48.setConfidenceFactor(((Number) params.get("confidenceFactor")).floatValue());
            }
            
            if (params.containsKey("minNumObj")) {
                j48.setMinNumObj(((Number) params.get("minNumObj")).intValue());
            }
            
            if (params.containsKey("binarySplits")) {
                j48.setBinarySplits((Boolean) params.get("binarySplits"));
            }
            
            if (params.containsKey("unpruned")) {
                j48.setUnpruned((Boolean) params.get("unpruned"));
            }
            
            if (params.containsKey("reducedErrorPruning")) {
                j48.setReducedErrorPruning((Boolean) params.get("reducedErrorPruning"));
            }
            
            if (params.containsKey("numFolds")) {
                j48.setNumFolds(((Number) params.get("numFolds")).intValue());
            }
            
        } catch (Exception e) {
            logger.warn("Error configuring J48 classifier: {}", e.getMessage());
        }
    }
    
    private Instance createInstanceForPrediction(Map<String, Object> inputFeatures, Instances trainingInstances) {
        try {
            Instance instance = new DenseInstance(trainingInstances.numAttributes());
            instance.setDataset(trainingInstances);
            
            for (int i = 0; i < trainingInstances.numAttributes(); i++) {
                Attribute attr = trainingInstances.attribute(i);
                Object value = inputFeatures.get(attr.name());
                
                if (value == null) {
                    instance.setMissing(attr);
                } else if (attr.isNominal()) {
                    instance.setValue(attr, value.toString());
                } else {
                    instance.setValue(attr, ((Number) value).doubleValue());
                }
            }
            
            return instance;
            
        } catch (Exception e) {
            logger.error("Error creating instance for prediction: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create prediction instance", e);
        }
    }
    
    private ModelPerformance evaluateModel(J48 model, Instances data) {
        try {
            int correct = 0;
            int total = data.numInstances();
            
            for (int i = 0; i < total; i++) {
                Instance instance = data.instance(i);
                double prediction = model.classifyInstance(instance);
                double actual = instance.classValue();
                
                if (prediction == actual) {
                    correct++;
                }
            }
            
            double accuracy = (double) correct / total;
            
            // Calculate additional metrics (simplified)
            double precision = accuracy; // Simplified
            double recall = accuracy; // Simplified
            double f1Score = 2 * (precision * recall) / (precision + recall);
            
            return new ModelPerformance(
                accuracy,
                precision,
                recall,
                f1Score,
                total,
                correct,
                LocalDateTime.now()
            );
            
        } catch (Exception e) {
            logger.error("Error evaluating model: {}", e.getMessage(), e);
            return new ModelPerformance(0.0, 0.0, 0.0, 0.0, 0, 0, LocalDateTime.now());
        }
    }
    
    // Inner classes
    public static class ModelTrainingResult {
        private String modelName;
        private boolean success;
        private String message;
        private long trainingTimeMs;
        private ModelPerformance performance;
        private String modelDescription;
        
        public ModelTrainingResult(String modelName, boolean success, String message, 
                                long trainingTimeMs, ModelPerformance performance, String modelDescription) {
            this.modelName = modelName;
            this.success = success;
            this.message = message;
            this.trainingTimeMs = trainingTimeMs;
            this.performance = performance;
            this.modelDescription = modelDescription;
        }
        
        // Getters
        public String getModelName() { return modelName; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public long getTrainingTimeMs() { return trainingTimeMs; }
        public ModelPerformance getPerformance() { return performance; }
        public String getModelDescription() { return modelDescription; }
    }
    
    public static class PredictionResult {
        private String modelName;
        private String predictedClass;
        private double confidence;
        private double[] probabilityDistribution;
        private Map<String, Object> inputFeatures;
        private LocalDateTime timestamp;
        private String error;
        
        public PredictionResult(String modelName, String predictedClass, double confidence, 
                            double[] probabilityDistribution, Map<String, Object> inputFeatures, 
                            LocalDateTime timestamp) {
            this.modelName = modelName;
            this.predictedClass = predictedClass;
            this.confidence = confidence;
            this.probabilityDistribution = probabilityDistribution;
            this.inputFeatures = inputFeatures;
            this.timestamp = timestamp;
        }
        
        public PredictionResult(String modelName, String predictedClass, double confidence, 
                            double[] probabilityDistribution, Map<String, Object> inputFeatures, 
                            LocalDateTime timestamp, String error) {
            this(modelName, predictedClass, confidence, probabilityDistribution, inputFeatures, timestamp);
            this.error = error;
        }
        
        // Getters
        public String getModelName() { return modelName; }
        public String getPredictedClass() { return predictedClass; }
        public double getConfidence() { return confidence; }
        public double[] getProbabilityDistribution() { return probabilityDistribution; }
        public Map<String, Object> getInputFeatures() { return inputFeatures; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getError() { return error; }
        public boolean hasError() { return error != null; }
    }
    
    public static class ModelInfo {
        private String modelName;
        private String modelType;
        private int numAttributes;
        private int numInstances;
        private ModelPerformance performance;
        private String modelDescription;
        private LocalDateTime lastUpdated;
        
        public ModelInfo(String modelName, String modelType, int numAttributes, int numInstances,
                        ModelPerformance performance, String modelDescription, LocalDateTime lastUpdated) {
            this.modelName = modelName;
            this.modelType = modelType;
            this.numAttributes = numAttributes;
            this.numInstances = numInstances;
            this.performance = performance;
            this.modelDescription = modelDescription;
            this.lastUpdated = lastUpdated;
        }
        
        // Getters
        public String getModelName() { return modelName; }
        public String getModelType() { return modelType; }
        public int getNumAttributes() { return numAttributes; }
        public int getNumInstances() { return numInstances; }
        public ModelPerformance getPerformance() { return performance; }
        public String getModelDescription() { return modelDescription; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }
    
    public static class ModelPerformance {
        private double accuracy;
        private double precision;
        private double recall;
        private double f1Score;
        private int totalInstances;
        private int correctPredictions;
        private LocalDateTime lastEvaluated;
        
        public ModelPerformance(double accuracy, double precision, double recall, double f1Score,
                            int totalInstances, int correctPredictions, LocalDateTime lastEvaluated) {
            this.accuracy = accuracy;
            this.precision = precision;
            this.recall = recall;
            this.f1Score = f1Score;
            this.totalInstances = totalInstances;
            this.correctPredictions = correctPredictions;
            this.lastEvaluated = lastEvaluated;
        }
        
        // Getters
        public double getAccuracy() { return accuracy; }
        public double getPrecision() { return precision; }
        public double getRecall() { return recall; }
        public double getF1Score() { return f1Score; }
        public int getTotalInstances() { return totalInstances; }
        public int getCorrectPredictions() { return correctPredictions; }
        public LocalDateTime getLastEvaluated() { return lastEvaluated; }
    }
}
