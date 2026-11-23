package com.omniguard.domain.model;

import com.omniguard.domain.vo.Hostname;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.MacAddress;
import java.time.Instant;
import java.util.UUID;

public record Asset(
        UUID id,
        IpAddress ipAddress,
        MacAddress macAddress,
        Hostname hostname,
        String osName,
        String status,
        Instant lastScannedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static Asset create(
            IpAddress ipAddress,
            MacAddress macAddress,
            Hostname hostname,
            String osName
    ) {
        Instant now = Instant.now();
        return new Asset(
                UUID.randomUUID(),
                ipAddress,
                macAddress,
                hostname,
                osName,
                "ACTIVE",
                null,
                now,
                now
        );
    }

    public Asset update(Hostname hostname, String osName, String status) {
        return new Asset(
                this.id,
                this.ipAddress,
                this.macAddress,
                hostname,
                osName,
                status,
                this.lastScannedAt,
                this.createdAt,
                Instant.now()
        );
    }

    public Asset recordScan() {
        return new Asset(
                this.id,
                this.ipAddress,
                this.macAddress,
                this.hostname,
                this.osName,
                this.status,
                Instant.now(),
                this.createdAt,
                Instant.now()
        );
    }

    public Asset deactivate() {
        return new Asset(
                this.id,
                this.ipAddress,
                this.macAddress,
                this.hostname,
                this.osName,
                "DECOMMISSIONED",
                this.lastScannedAt,
                this.createdAt,
                Instant.now()
        );
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }
}