package com.meditrack.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public class RequestAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
        "/actuator/health",
        "/actuator/info"
    );

    private final MeditrackSecurityProperties properties;
    private final MeditrackJwtUtil jwtUtil;
    private final ServiceJwtUtil serviceJwtUtil;
    private final String serviceName;

    public RequestAuthenticationFilter(
        MeditrackSecurityProperties properties,
        MeditrackJwtUtil jwtUtil,
        ServiceJwtUtil serviceJwtUtil,
        String serviceName
    ) {
        this.properties = properties;
        this.jwtUtil = jwtUtil;
        this.serviceJwtUtil = serviceJwtUtil;
        this.serviceName = serviceName;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        if (!properties.getAuth().isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (serviceJwtUtil.validateToken(token)) {
                String target = serviceJwtUtil.extractTargetService(token);
                if (target != null && !target.equalsIgnoreCase(serviceName) && !"*".equals(target)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Service JWT target mismatch");
                    return;
                }
                filterChain.doFilter(request, response);
                return;
            }
            if (jwtUtil.validateToken(token)) {
                AuthenticatedRequestWrapper wrapped = new AuthenticatedRequestWrapper(request);
                wrapped.setHeader("X-User-Email", jwtUtil.extractUsername(token));
                wrapped.setHeader("X-User-Role", normalizeRole(jwtUtil.extractRole(token)));
                wrapped.setHeader("X-User-Department", jwtUtil.extractDepartment(token));
                wrapped.setHeader("X-User-Id", jwtUtil.extractUserId(token));
                String displayName = jwtUtil.extractDisplayName(token);
                wrapped.setHeader("X-User-Display-Name", displayName != null ? displayName : jwtUtil.extractUsername(token));
                filterChain.doFilter(wrapped, response);
                return;
            }
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        String role = trimToNull(request.getHeader("X-User-Role"));
        String email = trimToNull(request.getHeader("X-User-Email"));
        if (role != null && email != null) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        return role.trim().toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
