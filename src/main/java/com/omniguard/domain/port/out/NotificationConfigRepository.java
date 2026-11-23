package com.omniguard.domain.port.out;

import com.omniguard.domain.model.NotificationConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationConfigRepository {
    NotificationConfig save(NotificationConfig config);

    Optional<NotificationConfig> findById(UUID id);

    List<NotificationConfig> findByUserId(UUID userId);

    Optional<NotificationConfig> findByUserIdAndPlatform(UUID userId, String platform);

    List<NotificationConfig> findByPlatform(String platform);

    List<NotificationConfig> findActiveByUserId(UUID userId);

    void deleteById(UUID id);

    void deleteByUserId(UUID userId);
}