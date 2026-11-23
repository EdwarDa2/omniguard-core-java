package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.AlertJpaEntity;
import com.omniguard.domain.model.Alert;
import com.omniguard.domain.model.Severity;
import com.omniguard.domain.vo.IpAddress;
import org.springframework.stereotype.Component;

@Component
public class AlertPersistenceMapper {

    public Alert toDomain(AlertJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Severity severity;
        try {
            severity = Severity.valueOf(entity.getSeverity().toUpperCase());
        } catch (Exception e) {
            severity = Severity.MEDIUM;
        }

        IpAddress srcIp = entity.getSourceIp() != null ? IpAddress.of(entity.getSourceIp()) : null;
        IpAddress dstIp = entity.getDestinationIp() != null ? IpAddress.of(entity.getDestinationIp()) : null;

        return new Alert(
                entity.getId(),
                entity.getRule(),
                severity,
                entity.getTitle(),
                entity.getDescription(),
                srcIp,
                dstIp,
                entity.getDestinationPort(),
                entity.getProtocol(),
                entity.getAppProtocol(),
                entity.getRawEvent(),
                entity.getStatus(),
                entity.getAssignedTo(),
                entity.getResolutionNotes(),
                entity.getDetectedAt(),
                entity.getResolvedAt()
        );
    }

    public AlertJpaEntity toEntity(Alert domain) {
        if (domain == null) {
            return null;
        }

        String srcIp = domain.sourceIp() != null ? domain.sourceIp().value() : null;
        String dstIp = domain.destinationIp() != null ? domain.destinationIp().value() : null;
        String severityStr = domain.severity() != null ? domain.severity().name() : "MEDIUM";

        return new AlertJpaEntity(
                domain.id(),
                domain.rule(),
                severityStr,
                domain.title(),
                domain.description(),
                srcIp,
                dstIp,
                domain.destinationPort(),
                domain.protocol(),
                domain.appProtocol(),
                domain.rawEvent() != null ? domain.rawEvent() : "{}",
                domain.detectedAt(),
                domain.status() != null ? domain.status() : "OPEN",
                domain.assignedTo(),
                domain.resolutionNotes(),
                domain.resolvedAt()
        );
    }
}
