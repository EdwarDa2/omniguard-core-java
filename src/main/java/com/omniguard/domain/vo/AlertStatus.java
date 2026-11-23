package com.omniguard.domain.vo;

import jakarta.persistence.Embeddable;
import java.util.Arrays;
import java.util.Objects;

@Embeddable
public record AlertStatus(String value) {
    private static final String[] VALID_STATUSES = {
        "OPEN", "IN_PROGRESS", "RESOLVED", "FALSE_POSITIVE"
    };

    public AlertStatus {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Estado de alerta no puede ser nulo o vacío");
        }
        String upper = value.trim().toUpperCase();
        if (Arrays.stream(VALID_STATUSES).noneMatch(s -> s.equals(upper))) {
            throw new IllegalArgumentException("Estado de alerta inválido: " + value);
        }
    }

    public static AlertStatus of(String value) {
        return new AlertStatus(value);
    }

    public static AlertStatus open() {
        return new AlertStatus("OPEN");
    }

    public static AlertStatus inProgress() {
        return new AlertStatus("IN_PROGRESS");
    }

    public static AlertStatus resolved() {
        return new AlertStatus("RESOLVED");
    }

    public static AlertStatus falsePositive() {
        return new AlertStatus("FALSE_POSITIVE");
    }

    public boolean isOpen() {
        return "OPEN".equals(value);
    }

    public boolean isClosed() {
        return "RESOLVED".equals(value) || "FALSE_POSITIVE".equals(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlertStatus that)) return false;
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