package com.omniguard.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bucket4j.rules")
public class RateLimitProperties {

    public record RateLimitRule(
            int capacity,
            long refillTokens,
            Duration refillPeriod,
            boolean distributed
    ) {}

    private Map<String, RateLimitRule> rules = new LinkedHashMap<>(Map.of(
            "auth:login", new RateLimitRule(5, 5, Duration.ofMinutes(1), true),
            "auth:register", new RateLimitRule(3, 3, Duration.ofMinutes(1), true),
            "auth:refresh", new RateLimitRule(10, 10, Duration.ofMinutes(1), true),
            "api:default", new RateLimitRule(100, 100, Duration.ofMinutes(1), true),
            "sse:stream", new RateLimitRule(10, 10, Duration.ofMinutes(1), true)
    ));

    public Map<String, RateLimitRule> getRules() {
        return rules;
    }

    public void setRules(Map<String, RateLimitRule> rules) {
        this.rules = rules;
    }
}