package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.NotificationConfigJpaEntity;
import com.omniguard.domain.model.NotificationConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface NotificationConfigPersistenceMapper {
    NotificationConfigPersistenceMapper INSTANCE = Mappers.getMapper(NotificationConfigPersistenceMapper.class);

    @Mapping(target = "platform", source = "platform.name")
    NotificationConfig toDomain(NotificationConfigJpaEntity entity);

    @Mapping(target = "platform", source = "platform")
    NotificationConfigJpaEntity toEntity(NotificationConfig domain);
}