package com.meditrack.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time vital updates, alerts, and predictions.
 * Enables STOMP messaging over WebSocket to push vitals to department-scoped dashboards.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable the built-in message broker with /topic for broadcasting
        config.enableSimpleBroker(
            "/topic/doctor-vitals",
            "/topic/doctor-predictions",
            "/topic/doctor-alerts",
            "/topic/nurse-vitals",
            "/topic/nurse-alerts",
            "/topic/admin-vitals",
            "/topic/admin-predictions",
            "/topic/admin-alerts",
            "/topic/alerts"
        );
        
        // Application destination prefix for client-to-server messaging
        config.setApplicationDestinationPrefixes("/app");
        
        // User destination prefix for one-to-one messaging
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoints for vital updates and alerts
        registry.addEndpoint("/ws-alerts")
            .setAllowedOriginPatterns(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:8090",
                "http://127.0.0.1:3000"
            )
            .withSockJS();
            
        registry.addEndpoint("/ws-vitals")
            .setAllowedOriginPatterns(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:8090",
                "http://127.0.0.1:3000"
            )
            .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Can add interceptors for auth/subscription validation here if needed
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        // Configure outbound channel if needed
    }
}
