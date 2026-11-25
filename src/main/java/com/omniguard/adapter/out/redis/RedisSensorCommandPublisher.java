package com.omniguard.adapter.out.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniguard.domain.port.out.SensorCommandPublisher;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisSensorCommandPublisher implements SensorCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisSensorCommandPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String sensorCommandsChannel;

    public RedisSensorCommandPublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.redis.sensor-commands-channel:sensor_commands_channel}") String sensorCommandsChannel
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.sensorCommandsChannel = sensorCommandsChannel;
    }

    @Override
    public void publishSensorCommand(String command) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("command", command);
        message.put("requested_at", Instant.now().toString());

        try {
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(sensorCommandsChannel, json);
            log.info("Publicado comando al sensor NIDS: command={}, canal={}", command, sensorCommandsChannel);

            // Actualizar estado deseado en Redis para persistir la intención
            String desiredStatus = "PAUSE".equalsIgnoreCase(command) ? "PAUSED" : "ACTIVE";
            redisTemplate.opsForValue().set("sensor:desired_state", desiredStatus);

            // Actualizar proactivamente metrics:traffic:latest para que /dashboard/stats responda inmediatamente
            Map<String, Object> latestMetrics = new LinkedHashMap<>();
            latestMetrics.put("packets_per_sec", 0);
            latestMetrics.put("bandwidth_mbps", 0.0);
            latestMetrics.put("sensor_status", desiredStatus);
            latestMetrics.put("inbound", 0);
            latestMetrics.put("outbound", 0);
            redisTemplate.opsForValue().set("metrics:traffic:latest", objectMapper.writeValueAsString(latestMetrics));
        } catch (JsonProcessingException e) {
            log.error("Fallo al serializar comando del sensor {}", command, e);
            throw new IllegalStateException("Error al serializar comando del sensor", e);
        }
    }
}
