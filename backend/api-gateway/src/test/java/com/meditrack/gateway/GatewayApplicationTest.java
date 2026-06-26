package com.meditrack.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.annotation.AnnotationUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class GatewayApplicationTest {

    @Test
    void applicationHasExpectedBootAnnotations() {
        assertNotNull(AnnotationUtils.findAnnotation(GatewayApplication.class, SpringBootApplication.class));
        assertNotNull(AnnotationUtils.findAnnotation(GatewayApplication.class, EnableDiscoveryClient.class));
    }
}
