package com.omniguard.adapter.in.rest.sse;

import com.omniguard.adapter.in.rest.dto.alert.AlertResponse;
import com.omniguard.domain.event.AlertCreatedEvent;
import com.omniguard.domain.port.in.AlertUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseAlertService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final AlertRestMapper alertRestMapper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Heartbeat cada 30 segundos para mantener conexiones vivas
    private static final long HEARTBEAT_INTERVAL_MS = 30_000;

    @EventListener
    public void onAlertCreated(AlertCreatedEvent event) {
        AlertResponse response = alertRestMapper.toResponse(event.getAlert());
        broadcast(response);
    }

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(-1L); // Sin timeout

        emitters.put(userId, emitter);
        log.info("Nueva suscripción SSE para usuario: {} (total: {})", userId, emitters.size());

        // Configurar callbacks
        emitter.onCompletion(() -> {
            emitters.remove(userId);
            log.info("SSE completado para usuario: {} (total: {})", userId, emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            log.info("SSE timeout para usuario: {} (total: {})", userId, emitters.size());
        });
        emitter.onError((ex) -> {
            emitters.remove(userId);
            log.warn("SSE error para usuario {}: {}", userId, ex.getMessage());
        });

        // Enviar evento de conexión inicial
        sendToEmitter(emitter, "connected", Map.of("message", "Conectado a stream de alertas", "userId", userId));

        // Programar heartbeat
        scheduler.scheduleAtFixedRate(() -> sendHeartbeat(userId), HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);

        return emitter;
    }

    public void unsubscribe(String userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("Usuario {} desuscrito manualmente (total: {})", userId, emitters.size());
        }
    }

    private void broadcast(AlertResponse alert) {
        emitters.forEach((userId, emitter) -> sendToEmitter(emitter, "alert", alert));
    }

    private void sendHeartbeat(String userId) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            sendToEmitter(emitter, "heartbeat", Map.of("timestamp", System.currentTimeMillis()));
        }
    }

    private void sendToEmitter(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data)
                    .reconnectTime(5000));
        } catch (IOException e) {
            log.debug("Error enviando SSE a {}: {}", emitter, e.getMessage());
            emitter.completeWithError(e);
        }
    }

    public int getActiveConnections() {
        return emitters.size();
    }
}