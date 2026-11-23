package com.omniguard.adapter.out.persistence.mapper;

import com.omniguard.adapter.out.persistence.entity.RoleJpaEntity;
import com.omniguard.adapter.out.persistence.entity.UserJpaEntity;
import com.omniguard.domain.model.Role;
import com.omniguard.domain.model.User;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class UserPersistenceMapper {

    private final RolePersistenceMapper roleMapper;

    public UserPersistenceMapper(RolePersistenceMapper roleMapper) {
        this.roleMapper = roleMapper;
    }

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        Role role = entity.getRole() != null ? roleMapper.toDomain(entity.getRole()) : null;
        Username username = entity.getUsername() != null ? Username.of(entity.getUsername()) : null;
        Email email = entity.getEmail() != null ? Email.of(entity.getEmail()) : null;
        boolean isActive = "active".equalsIgnoreCase(entity.getStatus());

        return new User(
                entity.getId(),
                role,
                username,
                email,
                entity.getPasswordHash(),
                isActive,
                entity.getTotpSecret(),
                entity.isTotpEnabled(),
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserJpaEntity toEntity(User domain, RoleJpaEntity roleEntity) {
        if (domain == null) {
            return null;
        }

        String status = domain.isActive() ? "active" : "disabled";
        String usernameStr = domain.username() != null ? domain.username().value() : null;
        String emailStr = domain.email() != null ? domain.email().value() : null;
        String displayName = usernameStr != null ? usernameStr : "User";

        return new UserJpaEntity(
                domain.id(),
                usernameStr,
                displayName,
                emailStr,
                domain.passwordHash(),
                status,
                roleEntity,
                domain.totpSecret(),
                domain.totpEnabled(),
                domain.lastLoginAt(),
                domain.createdAt() != null ? domain.createdAt() : Instant.now(),
                domain.updatedAt() != null ? domain.updatedAt() : Instant.now()
        );
    }
}
