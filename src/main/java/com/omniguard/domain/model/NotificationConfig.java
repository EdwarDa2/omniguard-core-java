package com.omniguard.domain.model;

import java.time.Instant;
import java.util.UUID;

public record NotificationConfig(
        UUID id,
        UUID userId,
        String platform,
        String webhookUrl,
        String chatId,
        String botToken,
        String emailAddress,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static NotificationConfig create(
            UUID userId,
            String platform,
            String webhookUrl,
            String chatId,
            String botToken,
            String emailAddress
    ) {
        Instant now = Instant.now();
        return new NotificationConfig(
                UUID.randomUUID(),
                userId,
                platform,
                webhookUrl,
                chatId,
                botToken,
                emailAddress,
                true,
                now,
                now
        );
    }

    public NotificationConfig update(
            String webhookUrl,
            String chatId,
            String botToken,
            String emailAddress,
            boolean isActive
    ) {
        return new NotificationConfig(
                this.id,
                this.userId,
                this.platform,
                webhookUrl,
                chatId,
                botToken,
                emailAddress,
                isActive,
                this.createdAt,
                Instant.now()
        );
    }

    public NotificationConfig toggleActive() {
        return new NotificationConfig(
                this.id,
                this.userId,
                this.platform,
                this.webhookUrl,
                this.chatId,
                this.botToken,
                this.emailAddress,
                !this.isActive,
                this.createdAt,
                Instant.now()
        );
    }
}