package com.omniguard.adapter.in.rest.dto.response;

import java.util.List;

public record AssetResponseDto(
        Long id,
        String hostname,
        String ip,
        String description,
        String os,
        String lastScanned,
        String status,
        int portsCount,
        boolean hasCve,
        String cveSeverity,
        String severityLabel,
        Double cvssScore,
        List<OpenPortResponseDto> ports,
        List<VulnerabilityResponseDto> vulnerabilities
) {}
