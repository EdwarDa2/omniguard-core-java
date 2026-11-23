package com.omniguard.domain.vo;

import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;

@Embeddable
public record MacAddress(String value) {
    private static final Pattern MAC_PATTERN =
            Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$");

    public MacAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MAC address no puede ser nula o vacía");
        }
        String trimmed = value.trim().toUpperCase().replace('-', ':');
        if (!MAC_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Formato MAC inválido (esperado XX:XX:XX:XX:XX:XX): " + value);
        }
    }

    public static MacAddress of(String value) {
        return new MacAddress(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MacAddress that)) return false;
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