package com.meditrack.gateway.controller;

import com.meditrack.gateway.dto.AccessPrincipalResponse;
import com.meditrack.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final WebClient webClient;
    private final String patientServiceBaseUrl;

    public AuthController(
        JwtUtil jwtUtil,
        WebClient.Builder webClientBuilder,
        @Value("${meditrack.services.patient-base-url:http://localhost:8082}") String patientServiceBaseUrl
    ) {
        this.jwtUtil = jwtUtil;
        this.webClient = webClientBuilder.build();
        this.patientServiceBaseUrl = patientServiceBaseUrl;
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<Map<String, String>>> login(@RequestBody Map<String, String> request) {
        return webClient.post()
            .uri(patientServiceBaseUrl + "/api/access/login")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(AccessPrincipalResponse.class)
            .map(principal -> {
                String role = normalizeRole(principal.getRole());
                String token = jwtUtil.generateToken(
                    principal.getEmail(),
                    role.toUpperCase(Locale.ROOT),
                    safe(principal.getDepartment()),
                    safe(principal.getId()),
                    safe(principal.getName())
                );

                Map<String, String> response = new LinkedHashMap<>();
                response.put("token", token);
                response.put("id", safe(principal.getId()));
                response.put("email", safe(principal.getEmail()));
                response.put("role", role);
                response.put("department", safe(principal.getDepartment()));
                response.put("name", safe(principal.getName()));
                response.put("bedNumber", safe(principal.getBedNumber()));
                response.put("patientIdentifier", safe(principal.getPatientIdentifier()));
                response.put("wardNumber", safe(principal.getWardNumber()));
                return ResponseEntity.ok(response);
            })
            .onErrorResume(WebClientResponseException.class, ex ->
                Mono.just(ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", messageOrDefault(ex.getResponseBodyAsString(), "Authentication failed")))))
            .onErrorResume(ex ->
                Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Authentication service unavailable"))));
    }

    private static String normalizeRole(String role) {
        return role == null ? "patient" : role.trim().toLowerCase(Locale.ROOT);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String messageOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
