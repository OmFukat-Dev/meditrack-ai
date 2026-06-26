package com.meditrack.alert.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StompSubscriptionAuthInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(StompSubscriptionAuthInterceptor.class);
    private static final Pattern DEPARTMENT_TOPIC = Pattern.compile(
        "^/topic/(?:doctor|nurse)-(?:vitals|predictions|alerts)/department/([a-z0-9_-]+)$|^/topic/alerts/department/([a-z0-9_-]+)$"
    );

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            throw new IllegalArgumentException("Unauthorized subscription: no session");
        }

        String userRole = stringValue(sessionAttributes.get("userRole"));
        String userDepartment = stringValue(sessionAttributes.get("userDepartment"));
        String destination = accessor.getDestination();

        if (destination == null) {
            throw new IllegalArgumentException("Unauthorized subscription: missing destination");
        }

        if (isAdminTopic(destination)) {
            if (!"ADMIN".equalsIgnoreCase(userRole)) {
                logger.warn("Blocked admin topic subscription for role {}", userRole);
                throw new IllegalArgumentException("Forbidden: admin topics require ADMIN role");
            }
            return message;
        }

        Matcher matcher = DEPARTMENT_TOPIC.matcher(destination);
        if (matcher.matches()) {
            String topicDepartment = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (userDepartment == null || userDepartment.isBlank()) {
                throw new IllegalArgumentException("Forbidden: department required for topic subscription");
            }
            String normalizedUserDept = normalizeDepartment(userDepartment);
            if (!normalizedUserDept.equals(topicDepartment)) {
                logger.warn("Blocked cross-department subscription: user={}, topic={}", normalizedUserDept, topicDepartment);
                throw new IllegalArgumentException("Forbidden: cross-department topic subscription denied");
            }
            return message;
        }

        logger.warn("Blocked subscription to unrecognized topic: {}", destination);
        throw new IllegalArgumentException("Forbidden: unrecognized topic");
    }

    private static boolean isAdminTopic(String destination) {
        return destination.startsWith("/topic/admin-");
    }

    private static String normalizeDepartment(String department) {
        return department.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
