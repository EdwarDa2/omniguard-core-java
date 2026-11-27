package com.omniguard.adapter.in.redis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniguard.adapter.in.rest.dto.response.DashboardStatsResponseDto;
import com.omniguard.adapter.in.rest.sse.AlertSseService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class TrafficMetricsRedisSubscriber implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(TrafficMetricsRedisSubscriber.class);

    private final AlertSseService alertSseService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TrafficMetricsRedisSubscriber(
            AlertSseService alertSseService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.alertSseService = alertSseService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (alertSseService.getActiveClientsCount() == 0) {
            return;
        }

        try {
            String payload = new String(message.getBody());
            JsonNode node = objectMapper.readTree(payload);

            long packetsPerSec = node.path("packets_per_sec").asLong(0L);
            double bandwidthMbps = node.path("bandwidth_mbps").asDouble(0.0);
            String sensorStatus = node.path("sensor_status").asText("ACTIVE");
            String sensorId = node.path("sensor_id").asText("default-host");

            // Leer serie temporal acumulada en Redis (por sensor o global)
            List<DashboardStatsResponseDto.TimeSeriesPointDto> timeSeries = Collections.emptyList();
            String sanitizedSensor = sensorId.replaceAll("[^a-zA-Z0-9_-]", "_");
            String historyKey = "metrics:traffic:" + sanitizedSensor + ":history";
            String historyJson = redisTemplate.opsForValue().get(historyKey);
            if (historyJson == null || historyJson.isBlank()) {
                historyJson = redisTemplate.opsForValue().get("metrics:traffic:history");
            }
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

            DashboardStatsResponseDto.TrafficActivityDto trafficActivity =
                    new DashboardStatsResponseDto.TrafficActivityDto(
                            packetsPerSec,
                            bandwidthMbps,
                            sensorStatus,
                            timeSeries,
                            sensorId
                    );

            alertSseService.broadcastTrafficActivity(trafficActivity);
        } catch (Exception e) {
            log.warn("Error procesando telemetría de tráfico desde Redis: {}", e.getMessage());
        }
    }
}
