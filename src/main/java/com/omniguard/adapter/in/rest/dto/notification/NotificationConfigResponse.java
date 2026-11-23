package com.omniguard.adapter.in.rest.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationConfigResponse {

    private UUID id;
    private UUID userId;
    private String platform;
    private String webhookUrl;
    private String chatId;
    private String emailAddress;
    private boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}