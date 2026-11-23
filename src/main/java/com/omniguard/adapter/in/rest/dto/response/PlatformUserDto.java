package com.omniguard.adapter.in.rest.dto.response;

public record PlatformUserDto(
        String id,
        String name,
        String email,
        String role,
        String status,
        String lastLogin,
        boolean isCurrentUser
) {}
