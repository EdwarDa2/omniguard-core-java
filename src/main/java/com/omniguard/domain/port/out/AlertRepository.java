package com.omniguard.domain.port.out;

import com.omniguard.domain.model.Alert;
import com.omniguard.domain.vo.AlertStatus;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.ThreatType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository {
    Alert save(Alert alert);

    Optional<Alert> findById(UUID id);

    List<Alert> findAll(AlertFilter filter, int page, int size);

    long count(AlertFilter filter);

    List<Alert> findByAssetId(UUID assetId, int page, int size);

    List<Alert> findByStatus(AlertStatus status, int page, int size);

    List<Alert> findByThreatType(ThreatType threatType, int page, int size);

    List<Alert> findBySourceIp(IpAddress sourceIp, int page, int size);

    List<Alert> findByDestinationIp(IpAddress destinationIp, int page, int size);

    List<Alert> findBySeverity(String severity, int page, int size);

    List<Alert> findByCreatedAtBetween(Instant from, Instant to, int page, int size);

    List<Alert> findByAssignedTo(UUID userId, int page, int size);

    void deleteById(UUID id);

    record AlertFilter(
            AlertStatus status,
            String severity,
            ThreatType threatType,
            UUID assetId,
            IpAddress sourceIp,
            IpAddress destinationIp,
            Instant from,
            Instant to,
            UUID assignedTo
    ) {}
}