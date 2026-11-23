package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.RoleJpaEntity;
import com.omniguard.domain.model.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RolePersistenceMapper {
    RolePersistenceMapper INSTANCE = Mappers.getMapper(RolePersistenceMapper.class);

    @Mapping(target = "name", source = "name.name")
    Role toDomain(RoleJpaEntity entity);

    @Mapping(target = "name", source = "name")
    RoleJpaEntity toEntity(Role domain);
}