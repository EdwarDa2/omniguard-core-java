package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.AlertJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.AlertPersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataAlertRepository;
import com.omniguard.domain.model.Alert;
import com.omniguard.domain.port.out.AlertRepository;
import com.omniguard.domain.vo.AlertStatus;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.ThreatType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AlertRepositoryAdapter implements AlertRepository {

    private final SpringDataAlertRepository springDataAlertRepository;
    private final AlertPersistenceMapper mapper;

    @Override
    public Alert save(Alert alert) {
        AlertJpaEntity entity = mapper.toEntity(alert);
        AlertJpaEntity saved = springDataAlertRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Alert> findById(UUID id) {
        return springDataAlertRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Alert> findAll(AlertFilter filter, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var page = springDataAlertRepository.findWithFilters(
                filter.status() != null ? AlertStatus.valueOf(filter.status().value()) : null,
                filter.severity() != null ? AlertJpaEntity.AlertSeverity.valueOf(filter.severity()) : null,
                filter.threatType() != null ? AlertJpaEntity.ThreatType.valueOf(filter.threatType().value()) : null,
                filter.assetId(),
                filter.sourceIp() != null ? filter.sourceIp().value() : null,
                filter.destinationIp() != null ? filter.destinationIp().value() : null,
                filter.from(),
                filter.to(),
                filter.assignedTo(),
                pageable
        );
        return page.stream().map(mapper::toDomain).toList();
    }

    @Override
    public long count(AlertFilter filter) {
        return springDataAlertRepository.countWithFilters(
                filter.status() != null ? AlertStatus.valueOf(filter.status().value()) : null,
                filter.severity() != null ? AlertJpaEntity.AlertSeverity.valueOf(filter.severity()) : null,
                filter.threatType() != null ? AlertJpaEntity.ThreatType.valueOf(filter.threatType().value()) : null,
                filter.assetId(),
                filter.sourceIp() != null ? filter.sourceIp().value() : null,
                filter.destinationIp() != null ? filter.destinationIp().value() : null,
                filter.from(),
                filter.to(),
                filter.assignedTo()
        );
    }

    @Override
    public List<Alert> findByAssetId(UUID assetId, int page, int size) {
        return springDataAlertRepository.findByAssetId(assetId, PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Alert> findByStatus(AlertStatus status, int page, int size) {
        return springDataAlertRepository.findByStatus(AlertJpaEntity.AlertStatus.valueOf(status.value()), PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Alert> findByThreatType(ThreatType threatType, int page, int size) {
        return springDataAlertRepository.findByThreatType(AlertJpaEntity.ThreatType.valueOf(threatType.value()), PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Alert> findBySourceIp(IpAddress sourceIp, int page, int size) {
        return springDataAlertRepository.findBySourceIp(sourceIp.value(), PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Alert> findByDestinationIp(IpAddress destinationIp, int page, int size) {
        return springDataAlertRepository.findByDestinationIp(destinationIp.value(), PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Alert> findBySeverity(String severity, int page, int size) {
        return springDataAlertRepository.findBySeverity(AlertJpaEntity.AlertSeverity.valueOf(severity), PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Alert> findByCreatedAtBetween(Instant from, Instant to, int page, int size) {
        return springDataAlertRepository.findByCreatedAtBetween(from, to, PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Alert> findByAssignedTo(UUID userId, int page, int size) {
        return springDataAlertRepository.findByAssignedTo(userId, PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataAlertRepository.deleteById(id);
    }

    @Override
    public AlertStats getStats() {
        var projection = springDataAlertRepository.getStats(Instant.now().minusSeconds(86400));
        return new AlertStats(
                projection.getTotalOpen() != null ? projection.getTotalOpen() : 0L,
                projection.getTotalInProgress() != null ? projection.getTotalInProgress() : 0L,
                projection.getTotalResolved() != null ? projection.getTotalResolved() : 0L,
                projection.getTotalFalsePositive() != null ? projection.getTotalFalsePositive() : 0L,
                projection.getCriticalCount() != null ? projection.getCriticalCount() : 0L,
                projection.getHighCount() != null ? projection.getHighCount() : 0L,
                projection.getMediumCount() != null ? projection.getMediumCount() : 0L,
                projection.getLowCount() != null ? projection.getLowCount() : 0L,
                projection.getInfoCount() != null ? projection.getInfoCount() : 0L,
                projection.getLast24Hours() != null ? projection.getLast24Hours() : 0L
        );
    }
}