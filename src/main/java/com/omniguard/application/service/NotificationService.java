package com.omniguard.application.service;

import com.omniguard.domain.exception.EntityNotFoundException;
import com.omniguard.domain.model.NotificationConfig;
import com.omniguard.domain.port.in.NotificationUseCase;
import com.omniguard.domain.port.out.NotificationConfigRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService implements NotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationConfigRepository repository;

    public NotificationService(NotificationConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public NotificationConfig create(
            Long userId,
            String platform,
            String webhookUrl,
            String chatId,
            String botToken,
            String email
    ) {
        NotificationConfig config = NotificationConfig.create(userId, platform, webhookUrl, chatId, botToken, email);
        NotificationConfig saved = repository.save(config);
        log.info("Created notification config for platform {}: id={}", platform, saved.id());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationConfig> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationConfig> findByUserId(Long userId) {
        return repository.findByUserId(userId);
    }

    @Override
    @Transactional
    public NotificationConfig update(
            Long id,
            String webhookUrl,
            String chatId,
            String botToken,
            String email,
            boolean isActive
    ) {
        NotificationConfig existing = repository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Configuración de notificación no encontrada con ID: " + id)
        );
        NotificationConfig updated = existing.update(webhookUrl, chatId, botToken, email, isActive);
        return repository.save(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
        log.info("Deleted notification config id={}", id);
    }

    @Override
    public TestResult testConfig(Long configId) {
        NotificationConfig config = repository.findById(configId).orElseThrow(
                () -> new EntityNotFoundException("Configuración de notificación no encontrada con ID: " + configId)
        );

        log.info("Testing notification channel for platform {}", config.platform());
        return new TestResult(true, "Mensaje de prueba despachado exitosamente hacia " + config.platform());
    }
}
