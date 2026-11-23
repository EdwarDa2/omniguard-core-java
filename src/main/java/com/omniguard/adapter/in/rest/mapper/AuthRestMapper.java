package com.omniguard.adapter.in.rest.mapper;

import com.omniguard.adapter.in.rest.dto.auth.AuthResponse;
import com.omniguard.adapter.in.rest.dto.user.UserResponse;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.in.AuthUseCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = {UserRestMapper.class})
public interface AuthRestMapper {
    AuthRestMapper INSTANCE = Mappers.getMapper(AuthRestMapper.class);

    @Mapping(target = "user", source = "user")
    AuthResponse toResponse(AuthUseCase.AuthResult result);
}