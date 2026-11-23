package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.RoleJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataRoleRepository extends JpaRepository<RoleJpaEntity, Long> {
    Optional<RoleJpaEntity> findByName(String name);
    boolean existsByName(String name);
}
