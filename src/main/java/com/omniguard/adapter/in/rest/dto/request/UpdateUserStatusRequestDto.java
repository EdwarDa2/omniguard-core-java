package com.omniguard.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserStatusRequestDto(
        @NotBlank(message = "El estado es obligatorio")
        String status
) {}
