package com.omniguard.adapter.out.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniguard.domain.port.out.ScanCommandPublisher;
import com.omniguard.domain.vo.IpAddress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisScanCommandPublisher implements ScanCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisScanCommandPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String scanCommandsChannel;

    public RedisScanCommandPublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.redis.scan-commands-channel:scan_commands_channel}") String scanCommandsChannel
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.scanCommandsChannel = scanCommandsChannel;
    }

    @Override
    public void publishScanCommand(UUID scanId, IpAddress target, String ports) {
        Map<String, Object> command = new LinkedHashMap<>();
        command.put("scan_id", scanId.toString());
        command.put("target", target.getValue());
        command.put("ports", ports != null && !ports.isBlank() ? ports : "1-1024");

        try {
            String message = objectMapper.writeValueAsString(command);
            redisTemplate.convertAndSend(scanCommandsChannel, message);
            log.info(
                    "Published active scan command: scanId={}, target={}, ports={}, channel={}",
                    scanId,
                    target.getValue(),
                    command.get("ports"),
                    scanCommandsChannel
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize scan command for target {}", target.getValue(), e);
            throw new IllegalStateException("Failed to serialize scan command", e);
        }
    }
}
