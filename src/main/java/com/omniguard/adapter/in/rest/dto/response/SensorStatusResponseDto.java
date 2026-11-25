package com.omniguard.adapter.in.rest.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Respuesta del estado del sensor de red")
public record SensorStatusResponseDto(
    @Schema(description = "Estado actual del sensor", example = "ACTIVE")
    String status,

    @Schema(description = "Mensaje informativo de la operación", example = "Sensor de red activado exitosamente")
    String message,

    @Schema(description = "Marca de tiempo de la respuesta")
    Instant timestamp
) {}
