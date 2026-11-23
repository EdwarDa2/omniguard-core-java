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
public class ChangePasswordRequest {

    @NotBlank(message = "Password actual es obligatorio")
    private String currentPassword;

    @NotBlank(message = "Nuevo password es obligatorio")
    @Size(min = 12, max = 128, message = "Password entre 12 y 128 caracteres")
    @jakarta.validation.constraints.Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
        message = "Password debe contener: mayúscula, minúscula, número y símbolo (@$!%*?&)"
    )
    private String newPassword;
}