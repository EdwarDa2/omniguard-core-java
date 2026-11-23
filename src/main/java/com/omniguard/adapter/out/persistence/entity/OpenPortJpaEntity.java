package com.omniguard.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "open_ports", indexes = {
        @Index(name = "idx_open_ports_asset_id", columnList = "asset_id"),
        @Index(name = "idx_open_ports_port_number", columnList = "port_number"),
        @Index(name = "idx_open_ports_protocol", columnList = "protocol"),
        @Index(name = "idx_open_ports_discovered_at", columnList = "discovered_at"),
        @Index(name = "uk_open_ports_asset_port_protocol", columnList = "asset_id, port_number, protocol", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenPortJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_open_ports_asset", value = ConstraintMode.CONSTRAINT))
    private AssetJpaEntity asset;

    @Column(name = "port_number", nullable = false)
    private Integer portNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", nullable = false, length = 10)
    private PortProtocol protocol;

    @Column(name = "service_banner", columnDefinition = "text")
    private String serviceBanner;

    @CreationTimestamp
    @Column(name = "discovered_at", nullable = false, updatable = false)
    private Instant discoveredAt;

    public enum PortProtocol {
        TCP, UDP
    }
}