package com.omniguard.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_configs", indexes = {
        @Index(name = "idx_notification_configs_user_id", columnList = "user_id"),
        @Index(name = "idx_notification_configs_platform", columnList = "platform"),
        @Index(name = "idx_notification_configs_is_active", columnList = "is_active"),
        @Index(name = "uk_notification_configs_user_platform", columnList = "user_id, platform", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationConfigJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_configs_user", value = ConstraintMode.CONSTRAINT))
    private UserJpaEntity user;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private NotificationPlatform platform;

    @Column(name = "webhook_url", columnDefinition = "text")
    private String webhookUrl;

    @Column(name = "chat_id", length = 100)
    private String chatId;

    @Column(name = "bot_token", length = 255)
    private String botToken;

    @Column(name = "email_address", length = 255)
    private String emailAddress;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum NotificationPlatform {
        TELEGRAM, DISCORD, SLACK, EMAIL, WEBHOOK
    }
}