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
@org.springframework.boot.context.properties.EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();
    private final RateLimitProperties properties;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${bucket4j.enabled:true}")
    private boolean enabled;

    public RateLimitConfig(RateLimitProperties properties) {
        this.properties = properties;
    }

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
        return new RateLimitService(proxyManager, enabled, properties);
    }

    public Map<String, RateLimitProperties.RateLimitRule> getRules() {
        return properties.getRules();
    }

    public boolean isEnabled() {
        return enabled;
    }
}