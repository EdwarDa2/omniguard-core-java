package com.omniguard.adapter.in.rest.dto.response;

import com.omniguard.domain.model.Alert;
import java.time.Instant;

public record AlertResponseDto(
        Long id,
        String rule,
        String severity,
        String title,
        String description,
        String sourceIp,
        String destinationIp,
        Integer destinationPort,
        String protocol,
        String appProtocol,
        String status,
        Long assignedTo,
        String resolutionNotes,
        Instant detectedAt,
        Instant resolvedAt
) {
    public static AlertResponseDto fromDomain(Alert alert) {
        if (alert == null) {
            return null;
        }
        return new AlertResponseDto(
                alert.id(),
                alert.rule(),
                alert.severity() != null ? alert.severity().name() : "MEDIUM",
                alert.title(),
                alert.description(),
                alert.sourceIp() != null ? alert.sourceIp().value() : null,
                alert.destinationIp() != null ? alert.destinationIp().value() : null,
                alert.destinationPort(),
                alert.protocol(),
                alert.appProtocol(),
                alert.status(),
                alert.assignedTo(),
                alert.resolutionNotes(),
                alert.detectedAt(),
                alert.resolvedAt()
        );
    }
}
