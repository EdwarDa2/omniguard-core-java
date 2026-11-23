package com.omniguard.adapter.in.rest.dto.auth;

import com.omniguard.adapter.in.rest.dto.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private UserResponse user;
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresInSeconds;
    private String tokenType = "Bearer";
}