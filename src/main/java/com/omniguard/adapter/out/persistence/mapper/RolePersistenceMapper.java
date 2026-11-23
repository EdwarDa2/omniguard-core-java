package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.RoleJpaEntity;
import com.omniguard.domain.model.Role;
import org.springframework.stereotype.Component;

@Component
public class RolePersistenceMapper {

    public Role toDomain(RoleJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Role(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    public RoleJpaEntity toEntity(Role domain) {
        if (domain == null) {
            return null;
        }
        return new RoleJpaEntity(
                domain.id(),
                domain.name(),
                domain.description(),
                domain.createdAt()
        );
    }
}
