package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.AlertJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.AlertPersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataAlertRepository;
import com.omniguard.domain.model.Alert;
import com.omniguard.domain.port.in.AlertUseCase.AlertFilter;
import com.omniguard.domain.port.in.AlertUseCase.AlertStats;
import com.omniguard.domain.port.out.AlertRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AlertPersistenceAdapter implements AlertRepository {

    private final SpringDataAlertRepository alertRepository;
    private final AlertPersistenceMapper alertMapper;

    public AlertPersistenceAdapter(
            SpringDataAlertRepository alertRepository,
            AlertPersistenceMapper alertMapper
    ) {
        this.alertRepository = alertRepository;
        this.alertMapper = alertMapper;
    }

    @Override
    @Transactional
    public Alert save(Alert alert) {
        AlertJpaEntity entity = alertMapper.toEntity(alert);
        AlertJpaEntity saved = alertRepository.save(entity);
        return alertMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alert> findById(Long id) {
        return alertRepository.findById(id).map(alertMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> findAll(AlertFilter filter, int page, int size) {
        Specification<AlertJpaEntity> spec = buildSpecification(filter);
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "detectedAt")
        );

        return alertRepository.findAll(spec, pageable).getContent().stream()
                .map(alertMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count(AlertFilter filter) {
        Specification<AlertJpaEntity> spec = buildSpecification(filter);
        return alertRepository.count(spec);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertStats getStats() {
        long totalOpen = alertRepository.countByStatus("OPEN");
        long totalInProgress = alertRepository.countByStatus("INVESTIGATING");
        long totalResolved = alertRepository.countByStatus("RESOLVED");
        long totalFalsePositive = alertRepository.countByStatus("FALSE_POSITIVE");

        long critical = alertRepository.countBySeverity("CRITICAL");
        long high = alertRepository.countBySeverity("HIGH");
        long medium = alertRepository.countBySeverity("MEDIUM");
        long low = alertRepository.countBySeverity("LOW");
        long info = alertRepository.countBySeverity("INFO");

        Instant last24HoursInstant = Instant.now().minus(24, ChronoUnit.HOURS);
        long last24Hours = alertRepository.countByDetectedAtAfter(last24HoursInstant);

        return new AlertStats(
                totalOpen,
                totalInProgress,
                totalResolved,
                totalFalsePositive,
                critical,
                high,
                medium,
                low,
                info,
                last24Hours
        );
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        alertRepository.deleteById(id);
    }

    private Specification<AlertJpaEntity> buildSpecification(AlertFilter filter) {
        return (root, query, cb) -> {
            if (filter == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (filter.status() != null && !filter.status().isBlank()) {
                predicates.add(cb.equal(root.get("status"), filter.status().trim().toUpperCase()));
            }

            if (filter.severity() != null && !filter.severity().isBlank()) {
                predicates.add(cb.equal(root.get("severity"), filter.severity().trim().toUpperCase()));
            }

            if (filter.threatType() != null && !filter.threatType().isBlank()) {
                predicates.add(cb.equal(root.get("rule"), filter.threatType().trim()));
            }

            if (filter.sourceIp() != null) {
                predicates.add(cb.equal(root.get("sourceIp"), filter.sourceIp().value()));
            }

            if (filter.destinationIp() != null) {
                predicates.add(cb.equal(root.get("destinationIp"), filter.destinationIp().value()));
            }

            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("detectedAt"), filter.from()));
            }

            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("detectedAt"), filter.to()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
