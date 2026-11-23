package com.omniguard.adapter.in.rest.mapper;

import com.omniguard.adapter.in.rest.dto.role.RoleResponse;
import com.omniguard.domain.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RoleRestMapper {
    RoleRestMapper INSTANCE = Mappers.getMapper(RoleRestMapper.class);

    @Mapping(target = "name", source = "name")
    RoleResponse toResponse(Role domain);
}