package com.meditrack.simulator.service;

import com.meditrack.simulator.config.SimulationConfig;
import com.meditrack.simulator.controller.SimulatorController;
import com.meditrack.simulator.kafka.VitalKafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VitalSimulationServiceTest {

    @Mock
    private VitalDataGenerator vitalDataGenerator;

    @Mock
    private VitalKafkaProducer kafkaProducer;

    private VitalSimulationService service;
    private Map<String, Object> activeSimulations;
    private AtomicLong totalVitalsGenerated;
    private AtomicLong totalAnomaliesGenerated;
    private AtomicLong totalSimulationsStarted;

    @BeforeEach
    void setUp() {
        service = spy(new VitalSimulationService());
        ReflectionTestUtils.setField(service, "vitalDataGenerator", vitalDataGenerator);
        ReflectionTestUtils.setField(service, "kafkaProducer", kafkaProducer);
        ReflectionTestUtils.setField(service, "simulationConfig", new SimulationConfig());

        @SuppressWarnings("unchecked")
        Map<String, Object> simulations = (Map<String, Object>) ReflectionTestUtils.getField(service, "activeSimulations");
        activeSimulations = simulations;
        totalVitalsGenerated = (AtomicLong) ReflectionTestUtils.getField(service, "totalVitalsGenerated");
        totalAnomaliesGenerated = (AtomicLong) ReflectionTestUtils.getField(service, "totalAnomaliesGenerated");
        totalSimulationsStarted = (AtomicLong) ReflectionTestUtils.getField(service, "totalSimulationsStarted");
    }

    @Test
    void generateVitalBatchCreatesExpectedNumberOfReadings() {
        VitalDataGenerator.VitalReading sampleReading = new VitalDataGenerator.VitalReading(
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
        when(vitalDataGenerator.generateVitalReading(
            anyString(),
            anyString(),
            any(VitalDataGenerator.VitalProfile.class),
            any(LocalDateTime.class)
        )).thenReturn(sampleReading);

        SimulatorController.BatchGenerationRequest request = new SimulatorController.BatchGenerationRequest();
        request.setPatientCount(2);
        request.setVitalsPerPatient(3);
        request.setVitalTypes(List.of(VitalDataGenerator.HEART_RATE, VitalDataGenerator.TEMPERATURE));

        List<VitalDataGenerator.VitalReading> readings = service.generateVitalBatch(request);

        assertEquals(12, readings.size());
        org.mockito.Mockito.verify(vitalDataGenerator, times(12)).generateVitalReading(
            anyString(),
            anyString(),
            any(VitalDataGenerator.VitalProfile.class),
            any(LocalDateTime.class)
        );
    }

    @Test
    void lifecycleMethodsUpdateSimulationState() throws Exception {
        String simulationId = "sim-001";
        Object instance = newSimulationInstance(
            simulationId,
            "RUNNING",
            LocalDateTime.now().minusMinutes(5)
        );
        activeSimulations.put(simulationId, instance);

        SimulatorController.SimulationStatus status = service.getSimulationStatus(simulationId);
        assertEquals("RUNNING", status.getStatus());
        assertEquals(2, status.getPatientCount());
        assertNotNull(status.getMetrics());

        SimulatorController.SimulationResult pause = service.pauseSimulation(simulationId);
        assertEquals("PAUSED", pause.getStatus());
        assertEquals("PAUSED", invokeString(instance, "getStatus"));

        SimulatorController.SimulationResult resume = service.resumeSimulation(simulationId);
        assertEquals("RUNNING", resume.getStatus());
        assertEquals("RUNNING", invokeString(instance, "getStatus"));

        SimulatorController.SimulationResult stop = service.stopSimulation(simulationId);
        assertEquals("STOPPED", stop.getStatus());
        assertFalse(activeSimulations.containsKey(simulationId));
        assertThrows(RuntimeException.class, () -> service.getSimulationStatus(simulationId));
    }

    @Test
    void cleanupOldSimulationsRemovesStoppedEntriesOnly() throws Exception {
        Object stopped = newSimulationInstance(
            "sim-old",
            "STOPPED",
            LocalDateTime.now().minusHours(2)
        );
        Object running = newSimulationInstance(
            "sim-active",
            "RUNNING",
            LocalDateTime.now().minusHours(2)
        );

        activeSimulations.put("sim-old", stopped);
        activeSimulations.put("sim-active", running);

        service.cleanupOldSimulations();

        assertFalse(activeSimulations.containsKey("sim-old"));
        assertTrue(activeSimulations.containsKey("sim-active"));
    }

    @Test
    void resetStatsClearsGlobalAndPerSimulationCounters() throws Exception {
        Object instance = newSimulationInstance(
            "sim-stats",
            "RUNNING",
            LocalDateTime.now()
        );
        setLongValue(instance, "setVitalsGenerated", 7L);
        setLongValue(instance, "setAnomaliesGenerated", 3L);
        activeSimulations.put("sim-stats", instance);

        totalVitalsGenerated.set(11L);
        totalAnomaliesGenerated.set(4L);
        totalSimulationsStarted.set(2L);

        service.resetStats();

        assertEquals(0L, totalVitalsGenerated.get());
        assertEquals(0L, totalAnomaliesGenerated.get());
        assertEquals(0L, totalSimulationsStarted.get());
        assertEquals(0L, getLongValue(instance, "getVitalsGenerated"));
        assertEquals(0L, getLongValue(instance, "getAnomaliesGenerated"));
    }

    @Test
    void getSimulationStatsIncludesActiveSimulationDetails() throws Exception {
        Object instance = newSimulationInstance(
            "sim-stats",
            "RUNNING",
            LocalDateTime.now()
        );
        setLongValue(instance, "setVitalsGenerated", 9L);
        setLongValue(instance, "setAnomaliesGenerated", 2L);
        activeSimulations.put("sim-stats", instance);

        Map<String, Object> stats = service.getSimulationStats();

        assertEquals(1, ((Number) stats.get("activeSimulations")).intValue());
        assertEquals(0L, ((Number) stats.get("totalVitalsGenerated")).longValue());
        assertEquals(0L, ((Number) stats.get("totalAnomaliesGenerated")).longValue());
        assertEquals(0L, ((Number) stats.get("totalSimulationsStarted")).longValue());
        @SuppressWarnings("unchecked")
        Map<String, Object> perSimulationStats = (Map<String, Object>) stats.get("perSimulationStats");
        @SuppressWarnings("unchecked")
        Map<String, Object> simStats = (Map<String, Object>) perSimulationStats.get("sim-stats");
        assertEquals("RUNNING", simStats.get("status"));
        assertEquals(2, ((Number) simStats.get("patientCount")).intValue());
        assertEquals(9L, ((Number) simStats.get("vitalsGenerated")).longValue());
        assertEquals(2L, ((Number) simStats.get("anomaliesGenerated")).longValue());
    }

    private Object newSimulationInstance(String simulationId, String status, LocalDateTime lastUpdate) throws Exception {
        Class<?> type = Class.forName("com.meditrack.simulator.service.VitalSimulationService$SimulationInstance");
        Constructor<?> constructor = type.getDeclaredConstructor(
            String.class,
            int.class,
            int.class,
            List.class,
            boolean.class,
            double.class,
            LocalDateTime.class
        );
        constructor.setAccessible(true);
        Object instance = constructor.newInstance(
            simulationId,
            2,
            5,
            List.of(VitalDataGenerator.HEART_RATE, VitalDataGenerator.SPO2),
            false,
            0.25d,
            lastUpdate
        );
        setStringValue(instance, "setStatus", status);
        return instance;
    }

    private void setStringValue(Object target, String methodName, String value) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        method.invoke(target, value);
    }

    private void setLongValue(Object target, String methodName, long value) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, long.class);
        method.setAccessible(true);
        method.invoke(target, value);
    }

    private String invokeString(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (String) method.invoke(target);
    }

    private long getLongValue(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return ((Number) method.invoke(target)).longValue();
    }
}
