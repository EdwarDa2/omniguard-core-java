package com.omniguard.domain.model;

import com.omniguard.domain.vo.IpAddress;
import java.time.Instant;
import java.util.UUID;

public record PasetoSession(
        UUID id,
        Long userId,
        String tokenHash,
        Instant issuedAt,
        Instant expiresAt,
        Instant revokedAt,
        String revokeReason,
        IpAddress ipAddress,
        String userAgent
) {
    public static PasetoSession create(
            UUID id,
            Long userId,
            String tokenHash,
            Instant expiresAt,
            IpAddress ipAddress,
            String userAgent
    ) {
        return new PasetoSession(
                id,
                userId,
                tokenHash,
                Instant.now(),
                expiresAt,
                null,
                null,
                ipAddress,
                userAgent
        );
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
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
                this.userAgent
        );
    }
}
