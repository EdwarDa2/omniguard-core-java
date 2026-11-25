package com.omniguard.adapter.in.rest.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Solicitud para cambiar el estado operativo del sensor de red")
public record SensorStatusRequestDto(
    @Schema(description = "Acción a ejecutar: START para activar, STOP para pausar", example = "START")
    @NotBlank(message = "La acción es obligatoria")
    @Pattern(regexp = "(?i)^(START|STOP|PAUSE|RESUME)$", message = "La acción debe ser START, STOP, PAUSE o RESUME")
    String action
) {}
