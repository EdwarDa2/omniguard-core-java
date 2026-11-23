package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.RoleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataRoleRepository extends JpaRepository<RoleJpaEntity, UUID> {
    Optional<RoleJpaEntity> findByName(RoleJpaEntity.RoleName name);
    boolean existsByName(RoleJpaEntity.RoleName name);
}