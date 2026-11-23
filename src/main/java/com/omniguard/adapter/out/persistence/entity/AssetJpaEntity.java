package com.omniguard.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assets", indexes = {
        @Index(name = "idx_assets_ip_address", columnList = "ip_address", unique = true),
        @Index(name = "idx_assets_mac_address", columnList = "mac_address", unique = true),
        @Index(name = "idx_assets_status", columnList = "status"),
        @Index(name = "idx_assets_last_scanned_at", columnList = "last_scanned_at"),
        @Index(name = "idx_assets_status_ip", columnList = "status, ip_address")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ip_address", nullable = false, unique = true, columnDefinition = "inet")
    private String ipAddress;

    @Column(name = "mac_address", unique = true, columnDefinition = "macaddr")
    private String macAddress;

    @Column(name = "hostname", length = 255)
    private String hostname;

    @Column(name = "os_name", length = 100)
    private String osName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AssetStatus status = AssetStatus.ACTIVE;

    @Column(name = "last_scanned_at")
    private Instant lastScannedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum AssetStatus {
        ACTIVE, INACTIVE, DECOMMISSIONED
    }
}