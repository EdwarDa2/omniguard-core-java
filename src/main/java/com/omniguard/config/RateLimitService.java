package com.omniguard.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ProxyManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties properties;
    private final boolean enabled;

    private final Map<String, Bucket> localBuckets = new ConcurrentHashMap<>();

    public RateLimitResult tryConsume(String ruleKey, String identifier, int tokens) {
        if (!enabled) {
            return RateLimitResult.allowed();
        }

        String bucketKey = ruleKey + ":" + identifier;
        Bucket bucket = getOrCreateBucket(ruleKey, bucketKey);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(tokens);
        if (probe.isConsumed()) {
            return RateLimitResult.allowed(probe.getRemainingTokens(), probe.getNanosToWaitForRefill());
        } else {
            long waitSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            return RateLimitResult.denied(waitSeconds, probe.getRemainingTokens());
        }
    }

    private Bucket getOrCreateBucket(String ruleKey, String bucketKey) {
        RateLimitProperties.RateLimitRule rule = properties.getRules().get(ruleKey);
        if (rule != null && rule.distributed() && proxyManager != null) {
            return proxyManager.builder().build(bucketKey);
        } else {
            return localBuckets.computeIfAbsent(bucketKey, k -> buildLocalBucket(ruleKey));
        }
    }

    private Bucket buildLocalBucket(String ruleKey) {
        RateLimitProperties.RateLimitRule rule = properties.getRules().getOrDefault(ruleKey, properties.getRules().get("api:default"));
        return Bucket.builder()
                .addLimit(Bandwidth.classic(rule.capacity(), Refill.greedy(rule.refillTokens(), rule.refillPeriod())))
                .build();
    }

    public record RateLimitResult(
            boolean allowed,
            long remainingTokens,
            long retryAfterSeconds
    ) {
        public static RateLimitResult allowed() {
            return new RateLimitResult(true, -1, 0);
        }

        public static RateLimitResult allowed(long remainingTokens, long nanosToWait) {
            return new RateLimitResult(true, remainingTokens, 0);
        }

        public static RateLimitResult denied(long retryAfterSeconds, long remainingTokens) {
            return new RateLimitResult(false, remainingTokens, retryAfterSeconds);
        }
    }
}