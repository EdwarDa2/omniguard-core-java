package com.omniguard.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateAlertStatusRequestDto(
        @NotBlank(message = "El estado es obligatorio (OPEN, INVESTIGATING, RESOLVED, FALSE_POSITIVE)")
        String status,

        Long assignedTo,

        String resolutionNotes
) {}
