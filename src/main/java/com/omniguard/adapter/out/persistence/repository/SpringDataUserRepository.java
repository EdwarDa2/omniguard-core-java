package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataUserRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByUsername(String username);
    Optional<UserJpaEntity> findByEmail(String email);
    Page<UserJpaEntity> findByRoleId(UUID roleId, Pageable pageable);
    long countByRoleId(UUID roleId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM UserJpaEntity u WHERE u.isActive = true")
    List<UserJpaEntity> findAllActive();
}