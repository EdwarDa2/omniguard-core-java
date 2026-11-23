package com.omniguard.domain.vo;

import java.util.Objects;
import java.util.regex.Pattern;

public record CveId(String value) {
    private static final Pattern CVE_PATTERN =
            Pattern.compile("^CVE-\\d{4}-\\d{4,}$", Pattern.CASE_INSENSITIVE);

    public CveId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CVE ID no puede ser nulo o vacío");
        }
        String upper = value.trim().toUpperCase();
        if (!CVE_PATTERN.matcher(upper).matches()) {
            throw new IllegalArgumentException("Formato CVE inválido (esperado CVE-YYYY-NNNNN): " + value);
        }
    }

    public static CveId of(String value) {
        return new CveId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CveId cveId)) return false;
        return Objects.equals(value, cveId.value);
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
