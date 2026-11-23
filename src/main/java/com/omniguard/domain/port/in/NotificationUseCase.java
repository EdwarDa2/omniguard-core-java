package com.omniguard.domain.port.in;

import com.omniguard.domain.model.NotificationConfig;
import java.util.List;
import java.util.Optional;

public interface NotificationUseCase {
    NotificationConfig create(Long userId, String platform, String webhookUrl, String chatId, String botToken, String email);

    Optional<NotificationConfig> findById(Long id);

    List<NotificationConfig> findByUserId(Long userId);

    NotificationConfig update(Long id, String webhookUrl, String chatId, String botToken, String email, boolean isActive);

    void delete(Long id);

    TestResult testConfig(Long configId);

    record TestResult(boolean success, String message) {}
}
