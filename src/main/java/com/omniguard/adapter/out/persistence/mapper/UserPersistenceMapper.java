package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.RoleJpaEntity;
import com.omniguard.adapter.out.persistence.entity.UserJpaEntity;
import com.omniguard.domain.model.Role;
import com.omniguard.domain.model.User;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", uses = {RolePersistenceMapper.class})
public interface UserPersistenceMapper {
    UserPersistenceMapper INSTANCE = Mappers.getMapper(UserPersistenceMapper.class);

    @Mapping(target = "username", source = "username")
    @Mapping(target = "email", expression = "java(new Email(entity.getEmail()))")
    @Mapping(target = "role", source = "role")
    User toDomain(UserJpaEntity entity);

    @Mapping(target = "username", source = "username.value")
    @Mapping(target = "email", source = "email.value")
    @Mapping(target = "role", source = "role")
    UserJpaEntity toEntity(User domain);

    default RoleJpaEntity mapRole(Role role) {
        if (role == null) return null;
        RoleJpaEntity entity = new RoleJpaEntity();
        entity.setId(role.id());
        entity.setName(RoleJpaEntity.RoleName.valueOf(role.name()));
        return entity;
    }
}