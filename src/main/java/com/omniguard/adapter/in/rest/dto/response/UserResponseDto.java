package com.omniguard.adapter.in.rest.dto.response;

import com.omniguard.domain.model.User;
import java.time.Instant;

public record UserResponseDto(
        Long id,
        String username,
        String email,
        String role,
        boolean isActive,
        Instant lastLoginAt,
        Instant createdAt
) {
    public static UserResponseDto fromDomain(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponseDto(
                user.id(),
                user.username() != null ? user.username().value() : null,
                user.email() != null ? user.email().value() : null,
                user.role() != null ? user.role().name() : null,
                user.isActive(),
                user.lastLoginAt(),
                user.createdAt()
        );
    }
}
