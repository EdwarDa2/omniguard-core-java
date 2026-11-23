package com.omniguard.domain.model;

import java.time.Instant;

public record NotificationConfig(
        Long id,
        Long userId,
        String platform,
        String webhookUrl,
        String chatId,
        String botToken,
        String email,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static NotificationConfig create(
            Long userId,
            String platform,
            String webhookUrl,
            String chatId,
            String botToken,
            String email
    ) {
        Instant now = Instant.now();
        return new NotificationConfig(
                null,
                userId,
                platform != null ? platform.toUpperCase() : "DISCORD",
                webhookUrl,
                chatId,
                botToken,
                email,
                true,
                now,
                now
        );
    }

    public NotificationConfig withId(Long id) {
        return new NotificationConfig(
                id,
                this.userId,
                this.platform,
                this.webhookUrl,
                this.chatId,
                this.botToken,
                this.email,
                this.isActive,
                this.createdAt,
                this.updatedAt
        );
    }

    public NotificationConfig update(
            String webhookUrl,
            String chatId,
            String botToken,
            String email,
            boolean isActive
    ) {
        return new NotificationConfig(
                this.id,
                this.userId,
                this.platform,
                webhookUrl != null ? webhookUrl : this.webhookUrl,
                chatId != null ? chatId : this.chatId,
                botToken != null ? botToken : this.botToken,
                email != null ? email : this.email,
                isActive,
                this.createdAt,
                Instant.now()
        );
    }
}
