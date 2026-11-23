package com.omniguard.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateDiscordRequestDto(
        @NotBlank(message = "La URL del webhook es obligatoria")
        String webhookUrl
) {}
