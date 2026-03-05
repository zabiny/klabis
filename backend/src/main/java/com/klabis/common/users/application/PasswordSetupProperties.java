package com.klabis.common.users.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klabis.password-setup")
public class PasswordSetupProperties {

    private Token token = new Token();
    private RateLimit rateLimit = new RateLimit();
    private String baseUrl = "https://localhost:8443";

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static class Token {
        private int expirationHours = 4;
        private String hashAlgorithm = "SHA-256";

        public int getExpirationHours() {
            return expirationHours;
        }

        public void setExpirationHours(int expirationHours) {
            this.expirationHours = expirationHours;
        }

        public String getHashAlgorithm() {
            return hashAlgorithm;
        }

        public void setHashAlgorithm(String hashAlgorithm) {
            this.hashAlgorithm = hashAlgorithm;
        }
    }

    public static class RateLimit {
        private int requests = 3;
        private long durationSeconds = 3600;

        public int getRequests() {
            return requests;
        }

        public void setRequests(int requests) {
            this.requests = requests;
        }

        public long getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(long durationSeconds) {
            this.durationSeconds = durationSeconds;
        }
    }
}
