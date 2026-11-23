package com.omniguard.adapter.in.rest.dto.alert;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertFilterRequest {

    @Pattern(regexp = "^(OPEN|IN_PROGRESS|RESOLVED|FALSE_POSITIVE)$", message = "Status inválido")
    private String status;

    @Pattern(regexp = "^(CRITICAL|HIGH|MEDIUM|LOW|INFO)$", message = "Severidad inválida")
    private String severity;

    @Pattern(regexp = "^(MALWARE_C2|PORT_SCAN|BRUTE_FORCE|DATA_EXFIL|LATERAL_MOVEMENT|ANOMALY|SUSPICIOUS_PROCESS|PRIVILEGE_ESCALATION|CREDENTIAL_ACCESS)$", message = "Tipo de amenaza inválido")
    private String threatType;

    private UUID assetId;

    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", message = "IP inválida")
    private String sourceIp;

    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", message = "IP inválida")
    private String destinationIp;

    private Instant from;
    private Instant to;

    private UUID assignedTo;

    @Min(value = 0, message = "Page debe ser >= 0")
    @Builder.Default
    private int page = 0;

    @Min(value = 1, message = "Size debe ser >= 1")
    @Max(value = 100, message = "Size máximo 100")
    @Builder.Default
    private int size = 20;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDir = "DESC";
}