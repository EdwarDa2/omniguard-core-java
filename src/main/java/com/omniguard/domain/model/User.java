package com.omniguard.domain.model;

import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import java.time.Instant;
import java.util.UUID;

public record User(
        UUID id,
        Role role,
        Username username,
        Email email,
        String passwordHash,
        boolean isActive,
        String totpSecret,
        boolean totpEnabled,
        Instant lastLoginAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static User create(
            Role role,
            Username username,
            Email email,
            String passwordHash
    ) {
        Instant now = Instant.now();
        return new User(
                UUID.randomUUID(),
                role,
                username,
                email,
                passwordHash,
                true,
                null,
                false,
                null,
                now,
                now
        );
    }

    public User updateProfile(Username username, Email email) {
        return new User(
                this.id,
                this.role,
                username,
                email,
                this.passwordHash,
                this.isActive,
                this.totpSecret,
                this.totpEnabled,
                this.lastLoginAt,
                this.createdAt,
                Instant.now()
        );
    }

    public User changePassword(String newPasswordHash) {
        return new User(
                this.id,
                this.role,
                this.username,
                this.email,
                newPasswordHash,
                this.isActive,
                this.totpSecret,
                this.totpEnabled,
                this.lastLoginAt,
                this.createdAt,
                Instant.now()
        );
    }

    public User enableTotp(String totpSecret) {
        return new User(
                this.id,
                this.role,
                this.username,
                this.email,
                this.passwordHash,
                this.isActive,
                totpSecret,
                true,
                this.lastLoginAt,
                this.createdAt,
                Instant.now()
        );
    }

    public User disableTotp() {
        return new User(
                this.id,
                this.role,
                this.username,
                this.email,
                this.passwordHash,
                this.isActive,
                null,
                false,
                this.lastLoginAt,
                this.createdAt,
                Instant.now()
        );
    }

    public User recordLogin() {
        return new User(
                this.id,
                this.role,
                this.username,
                this.email,
                this.passwordHash,
                this.isActive,
                this.totpSecret,
                this.totpEnabled,
                Instant.now(),
                this.createdAt,
                this.updatedAt
        );
    }

    public User deactivate() {
        return new User(
                this.id,
                this.role,
                this.username,
                this.email,
                this.passwordHash,
                false,
                this.totpSecret,
                this.totpEnabled,
                this.lastLoginAt,
                this.createdAt,
                Instant.now()
        );
    }

    public User activate() {
        return new User(
                this.id,
                this.role,
                this.username,
                this.email,
                this.passwordHash,
                true,
                this.totpSecret,
                this.totpEnabled,
                this.lastLoginAt,
                this.createdAt,
                Instant.now()
        );
    }

    public User assignRole(Role newRole) {
        return new User(
                this.id,
                newRole,
                this.username,
                this.email,
                this.passwordHash,
                this.isActive,
                this.totpSecret,
                this.totpEnabled,
                this.lastLoginAt,
                this.createdAt,
                Instant.now()
        );
    }
}