package com.omniguard.adapter.in.rest.dto.user;

import com.omniguard.adapter.in.rest.dto.auth.RegisterRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {

    private String username;
    private String email;
    private String password;
    private String role;
    private boolean isActive;
}