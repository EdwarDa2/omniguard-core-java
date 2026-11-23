package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.PasetoSessionJpaEntity;
import com.omniguard.domain.model.PasetoSession;
import com.omniguard.domain.vo.IpAddress;
import org.springframework.stereotype.Component;

@Component
public class PasetoSessionPersistenceMapper {

    public PasetoSession toDomain(PasetoSessionJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        IpAddress ip = entity.getIpAddress() != null && !entity.getIpAddress().isBlank()
                ? IpAddress.of(entity.getIpAddress())
                : null;

        return new PasetoSession(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getRevokeReason(),
                ip,
                entity.getUserAgent()
        );
    }

    public PasetoSessionJpaEntity toEntity(PasetoSession domain) {
        if (domain == null) {
            return null;
        }

        String ipStr = domain.ipAddress() != null ? domain.ipAddress().value() : null;

        return new PasetoSessionJpaEntity(
                domain.id(),
                domain.userId(),
                domain.tokenHash(),
                domain.issuedAt(),
                domain.expiresAt(),
                domain.revokedAt(),
                domain.revokeReason(),
                ipStr,
                domain.userAgent()
        );
    }
}
