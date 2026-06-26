package com.meditrack.simulator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class VitalSimulatorApplicationTest {

    @Test
    void applicationHasExpectedBootAnnotations() {
        assertNotNull(AnnotationUtils.findAnnotation(VitalSimulatorApplication.class, SpringBootApplication.class));
        assertNotNull(AnnotationUtils.findAnnotation(VitalSimulatorApplication.class, EnableDiscoveryClient.class));
        assertNotNull(AnnotationUtils.findAnnotation(VitalSimulatorApplication.class, EnableKafka.class));
        assertNotNull(AnnotationUtils.findAnnotation(VitalSimulatorApplication.class, EnableScheduling.class));
    }
}
