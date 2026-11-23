package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.AuditLogJpaEntity;
import com.omniguard.domain.model.AuditLog;
import com.omniguard.domain.vo.IpAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AuditLogPersistenceMapper {
    AuditLogPersistenceMapper INSTANCE = Mappers.getMapper(AuditLogPersistenceMapper.class);

    @Mapping(target = "ipAddress", expression = "java(entity.getIpAddress() != null ? new IpAddress(entity.getIpAddress()) : null)")
    @Mapping(target = "action", source = "action.name")
    @Mapping(target = "changedBy", source = "changedBy.id")
    AuditLog toDomain(AuditLogJpaEntity entity);

    @Mapping(target = "ipAddress", source = "ipAddress.value")
    @Mapping(target = "action", source = "action")
    AuditLogJpaEntity toEntity(AuditLog domain);
}