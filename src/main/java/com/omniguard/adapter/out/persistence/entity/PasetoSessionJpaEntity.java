package com.omniguard.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "paseto_sessions", indexes = {
        @Index(name = "idx_paseto_sessions_user_id", columnList = "user_id"),
        @Index(name = "idx_paseto_sessions_expires_at", columnList = "expires_at"),
        @Index(name = "idx_paseto_sessions_revoked_at", columnList = "revoked_at"),
        @Index(name = "idx_paseto_sessions_token_hash", columnList = "token_hash", unique = true),
        @Index(name = "idx_paseto_sessions_expires_revoked", columnList = "expires_at, revoked_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasetoSessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_paseto_sessions_user", value = ConstraintMode.CONSTRAINT))
    private UserJpaEntity user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_reason", length = 100)
    private String revokedReason;

    @Column(name = "ip_address", columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "device_fingerprint", length = 64)
    private String deviceFingerprint;
}