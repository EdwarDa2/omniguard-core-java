package com.omniguard.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "cve_records", schema = "nvd")
public class CveRecordJpaEntity {

    @Id
    @Column(name = "cve_id", length = 25, nullable = false)
    private String cveId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cvss_version", length = 10)
    private String cvssVersion;

    @Column(name = "cvss_score")
    private Double cvssScore;

    @Column(name = "cvss_severity", length = 20)
    private String cvssSeverity;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_modified_at")
    private Instant lastModifiedAt;

    @Column(name = "nvd_json", columnDefinition = "TEXT")
    private String nvdJson;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt = Instant.now();

    public CveRecordJpaEntity() {}

    public CveRecordJpaEntity(
            String cveId,
            String description,
            String cvssVersion,
            Double cvssScore,
            String cvssSeverity,
            Instant publishedAt,
            Instant lastModifiedAt,
            String nvdJson,
            Instant syncedAt
    ) {
        this.cveId = cveId;
        this.description = description;
        this.cvssVersion = cvssVersion;
        this.cvssScore = cvssScore;
        this.cvssSeverity = cvssSeverity;
        this.publishedAt = publishedAt;
        this.lastModifiedAt = lastModifiedAt;
        this.nvdJson = nvdJson;
        this.syncedAt = syncedAt != null ? syncedAt : Instant.now();
    }

    public String getCveId() {
        return cveId;
    }

    public void setCveId(String cveId) {
        this.cveId = cveId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCvssVersion() {
        return cvssVersion;
    }

    public void setCvssVersion(String cvssVersion) {
        this.cvssVersion = cvssVersion;
    }

    public Double getCvssScore() {
        return cvssScore;
    }

    public void setCvssScore(Double cvssScore) {
        this.cvssScore = cvssScore;
    }

    public String getCvssSeverity() {
        return cvssSeverity;
    }

    public void setCvssSeverity(String cvssSeverity) {
        this.cvssSeverity = cvssSeverity;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(Instant lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public String getNvdJson() {
        return nvdJson;
    }

    public void setNvdJson(String nvdJson) {
        this.nvdJson = nvdJson;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CveRecordJpaEntity that)) return false;
        return Objects.equals(cveId, that.cveId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cveId);
    }
}
