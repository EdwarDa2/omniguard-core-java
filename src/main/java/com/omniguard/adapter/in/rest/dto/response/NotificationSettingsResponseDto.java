package com.omniguard.adapter.in.rest.dto.response;

public record NotificationSettingsResponseDto(
        DiscordSettingsDto discord,
        TelegramSettingsDto telegram,
        SeverityRulesDto severityRules
) {
    public record DiscordSettingsDto(
            String webhookUrlMasked,
            boolean isConfigured
    ) {}

    public record TelegramSettingsDto(
            String chatIds,
            boolean isConfigured
    ) {}

    public record SeverityRulesDto(
            boolean notifyCritical,
            boolean notifyHigh,
            boolean notifyMedium,
            boolean notifyLow
    ) {}
}
