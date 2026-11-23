package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.PasetoSessionJpaEntity;
import com.omniguard.domain.model.PasetoSession;
import com.omniguard.domain.vo.IpAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface PasetoSessionPersistenceMapper {
    PasetoSessionPersistenceMapper INSTANCE = Mappers.getMapper(PasetoSessionPersistenceMapper.class);

    @Mapping(target = "ipAddress", expression = "java(entity.getIpAddress() != null ? new IpAddress(entity.getIpAddress()) : null)")
    PasetoSession toDomain(PasetoSessionJpaEntity entity);

    @Mapping(target = "ipAddress", source = "ipAddress.value")
    PasetoSessionJpaEntity toEntity(PasetoSession domain);
}