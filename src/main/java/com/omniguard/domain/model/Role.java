package com.omniguard.domain.model;

import java.time.Instant;

public record Role(
        Long id,
        String name,
        String description,
        Instant createdAt
) {
    public static Role create(String name, String description) {
        return new Role(null, name, description, Instant.now());
    }

    public static Role of(Long id, String name) {
        return new Role(id, name, null, null);
    }

    public Role updateDescription(String description) {
        return new Role(this.id, this.name, description, this.createdAt);
    }
}
