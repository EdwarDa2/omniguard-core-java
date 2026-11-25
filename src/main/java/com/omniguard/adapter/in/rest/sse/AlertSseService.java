package com.omniguard.adapter.in.rest.sse;

import com.omniguard.adapter.in.rest.dto.response.AlertResponseDto;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AlertSseService {

    private static final Logger log = LoggerFactory.getLogger(AlertSseService.class);
    private static final Long SSE_TIMEOUT_MS = 180_000L; // 3 minutos

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter registerClient() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        emitter.onCompletion(() -> {
            log.debug("SSE client completed connection");
            emitters.remove(emitter);
        });

        emitter.onTimeout(() -> {
            log.debug("SSE client connection timed out");
            emitters.remove(emitter);
        });

        emitter.onError(e -> {
            log.debug("SSE client connection error: {}", e.getMessage());
            emitters.remove(emitter);
        });

        emitters.add(emitter);

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("init")
                            .data(Map.of(
                                    "status", "CONNECTED",
                                    "message", "Conexión en vivo al monitor NIDS establecida",
                                    "timestamp", Instant.now().toString()
                            ))
            );
        } catch (IOException e) {
            log.warn("Failed to send initial SSE payload: {}", e.getMessage());
            emitters.remove(emitter);
        }

        return emitter;
    }

    @Scheduled(fixedRate = 15000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("keep-alive"));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public void broadcastNewAlert(AlertResponseDto alert) {
        if (emitters.isEmpty()) {
            return;
        }

        log.info("Broadcasting new NIDS alert {} via SSE to {} active clients", alert.id(), emitters.size());
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("alert:new").data(alert));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public void broadcastAlertUpdate(Long alertId, String newStatus) {
        if (emitters.isEmpty()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "id", alertId.toString(),
                "status", newStatus,
                "updatedAt", Instant.now().toString()
        );

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("alert:update").data(payload));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public void broadcastScanStatus(String scanId, String status, String target) {
        if (emitters.isEmpty()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "scanId", scanId,
                "status", status,
                "target", target,
                "timestamp", Instant.now().toString()
        );

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("scan:status").data(payload));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public void broadcastScanCompleted(String scanId, Long assetId, String targetIp, int openPortsCount, String scannedAt) {
        if (emitters.isEmpty()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "scanId", scanId != null ? scanId : "",
                "assetId", assetId != null ? assetId : 0L,
                "targetIp", targetIp != null ? targetIp : "",
                "status", "COMPLETED",
                "openPortsCount", openPortsCount,
                "scannedAt", scannedAt != null ? scannedAt : Instant.now().toString()
        );

        log.info("Broadcasting scan:completed event via SSE for scanId={}, assetId={}, ip={} ({} open ports) to {} clients",
                scanId, assetId, targetIp, openPortsCount, emitters.size());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("scan:completed").data(payload));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }

    public int getActiveClientsCount() {
        return emitters.size();
    }
}
