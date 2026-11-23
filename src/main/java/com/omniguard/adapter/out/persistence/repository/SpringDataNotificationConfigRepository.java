package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.NotificationConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataNotificationConfigRepository extends JpaRepository<NotificationConfigJpaEntity, UUID> {
    List<NotificationConfigJpaEntity> findByUserId(UUID userId);
    Optional<NotificationConfigJpaEntity> findByUserIdAndPlatform(UUID userId, NotificationConfigJpaEntity.NotificationPlatform platform);
    List<NotificationConfigJpaEntity> findByPlatform(NotificationConfigJpaEntity.NotificationPlatform platform);
    List<NotificationConfigJpaEntity> findByUserIdAndIsActiveTrue(UUID userId);
    void deleteByUserId(UUID userId);
}