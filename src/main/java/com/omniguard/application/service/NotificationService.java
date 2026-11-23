package com.omniguard.application.service;

import com.omniguard.adapter.out.persistence.adapter.NotificationConfigRepositoryAdapter;
import com.omniguard.domain.exception.EntityNotFoundException;
import com.omniguard.domain.model.NotificationConfig;
import com.omniguard.domain.port.in.NotificationUseCase;
import com.omniguard.domain.port.out.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final NotificationConfigRepository notificationConfigRepository;

    @Override
    public NotificationConfig create(UUID userId, String platform, String webhookUrl, String chatId, String botToken, String email) {
        NotificationConfig config = NotificationConfig.create(userId, platform, webhookUrl, chatId, botToken, email);
        return notificationConfigRepository.save(config);
    }

    @Override
    public Optional<NotificationConfig> findById(UUID id) {
        return notificationConfigRepository.findById(id);
    }

    @Override
    public List<NotificationConfig> findByUserId(UUID userId) {
        return notificationConfigRepository.findByUserId(userId);
    }

    @Override
    public NotificationConfig update(UUID id, String webhookUrl, String chatId, String botToken, String email, boolean isActive) {
        return notificationConfigRepository.findById(id)
                .map(config -> {
                    NotificationConfig updated = config.update(webhookUrl, chatId, botToken, email, isActive);
                    return notificationConfigRepository.save(updated);
                })
                .orElseThrow(() -> new EntityNotFoundException("NotificationConfig", id));
    }

    @Override
    public void delete(UUID id) {
        notificationConfigRepository.deleteById(id);
    }

    @Override
    public void testConfig(UUID configId) {
        NotificationConfig config = notificationConfigRepository.findById(configId)
                .orElseThrow(() -> new EntityNotFoundException("NotificationConfig", configId));

        // TODO: Implementar envío real de notificación de prueba según plataforma
        // Por ahora solo validamos que la configuración existe
        switch (config.platform()) {
            case "TELEGRAM" -> {
                if (config.chatId() == null || config.chatId().isBlank()) {
                    throw new IllegalStateException("Chat ID requerido para Telegram");
                }
                // TODO: Enviar mensaje de prueba via Telegram Bot API
            }
            case "DISCORD" -> {
                if (config.webhookUrl() == null || config.webhookUrl().isBlank()) {
                    throw new IllegalStateException("Webhook URL requerido para Discord");
                }
                // TODO: Enviar mensaje de prueba via Discord Webhook
            }
            case "SLACK" -> {
                if (config.webhookUrl() == null || config.webhookUrl().isBlank()) {
                    throw new IllegalStateException("Webhook URL requerido para Slack");
                }
                // TODO: Enviar mensaje de prueba via Slack Webhook
            }
            case "EMAIL" -> {
                if (config.emailAddress() == null || config.emailAddress().isBlank()) {
                    throw new IllegalStateException("Email requerido para notificaciones por email");
                }
                // TODO: Enviar email de prueba
            }
            case "WEBHOOK" -> {
                if (config.webhookUrl() == null || config.webhookUrl().isBlank()) {
                    throw new IllegalStateException("Webhook URL requerido");
                }
                // TODO: Enviar POST a webhook genérico
            }
            default -> throw new IllegalStateException("Plataforma no soportada: " + config.platform());
        }
    }
}