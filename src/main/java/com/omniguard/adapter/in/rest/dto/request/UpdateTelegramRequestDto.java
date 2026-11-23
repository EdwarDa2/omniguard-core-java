package com.omniguard.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateTelegramRequestDto(
        @NotBlank(message = "Los chat IDs son obligatorios")
        String chatIds
) {}
