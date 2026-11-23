package com.omniguard.domain.port.in;

import com.omniguard.domain.model.Alert;
import com.omniguard.domain.vo.IpAddress;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertUseCase {
    Alert create(Alert alert);

    Optional<Alert> findById(Long id);

    List<Alert> findAll(AlertFilter filter, int page, int size);

    long count(AlertFilter filter);

    Alert updateStatus(Long id, String status, Long assignedTo, String resolutionNotes);

    AlertStats getStats();

    /**
     * Stream de alertas en tiempo real (SSE/WebSocket).
     */
    interface AlertStream {
        void subscribe(Long userId, AlertStreamHandler handler);
        void unsubscribe(Long userId);
    }

    interface AlertStreamHandler {
        void onAlert(Alert alert);
        void onError(Throwable error);
    }

    record AlertFilter(
            String status,
            String severity,
            String threatType,
            Long assetId,
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
