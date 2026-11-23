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
@Table(name = "services", schema = "assets")
public class ServiceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false, length = 10)
    private String protocol = "TCP";

    @Column(name = "app_protocol", nullable = false, length = 100)
    private String appProtocol = "Desconocido";

    @Column(name = "cpe_name", length = 500)
    private String cpeName;

    @Column(name = "product_version", length = 100)
    private String productVersion;

    @Column(columnDefinition = "TEXT")
    private String banner;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt = Instant.now();

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt = Instant.now();

    public ServiceJpaEntity() {}

    public ServiceJpaEntity(
            Long id,
            Long assetId,
            Integer port,
            String protocol,
            String appProtocol,
            String cpeName,
            String productVersion,
            String banner,
            Instant firstSeenAt,
            Instant lastSeenAt
    ) {
        this.id = id;
        this.assetId = assetId;
        this.port = port;
        this.protocol = protocol != null ? protocol.toUpperCase() : "TCP";
        this.appProtocol = appProtocol != null ? appProtocol : "Desconocido";
        this.cpeName = cpeName;
        this.productVersion = productVersion;
        this.banner = banner;
        this.firstSeenAt = firstSeenAt != null ? firstSeenAt : Instant.now();
        this.lastSeenAt = lastSeenAt != null ? lastSeenAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
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

    public String getCpeName() {
        return cpeName;
    }

    public void setCpeName(String cpeName) {
        this.cpeName = cpeName;
    }

    public String getProductVersion() {
        return productVersion;
    }

    public void setProductVersion(String productVersion) {
        this.productVersion = productVersion;
    }

    public String getBanner() {
        return banner;
    }

    public void setBanner(String banner) {
        this.banner = banner;
    }

    public Instant getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(Instant firstSeenAt) {
        this.firstSeenAt = firstSeenAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ServiceJpaEntity that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
