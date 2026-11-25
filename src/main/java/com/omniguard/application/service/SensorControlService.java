package com.omniguard.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniguard.adapter.in.rest.sse.AlertSseService;
import com.omniguard.domain.port.in.SensorControlUseCase;
import com.omniguard.domain.port.out.SensorCommandPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SensorControlService implements SensorControlUseCase {

    private static final Logger log = LoggerFactory.getLogger(SensorControlService.class);

    private final SensorCommandPublisher sensorCommandPublisher;
    private final StringRedisTemplate redisTemplate;
    private final AlertSseService alertSseService;
    private final ObjectMapper objectMapper;

    public SensorControlService(
            SensorCommandPublisher sensorCommandPublisher,
            StringRedisTemplate redisTemplate,
            AlertSseService alertSseService,
            ObjectMapper objectMapper
    ) {
        this.sensorCommandPublisher = sensorCommandPublisher;
        this.redisTemplate = redisTemplate;
        this.alertSseService = alertSseService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String changeSensorStatus(String action) {
        String normalized = action != null ? action.trim().toUpperCase() : "STOP";
        String command;
        String resultingStatus;

        if ("START".equals(normalized) || "RESUME".equals(normalized) || "ACTIVE".equals(normalized)) {
            command = "RESUME";
            resultingStatus = "ACTIVE";
        } else {
            command = "PAUSE";
            resultingStatus = "PAUSED";
        }

        log.info("Solicitud de cambio de estado del sensor: accion='{}' -> comando='{}', estadoResultante='{}'",
                action, command, resultingStatus);

        // 1. Publicar en el bus de Redis hacia el sensor C++
        sensorCommandPublisher.publishSensorCommand(command);

        // 2. Notificar en tiempo real a clientes SSE conectados
        try {
            alertSseService.broadcastSensorStatus(resultingStatus);
        } catch (Exception e) {
            log.warn("No se pudo emitir evento SSE sensor:status: {}", e.getMessage());
        }

        return resultingStatus;
    }

    @Override
    public String getSensorStatus() {
        // Consultar métricas en Redis primero
        try {
            String trafficJson = redisTemplate.opsForValue().get("metrics:traffic:latest");
            if (trafficJson != null && !trafficJson.isBlank()) {
                JsonNode node = objectMapper.readTree(trafficJson);
                String status = node.path("sensor_status").asText("");
                if (!status.isBlank()) {
                    return status;
                }
            }
        } catch (Exception e) {
            log.debug("No se pudo leer sensor_status desde metrics:traffic:latest: {}", e.getMessage());
        }

        // Consultar estado deseado si no hay telemetría reciente
        String desired = redisTemplate.opsForValue().get("sensor:desired_state");
        if (desired != null && !desired.isBlank()) {
            return desired;
        }

        return "INACTIVE";
    }
}
