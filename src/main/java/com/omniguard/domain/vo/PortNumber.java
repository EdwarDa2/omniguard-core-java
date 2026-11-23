package com.omniguard.domain.vo;

import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public record PortNumber(int value) {
    public PortNumber {
        if (value < 1 || value > 65535) {
            throw new IllegalArgumentException("Puerto inválido (1-65535): " + value);
        }
    }

    public static PortNumber of(int value) {
        return new PortNumber(value);
    }

    public static PortNumber of(String value) {
        return new PortNumber(Integer.parseInt(value));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PortNumber that)) return false;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}