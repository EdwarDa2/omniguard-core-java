package com.omniguard.adapter.in.rest.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username o email es obligatorio")
    @Size(max = 255, message = "Máximo 255 caracteres")
    private String usernameOrEmail;

    @NotBlank(message = "Password es obligatorio")
    @Size(min = 8, max = 128, message = "Password entre 8 y 128 caracteres")
    private String password;

    @Size(max = 64, message = "Device fingerprint máximo 64 caracteres")
    private String deviceFingerprint;
}