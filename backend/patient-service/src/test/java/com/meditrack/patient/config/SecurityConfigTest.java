package com.meditrack.patient.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void corsConfigurationIncludesExpectedOriginsAndMethods() {
        CorsConfiguration corsConfiguration = securityConfig
            .corsConfigurationSource()
            .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/fhir/patients"));

        assertNotNull(corsConfiguration);
        assertTrue(corsConfiguration.getAllowedOrigins().contains("http://localhost:8082"));
        assertTrue(corsConfiguration.getAllowedMethods().contains("GET"));
        assertTrue(corsConfiguration.getAllowedMethods().contains("POST"));
    }
}
