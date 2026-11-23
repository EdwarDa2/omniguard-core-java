package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.UserJpaEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByUsername(String username);
    Optional<UserJpaEntity> findByEmail(String email);
    Page<UserJpaEntity> findByRoleId(Long roleId, Pageable pageable);
    long countByRoleId(Long roleId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
