package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.AlertJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataAlertRepository extends JpaRepository<AlertJpaEntity, UUID> {
    Page<AlertJpaEntity> findByAssetId(UUID assetId, Pageable pageable);
    Page<AlertJpaEntity> findByStatus(AlertJpaEntity.AlertStatus status, Pageable pageable);
    Page<AlertJpaEntity> findByThreatType(AlertJpaEntity.ThreatType threatType, Pageable pageable);
    Page<AlertJpaEntity> findBySourceIp(String sourceIp, Pageable pageable);
    Page<AlertJpaEntity> findByDestinationIp(String destinationIp, Pageable pageable);
    Page<AlertJpaEntity> findBySeverity(AlertJpaEntity.AlertSeverity severity, Pageable pageable);
    Page<AlertJpaEntity> findByCreatedAtBetween(Instant from, Instant to, Pageable pageable);
    Page<AlertJpaEntity> findByAssignedTo(UUID userId, Pageable pageable);

    @Query("""
        SELECT a FROM AlertJpaEntity a
        WHERE (:status IS NULL OR a.status = :status)
        AND (:severity IS NULL OR a.severity = :severity)
        AND (:threatType IS NULL OR a.threatType = :threatType)
        AND (:assetId IS NULL OR a.asset.id = :assetId)
        AND (:sourceIp IS NULL OR a.sourceIp = :sourceIp)
        AND (:destinationIp IS NULL OR a.destinationIp = :destinationIp)
        AND (:from IS NULL OR a.createdAt >= :from)
        AND (:to IS NULL OR a.createdAt <= :to)
        AND (:assignedTo IS NULL OR a.assignedTo.id = :assignedTo)
        ORDER BY a.createdAt DESC
    """)
    Page<AlertJpaEntity> findWithFilters(
            @Param("status") AlertJpaEntity.AlertStatus status,
            @Param("severity") AlertJpaEntity.AlertSeverity severity,
            @Param("threatType") AlertJpaEntity.ThreatType threatType,
            @Param("assetId") UUID assetId,
            @Param("sourceIp") String sourceIp,
            @Param("destinationIp") String destinationIp,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("assignedTo") UUID assignedTo,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(a) FROM AlertJpaEntity a
        WHERE (:status IS NULL OR a.status = :status)
        AND (:severity IS NULL OR a.severity = :severity)
        AND (:threatType IS NULL OR a.threatType = :threatType)
        AND (:assetId IS NULL OR a.asset.id = :assetId)
        AND (:sourceIp IS NULL OR a.sourceIp = :sourceIp)
        AND (:destinationIp IS NULL OR a.destinationIp = :destinationIp)
        AND (:from IS NULL OR a.createdAt >= :from)
        AND (:to IS NULL OR a.createdAt <= :to)
        AND (:assignedTo IS NULL OR a.assignedTo.id = :assignedTo)
    """)
    long countWithFilters(
            @Param("status") AlertJpaEntity.AlertStatus status,
            @Param("severity") AlertJpaEntity.AlertSeverity severity,
            @Param("threatType") AlertJpaEntity.ThreatType threatType,
            @Param("assetId") UUID assetId,
            @Param("sourceIp") String sourceIp,
            @Param("destinationIp") String destinationIp,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("assignedTo") UUID assignedTo
    );

    @Query("""
        SELECT NEW com.omniguard.adapter.out.persistence.projection.AlertStatsProjection(
            COUNT(CASE WHEN a.status = 'OPEN' THEN 1 END),
            COUNT(CASE WHEN a.status = 'IN_PROGRESS' THEN 1 END),
            COUNT(CASE WHEN a.status = 'RESOLVED' THEN 1 END),
            COUNT(CASE WHEN a.status = 'FALSE_POSITIVE' THEN 1 END),
            COUNT(CASE WHEN a.severity = 'CRITICAL' THEN 1 END),
            COUNT(CASE WHEN a.severity = 'HIGH' THEN 1 END),
            COUNT(CASE WHEN a.severity = 'MEDIUM' THEN 1 END),
            COUNT(CASE WHEN a.severity = 'LOW' THEN 1 END),
            COUNT(CASE WHEN a.severity = 'INFO' THEN 1 END),
            COUNT(CASE WHEN a.createdAt >= :since THEN 1 END)
        )
        FROM AlertJpaEntity a
    """)
    AlertStatsProjection getStats(@Param("since") Instant since);

    interface AlertStatsProjection {
        Long getTotalOpen();
        Long getTotalInProgress();
        Long getTotalResolved();
        Long getTotalFalsePositive();
        Long getCriticalCount();
        Long getHighCount();
        Long getMediumCount();
        Long getLowCount();
        Long getInfoCount();
        Long getLast24Hours();
    }
}