package com.meditrack.simulator.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void corsConfigurationIncludesExpectedOriginsAndMethods() {
        CorsConfiguration corsConfiguration = securityConfig
            .corsConfigurationSource()
            .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/simulator/health"));

        assertNotNull(corsConfiguration);
        assertTrue(corsConfiguration.getAllowedOrigins().contains("http://localhost:3000"));
        assertTrue(corsConfiguration.getAllowedMethods().contains("GET"));
        assertTrue(corsConfiguration.getAllowedMethods().contains("POST"));
    }

    @Test
    void jwtDecoderBeanIsCreated() {
        JwtDecoder jwtDecoder = securityConfig.jwtDecoder();
        assertNotNull(jwtDecoder);
    }
}
