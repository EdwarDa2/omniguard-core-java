package com.omniguard.domain.model;

import com.omniguard.domain.vo.IpAddress;
import java.time.Instant;
import java.util.UUID;

public record AuditLog(
        UUID id,
        String entityType,
        UUID entityId,
        String action,
        String oldValues,
        String newValues,
        UUID changedBy,
        Instant changedAt,
        IpAddress ipAddress,
        String userAgent,
        String requestId,
        String correlationId
) {
    public static AuditLog create(
            String entityType,
            UUID entityId,
            String action,
            String oldValues,
            String newValues,
            UUID changedBy,
            IpAddress ipAddress,
            String userAgent,
            String requestId,
            String correlationId
    ) {
        return new AuditLog(
                UUID.randomUUID(),
                entityType,
                entityId,
                action,
                oldValues,
                newValues,
                changedBy,
                Instant.now(),
                ipAddress,
                userAgent,
                requestId,
                correlationId
        );
    }

    public static String maskSensitive(String json) {
        if (json == null) return null;
        return json
                .replaceAll("(?i)\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"")
                .replaceAll("(?i)\"password_hash\"\\s*:\\s*\"[^\"]*\"", "\"password_hash\":\"***\"")
                .replaceAll("(?i)\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"")
                .replaceAll("(?i)\"webhook_url\"\\s*:\\s*\"[^\"]*\"", "\"webhook_url\":\"***\"")
                .replaceAll("(?i)\"bot_token\"\\s*:\\s*\"[^\"]*\"", "\"bot_token\":\"***\"");
    }
}