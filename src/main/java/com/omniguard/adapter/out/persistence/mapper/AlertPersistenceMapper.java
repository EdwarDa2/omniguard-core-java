package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.AlertJpaEntity;
import com.omniguard.domain.model.Alert;
import com.omniguard.domain.vo.AlertStatus;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.Protocol;
import com.omniguard.domain.vo.ThreatType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface AlertPersistenceMapper {
    AlertPersistenceMapper INSTANCE = Mappers.getMapper(AlertPersistenceMapper.class);

    @Mapping(target = "sourceIp", expression = "java(new IpAddress(entity.getSourceIp()))")
    @Mapping(target = "destinationIp", expression = "java(new IpAddress(entity.getDestinationIp()))")
    @Mapping(target = "protocol", expression = "java(Protocol.of(entity.getProtocol().name()))")
    @Mapping(target = "threatType", expression = "java(ThreatType.of(entity.getThreatType().name()))")
    @Mapping(target = "status", expression = "java(AlertStatus.of(entity.getStatus().name()))")
    @Mapping(target = "assignedTo", source = "assignedTo.id")
    Alert toDomain(AlertJpaEntity entity);

    @Mapping(target = "sourceIp", source = "sourceIp.value")
    @Mapping(target = "destinationIp", source = "destinationIp.value")
    @Mapping(target = "protocol", source = "protocol.value")
    @Mapping(target = "threatType", source = "threatType.value")
    @Mapping(target = "status", source = "status.value")
    AlertJpaEntity toEntity(Alert domain);
}