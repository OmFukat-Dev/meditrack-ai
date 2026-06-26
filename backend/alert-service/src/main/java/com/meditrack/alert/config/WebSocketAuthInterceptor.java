package com.meditrack.alert.config;

import com.meditrack.security.MeditrackJwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Locale;
import java.util.Map;

@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final MeditrackJwtUtil jwtUtil;

    public WebSocketAuthInterceptor(MeditrackJwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean beforeHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            logger.warn("WebSocket connection rejected: missing token");
            return false;
        }

        if (!jwtUtil.validateToken(token)) {
            logger.warn("WebSocket connection rejected: invalid token");
            return false;
        }

        String role = jwtUtil.extractRole(token);
        String department = jwtUtil.extractDepartment(token);
        String email = jwtUtil.extractUsername(token);

        if (role == null || email == null) {
            logger.warn("WebSocket connection rejected: missing role or email in token");
            return false;
        }

        attributes.put("userRole", role.trim().toUpperCase(Locale.ROOT));
        attributes.put("userDepartment", department);
        attributes.put("userEmail", email);
        return true;
    }

    @Override
    public void afterHandshake(
        ServerHttpRequest request,
        ServerHttpResponse response,
        WebSocketHandler wsHandler,
        Exception exception
    ) {
        // no-op
    }
}
