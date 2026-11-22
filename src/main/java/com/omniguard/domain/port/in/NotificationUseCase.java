package com.omniguard.domain.port.in;

import com.omniguard.domain.model.NotificationConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationUseCase {
    NotificationConfig create(UUID userId, String platform, String webhookUrl, String chatId, String botToken, String email);

    Optional<NotificationConfig> findById(UUID id);

    List<NotificationConfig> findByUserId(UUID userId);

    NotificationConfig update(UUID id, String webhookUrl, String chatId, String botToken, String email, boolean isActive);

    void delete(UUID id);

    void testConfig(UUID configId);

    record TestResult(boolean success, String message) {}
}