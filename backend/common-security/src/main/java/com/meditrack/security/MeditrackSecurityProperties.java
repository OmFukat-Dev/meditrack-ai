package com.meditrack.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meditrack.security")
public class MeditrackSecurityProperties {

    private Auth auth = new Auth();
    private Jwt jwt = new Jwt();
    private ServiceJwt serviceJwt = new ServiceJwt();

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public ServiceJwt getServiceJwt() {
        return serviceJwt;
    }

    public void setServiceJwt(ServiceJwt serviceJwt) {
        this.serviceJwt = serviceJwt;
    }

    public static class Auth {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Jwt {
        private String secret = "meditrack-ai-gateway-signing-key-2026-local-development";
        private long expirationMs = 86400000L;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }

    public static class ServiceJwt {
        private String secret = "meditrack-ai-service-signing-key-2026-local-development";
        private long expirationMs = 3600000L;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }
}
