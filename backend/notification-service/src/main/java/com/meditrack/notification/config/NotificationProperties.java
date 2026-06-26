package com.meditrack.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meditrack.notification")
public class NotificationProperties {

    private final Kafka kafka = new Kafka();
    private final Email email = new Email();
    private final Sms sms = new Sms();
    private final Push push = new Push();
    private final Webhook webhook = new Webhook();
    private final Retry retry = new Retry();
    private final RateLimiting rateLimiting = new RateLimiting();

    public Kafka getKafka() {
        return kafka;
    }

    public Email getEmail() {
        return email;
    }

    public Sms getSms() {
        return sms;
    }

    public Push getPush() {
        return push;
    }

    public Webhook getWebhook() {
        return webhook;
    }

    public Retry getRetry() {
        return retry;
    }

    public RateLimiting getRateLimiting() {
        return rateLimiting;
    }

    public static class Kafka {
        private String topic = "notification-requests";

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }

    public static class Email {
        private boolean simulated = true;
        private String defaultFrom = "meditrack@hospital.com";
        private String defaultReplyTo = "support@hospital.com";

        public boolean isSimulated() {
            return simulated;
        }

        public void setSimulated(boolean simulated) {
            this.simulated = simulated;
        }

        public String getDefaultFrom() {
            return defaultFrom;
        }

        public void setDefaultFrom(String defaultFrom) {
            this.defaultFrom = defaultFrom;
        }

        public String getDefaultReplyTo() {
            return defaultReplyTo;
        }

        public void setDefaultReplyTo(String defaultReplyTo) {
            this.defaultReplyTo = defaultReplyTo;
        }
    }

    public static class Sms {
        private boolean simulated = true;
        private String provider = "SIMULATED";
        private String fromNumber = "+1234567890";
        private String endpoint;
        private String apiKey;
        private String apiSecret;

        public boolean isSimulated() {
            return simulated;
        }

        public void setSimulated(boolean simulated) {
            this.simulated = simulated;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getFromNumber() {
            return fromNumber;
        }

        public void setFromNumber(String fromNumber) {
            this.fromNumber = fromNumber;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }
    }

    public static class Push {
        private boolean simulated = true;
        private String provider = "SIMULATED";
        private String endpoint;
        private String apiKey;
        private String fcmServerKey;

        public boolean isSimulated() {
            return simulated;
        }

        public void setSimulated(boolean simulated) {
            this.simulated = simulated;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getFcmServerKey() {
            return fcmServerKey;
        }

        public void setFcmServerKey(String fcmServerKey) {
            this.fcmServerKey = fcmServerKey;
        }
    }

    public static class Webhook {
        private boolean simulated = true;
        private String provider = "SIMULATED";
        private String signatureHeader = "X-Meditrack-Signature";
        private String secretKey = "test-key";
        private int timeoutSeconds = 30;

        public boolean isSimulated() {
            return simulated;
        }

        public void setSimulated(boolean simulated) {
            this.simulated = simulated;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getSignatureHeader() {
            return signatureHeader;
        }

        public void setSignatureHeader(String signatureHeader) {
            this.signatureHeader = signatureHeader;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Retry {
        private int maxAttempts = 3;
        private int delaySeconds = 60;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getDelaySeconds() {
            return delaySeconds;
        }

        public void setDelaySeconds(int delaySeconds) {
            this.delaySeconds = delaySeconds;
        }
    }

    public static class RateLimiting {
        private int maxPerHour = 100;
        private int maxPerDay = 1000;

        public int getMaxPerHour() {
            return maxPerHour;
        }

        public void setMaxPerHour(int maxPerHour) {
            this.maxPerHour = maxPerHour;
        }

        public int getMaxPerDay() {
            return maxPerDay;
        }

        public void setMaxPerDay(int maxPerDay) {
            this.maxPerDay = maxPerDay;
        }
    }
}
