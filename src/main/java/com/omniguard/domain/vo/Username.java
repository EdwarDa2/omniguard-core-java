package com.omniguard.domain.vo;

import java.util.Objects;
import java.util.regex.Pattern;

public record Username(String value) {
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_.-]{3,50}$");

    public Username {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Username no puede ser nulo o vacío");
        }
        String trimmed = value.trim();
        if (!USERNAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Username inválido: solo letras, números, _, ., - (3-50 caracteres)");
        }
    }

    public static Username of(String value) {
        return new Username(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Username username)) return false;
        return Objects.equals(value, username.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
