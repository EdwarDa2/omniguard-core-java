package com.omniguard.adapter.in.rest.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniguard.adapter.in.rest.dto.response.DashboardStatsResponseDto;
import com.omniguard.adapter.out.persistence.entity.AlertJpaEntity;
import com.omniguard.adapter.out.persistence.repository.SpringDataAlertRepository;
import com.omniguard.adapter.out.persistence.repository.SpringDataCveRecordRepository;
import com.omniguard.adapter.out.persistence.repository.SpringDataServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard SOC", description = "Endpoints de resumen ejecutivo y métricas consolidadas para el Blue Team")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.of("UTC"));

    private final SpringDataAlertRepository alertRepository;
    private final SpringDataServiceRepository serviceRepository;
    private final SpringDataCveRecordRepository cveRecordRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public DashboardController(
            SpringDataAlertRepository alertRepository,
            SpringDataServiceRepository serviceRepository,
            SpringDataCveRecordRepository cveRecordRepository,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.alertRepository = alertRepository;
        this.serviceRepository = serviceRepository;
        this.cveRecordRepository = cveRecordRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/stats")
    @Operation(summary = "Métricas consolidadas del Dashboard", description = "Consolida los 4 KPIs principales, telemetría de tráfico y alertas recientes")
    public ResponseEntity<DashboardStatsResponseDto> getDashboardStats(
            @org.springframework.web.bind.annotation.RequestParam(name = "sensor_id", required = false) String sensorId,
            @org.springframework.web.bind.annotation.RequestParam(name = "since", required = false) String sinceIso
    ) {
        // Filtrado temporal para ciclo de vida de alertas por sesión activa (Opción C)
        Instant sessionSince = null;
        if (sinceIso != null && !sinceIso.isBlank()) {
            try {
                sessionSince = Instant.parse(sinceIso);
            } catch (Exception e) {
                log.warn("Formato ISO inválido en parámetro since: {}", sinceIso);
            }
        }

        // Determinar claves de Redis (segmentadas por sensor_id si está presente)
        String targetLatestKey = "metrics:traffic:latest";
        String targetHistoryKey = "metrics:traffic:history";
        String activeSensorId = (sensorId != null && !sensorId.isBlank()) ? sensorId : "default-host";

        if (sensorId != null && !sensorId.isBlank()) {
            String sanitizedSensor = sensorId.replaceAll("[^a-zA-Z0-9_-]", "_");
            String specificKey = "metrics:traffic:" + sanitizedSensor + ":latest";
            if (Boolean.TRUE.equals(redisTemplate.hasKey(specificKey))) {
                targetLatestKey = specificKey;
                targetHistoryKey = "metrics:traffic:" + sanitizedSensor + ":history";
                activeSensorId = sensorId;
            }
        }

        // 1. Actividad de tráfico y auditoría en tiempo real (consultada desde Redis)
        long currentPacketsPerSec = 0L;
        double bandwidthMbps = 0.0;
        String sensorStatus = "INACTIVE";
        long openPorts = 0L;

        try {
            String trafficJson = redisTemplate.opsForValue().get(targetLatestKey);
            if (trafficJson != null && !trafficJson.isBlank()) {
                JsonNode node = objectMapper.readTree(trafficJson);
                currentPacketsPerSec = node.path("packets_per_sec").asLong(0L);
                bandwidthMbps = node.path("bandwidth_mbps").asDouble(0.0);
                sensorStatus = node.path("sensor_status").asText("");
                if (node.has("open_ports")) {
                    openPorts = node.path("open_ports").asLong(0L);
                }
                if (node.has("sensor_id")) {
                    activeSensorId = node.path("sensor_id").asText(activeSensorId);
                }
            }
        } catch (Exception e) {
            log.warn("Error leyendo telemetría en tiempo real desde Redis: {}", e.getMessage());
        }

        // Si metrics:traffic:latest no reporta estado o está vacío, consultar sensor:desired_state
        if (sensorStatus == null || sensorStatus.isBlank() || "INACTIVE".equalsIgnoreCase(sensorStatus)) {
            String desired = redisTemplate.opsForValue().get("sensor:desired_state");
            if (desired != null && !desired.isBlank()) {
                sensorStatus = desired;
            } else {
                sensorStatus = "INACTIVE";
            }
        }

        // 2. Calcular KPIs (si hay sesión activa, limitar activeThreats a las surgidas desde el inicio de sesión)
        long activeThreats = (sessionSince != null)
                ? alertRepository.countActiveSince(sessionSince)
                : alertRepository.countActive();
        long vulnerableAssets = cveRecordRepository.countVulnerableAssets();
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
        long alerts24h = alertRepository.countRecent(since24h);

        String openPortsSeverity = openPorts > 15 ? "CRITICAL" : (openPorts > 5 ? "WARNING" : "NORMAL");

        DashboardStatsResponseDto.KpisDto kpis = new DashboardStatsResponseDto.KpisDto(
                new DashboardStatsResponseDto.KpiItemDto("Open Ports", openPorts, openPortsSeverity),
                new DashboardStatsResponseDto.KpiItemDto("Active Threats", activeThreats, activeThreats > 0 ? "HIGH" : "NORMAL"),
                new DashboardStatsResponseDto.KpiItemDto("Vulnerable Assets", vulnerableAssets, vulnerableAssets > 0 ? "WARNING" : "NORMAL"),
                new DashboardStatsResponseDto.KpiItemDto("Alerts (24h)", alerts24h, "NORMAL")
        );

        // 3. Alertas recientes (filtradas por sesión activa si se especificó since; en caso contrario, últimas 5 globales)
        List<AlertJpaEntity> recentAlertEntities = (sessionSince != null)
                ? alertRepository.findRecentSince(sessionSince, PageRequest.of(0, 5))
                : alertRepository.findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "detectedAt"))).getContent();

        List<DashboardStatsResponseDto.RecentAlertDto> recentAlerts = recentAlertEntities.stream()
                .map(a -> new DashboardStatsResponseDto.RecentAlertDto(
                        String.valueOf(a.getId()),
                        TIME_FORMATTER.format(a.getDetectedAt()),
                        a.getSourceIp(),
                        a.getTitle() != null ? a.getTitle() : a.getRule(),
                        mapSeverityToFrontend(a.getSeverity()),
                        a.getDetectedAt() != null ? a.getDetectedAt().toString() : Instant.now().toString()
                ))
                .toList();

        // 4. Serie temporal verídica: devolver datos únicamente cuando exista una fuente histórica real en Redis; mientras tanto, lista vacía
        List<DashboardStatsResponseDto.TimeSeriesPointDto> timeSeries = java.util.Collections.emptyList();
        try {
            String historyJson = redisTemplate.opsForValue().get(targetHistoryKey);
            if (historyJson != null && !historyJson.isBlank()) {
                JsonNode historyNode = objectMapper.readTree(historyJson);
                if (historyNode.isArray() && !historyNode.isEmpty()) {
                    List<DashboardStatsResponseDto.TimeSeriesPointDto> points = new ArrayList<>();
                    for (JsonNode item : historyNode) {
                        points.add(new DashboardStatsResponseDto.TimeSeriesPointDto(
                                item.path("time").asText(""),
                                item.path("inbound").asLong(0L),
                                item.path("outbound").asLong(0L)
                        ));
                    }
                    timeSeries = points;
                }
            }
        } catch (Exception e) {
            log.debug("No hay serie temporal histórica disponible en Redis: {}", e.getMessage());
        }

        DashboardStatsResponseDto.TrafficActivityDto trafficActivity =
                new DashboardStatsResponseDto.TrafficActivityDto(
                        currentPacketsPerSec,
                        bandwidthMbps,
                        sensorStatus,
                        timeSeries,
                        activeSensorId
                );

        return ResponseEntity.ok(new DashboardStatsResponseDto(kpis, trafficActivity, recentAlerts));
    }

    private String mapSeverityToFrontend(String severity) {
        if (severity == null) return "NORMAL";
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> "CRITICAL";
            case "HIGH" -> "HIGH";
            case "MEDIUM" -> "WARNING";
            case "LOW", "NONE" -> "NORMAL";
            case "INFO" -> "INFO";
            default -> "NORMAL";
        };
    }
}
