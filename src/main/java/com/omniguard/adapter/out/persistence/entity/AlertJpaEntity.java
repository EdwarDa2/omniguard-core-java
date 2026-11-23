package com.omniguard.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_asset_id", columnList = "asset_id"),
        @Index(name = "idx_alerts_threat_type", columnList = "threat_type"),
        @Index(name = "idx_alerts_severity", columnList = "severity"),
        @Index(name = "idx_alerts_status", columnList = "status"),
        @Index(name = "idx_alerts_source_ip", columnList = "source_ip"),
        @Index(name = "idx_alerts_destination_ip", columnList = "destination_ip"),
        @Index(name = "idx_alerts_created_at", columnList = "created_at"),
        @Index(name = "idx_alerts_assigned_to", columnList = "assigned_to"),
        @Index(name = "idx_alerts_status_severity_created", columnList = "status, severity, created_at"),
        @Index(name = "idx_alerts_asset_status_created", columnList = "asset_id, status, created_at"),
        @Index(name = "idx_alerts_threat_severity_created", columnList = "threat_type, severity, created_at"),
        @Index(name = "idx_alerts_source_ip_created", columnList = "source_ip, created_at"),
        @Index(name = "idx_alerts_dest_ip_created", columnList = "destination_ip, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id",
            foreignKey = @ForeignKey(name = "fk_alerts_asset", value = ConstraintMode.CONSTRAINT))
    private AssetJpaEntity asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "threat_type", nullable = false, length = 50)
    private ThreatType threatType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private AlertSeverity severity;

    @Column(name = "source_ip", nullable = false, columnDefinition = "inet")
    private String sourceIp;

    @Column(name = "destination_ip", nullable = false, columnDefinition = "inet")
    private String destinationIp;

    @Column(name = "source_port")
    private Integer sourcePort;

    @Column(name = "destination_port")
    private Integer destinationPort;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", length = 10)
    private NetworkProtocol protocol;

    @Column(name = "payload_snippet", columnDefinition = "text")
    private String payloadSnippet;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to",
            foreignKey = @ForeignKey(name = "fk_alerts_assigned_to", value = ConstraintMode.CONSTRAINT))
    private UserJpaEntity assignedTo;

    @Column(name = "resolution_notes", columnDefinition = "text")
    private String resolutionNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public enum ThreatType {
        MALWARE_C2, PORT_SCAN, BRUTE_FORCE, DATA_EXFIL,
        LATERAL_MOVEMENT, ANOMALY, SUSPICIOUS_PROCESS,
        PRIVILEGE_ESCALATION, CREDENTIAL_ACCESS
    }

    public enum AlertSeverity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO
    }

    public enum AlertStatus {
        OPEN, IN_PROGRESS, RESOLVED, FALSE_POSITIVE
    }

    public enum NetworkProtocol {
        TCP, UDP, ICMP, IGMP, GRE, ESP, AH, OSPF, VRRP, SCTP, IPIP, IPV6
    }
}