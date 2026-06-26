package com.meditrack.patient.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebConfigTest {

    private final WebConfig webConfig = new WebConfig();

    @Test
    void customOpenApiContainsExpectedMetadata() {
        OpenAPI openAPI = webConfig.customOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("MediTrack AI Patient Service API", openAPI.getInfo().getTitle());
        assertEquals("Patient Management and Medical History API with FHIR Compliance", openAPI.getInfo().getDescription());
        assertEquals(2, openAPI.getServers().size());
        assertEquals("http://localhost:8082", openAPI.getServers().get(0).getUrl());
    }
}
