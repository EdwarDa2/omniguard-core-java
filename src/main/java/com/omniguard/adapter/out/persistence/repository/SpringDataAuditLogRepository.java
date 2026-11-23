package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, UUID> {
    Page<AuditLogJpaEntity> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);
    Page<AuditLogJpaEntity> findByChangedBy(UUID changedBy, Pageable pageable);
    Page<AuditLogJpaEntity> findByAction(AuditLogJpaEntity.AuditAction action, Pageable pageable);
    Page<AuditLogJpaEntity> findByChangedAtBetween(Instant from, Instant to, Pageable pageable);

    @Query("""
        SELECT a FROM AuditLogJpaEntity a
        WHERE a.entityType = :entityType
        AND a.entityId = :entityId
        AND a.changedAt BETWEEN :from AND :to
        ORDER BY a.changedAt DESC
    """)
    Page<AuditLogJpaEntity> findByEntityAndChangedAtBetween(
            @Param("entityType") String entityType,
            @Param("entityId") UUID entityId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    long countByEntityTypeAndEntityId(String entityType, UUID entityId);
    long countByChangedAtBetween(Instant from, Instant to);
}