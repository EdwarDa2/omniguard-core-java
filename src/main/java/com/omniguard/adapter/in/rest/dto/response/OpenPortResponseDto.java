package com.omniguard.adapter.in.rest.dto.response;

public record OpenPortResponseDto(
        int port,
        String proto,
        String service,
        String version,
        String status,
        VulnBadgeDto vulnBadge
) {
    public record VulnBadgeDto(
            int count,
            String severity
    ) {}
}
