package com.meditrack.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import weka.classifiers.trees.J48;
import weka.core.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeatureImportanceService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureImportanceService.class);
    
    // Feature importance storage
    private final Map<String, FeatureImportanceResults> modelFeatureImportance = new HashMap<>();
    
    // Extract feature importance from J48 model
    public FeatureImportanceResults extractFeatureImportance(String modelName, J48 model, Instances data) {
        try {
            logger.info("Extracting feature importance for model: {}", modelName);
            
            FeatureImportanceResults results = new FeatureImportanceResults();
            results.setModelName(modelName);
            results.setExtractionTimestamp(LocalDateTime.now());
            
            // Extract tree-based importance
            List<FeatureImportance> treeImportance = extractTreeBasedImportance(model, data);
            results.setTreeBasedImportance(treeImportance);
            
            // Extract statistical importance
            List<FeatureImportance> statisticalImportance = extractStatisticalImportance(data);
            results.setStatisticalImportance(statisticalImportance);
            
            // Extract correlation-based importance
            List<FeatureImportance> correlationImportance = extractCorrelationImportance(data);
            results.setCorrelationImportance(correlationImportance);
            
            // Calculate combined importance
            List<FeatureImportance> combinedImportance = calculateCombinedImportance(
                treeImportance, statisticalImportance, correlationImportance);
            results.setCombinedImportance(combinedImportance);
            
            // Generate explanation summary
            results.setExplanationSummary(generateExplanationSummary(combinedImportance));
            
            // Store results
            modelFeatureImportance.put(modelName, results);
            
            logger.info("Successfully extracted feature importance for model: {} ({} features)", 
                       modelName, combinedImportance.size());
            
            return results;
            
        } catch (Exception e) {
            logger.error("Error extracting feature importance for model: {}", modelName, e);
            throw new RuntimeException("Failed to extract feature importance", e);
        }
    }
    
    // Extract tree-based feature importance from J48
    private List<FeatureImportance> extractTreeBasedImportance(J48 model, Instances data) {
        try {
            List<FeatureImportance> importance = new ArrayList<>();
            
            // Get the tree structure
            String treeString = model.toString();
            
            // Count feature occurrences in the tree
            Map<String, Integer> featureCounts = new HashMap<>();
            Map<String, Double> featureDepths = new HashMap<>();
            
            // Parse tree string to extract feature usage
            String[] lines = treeString.split("\n");
            for (String line : lines) {
                if (line.contains(":")) {
                    String feature = extractFeatureFromTreeLine(line);
                    if (feature != null) {
                        int depth = countTreeDepth(line);
                        
                        featureCounts.put(feature, featureCounts.getOrDefault(feature, 0) + 1);
                        featureDepths.put(feature, Math.min(
                            featureDepths.getOrDefault(feature, Double.MAX_VALUE), depth));
                    }
                }
            }
            
            // Calculate importance scores
            int totalFeatures = featureCounts.size();
            for (Map.Entry<String, Integer> entry : featureCounts.entrySet()) {
                String feature = entry.getKey();
                int count = entry.getValue();
                double avgDepth = featureDepths.get(feature);
                
                // Importance based on frequency and depth (higher frequency, lower depth = more important)
                double importanceScore = (double) count / totalFeatures * (1.0 / (1.0 + avgDepth));
                
                importance.add(new FeatureImportance(
                    feature,
                    importanceScore,
                    "TREE_BASED",
                    Map.of(
                        "frequency", count,
                        "averageDepth", avgDepth,
                        "totalFeatures", totalFeatures
                    )
                ));
            }
            
            // Sort by importance score
            importance.sort((a, b) -> Double.compare(b.getImportanceScore(), a.getImportanceScore()));
            
            return importance;
            
        } catch (Exception e) {
            logger.error("Error extracting tree-based importance", e);
            return new ArrayList<>();
        }
    }
    
    // Extract statistical feature importance
    private List<FeatureImportance> extractStatisticalImportance(Instances data) {
        try {
            List<FeatureImportance> importance = new ArrayList<>();
            
            for (int i = 0; i < data.numAttributes() - 1; i++) { // Exclude class attribute
                Attribute attribute = data.attribute(i);
                String featureName = attribute.name();
                
                // Calculate statistical measures
                double variance = calculateAttributeVariance(data, i);
                double entropy = calculateAttributeEntropy(data, i);
                double correlation = calculateAttributeClassCorrelation(data, i);
                
                // Combined statistical importance
                double importanceScore = (variance + entropy + Math.abs(correlation)) / 3.0;
                
                importance.add(new FeatureImportance(
                    featureName,
                    importanceScore,
                    "STATISTICAL",
                    Map.of(
                        "variance", variance,
                        "entropy", entropy,
                        "correlation", correlation
                    )
                ));
            }
            
            // Sort by importance score
            importance.sort((a, b) -> Double.compare(b.getImportanceScore(), a.getImportanceScore()));
            
            return importance;
            
        } catch (Exception e) {
            logger.error("Error extracting statistical importance", e);
            return new ArrayList<>();
        }
    }
    
    // Extract correlation-based feature importance
    private List<FeatureImportance> extractCorrelationImportance(Instances data) {
        try {
            List<FeatureImportance> importance = new ArrayList<>();
            
            for (int i = 0; i < data.numAttributes() - 1; i++) { // Exclude class attribute
                Attribute attribute = data.attribute(i);
                String featureName = attribute.name();
                
                // Calculate correlation with class
                double correlation = calculateAttributeClassCorrelation(data, i);
                
                // Calculate mutual information (simplified)
                double mutualInformation = calculateMutualInformation(data, i);
                
                // Combined correlation importance
                double importanceScore = (Math.abs(correlation) + mutualInformation) / 2.0;
                
                importance.add(new FeatureImportance(
                    featureName,
                    importanceScore,
                    "CORRELATION",
                    Map.of(
                        "correlation", correlation,
                        "mutualInformation", mutualInformation
                    )
                ));
            }
            
            // Sort by importance score
            importance.sort((a, b) -> Double.compare(b.getImportanceScore(), a.getImportanceScore()));
            
            return importance;
            
        } catch (Exception e) {
            logger.error("Error extracting correlation importance", e);
            return new ArrayList<>();
        }
    }
    
    // Calculate combined feature importance
    private List<FeatureImportance> calculateCombinedImportance(
            List<FeatureImportance> treeImportance,
            List<FeatureImportance> statisticalImportance,
            List<FeatureImportance> correlationImportance) {
        
        try {
            Map<String, FeatureImportance> combined = new HashMap<>();
            
            // Combine tree-based importance
            for (FeatureImportance fi : treeImportance) {
                combined.put(fi.getFeatureName(), fi);
            }
            
            // Add statistical importance
            for (FeatureImportance fi : statisticalImportance) {
                String feature = fi.getFeatureName();
                if (combined.containsKey(feature)) {
                    // Average the scores
                    FeatureImportance existing = combined.get(feature);
                    double newScore = (existing.getImportanceScore() + fi.getImportanceScore()) / 2.0;
                    existing.setImportanceScore(newScore);
                    existing.setMethod("COMBINED");
                } else {
                    combined.put(feature, fi);
                }
            }
            
            // Add correlation importance
            for (FeatureImportance fi : correlationImportance) {
                String feature = fi.getFeatureName();
                if (combined.containsKey(feature)) {
                    // Average the scores
                    FeatureImportance existing = combined.get(feature);
                    double newScore = (existing.getImportanceScore() + fi.getImportanceScore()) / 2.0;
                    existing.setImportanceScore(newScore);
                    existing.setMethod("COMBINED");
                } else {
                    combined.put(feature, fi);
                }
            }
            
            // Convert to list and sort
            List<FeatureImportance> result = new ArrayList<>(combined.values());
            result.sort((a, b) -> Double.compare(b.getImportanceScore(), a.getImportanceScore()));
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error calculating combined importance", e);
            return new ArrayList<>();
        }
    }
    
    // Generate explanation summary
    private String generateExplanationSummary(List<FeatureImportance> combinedImportance) {
        try {
            if (combinedImportance.isEmpty()) {
                return "No features available for importance analysis.";
            }
            
            StringBuilder summary = new StringBuilder();
            summary.append("Feature Importance Analysis:\n");
            summary.append("========================\n\n");
            
            // Top 5 features
            summary.append("Top 5 Most Important Features:\n");
            for (int i = 0; i < Math.min(5, combinedImportance.size()); i++) {
                FeatureImportance fi = combinedImportance.get(i);
                summary.append(String.format("%d. %s (%.3f)\n", 
                    i + 1, fi.getFeatureName(), fi.getImportanceScore()));
            }
            
            // Feature distribution
            summary.append("\nFeature Distribution:\n");
            Map<String, Long> methodDistribution = combinedImportance.stream()
                .collect(Collectors.groupingBy(FeatureImportance::getMethod, Collectors.counting()));
            
            for (Map.Entry<String, Long> entry : methodDistribution.entrySet()) {
                summary.append(String.format("- %s: %d features\n", entry.getKey(), entry.getValue()));
            }
            
            // Insights
            summary.append("\nKey Insights:\n");
            if (combinedImportance.size() > 0) {
                FeatureImportance top = combinedImportance.get(0);
                summary.append(String.format("- Most important feature: %s (%.3f)\n", 
                    top.getFeatureName(), top.getImportanceScore()));
                
                // Check for dominance
                double topScore = top.getImportanceScore();
                long dominantFeatures = combinedImportance.stream()
                    .mapToDouble(FeatureImportance::getImportanceScore)
                    .filter(score -> score >= topScore * 0.8)
                    .count();
                
                if (dominantFeatures <= 3) {
                    summary.append("- Few dominant features detected - model relies heavily on specific features\n");
                } else {
                    summary.append("- Distributed importance across multiple features\n");
                }
            }
            
            return summary.toString();
            
        } catch (Exception e) {
            logger.error("Error generating explanation summary", e);
            return "Error generating explanation summary.";
        }
    }
    
    // Get feature importance for a model
    public FeatureImportanceResults getFeatureImportance(String modelName) {
        return modelFeatureImportance.get(modelName);
    }
    
    // Get all feature importance results
    public Map<String, FeatureImportanceResults> getAllFeatureImportance() {
        return new HashMap<>(modelFeatureImportance);
    }
    
    // Generate feature importance report
    public String generateFeatureImportanceReport(String modelName) {
        try {
            FeatureImportanceResults results = modelFeatureImportance.get(modelName);
            if (results == null) {
                return "No feature importance data available for model: " + modelName;
            }
            
            StringBuilder report = new StringBuilder();
            report.append("Feature Importance Report\n");
            report.append("========================\n");
            report.append("Model: ").append(modelName).append("\n");
            report.append("Generated: ").append(results.getExtractionTimestamp()).append("\n\n");
            
            // Combined importance
            report.append("Combined Feature Importance:\n");
            report.append("-----------------------------\n");
            for (int i = 0; i < results.getCombinedImportance().size(); i++) {
                FeatureImportance fi = results.getCombinedImportance().get(i);
                report.append(String.format("%d. %s: %.4f (%s)\n", 
                    i + 1, fi.getFeatureName(), fi.getImportanceScore(), fi.getMethod()));
            }
            
            // Method-specific importance
            report.append("\nTree-Based Importance:\n");
            report.append("---------------------\n");
            for (int i = 0; i < Math.min(10, results.getTreeBasedImportance().size()); i++) {
                FeatureImportance fi = results.getTreeBasedImportance().get(i);
                report.append(String.format("%d. %s: %.4f\n", 
                    i + 1, fi.getFeatureName(), fi.getImportanceScore()));
            }
            
            report.append("\nStatistical Importance:\n");
            report.append("----------------------\n");
            for (int i = 0; i < Math.min(10, results.getStatisticalImportance().size()); i++) {
                FeatureImportance fi = results.getStatisticalImportance().get(i);
                report.append(String.format("%d. %s: %.4f\n", 
                    i + 1, fi.getFeatureName(), fi.getImportanceScore()));
            }
            
            report.append("\nCorrelation Importance:\n");
            report.append("----------------------\n");
            for (int i = 0; i < Math.min(10, results.getCorrelationImportance().size()); i++) {
                FeatureImportance fi = results.getCorrelationImportance().get(i);
                report.append(String.format("%d. %s: %.4f\n", 
                    i + 1, fi.getFeatureName(), fi.getImportanceScore()));
            }
            
            report.append("\nExplanation Summary:\n");
            report.append("--------------------\n");
            report.append(results.getExplanationSummary());
            
            return report.toString();
            
        } catch (Exception e) {
            logger.error("Error generating feature importance report for model: {}", modelName, e);
            return "Error generating feature importance report.";
        }
    }
    
    // Utility methods
    private String extractFeatureFromTreeLine(String line) {
        try {
            // Remove leading spaces and tree symbols
            String cleanLine = line.trim().replaceAll("[|\\-+\\s]+", " ");
            
            // Look for pattern like "feature_name <= value"
            if (cleanLine.contains("<=") || cleanLine.contains(">=") || cleanLine.contains("<") || cleanLine.contains(">")) {
                String[] parts = cleanLine.split("[<>=]");
                if (parts.length > 0) {
                    return parts[0].trim();
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    private int countTreeDepth(String line) {
        try {
            int depth = 0;
            for (char c : line.toCharArray()) {
                if (c == '|' || c == '-') {
                    depth++;
                } else if (c != ' ') {
                    break;
                }
            }
            return depth;
        } catch (Exception e) {
            return 0;
        }
    }
    
    private double calculateAttributeVariance(Instances data, int attributeIndex) {
        try {
            double[] values = new double[data.numInstances()];
            for (int i = 0; i < data.numInstances(); i++) {
                if (!data.instance(i).isMissing(attributeIndex)) {
                    values[i] = data.instance(i).value(attributeIndex);
                }
            }
            
            double mean = Arrays.stream(values).average().orElse(0.0);
            double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0);
            
            return variance;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private double calculateAttributeEntropy(Instances data, int attributeIndex) {
        try {
            Attribute attribute = data.attribute(attributeIndex);
            
            if (attribute.isNominal()) {
                // Calculate entropy for nominal attributes
                Map<String, Integer> valueCounts = new HashMap<>();
                for (int i = 0; i < data.numInstances(); i++) {
                    if (!data.instance(i).isMissing(attributeIndex)) {
                        String value = attribute.value((int) data.instance(i).value(attributeIndex));
                        valueCounts.put(value, valueCounts.getOrDefault(value, 0) + 1);
                    }
                }
                
                double entropy = 0.0;
                int total = valueCounts.values().stream().mapToInt(Integer::intValue).sum();
                for (int count : valueCounts.values()) {
                    double probability = (double) count / total;
                    if (probability > 0) {
                        entropy -= probability * Math.log(probability) / Math.log(2);
                    }
                }
                
                return entropy;
            } else {
                // For numeric attributes, use binning
                return 0.0; // Simplified
            }
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private double calculateAttributeClassCorrelation(Instances data, int attributeIndex) {
        try {
            int classIndex = data.classIndex();
            
            if (data.attribute(attributeIndex).isNumeric()) {
                // Calculate Pearson correlation for numeric attributes
                double[] attrValues = new double[data.numInstances()];
                double[] classValues = new double[data.numInstances()];
                
                for (int i = 0; i < data.numInstances(); i++) {
                    if (!data.instance(i).isMissing(attributeIndex) && !data.instance(i).isMissing(classIndex)) {
                        attrValues[i] = data.instance(i).value(attributeIndex);
                        classValues[i] = data.instance(i).value(classIndex);
                    }
                }
                
                return calculatePearsonCorrelation(attrValues, classValues);
            } else {
                // For nominal attributes, use contingency table
                return 0.0; // Simplified
            }
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private double calculatePearsonCorrelation(double[] x, double[] y) {
        try {
            int n = x.length;
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0, sumY2 = 0;
            
            for (int i = 0; i < n; i++) {
                sumX += x[i];
                sumY += y[i];
                sumXY += x[i] * y[i];
                sumX2 += x[i] * x[i];
                sumY2 += y[i] * y[i];
            }
            
            double numerator = n * sumXY - sumX * sumY;
            double denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
            
            return denominator == 0 ? 0.0 : numerator / denominator;
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private double calculateMutualInformation(Instances data, int attributeIndex) {
        try {
            // Simplified mutual information calculation
            double correlation = Math.abs(calculateAttributeClassCorrelation(data, attributeIndex));
            return correlation * 0.5; // Simplified approximation
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    // Inner classes
    public static class FeatureImportanceResults {
        private String modelName;
        private LocalDateTime extractionTimestamp;
        private List<FeatureImportance> treeBasedImportance;
        private List<FeatureImportance> statisticalImportance;
        private List<FeatureImportance> correlationImportance;
        private List<FeatureImportance> combinedImportance;
        private String explanationSummary;
        
        // Getters and setters
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        
        public LocalDateTime getExtractionTimestamp() { return extractionTimestamp; }
        public void setExtractionTimestamp(LocalDateTime extractionTimestamp) { this.extractionTimestamp = extractionTimestamp; }
        
        public List<FeatureImportance> getTreeBasedImportance() { return treeBasedImportance; }
        public void setTreeBasedImportance(List<FeatureImportance> treeBasedImportance) { this.treeBasedImportance = treeBasedImportance; }
        
        public List<FeatureImportance> getStatisticalImportance() { return statisticalImportance; }
        public void setStatisticalImportance(List<FeatureImportance> statisticalImportance) { this.statisticalImportance = statisticalImportance; }
        
        public List<FeatureImportance> getCorrelationImportance() { return correlationImportance; }
        public void setCorrelationImportance(List<FeatureImportance> correlationImportance) { this.correlationImportance = correlationImportance; }
        
        public List<FeatureImportance> getCombinedImportance() { return combinedImportance; }
        public void setCombinedImportance(List<FeatureImportance> combinedImportance) { this.combinedImportance = combinedImportance; }
        
        public String getExplanationSummary() { return explanationSummary; }
        public void setExplanationSummary(String explanationSummary) { this.explanationSummary = explanationSummary; }
    }
    
    public static class FeatureImportance {
        private String featureName;
        private double importanceScore;
        private String method;
        private Map<String, Object> details;
        
        public FeatureImportance(String featureName, double importanceScore, String method, Map<String, Object> details) {
            this.featureName = featureName;
            this.importanceScore = importanceScore;
            this.method = method;
            this.details = details;
        }
        
        // Getters and setters
        public String getFeatureName() { return featureName; }
        public void setFeatureName(String featureName) { this.featureName = featureName; }
        
        public double getImportanceScore() { return importanceScore; }
        public void setImportanceScore(double importanceScore) { this.importanceScore = importanceScore; }
        
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        
        public Map<String, Object> getDetails() { return details; }
        public void setDetails(Map<String, Object> details) { this.details = details; }
    }
}
