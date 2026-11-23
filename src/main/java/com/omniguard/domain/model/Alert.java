package com.omniguard.domain.model;

import com.omniguard.domain.vo.IpAddress;
import java.time.Instant;

public record Alert(
        Long id,
        String rule,
        Severity severity,
        String title,
        String description,
        IpAddress sourceIp,
        IpAddress destinationIp,
        Integer destinationPort,
        String protocol,
        String appProtocol,
        String rawEvent,
        String status,
        Long assignedTo,
        String resolutionNotes,
        Instant detectedAt,
        Instant resolvedAt
) {
    public static Alert create(
            String rule,
            Severity severity,
            String title,
            String description,
            IpAddress sourceIp,
            IpAddress destinationIp,
            Integer destinationPort,
            String protocol,
            String appProtocol,
            String rawEvent
    ) {
        return new Alert(
                null,
                rule,
                severity,
                title,
                description,
                sourceIp,
                destinationIp,
                destinationPort,
                protocol,
                appProtocol,
                rawEvent,
                "OPEN",
                null,
                null,
                Instant.now(),
                null
        );
    }

    public Alert withId(Long id) {
        return new Alert(
                id,
                this.rule,
                this.severity,
                this.title,
                this.description,
                this.sourceIp,
                this.destinationIp,
                this.destinationPort,
                this.protocol,
                this.appProtocol,
                this.rawEvent,
                this.status,
                this.assignedTo,
                this.resolutionNotes,
                this.detectedAt,
                this.resolvedAt
        );
    }

    public Alert updateStatus(String newStatus, Long assignedTo, String resolutionNotes) {
        Instant resolved = ("RESOLVED".equalsIgnoreCase(newStatus) || "FALSE_POSITIVE".equalsIgnoreCase(newStatus))
                ? Instant.now()
                : this.resolvedAt;

        return new Alert(
                this.id,
                this.rule,
                this.severity,
                this.title,
                this.description,
                this.sourceIp,
                this.destinationIp,
                this.destinationPort,
                this.protocol,
                this.appProtocol,
                this.rawEvent,
                newStatus,
                assignedTo != null ? assignedTo : this.assignedTo,
                resolutionNotes != null ? resolutionNotes : this.resolutionNotes,
                this.detectedAt,
                resolved
        );
    }
}
