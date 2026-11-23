package com.omniguard.adapter.out.persistence.repository;

import com.omniguard.adapter.out.persistence.entity.NotificationConfigJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataNotificationConfigRepository extends JpaRepository<NotificationConfigJpaEntity, Long> {

    List<NotificationConfigJpaEntity> findByUserId(Long userId);

    List<NotificationConfigJpaEntity> findByPlatformIgnoreCaseAndIsActiveTrue(String platform);

    List<NotificationConfigJpaEntity> findByPlatformIgnoreCase(String platform);
}
