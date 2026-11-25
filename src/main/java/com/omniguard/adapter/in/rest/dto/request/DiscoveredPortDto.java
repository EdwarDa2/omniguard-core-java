package com.omniguard.adapter.in.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiscoveredPortDto(
        @NotNull(message = "El puerto es obligatorio")
        @Min(value = 1, message = "El puerto debe ser mayor o igual a 1")
        @Max(value = 65535, message = "El puerto debe ser menor o igual a 65535")
        Integer port,

        @JsonProperty("protocol")
        @Size(max = 10, message = "El protocolo no puede superar 10 caracteres")
        String protocol,

        @JsonProperty("app_protocol")
        @Size(max = 100, message = "El protocolo de aplicación no puede superar 100 caracteres")
        String appProtocol,

        @JsonProperty("product_version")
        @Size(max = 100, message = "La versión del producto no puede superar 100 caracteres")
        String productVersion,

        @JsonProperty("banner")
        @Size(max = 2048, message = "El banner no puede superar 2048 caracteres")
        String banner,

        @JsonProperty("status")
        @Size(max = 20, message = "El estado no puede superar 20 caracteres")
        String status
) {}
