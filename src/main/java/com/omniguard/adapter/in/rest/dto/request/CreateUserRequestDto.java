package com.omniguard.adapter.in.rest.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequestDto(
        @NotBlank(message = "El nombre es obligatorio")
        String name,
        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email,
        String role
) {}
