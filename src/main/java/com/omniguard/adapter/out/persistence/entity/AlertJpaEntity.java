package com.omniguard.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "events", schema = "alerts")
public class AlertJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String rule;

    @Column(nullable = false, length = 10)
    private String severity;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "source_ip", nullable = false)
    @org.hibernate.annotations.ColumnTransformer(read = "host(source_ip)", write = "?::inet")
    private String sourceIp;

    @Column(name = "destination_ip", nullable = false)
    @org.hibernate.annotations.ColumnTransformer(read = "host(destination_ip)", write = "?::inet")
    private String destinationIp;

    @Column(name = "destination_port")
    private Integer destinationPort;

    @Column(length = 20)
    private String protocol;

    @Column(name = "app_protocol", length = 100)
    private String appProtocol;

    @Column(name = "raw_event", nullable = false, columnDefinition = "jsonb")
    @org.hibernate.annotations.ColumnTransformer(read = "raw_event::text", write = "?::jsonb")
    private String rawEvent;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt = Instant.now();

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public AlertJpaEntity() {}

    public AlertJpaEntity(
            Long id,
            String rule,
            String severity,
            String title,
            String description,
            String sourceIp,
            String destinationIp,
            Integer destinationPort,
            String protocol,
            String appProtocol,
            String rawEvent,
            Instant detectedAt,
            String status,
            Long assignedTo,
            String resolutionNotes,
            Instant resolvedAt
    ) {
        this.id = id;
        this.rule = rule;
        this.severity = severity;
        this.title = title;
        this.description = description;
        this.sourceIp = sourceIp;
        this.destinationIp = destinationIp;
        this.destinationPort = destinationPort;
        this.protocol = protocol;
        this.appProtocol = appProtocol;
        this.rawEvent = rawEvent;
        this.detectedAt = detectedAt != null ? detectedAt : Instant.now();
        this.status = status != null ? status : "OPEN";
        this.assignedTo = assignedTo;
        this.resolutionNotes = resolutionNotes;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getDestinationIp() {
        return destinationIp;
    }

    public void setDestinationIp(String destinationIp) {
        this.destinationIp = destinationIp;
    }

    public Integer getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(Integer destinationPort) {
        this.destinationPort = destinationPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAppProtocol() {
        return appProtocol;
    }

    public void setAppProtocol(String appProtocol) {
        this.appProtocol = appProtocol;
    }

    public String getRawEvent() {
        return rawEvent;
    }

    public void setRawEvent(String rawEvent) {
        this.rawEvent = rawEvent;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(Instant detectedAt) {
        this.detectedAt = detectedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(Long assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlertJpaEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
