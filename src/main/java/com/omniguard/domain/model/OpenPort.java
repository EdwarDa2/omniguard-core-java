package com.omniguard.domain.model;

import java.time.Instant;

public record OpenPort(
        Long id,
        Long assetId,
        int port,
        String protocol,
        String appProtocol,
        String banner,
        String productVersion,
        String cpeName,
        Instant firstSeenAt,
        Instant lastSeenAt
) {
    public static OpenPort create(
            Long assetId,
            int port,
            String protocol,
            String appProtocol,
            String banner,
            String productVersion,
            String cpeName
    ) {
        Instant now = Instant.now();
        return new OpenPort(
                null,
                assetId,
                port,
                protocol != null ? protocol.toUpperCase() : "TCP",
                appProtocol != null ? appProtocol : "Desconocido",
                banner,
                productVersion,
                cpeName,
                now,
                now
        );
    }

    public OpenPort withId(Long id) {
        return new OpenPort(
                id,
                this.assetId,
                this.port,
                this.protocol,
                this.appProtocol,
                this.banner,
                this.productVersion,
                this.cpeName,
                this.firstSeenAt,
                this.lastSeenAt
        );
    }
}
