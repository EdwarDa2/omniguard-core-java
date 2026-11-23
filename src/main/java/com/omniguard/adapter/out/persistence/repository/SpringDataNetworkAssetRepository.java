package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.NetworkAssetJpaEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataNetworkAssetRepository
        extends JpaRepository<NetworkAssetJpaEntity, Long>,
                JpaSpecificationExecutor<NetworkAssetJpaEntity> {

    Optional<NetworkAssetJpaEntity> findByIpAddress(String ipAddress);

    Page<NetworkAssetJpaEntity> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);
}
