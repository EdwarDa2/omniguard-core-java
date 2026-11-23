package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.OpenPortJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataOpenPortRepository extends JpaRepository<OpenPortJpaEntity, UUID> {
    List<OpenPortJpaEntity> findByAssetId(UUID assetId);
    Optional<OpenPortJpaEntity> findByAssetIdAndPortNumberAndProtocol(UUID assetId, Integer portNumber, OpenPortJpaEntity.PortProtocol protocol);
    List<OpenPortJpaEntity> findByPortNumber(Integer portNumber);
    void deleteByAssetId(UUID assetId);
    long countByAssetId(UUID assetId);
}