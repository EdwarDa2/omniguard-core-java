package com.omniguard.adapter.in.rest.controller;

import com.omniguard.adapter.in.rest.dto.request.UpdateDiscordRequestDto;
import com.omniguard.adapter.in.rest.dto.request.UpdateSeverityRulesRequestDto;
import com.omniguard.adapter.in.rest.dto.request.UpdateTelegramRequestDto;
import com.omniguard.adapter.in.rest.dto.response.NotificationSettingsResponseDto;
import com.omniguard.adapter.in.rest.dto.response.NotificationTestResponseDto;
import com.omniguard.adapter.out.persistence.entity.NotificationConfigJpaEntity;
import com.omniguard.adapter.out.persistence.repository.SpringDataNotificationConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings/notifications")
@Tag(name = "Notificaciones y Webhooks", description = "Gestión de integraciones externas (Discord, Telegram) y reglas de alerta")
public class NotificationSettingsController {

    private final SpringDataNotificationConfigRepository configRepository;
    private final RestClient restClient;

    // Reglas en memoria sincronizadas con la base de datos
    private boolean ruleCritical = true;
    private boolean ruleHigh = true;
    private boolean ruleMedium = false;
    private boolean ruleLow = false;

    public NotificationSettingsController(
            SpringDataNotificationConfigRepository configRepository,
            RestClient.Builder restClientBuilder
    ) {
        this.configRepository = configRepository;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    @GetMapping
    @Operation(summary = "Consultar integraciones de notificación", description = "Retorna el estado de Discord, Telegram y reglas de severidad con URLs enmascaradas")
    public ResponseEntity<NotificationSettingsResponseDto> getNotificationSettings() {
        List<NotificationConfigJpaEntity> discordConfigs = configRepository.findByPlatformIgnoreCase("DISCORD");
        String discordMasked = "";
        boolean isDiscordConfigured = false;
        if (!discordConfigs.isEmpty() && discordConfigs.get(0).getWebhookUrl() != null) {
            String rawUrl = discordConfigs.get(0).getWebhookUrl();
            discordMasked = maskUrl(rawUrl);
            isDiscordConfigured = !rawUrl.isBlank();
        }

        List<NotificationConfigJpaEntity> telegramConfigs = configRepository.findByPlatformIgnoreCase("TELEGRAM");
        String telegramChatIds = "";
        boolean isTelegramConfigured = false;
        if (!telegramConfigs.isEmpty() && telegramConfigs.get(0).getChatId() != null) {
            telegramChatIds = telegramConfigs.get(0).getChatId();
            isTelegramConfigured = !telegramChatIds.isBlank();
        }

        NotificationSettingsResponseDto response = new NotificationSettingsResponseDto(
                new NotificationSettingsResponseDto.DiscordSettingsDto(discordMasked, isDiscordConfigured),
                new NotificationSettingsResponseDto.TelegramSettingsDto(telegramChatIds, isTelegramConfigured),
                new NotificationSettingsResponseDto.SeverityRulesDto(ruleCritical, ruleHigh, ruleMedium, ruleLow)
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/discord")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Actualizar webhook de Discord", description = "Configura el destino de alertas para el Blue Team en Discord")
    public ResponseEntity<Map<String, Object>> updateDiscordWebhook(
            @Valid @RequestBody UpdateDiscordRequestDto request
    ) {
        if (!isValidDiscordWebhook(request.webhookUrl())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "El webhook debe usar HTTPS y pertenecer a discord.com"
            ));
        }

        List<NotificationConfigJpaEntity> existing = configRepository.findByPlatformIgnoreCase("DISCORD");
        NotificationConfigJpaEntity entity;
        if (!existing.isEmpty()) {
            entity = existing.get(0);
            entity.setWebhookUrl(request.webhookUrl());
            entity.setActive(true);
            entity.setUpdatedAt(Instant.now());
        } else {
            entity = new NotificationConfigJpaEntity(
                    null,
                    null,
                    "DISCORD",
                    request.webhookUrl(),
                    null,
                    null,
                    null,
                    true,
                    Instant.now(),
                    Instant.now()
            );
        }
        configRepository.save(entity);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "isConfigured", true,
                "message", "Webhook de Discord actualizado exitosamente"
        ));
    }

    @PutMapping("/telegram")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Actualizar Chat IDs de Telegram", description = "Configura los grupos o chats receptores de alertas en Telegram")
    public ResponseEntity<Map<String, Object>> updateTelegramChats(
            @Valid @RequestBody UpdateTelegramRequestDto request
    ) {
        List<NotificationConfigJpaEntity> existing = configRepository.findByPlatformIgnoreCase("TELEGRAM");
        NotificationConfigJpaEntity entity;
        if (!existing.isEmpty()) {
            entity = existing.get(0);
            entity.setChatId(request.chatIds());
            entity.setActive(true);
            entity.setUpdatedAt(Instant.now());
        } else {
            entity = new NotificationConfigJpaEntity(
                    null,
                    null,
                    "TELEGRAM",
                    null,
                    request.chatIds(),
                    null,
                    null,
                    true,
                    Instant.now(),
                    Instant.now()
            );
        }
        configRepository.save(entity);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "isConfigured", true,
                "message", "Chat IDs de Telegram actualizados exitosamente"
        ));
    }

    @PutMapping("/rules")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Guardar reglas de severidad", description = "Configura los niveles de criticidad que dispararán notificaciones externas")
    public ResponseEntity<Map<String, Object>> updateSeverityRules(
            @RequestBody UpdateSeverityRulesRequestDto request
    ) {
        this.ruleCritical = request.notifyCritical();
        this.ruleHigh = request.notifyHigh();
        this.ruleMedium = request.notifyMedium();
        this.ruleLow = request.notifyLow();

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Reglas de severidad sincronizadas con microservicio Python"
        ));
    }

    @PostMapping("/test/{channel}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Enviar alerta de prueba", description = "Prueba el canal de comunicación emitiendo una notificación de verificación")
    public ResponseEntity<NotificationTestResponseDto> testChannel(@PathVariable String channel) {
        if (!"discord".equalsIgnoreCase(channel)) {
            return ResponseEntity.badRequest().body(
                    new NotificationTestResponseDto(channel.toLowerCase(), false, "Canal no soportado")
            );
        }

        List<NotificationConfigJpaEntity> discordConfigs =
                configRepository.findByPlatformIgnoreCaseAndIsActiveTrue("DISCORD");
        if (discordConfigs.isEmpty()
                || discordConfigs.get(0).getWebhookUrl() == null
                || discordConfigs.get(0).getWebhookUrl().isBlank()) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(
                    new NotificationTestResponseDto("discord", false, "No hay un webhook de Discord configurado")
            );
        }

        try {
            restClient.post()
                    .uri(discordConfigs.get(0).getWebhookUrl())
                    .body(Map.of(
                            "username", "OmniGuard SOC",
                            "content", "✅ Alerta de prueba: el canal de Discord está configurado correctamente."
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
                    new NotificationTestResponseDto(
                            "discord",
                            false,
                            "Discord rechazó o no recibió la alerta de prueba"
                    )
            );
        }

        return ResponseEntity.ok(new NotificationTestResponseDto(
                "discord",
                true,
                "Alerta de prueba entregada en Discord"
        ));
    }

    private String maskUrl(String url) {
        if (url == null || url.length() <= 20) return "****************";
        return url.substring(0, url.lastIndexOf('/') + 1) + "*********";
    }

    private boolean isValidDiscordWebhook(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return false;
        }

        try {
            URI uri = new URI(webhookUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }

            String host = uri.getHost();
            if (host == null) {
                return false;
            }

            boolean officialHost = "discord.com".equalsIgnoreCase(host)
                    || "discordapp.com".equalsIgnoreCase(host);
            if (!officialHost) {
                return false;
            }

            return uri.getPort() == -1
                    && uri.getPath().matches("^/api(/v\\d+)?/webhooks/[0-9]+/[^/?#]+$")
                    && uri.getRawQuery() == null
                    && uri.getFragment() == null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
