package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.CveRecordJpaEntity;
import com.omniguard.adapter.out.persistence.entity.NetworkAssetJpaEntity;
import com.omniguard.adapter.out.persistence.entity.ServiceJpaEntity;
import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.model.Severity;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.vo.CveId;
import com.omniguard.domain.vo.IpAddress;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AssetPersistenceMapper {

    public Asset toDomain(NetworkAssetJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Asset(
                entity.getId(),
                IpAddress.of(entity.getIpAddress()),
                entity.getHostname(),
                entity.getDescription(),
                entity.getOs(),
                entity.getStatus(),
                entity.getFirstSeenAt(),
                entity.getLastSeenAt(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    public Asset toDomain(
            NetworkAssetJpaEntity entity,
            List<OpenPort> ports,
            List<Vulnerability> vulnerabilities
    ) {
        if (entity == null) {
            return null;
        }
        return new Asset(
                entity.getId(),
                IpAddress.of(entity.getIpAddress()),
                entity.getHostname(),
                entity.getDescription(),
                entity.getOs(),
                entity.getStatus(),
                entity.getFirstSeenAt(),
                entity.getLastSeenAt(),
                ports != null ? ports : Collections.emptyList(),
                vulnerabilities != null ? vulnerabilities : Collections.emptyList()
        );
    }

    public NetworkAssetJpaEntity toJpaEntity(Asset domain) {
        if (domain == null) {
            return null;
        }
        return new NetworkAssetJpaEntity(
                domain.id(),
                domain.ipAddress().getValue(),
                domain.hostname(),
                domain.description(),
                domain.os(),
                domain.status(),
                domain.firstSeenAt(),
                domain.lastSeenAt()
        );
    }

    public OpenPort toDomain(ServiceJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OpenPort(
                entity.getId(),
                entity.getAssetId(),
                entity.getPort(),
                entity.getProtocol(),
                entity.getAppProtocol(),
                entity.getBanner(),
                entity.getProductVersion(),
                entity.getCpeName(),
                entity.getFirstSeenAt(),
                entity.getLastSeenAt()
        );
    }

    public ServiceJpaEntity toJpaEntity(OpenPort domain) {
        if (domain == null) {
            return null;
        }
        return new ServiceJpaEntity(
                domain.id(),
                domain.assetId(),
                domain.port(),
                domain.protocol(),
                domain.appProtocol(),
                domain.cpeName(),
                domain.productVersion(),
                domain.banner(),
                domain.firstSeenAt(),
                domain.lastSeenAt()
        );
    }

    public Vulnerability toDomain(CveRecordJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Vulnerability(
                CveId.of(entity.getCveId()),
                entity.getDescription(),
                entity.getCvssVersion(),
                entity.getCvssScore(),
                Severity.fromString(entity.getCvssSeverity()),
                entity.getPublishedAt(),
                entity.getLastModifiedAt(),
                entity.getSyncedAt()
        );
    }
}
