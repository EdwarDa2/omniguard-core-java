package com.omniguard.adapter.in.rest.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username es obligatorio")
    @Size(min = 3, max = 50, message = "Username entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Username solo permite letras, números, _, ., -")
    private String username;

    @NotBlank(message = "Email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Size(max = 255, message = "Email máximo 255 caracteres")
    private String email;

    @NotBlank(message = "Password es obligatorio")
    @Size(min = 12, max = 128, message = "Password entre 12 y 128 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
        message = "Password debe contener: mayúscula, minúscula, número y símbolo (@$!%*?&)"
    )
    private String password;

    @NotBlank(message = "Rol es obligatorio")
    @Pattern(regexp = "^(ADMIN|ANALYST|VIEWER)$", message = "Rol inválido: ADMIN, ANALYST o VIEWER")
    private String role;
}