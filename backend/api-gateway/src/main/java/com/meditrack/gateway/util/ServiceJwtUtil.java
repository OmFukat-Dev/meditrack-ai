package com.meditrack.gateway.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class ServiceJwtUtil {

    public static final String TOKEN_TYPE = "SERVICE";

    private final Key key;
    private final long expirationMs;

    public ServiceJwtUtil(
        @Value("${meditrack.security.service-jwt.secret:meditrack-ai-service-signing-key-2026-local-development}") String secret,
        @Value("${meditrack.security.service-jwt.expiration-ms:3600000}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String sourceService, String targetService) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", TOKEN_TYPE);
        claims.put("service", sourceService);
        claims.put("target", targetService);
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(sourceService)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(key)
            .compact();
    }
}
