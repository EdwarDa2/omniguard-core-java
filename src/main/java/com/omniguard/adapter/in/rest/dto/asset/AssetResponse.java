package com.omniguard.adapter.in.rest.dto.asset;

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
public class AssetResponse {

    private UUID id;
    private String ipAddress;
    private String macAddress;
    private String hostname;
    private String osName;
    private String status;
    private Instant lastScannedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private int openPortsCount;
    private int vulnerabilitiesCount;
    private int criticalVulnsCount;
}