package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.AlertJpaEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataAlertRepository extends JpaRepository<AlertJpaEntity, Long>, JpaSpecificationExecutor<AlertJpaEntity> {

    long countByStatus(String status);

    long countBySeverity(String severity);

    long countByDetectedAtAfter(Instant after);
}
