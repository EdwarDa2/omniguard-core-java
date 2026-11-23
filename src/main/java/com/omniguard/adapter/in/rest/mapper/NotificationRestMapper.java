package com.omniguard.adapter.in.rest.mapper;

import com.omniguard.adapter.in.rest.dto.notification.NotificationConfigRequest;
import com.omniguard.adapter.in.rest.dto.notification.NotificationConfigResponse;
import com.omniguard.domain.model.NotificationConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationRestMapper {
    NotificationRestMapper INSTANCE = Mappers.getMapper(NotificationRestMapper.class);

    @Mapping(target = "platform", source = "platform")
    NotificationConfig toDomain(NotificationConfigRequest request);

    @Mapping(target = "platform", source = "platform.name")
    NotificationConfigResponse toResponse(NotificationConfig domain);

    List<NotificationConfigResponse> toResponseList(List<NotificationConfig> domains);
}