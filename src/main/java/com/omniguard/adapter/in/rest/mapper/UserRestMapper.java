package com.omniguard.adapter.in.rest.mapper;

import com.omniguard.adapter.in.rest.dto.role.RoleResponse;
import com.omniguard.adapter.in.rest.dto.user.UserResponse;
import com.omniguard.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = {RoleRestMapper.class})
public interface UserRestMapper {
    UserRestMapper INSTANCE = Mappers.getMapper(UserRestMapper.class);

    @Mapping(target = "role", source = "role")
    UserResponse toResponse(User domain);
}