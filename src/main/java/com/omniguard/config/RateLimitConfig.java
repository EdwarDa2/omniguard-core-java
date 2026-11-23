package com.omniguard.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManagerBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${bucket4j.enabled:true}")
    private boolean enabled;

    @Bean
    public ProxyManager<String> bucket4jProxyManager() {
        if (!enabled) {
            return null;
        }

        RedisClient redisClient = RedisClient.create(
                "redis://" + (redisPassword.isBlank() ? "" : ":" + redisPassword + "@") + redisHost + ":" + redisPort
        );
        StatefulRedisConnection<String, String> connection = redisClient.connect();

        return LettuceBasedProxyManager.builderFor(connection)
                .withClientSideConfig(cfg -> cfg.defaultProxyManagerConfig())
                .build();
    }

    @Bean
    public RateLimitService rateLimitService(ProxyManager<String> proxyManager) {
        return new RateLimitService(proxyManager, enabled);
    }

    // Configuración de buckets por tipo de endpoint
    public record RateLimitRule(
            String key,
            int capacity,
            long refillTokens,
            Duration refillPeriod,
            boolean distributed
    ) {}

    public static final Map<String, RateLimitRule> RULES = Map.of(
            "auth:login", new RateLimitRule("auth:login", 5, 5, java.time.Duration.ofMinutes(1), true),
            "auth:register", new RateLimitRule("auth:register", 3, 3, java.time.Duration.ofMinutes(1), true),
            "auth:refresh", new RateLimitRule("auth:refresh", 10, 10, java.time.Duration.ofMinutes(1), true),
            "api:default", new RateLimitRule("api:default", 100, 100, java.time.Duration.ofMinutes(1), true),
            "sse:stream", new RateLimitRule("sse:stream", 10, 10, java.time.Duration.ofMinutes(1), true)
    );
}