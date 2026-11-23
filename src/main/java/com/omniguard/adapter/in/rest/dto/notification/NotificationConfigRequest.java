package com.omniguard.adapter.in.rest.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationConfigRequest {

    @NotBlank(message = "Plataforma es obligatoria")
    @Pattern(regexp = "^(TELEGRAM|DISCORD|SLACK|EMAIL|WEBHOOK)$", message = "Plataforma inválida: TELEGRAM, DISCORD, SLACK, EMAIL, WEBHOOK")
    private String platform;

    @Size(max = 2048, message = "Webhook URL máximo 2048 caracteres")
    @Pattern(regexp = "^https?://.+", message = "Webhook URL debe empezar con http:// o https://")
    private String webhookUrl;

    @Size(max = 100, message = "Chat ID máximo 100 caracteres")
    private String chatId;

    @Size(max = 255, message = "Bot token máximo 255 caracteres")
    private String botToken;

    @jakarta.validation.constraints.Email(message = "Email inválido")
    @Size(max = 255, message = "Email máximo 255 caracteres")
    private String emailAddress;

    @Builder.Default
    private boolean isActive = true;
}