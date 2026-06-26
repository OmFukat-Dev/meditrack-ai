package com.meditrack.eureka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.core.annotation.AnnotationUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class EurekaServerApplicationTest {

    @Test
    void applicationHasExpectedBootAnnotations() {
        assertNotNull(AnnotationUtils.findAnnotation(EurekaServerApplication.class, SpringBootApplication.class));
        assertNotNull(AnnotationUtils.findAnnotation(EurekaServerApplication.class, EnableEurekaServer.class));
    }
}
