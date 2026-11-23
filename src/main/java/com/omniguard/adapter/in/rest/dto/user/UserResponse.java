package com.omniguard.adapter.in.rest.dto.user;

import com.omniguard.adapter.in.rest.dto.role.RoleResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private boolean isActive;
    private boolean totpEnabled;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
    private RoleResponse role;
}