package com.omniguard.domain.vo;

import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String value) {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", Pattern.CASE_INSENSITIVE);

    public Email {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Email no puede ser nulo o vacío");
        }
        String trimmed = value.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Formato de email inválido: " + value);
        }
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("Email demasiado largo (máx 255 caracteres)");
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email email)) return false;
        return Objects.equals(value, email.value);
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
