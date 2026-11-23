package com.omniguard.domain.model;

import com.omniguard.domain.vo.IpAddress;
import java.time.Instant;
import java.util.UUID;

public record PasetoSession(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant issuedAt,
        Instant expiresAt,
        Instant revokedAt,
        String revokedReason,
        IpAddress ipAddress,
        String userAgent,
        String deviceFingerprint
) {
    public static PasetoSession create(
            UUID userId,
            String tokenHash,
            Instant expiresAt,
            IpAddress ipAddress,
            String userAgent,
            String deviceFingerprint
    ) {
        Instant now = Instant.now();
        return new PasetoSession(
                UUID.randomUUID(),
                userId,
                tokenHash,
                now,
                expiresAt,
                null,
                null,
                ipAddress,
                userAgent,
                deviceFingerprint
        );
    }

    public PasetoSession revoke(String reason) {
        return new PasetoSession(
                this.id,
                this.userId,
                this.tokenHash,
                this.issuedAt,
                this.expiresAt,
                Instant.now(),
                reason,
                this.ipAddress,
                this.userAgent,
                this.deviceFingerprint
        );
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isRevoked() && !isExpired();
    }
}