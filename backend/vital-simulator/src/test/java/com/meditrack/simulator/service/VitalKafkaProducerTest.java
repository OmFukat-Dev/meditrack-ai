package com.meditrack.simulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.simulator.config.SimulationConfig;
import com.meditrack.simulator.kafka.VitalKafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VitalKafkaProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private VitalKafkaProducer producer;
    private SimulationConfig simulationConfig;

    @BeforeEach
    void setUp() {
        producer = new VitalKafkaProducer();
        simulationConfig = new SimulationConfig();
        simulationConfig.getKafkaProducer().setTopic("vitals-topic");
        simulationConfig.getPerformance().setBatchSize(2);

        ReflectionTestUtils.setField(producer, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(producer, "objectMapper", new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(producer, "simulationConfig", simulationConfig);
    }

    @Test
    void sendVitalReadingCompletesWhenKafkaSendSucceeds() throws Exception {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("vitals-topic"), eq("patient-1"), anyString())).thenReturn(future);

        VitalDataGenerator.VitalReading reading = sampleReading();
        CompletableFuture<Boolean> result = producer.sendVitalReading(reading);

        future.complete(mock(SendResult.class));

        assertTrue(result.get(2, TimeUnit.SECONDS));
        assertEquals(1L, producer.getProductionStats().getMessagesSent());
        verify(kafkaTemplate).send(eq("vitals-topic"), eq("patient-1"), anyString());
    }

    @Test
    void sendVitalReadingCompletesFalseWhenKafkaSendFails() throws Exception {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("vitals-topic"), eq("patient-1"), anyString())).thenReturn(future);

        CompletableFuture<Boolean> result = producer.sendVitalReading(sampleReading());
        future.completeExceptionally(new RuntimeException("boom"));

        assertFalse(result.get(2, TimeUnit.SECONDS));
        assertEquals(1L, producer.getProductionStats().getMessagesFailed());
    }

    @Test
    void sendVitalBatchHandlesEmptyInputWithoutKafkaCall() throws Exception {
        assertTrue(producer.sendVitalBatch(List.of()).get(2, TimeUnit.SECONDS));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void addToBatchFlushesWhenThresholdReached() throws Exception {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("vital-batches"), anyString(), anyString())).thenReturn(future);

        producer.addToBatch(sampleReading());
        assertEquals(1, producer.getProductionStats().getQueueSize());

        producer.addToBatch(sampleReading());
        future.complete(mock(SendResult.class));

        assertEquals(2L, producer.getProductionStats().getMessagesSent());
        assertEquals(0, producer.getProductionStats().getQueueSize());
        verify(kafkaTemplate).send(eq("vital-batches"), anyString(), anyString());
    }

    @Test
    void sendAnomalyAlertCompletesWhenKafkaSendSucceeds() throws Exception {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("vital-alerts"), eq("patient-1_HEART_RATE"), anyString())).thenReturn(future);

        CompletableFuture<Boolean> result = producer.sendAnomalyAlert(
            "patient-1",
            VitalDataGenerator.HEART_RATE,
            "HIGH",
            "Simulated anomaly"
        );
        future.complete(mock(SendResult.class));

        assertTrue(result.get(2, TimeUnit.SECONDS));
        verify(kafkaTemplate).send(eq("vital-alerts"), eq("patient-1_HEART_RATE"), anyString());
    }

    @Test
    void sendSimulationStatusCompletesWhenKafkaSendSucceeds() throws Exception {
        CompletableFuture<SendResult<String, String>> future = new CompletableFuture<>();
        when(kafkaTemplate.send(eq("simulation-status"), anyString(), anyString())).thenReturn(future);

        Map<String, Object> metrics = new java.util.HashMap<>();
        metrics.put("queueSize", 0);

        CompletableFuture<Boolean> result = producer.sendSimulationStatus(
            new VitalKafkaProducer.SimulationStatus(
                "RUNNING",
                1,
                3,
                "2026-04-23T10:15:00",
                metrics
            )
        );
        future.complete(mock(SendResult.class));

        assertTrue(result.get(2, TimeUnit.SECONDS));
        verify(kafkaTemplate).send(eq("simulation-status"), anyString(), anyString());
    }

    @Test
    void resetStatsClearsCountersAndQueue() {
        producer.addToBatch(sampleReading());
        producer.resetStats();

        VitalKafkaProducer.ProductionStats stats = producer.getProductionStats();
        assertEquals(0L, stats.getMessagesSent());
        assertEquals(0L, stats.getMessagesFailed());
        assertEquals(0, stats.getQueueSize());
        assertEquals(0.0, stats.getSuccessRate());
    }

    private VitalDataGenerator.VitalReading sampleReading() {
        return new VitalDataGenerator.VitalReading(
            "patient-1",
            VitalDataGenerator.HEART_RATE,
            new BigDecimal("88"),
            "bpm",
            LocalDateTime.of(2026, 4, 23, 10, 5),
            "SIMULATOR",
            "DEVICE_patient-1",
            "ICU_BED_patient-1",
            new BigDecimal("0.95"),
            "Generated heart rate reading"
        );
    }
}
