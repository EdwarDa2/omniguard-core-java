package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.AssetJpaEntity;
import com.omniguard.domain.model.Asset;
import com.omniguard.domain.vo.Hostname;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.MacAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AssetPersistenceMapper {
    AssetPersistenceMapper INSTANCE = Mappers.getMapper(AssetPersistenceMapper.class);

    @Mapping(target = "ipAddress", expression = "java(new IpAddress(entity.getIpAddress()))")
    @Mapping(target = "macAddress", expression = "java(entity.getMacAddress() != null ? new MacAddress(entity.getMacAddress()) : null)")
    @Mapping(target = "hostname", expression = "java(entity.getHostname() != null ? new Hostname(entity.getHostname()) : null)")
    @Mapping(target = "status", source = "status.name")
    Asset toDomain(AssetJpaEntity entity);

    @Mapping(target = "ipAddress", source = "ipAddress.value")
    @Mapping(target = "macAddress", source = "macAddress.value")
    @Mapping(target = "hostname", source = "hostname.value")
    @Mapping(target = "status", source = "status")
    AssetJpaEntity toEntity(Asset domain);
}