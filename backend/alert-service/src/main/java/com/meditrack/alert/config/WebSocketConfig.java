package com.meditrack.alert.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final StompSubscriptionAuthInterceptor stompSubscriptionAuthInterceptor;

    public WebSocketConfig(
        WebSocketAuthInterceptor webSocketAuthInterceptor,
        StompSubscriptionAuthInterceptor stompSubscriptionAuthInterceptor
    ) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.stompSubscriptionAuthInterceptor = stompSubscriptionAuthInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-alerts")
            .addInterceptors(webSocketAuthInterceptor)
            .setAllowedOriginPatterns(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:5173",
                "http://localhost:8090"
            )
            .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompSubscriptionAuthInterceptor);
    }
}
