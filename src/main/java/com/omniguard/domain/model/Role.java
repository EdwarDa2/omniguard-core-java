package com.omniguard.domain.model;

import java.time.Instant;
import java.util.UUID;

public record Role(
        UUID id,
        String name,
        String description,
        Instant createdAt
) {
    public static Role create(String name, String description) {
        return new Role(UUID.randomUUID(), name, description, Instant.now());
    }

    public static Role of(UUID id, String name) {
        return new Role(id, name, null, null);
    }

    public Role updateDescription(String description) {
        return new Role(this.id, this.name, description, this.createdAt);
    }
}