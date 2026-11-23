package com.omniguard.domain.model;

import com.omniguard.domain.vo.AlertStatus;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.Protocol;
import com.omniguard.domain.vo.ThreatType;
import java.time.Instant;
import java.util.UUID;

public record Alert(
        UUID id,
        UUID assetId,
        ThreatType threatType,
        String severity,
        IpAddress sourceIp,
        IpAddress destinationIp,
        int sourcePort,
        int destinationPort,
        Protocol protocol,
        String payloadSnippet,
        AlertStatus status,
        UUID assignedTo,
        String resolutionNotes,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt
) {
    public static Alert create(
            UUID assetId,
            ThreatType threatType,
            String severity,
            IpAddress sourceIp,
            IpAddress destinationIp,
            int sourcePort,
            int destinationPort,
            Protocol protocol,
            String payloadSnippet
    ) {
        Instant now = Instant.now();
        return new Alert(
                UUID.randomUUID(),
                assetId,
                threatType,
                severity,
                sourceIp,
                destinationIp,
                sourcePort,
                destinationPort,
                protocol,
                payloadSnippet,
                AlertStatus.open(),
                null,
                null,
                now,
                now,
                null
        );
    }

    public Alert assignTo(UUID userId) {
        return new Alert(
                this.id,
                this.assetId,
                this.threatType,
                this.severity,
                this.sourceIp,
                this.destinationIp,
                this.sourcePort,
                this.destinationPort,
                this.protocol,
                this.payloadSnippet,
                this.status,
                userId,
                this.resolutionNotes,
                this.createdAt,
                Instant.now(),
                this.resolvedAt
        );
    }

    public Alert updateStatus(AlertStatus status, UUID assignedTo, String resolutionNotes) {
        return new Alert(
                this.id,
                this.assetId,
                this.threatType,
                this.severity,
                this.sourceIp,
                this.destinationIp,
                this.sourcePort,
                this.destinationPort,
                this.protocol,
                this.payloadSnippet,
                status,
                assignedTo != null ? assignedTo : this.assignedTo,
                resolutionNotes != null ? resolutionNotes : this.resolutionNotes,
                this.createdAt,
                Instant.now(),
                status.isClosed() ? Instant.now() : null
        );
    }

    public boolean isOpen() {
        return status.isOpen();
    }

    public boolean isClosed() {
        return status.isClosed();
    }
}