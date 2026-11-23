package com.omniguard.domain.port.out;

import com.omniguard.domain.model.NotificationConfig;
import java.util.List;
import java.util.Optional;

public interface NotificationConfigRepository {
    NotificationConfig save(NotificationConfig config);

    Optional<NotificationConfig> findById(Long id);

    List<NotificationConfig> findByUserId(Long userId);

    List<NotificationConfig> findActiveByPlatform(String platform);

    void deleteById(Long id);
}
