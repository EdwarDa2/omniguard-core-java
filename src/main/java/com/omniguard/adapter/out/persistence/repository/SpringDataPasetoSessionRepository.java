package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.PasetoSessionJpaEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataPasetoSessionRepository extends JpaRepository<PasetoSessionJpaEntity, UUID> {
    List<PasetoSessionJpaEntity> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE PasetoSessionJpaEntity s SET s.revokedAt = CURRENT_TIMESTAMP, s.revokeReason = :reason WHERE s.userId = :userId AND s.revokedAt IS NULL")
    void revokeAllByUserId(@Param("userId") Long userId, @Param("reason") String reason);
}
