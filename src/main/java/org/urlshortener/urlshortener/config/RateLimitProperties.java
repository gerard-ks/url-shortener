package org.urlshortener.urlshortener.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitProperties {
    private boolean enabled = true;
    private EndpointLimit defaultLimit = new EndpointLimit();
    private Map<String, EndpointLimit> endpoints = new HashMap<>();

    @Getter
    @Setter
    public static class EndpointLimit {
        private int maxRequests = 10;
        private int windowSeconds = 60;
    }

    public EndpointLimit getLimitForEndpoint(String method, String path) {
        String key = method + ":" + path;

        // Recherche exacte
        if (endpoints.containsKey(key)) {
            return endpoints.get(key);
        }

        // Recherche avec wildcard
        for (Map.Entry<String, EndpointLimit> entry : endpoints.entrySet()) {
            if (matchesPattern(entry.getKey(), key)) {
                return entry.getValue();
            }
        }

        // Fallback sur default
        return defaultLimit;
    }

    private boolean matchesPattern(String pattern, String key) {
        String regex = pattern.replace("*", ".*");
        return key.matches(regex);
    }
}
