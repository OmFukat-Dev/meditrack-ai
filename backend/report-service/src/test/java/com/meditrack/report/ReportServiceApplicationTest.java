package com.meditrack.report;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.kafka.annotation.EnableKafka;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReportServiceApplicationTest {

    @Test
    void applicationHasExpectedBootAnnotations() {
        assertNotNull(AnnotationUtils.findAnnotation(ReportServiceApplication.class, SpringBootApplication.class));
        assertNotNull(AnnotationUtils.findAnnotation(ReportServiceApplication.class, EnableDiscoveryClient.class));
        assertNotNull(AnnotationUtils.findAnnotation(ReportServiceApplication.class, EnableJpaAuditing.class));
        assertNotNull(AnnotationUtils.findAnnotation(ReportServiceApplication.class, EnableKafka.class));
    }
}
