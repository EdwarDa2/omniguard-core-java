package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.PasetoSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataPasetoSessionRepository extends JpaRepository<PasetoSessionJpaEntity, UUID> {
    List<PasetoSessionJpaEntity> findByUserId(UUID userId);
    List<PasetoSessionJpaEntity> findByUserIdAndRevokedAtIsNull(UUID userId);
    List<PasetoSessionJpaEntity> findByExpiresAtBefore(Instant before);
    List<PasetoSessionJpaEntity> findByRevokedAtBefore(Instant before);
    long countByUserId(UUID userId);

    @Modifying
    @Transactional
    @Query("""
        UPDATE PasetoSessionJpaEntity p
        SET p.revokedAt = :now, p.revokedReason = :reason
        WHERE p.id = :id AND p.revokedAt IS NULL
    """)
    int revokeSession(@Param("id") UUID id, @Param("now") Instant now, @Param("reason") String reason);

    @Modifying
    @Transactional
    @Query("""
        UPDATE PasetoSessionJpaEntity p
        SET p.revokedAt = :now, p.revokedReason = :reason
        WHERE p.userId = :userId AND p.revokedAt IS NULL
    """)
    int revokeAllUserSessions(@Param("userId") UUID userId, @Param("now") Instant now, @Param("reason") String reason);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasetoSessionJpaEntity p WHERE p.expiresAt < :before")
    int deleteExpiredSessions(@Param("before") Instant before);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasetoSessionJpaEntity p WHERE p.revokedAt IS NOT NULL AND p.revokedAt < :before")
    int deleteOldRevokedSessions(@Param("before") Instant before);
}