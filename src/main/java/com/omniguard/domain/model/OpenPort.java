package com.omniguard.domain.model;

import com.omniguard.domain.vo.PortNumber;
import com.omniguard.domain.vo.Protocol;
import java.time.Instant;
import java.util.UUID;

public record OpenPort(
        UUID id,
        UUID assetId,
        PortNumber portNumber,
        Protocol protocol,
        String serviceBanner,
        Instant discoveredAt
) {
    public static OpenPort create(
            UUID assetId,
            PortNumber portNumber,
            Protocol protocol,
            String serviceBanner
    ) {
        return new OpenPort(
                UUID.randomUUID(),
                assetId,
                portNumber,
                protocol,
                serviceBanner,
                Instant.now()
        );
    }

    public OpenPort updateBanner(String serviceBanner) {
        return new OpenPort(
                this.id,
                this.assetId,
                this.portNumber,
                this.protocol,
                serviceBanner,
                this.discoveredAt
        );
    }
}