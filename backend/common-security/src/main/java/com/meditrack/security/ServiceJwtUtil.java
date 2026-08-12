package com.meditrack.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class ServiceJwtUtil {

    public static final String TOKEN_TYPE = "SERVICE";

    private final Key key;
    private final long expirationMs;

    public ServiceJwtUtil(MeditrackSecurityProperties properties) {
        String secret = properties.getServiceJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Service JWT secret must be configured via meditrack.security.service-jwt.secret");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("Service JWT secret must be at least 32 bytes (256 bits) long");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = properties.getServiceJwt().getExpirationMs();
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

    public boolean validateToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return TOKEN_TYPE.equals(claims.get("type", String.class)) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractSourceService(String token) {
        return extractClaim(token, claims -> claims.get("service", String.class));
    }

    public String extractTargetService(String token) {
        return extractClaim(token, claims -> claims.get("target", String.class));
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}
