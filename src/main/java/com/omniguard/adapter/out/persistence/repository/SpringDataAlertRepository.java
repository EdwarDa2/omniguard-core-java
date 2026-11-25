package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.AlertJpaEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAlertRepository extends JpaRepository<AlertJpaEntity, Long>, JpaSpecificationExecutor<AlertJpaEntity> {

    long countByStatus(String status);

    long countBySeverity(String severity);

    long countByDetectedAtAfter(Instant after);

    long countByDetectedAtBetween(Instant start, Instant end);

    @Query("SELECT COUNT(a) FROM AlertJpaEntity a WHERE a.status = 'ACTIVE' OR a.status = 'OPEN'")
    long countActive();

    default long countRecent(Instant after) {
        return countByDetectedAtAfter(after);
    }
}
