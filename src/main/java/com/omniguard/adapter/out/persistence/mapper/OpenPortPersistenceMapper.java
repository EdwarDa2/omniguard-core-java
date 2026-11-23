package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.OpenPortJpaEntity;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.vo.PortNumber;
import com.omniguard.domain.vo.Protocol;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface OpenPortPersistenceMapper {
    OpenPortPersistenceMapper INSTANCE = Mappers.getMapper(OpenPortPersistenceMapper.class);

    @Mapping(target = "portNumber", expression = "java(new PortNumber(entity.getPortNumber()))")
    @Mapping(target = "protocol", expression = "java(Protocol.of(entity.getProtocol().name()))")
    OpenPort toDomain(OpenPortJpaEntity entity);

    @Mapping(target = "portNumber", source = "portNumber.value")
    @Mapping(target = "protocol", source = "protocol.value")
    OpenPortJpaEntity toEntity(OpenPort domain);
}