package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.ServiceJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataServiceRepository extends JpaRepository<ServiceJpaEntity, Long> {

    List<ServiceJpaEntity> findByAssetId(Long assetId);

    Optional<ServiceJpaEntity> findByAssetIdAndPortAndProtocol(Long assetId, Integer port, String protocol);

    long count();
}
