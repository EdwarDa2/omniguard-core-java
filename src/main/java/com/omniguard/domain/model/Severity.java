package com.omniguard.domain.model;

public enum Severity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    NONE,
    INFO;

    public static Severity fromString(String value) {
        if (value == null || value.isBlank()) {
            return INFO;
        }
        try {
            return Severity.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return INFO;
        }
    }
}