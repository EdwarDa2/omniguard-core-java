package com.omniguard.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class BusinessMetrics {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<String, Counter> alertCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> authCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> apiTimers = new ConcurrentHashMap<>();
    private final AtomicLong activeSseConnections = new AtomicLong(0);
    private final AtomicLong activeUsers = new AtomicLong(0);

    // ============================================================
    // Alertas
    // ============================================================
    public void incrementAlertCreated(String severity, String threatType) {
        String key = "alerts.created." + severity.toLowerCase() + "." + threatType.toLowerCase();
        Counter counter = alertCounters.computeIfAbsent(key, k ->
                Counter.builder("omniguard.alerts.created")
                        .tag("severity", severity)
                        .tag("threat_type", threatType)
                        .register(registry)
        );
        counter.increment();
    }

    public void incrementAlertUpdated(String status) {
        Counter.builder("omniguard.alerts.updated")
                .tag("status", status)
                .register(registry)
                .increment();
    }

    // ============================================================
    // Autenticación
    // ============================================================
    public void incrementAuthLogin(boolean success) {
        Counter.builder("omniguard.auth.login")
                .tag("result", success ? "success" : "failure")
                .register(registry)
                .increment();
    }

    public void incrementAuthRefresh(boolean success) {
        Counter.builder("omniguard.auth.refresh")
                .tag("result", success ? "success" : "failure")
                .register(registry)
                .increment();
    }

    public void incrementAuthLogout() {
        Counter.builder("omniguard.auth.logout").register(registry).increment();
    }

    public void incrementTokenValidationFailed(String reason) {
        Counter.builder("omniguard.auth.token_validation_failed")
                .tag("reason", reason)
                .register(registry)
                .increment();
    }

    // ============================================================
    // API Timing
    // ============================================================
    public Timer.Sample startApiTimer() {
        return Timer.start(registry);
    }

    public void recordApiCall(Timer.Sample sample, String endpoint, String method, int status) {
        Timer timer = apiTimers.computeIfAbsent(endpoint + ":" + method, k ->
                Timer.builder("omniguard.api.request.duration")
                        .tag("endpoint", endpoint)
                        .tag("method", method)
                        .register(registry)
        );
        sample.stop(timer);

        // Contador de status
        Counter.builder("omniguard.api.request.status")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .tag("status", String.valueOf(status))
                .register(registry)
                .increment();
    }

    // ============================================================
    // Conexiones SSE
    // ============================================================
    public void incrementSseConnections() {
        activeSseConnections.incrementAndGet();
        Gauge.builder("omniguard.sse.connections.active", activeSseConnections, AtomicLong::get)
                .description("Active SSE connections")
                .register(registry);
    }

    public void decrementSseConnections() {
        activeSseConnections.decrementAndGet();
    }

    public long getActiveSseConnections() {
        return activeSseConnections.get();
    }

    // ============================================================
    // Usuarios activos
    // ============================================================
    public void setActiveUsers(long count) {
        activeUsers.set(count);
        Gauge.builder("omniguard.users.active", activeUsers, AtomicLong::get)
                .description("Active users in last 5 minutes")
                .register(registry);
    }

    // ============================================================
    // Assets y Vulnerabilidades
    // ============================================================
    public void recordAssetsCount(long count) {
        Gauge.builder("omniguard.assets.total", count, Number::longValue)
                .description("Total assets in inventory")
                .register(registry);
    }

    public void recordVulnerabilitiesCount(String severity, long count) {
        Gauge.builder("omniguard.vulnerabilities.by_severity", count, Number::longValue)
                .tag("severity", severity)
                .description("Vulnerabilities by severity")
                .register(registry);
    }
}