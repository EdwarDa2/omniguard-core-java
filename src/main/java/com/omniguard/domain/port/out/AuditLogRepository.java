package com.omniguard.domain.port.out;

import com.omniguard.domain.model.AuditLog;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository {
    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByEntity(String entityType, UUID entityId, int page, int size);

    List<AuditLog> findByChangedBy(UUID changedBy, int page, int size);

    List<AuditLog> findByAction(String action, int page, int size);

    List<AuditLog> findByChangedAtBetween(Instant from, Instant to, int page, int size);

    List<AuditLog> findByEntityAndChangedAtBetween(String entityType, UUID entityId, Instant from, Instant to, int page, int size);

    long countByEntity(String entityType, UUID entityId);

    long countByChangedAtBetween(Instant from, Instant to);
}