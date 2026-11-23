package com.omniguard.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_entity", columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_logs_changed_by", columnList = "changed_by"),
        @Index(name = "idx_audit_logs_changed_at", columnList = "changed_at"),
        @Index(name = "idx_audit_logs_action", columnList = "action"),
        @Index(name = "idx_audit_logs_entity_changed", columnList = "entity_type, entity_id, changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AuditAction action;

    @Column(name = "old_values", columnDefinition = "jsonb")
    private String oldValues;

    @Column(name = "new_values", columnDefinition = "jsonb")
    private String newValues;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by",
            foreignKey = @ForeignKey(name = "fk_audit_logs_changed_by", value = ConstraintMode.CONSTRAINT))
    private UserJpaEntity changedBy;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    public enum AuditAction {
        CREATE, UPDATE, DELETE, STATUS_CHANGE, LOGIN, LOGOUT, PASSWORD_CHANGE, ROLE_CHANGE
    }
}