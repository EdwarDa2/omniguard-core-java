package com.omniguard.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateAssetRequestDto(
        @NotBlank(message = "IP address is required")
        String ip,
        String hostname,
        String description,
        String os
) {}
