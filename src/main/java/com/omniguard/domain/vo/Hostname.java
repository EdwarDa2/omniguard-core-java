package com.omniguard.domain.vo;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public record Hostname(String value) {
    private static final Pattern HOSTNAME_PATTERN =
            Pattern.compile("^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$|^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)$");

    public Hostname {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hostname no puede ser nulo o vacío");
        }
        String trimmed = value.trim().toLowerCase();
        if (trimmed.length() > 255) {
            throw new IllegalArgumentException("Hostname demasiado largo (máx 255 caracteres)");
        }
        if (!HOSTNAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Formato hostname inválido: " + value);
        }
    }

    public static Hostname of(String value) {
        return new Hostname(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hostname that)) return false;
        return Objects.equals(value, that.value);
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