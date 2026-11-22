package com.omniguard.domain.port.in;

import com.omniguard.domain.model.Alert;
import com.omniguard.domain.vo.IpAddress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertUseCase {
    Alert create(Alert alert);

    Optional<Alert> findById(UUID id);

    List<Alert> findAll(AlertFilter filter, int page, int size);

    long count(AlertFilter filter);

    Alert updateStatus(UUID id, String status, UUID assignedTo, String resolutionNotes);

    AlertStats getStats();

    /**
     * Stream de alertas en tiempo real (SSE/WebSocket).
     * Implementación en adapter.in.rest.sse o adapter.in.rest.websocket.
     */
    interface AlertStream {
        void subscribe(UUID userId, AlertStreamHandler handler);
        void unsubscribe(UUID userId);
    }

    interface AlertStreamHandler {
        void onAlert(Alert alert);
        void onError(Throwable error);
    }

    record AlertFilter(
            String status,
            String severity,
            String threatType,
            UUID assetId,
            IpAddress sourceIp,
            IpAddress destinationIp,
            Instant from,
            Instant to
    ) {}

    record AlertStats(
            long totalOpen,
            long totalInProgress,
            long totalResolved,
            long totalFalsePositive,
            long criticalCount,
            long highCount,
            long mediumCount,
            long lowCount,
            long infoCount,
            long last24Hours
    ) {}
}