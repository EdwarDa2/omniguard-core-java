package com.omniguard.adapter.in.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/telemetry")
@Tag(name = "Ingesta de Telemetría (M2M)", description = "Endpoints para ingesta segura de eventos de red y métricas desde sensores remotos")
public class InternalTelemetryController {

    private static final Logger log = LoggerFactory.getLogger(InternalTelemetryController.class);
    private static final String CHANNEL_NETWORK_EVENTS = "network_events";
    private static final String CHANNEL_METRICS_TRAFFIC = "metrics:traffic";
    private static final String KEY_METRICS_LATEST = "metrics:traffic:latest";
    private static final String KEY_METRICS_HISTORY = "metrics:traffic:history";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public InternalTelemetryController(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/events")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL')")
    @Operation(summary = "Ingesta de eventos de red", description = "Recibe uno o múltiples eventos de red JSON desde el sensor remoto y los publica en Redis Pub/Sub")
    public ResponseEntity<Map<String, Object>> ingestEvents(@RequestBody String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Payload vacío"));
        }

        try {
            JsonNode rootNode = objectMapper.readTree(rawPayload);
            int count = 0;
            if (rootNode.isArray()) {
                for (JsonNode eventNode : rootNode) {
                    redisTemplate.convertAndSend(CHANNEL_NETWORK_EVENTS, eventNode.toString());
                    count++;
                }
            } else {
                redisTemplate.convertAndSend(CHANNEL_NETWORK_EVENTS, rootNode.toString());
                count = 1;
            }
            return ResponseEntity.accepted().body(Map.of("status", "PUBLISHED", "events_count", count));
        } catch (Exception e) {
            log.error("Error al procesar eventos de telemetría: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "JSON inválido: " + e.getMessage()));
        }
    }

    @PostMapping("/metrics")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL')")
    @Operation(summary = "Ingesta de métricas de tráfico", description = "Recibe métricas de rendimiento del sensor remoto y actualiza las claves en Redis con TTL")
    public ResponseEntity<Map<String, Object>> ingestMetrics(@RequestBody String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Payload vacío"));
        }

        try {
            JsonNode rootNode = objectMapper.readTree(rawPayload);
            String latestMetrics;
            String historyMetrics = null;
            String sensorId = "default-host";

            if (rootNode.has("metrics")) {
                JsonNode metricsNode = rootNode.path("metrics");
                latestMetrics = metricsNode.toString();
                if (metricsNode.has("sensor_id")) {
                    sensorId = metricsNode.path("sensor_id").asText("default-host");
                }
                if (rootNode.has("history")) {
                    historyMetrics = rootNode.path("history").toString();
                }
            } else {
                latestMetrics = rootNode.toString();
                if (rootNode.has("sensor_id")) {
                    sensorId = rootNode.path("sensor_id").asText("default-host");
                }
            }

            // Sanitizar identificador de sensor
            String sanitizedSensor = sensorId.replaceAll("[^a-zA-Z0-9_-]", "_");

            // 1. Almacenar por sensor específico con TTL de 25 segundos
            String sensorLatestKey = "metrics:traffic:" + sanitizedSensor + ":latest";
            String sensorHistoryKey = "metrics:traffic:" + sanitizedSensor + ":history";
            redisTemplate.opsForValue().set(sensorLatestKey, latestMetrics, Duration.ofSeconds(25));

            // 2. Almacenar también en clave global para fallback
            redisTemplate.opsForValue().set(KEY_METRICS_LATEST, latestMetrics, Duration.ofSeconds(25));

            // Si viene historial, almacenarlo por sensor y global
            if (historyMetrics != null && !historyMetrics.isBlank()) {
                redisTemplate.opsForValue().set(sensorHistoryKey, historyMetrics, Duration.ofSeconds(60));
                redisTemplate.opsForValue().set(KEY_METRICS_HISTORY, historyMetrics, Duration.ofSeconds(60));
            }

            // Publicar en canal de Redis para que el suscriptor SSE notifique al Frontend
            redisTemplate.convertAndSend(CHANNEL_METRICS_TRAFFIC, latestMetrics);

            return ResponseEntity.ok(Map.of("status", "METRICS_UPDATED"));
        } catch (Exception e) {
            log.error("Error al procesar métricas de telemetría: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "JSON inválido: " + e.getMessage()));
        }
    }
}
