package com.omniguard.adapter.in.rest.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public record ScanResultRequestDto(
        @JsonAlias({"scanId", "scan_id"})
        @JsonProperty("scan_id")
        @NotBlank(message = "El scan_id es obligatorio")
        @Size(max = 64, message = "El scan_id no puede superar 64 caracteres")
        String scanId,

        @JsonAlias({"targetIp", "target_ip"})
        @JsonProperty("target_ip")
        @NotBlank(message = "La IP objetivo (target_ip) es obligatoria")
        String targetIp,

        @JsonProperty("status")
        @Pattern(regexp = "^(?i)(COMPLETED|FAILED|PARTIAL)$", message = "El estado debe ser COMPLETED, FAILED o PARTIAL")
        String status,

        @JsonAlias({"scannedAt", "scanned_at"})
        @JsonProperty("scanned_at")
        Instant scannedAt,

        @JsonAlias({"openPorts", "open_ports"})
        @JsonProperty("open_ports")
        @Valid
        @Size(max = 1024, message = "La lista de puertos abiertos no puede superar 1024 elementos")
        List<DiscoveredPortDto> openPorts
) {}
