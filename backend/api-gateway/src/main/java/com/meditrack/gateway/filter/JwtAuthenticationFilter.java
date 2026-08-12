package com.meditrack.gateway.filter;

import com.meditrack.gateway.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${meditrack.security.auth.enabled:true}")
    private boolean authEnabled;
    @Value("${meditrack.security.auth.test-bypass:false}")
    private boolean testBypass;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Skip authentication for public endpoints only
            if (path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/refresh")
                || path.equals("/actuator/health")
                || path.startsWith("/eureka")
                || path.startsWith("/swagger")) {
                return chain.filter(exchange);
            }

            // TEMPORARY: Disable authentication for testing only when explicit test-bypass is enabled
            if (!authEnabled) {
                if (testBypass) {
                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(exchange.getRequest().mutate()
                                    .header("X-User-Email", "test@example.com")
                                    .header("X-User-Role", "admin")
                                    .header("X-User-Department", "ICU")
                                    .header("X-User-Id", "1")
                                    .header("X-User-Display-Name", "Test User")
                                    .build())
                            .build();
                    return chain.filter(mutatedExchange);
                } else {
                    return onError(exchange, "Authentication disabled by configuration", HttpStatus.FORBIDDEN);
                }
            }

            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing authorization header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid authorization header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            if (!jwtUtil.validateToken(token)) {
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

            try {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractClaim(token, claims -> claims.get("role", String.class));
                String department = jwtUtil.extractClaim(token, claims -> claims.get("department", String.class));
                String id = jwtUtil.extractClaim(token, claims -> claims.get("id", String.class));
                String displayName = jwtUtil.extractClaim(token, claims -> claims.get("displayName", String.class));

                // Mutate request to pass claims as headers to downstream microservices
                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(exchange.getRequest().mutate()
                                .header("X-User-Email", username)
                                .header("X-User-Role", role)
                                .header("X-User-Department", department)
                                .header("X-User-Id", id)
                                .header("X-User-Display-Name", displayName != null ? displayName : username)
                                .build())
                        .build();

                return chain.filter(mutatedExchange);
            } catch (Exception e) {
                return onError(exchange, "Failed to parse JWT claims", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        exchange.getResponse().setStatusCode(httpStatus);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // configuration properties
    }
}
