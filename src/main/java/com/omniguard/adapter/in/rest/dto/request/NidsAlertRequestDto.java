package com.omniguard.adapter.in.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Map;

public record NidsAlertRequestDto(
        @JsonProperty("threat_type")
        @NotBlank(message = "El tipo de amenaza (threat_type) es obligatorio")
        String threatType,

        @JsonProperty("rule")
        String rule,

        @JsonProperty("severity")
        @NotBlank(message = "La severidad es obligatoria")
        String severity,

        @JsonProperty("title")
        @NotBlank(message = "El título es obligatorio")
        String title,

        @JsonProperty("description")
        String description,

        @JsonProperty("source_ip")
        @NotBlank(message = "La IP de origen (source_ip) es obligatoria")
        String sourceIp,

        @JsonProperty("destination_ip")
        @NotBlank(message = "La IP de destino (destination_ip) es obligatoria")
        String destinationIp,

        @JsonProperty("destination_port")
        Integer destinationPort,

        @JsonProperty("protocol")
        String protocol,

        @JsonProperty("app_protocol")
        String appProtocol,

        @JsonProperty("timestamp")
        Instant timestamp,

        @JsonProperty("raw_event")
        Map<String, Object> rawEvent
) {}
