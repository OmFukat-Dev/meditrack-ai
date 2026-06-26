package com.meditrack.simulator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.simulator.config.SimulationConfig;
import com.meditrack.simulator.kafka.VitalKafkaProducer;
import com.meditrack.simulator.service.VitalDataGenerator;
import com.meditrack.simulator.service.VitalSimulationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SimulatorControllerTest {

    @Mock
    private VitalSimulationService simulationService;

    @Mock
    private VitalDataGenerator vitalDataGenerator;

    @Mock
    private VitalKafkaProducer kafkaProducer;

    private final SimulationConfig simulationConfig = new SimulationConfig();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SimulatorController controller = new SimulatorController();
        ReflectionTestUtils.setField(controller, "simulationService", simulationService);
        ReflectionTestUtils.setField(controller, "vitalDataGenerator", vitalDataGenerator);
        ReflectionTestUtils.setField(controller, "kafkaProducer", kafkaProducer);
        ReflectionTestUtils.setField(controller, "simulationConfig", simulationConfig);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper().findAndRegisterModules()))
            .build();
    }

    @Test
    void startSimulationReturnsAcceptedResult() throws Exception {
        when(simulationService.startSimulation(any())).thenReturn(
            new SimulatorController.SimulationResult(
                "sim-123",
                "STARTED",
                "Simulation started successfully",
                LocalDateTime.of(2026, 4, 23, 10, 0)
            )
        );

        mockMvc.perform(post("/api/simulator/start")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "patientCount": 3,
                      "intervalSeconds": 15,
                      "vitalTypes": ["HEART_RATE"],
                      "generateAnomalies": false,
                      "anomalyProbability": 0.0,
                      "sendToKafka": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.simulationId").value("sim-123"))
            .andExpect(jsonPath("$.status").value("STARTED"));

        verify(simulationService, times(1)).startSimulation(any());
    }

    @Test
    void generateSingleVitalReturnsGeneratorOutput() throws Exception {
        VitalDataGenerator.VitalReading reading = new VitalDataGenerator.VitalReading(
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
            eq("patient-1"),
            eq(VitalDataGenerator.HEART_RATE),
            isNull(),
            any()
        )).thenReturn(reading);

        mockMvc.perform(post("/api/simulator/generate/single")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "patientId": "patient-1",
                      "vitalType": "HEART_RATE",
                      "sendToKafka": false
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.patientId").value("patient-1"))
            .andExpect(jsonPath("$.vitalType").value("HEART_RATE"))
            .andExpect(jsonPath("$.value").value(88));

        verify(vitalDataGenerator, times(1)).generateVitalReading(
            eq("patient-1"),
            eq(VitalDataGenerator.HEART_RATE),
            isNull(),
            any()
        );
    }

    @Test
    void healthCheckReportsUpWhenDependenciesAreHealthy() throws Exception {
        when(simulationService.isHealthy()).thenReturn(true);
        when(kafkaProducer.isHealthy()).thenReturn(true);

        mockMvc.perform(get("/api/simulator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("vital-simulator"))
            .andExpect(jsonPath("$.simulationService").value("UP"))
            .andExpect(jsonPath("$.kafkaProducer").value("UP"));
    }
}
