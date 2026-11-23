package com.omniguard.adapter.in.rest.dto.alert;

import com.omniguard.adapter.in.rest.dto.asset.AssetResponse;
import com.omniguard.adapter.in.rest.dto.user.UserResponse;
import com.omniguard.adapter.in.rest.dto.asset.VulnerabilityResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {

    private UUID id;
    private AssetResponse asset;
    private String threatType;
    private String severity;
    private String sourceIp;
    private String destinationIp;
    private int sourcePort;
    private int destinationPort;
    private String protocol;
    private String payloadSnippet;
    private String status;
    private UserResponse assignedTo;
    private String resolutionNotes;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
    private List<VulnerabilityResponse> correlatedVulnerabilities;
}