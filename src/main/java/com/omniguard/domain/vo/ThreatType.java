package com.omniguard.domain.vo;

import jakarta.persistence.Embeddable;
import java.util.Arrays;
import java.util.Objects;

@Embeddable
public record ThreatType(String value) {
    private static final String[] VALID_TYPES = {
        "MALWARE_C2", "PORT_SCAN", "BRUTE_FORCE", "DATA_EXFIL",
        "LATERAL_MOVEMENT", "ANOMALY", "SUSPICIOUS_PROCESS",
        "PRIVILEGE_ESCALATION", "CREDENTIAL_ACCESS"
    };

    public ThreatType {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Tipo de amenaza no puede ser nulo o vacío");
        }
        String upper = value.trim().toUpperCase();
        if (Arrays.stream(VALID_TYPES).noneMatch(t -> t.equals(upper))) {
            throw new IllegalArgumentException("Tipo de amenaza inválido: " + value);
        }
    }

    public static ThreatType of(String value) {
        return new ThreatType(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ThreatType that)) return false;
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