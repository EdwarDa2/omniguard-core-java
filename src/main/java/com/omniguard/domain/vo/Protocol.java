package com.omniguard.domain.vo;

import jakarta.persistence.Embeddable;
import java.util.Arrays;
import java.util.Objects;

@Embeddable
public record Protocol(String value) {
    private static final String[] VALID_PROTOCOLS = {
        "TCP", "UDP", "ICMP", "IGMP", "GRE", "ESP", "AH", "OSPF", "VRRP", "SCTP", "IPIP", "IPV6"
    };

    public Protocol {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Protocolo no puede ser nulo o vacío");
        }
        String upper = value.trim().toUpperCase();
        boolean valid = Arrays.stream(VALID_PROTOCOLS).anyMatch(p -> p.equals(upper))
                || upper.matches("^IP-\\d+$");
        if (!valid) {
            throw new IllegalArgumentException("Protocolo inválido: " + value);
        }
    }

    public static Protocol of(String value) {
        return new Protocol(value);
    }

    public static Protocol of(int ipProtocolNumber) {
        return switch (ipProtocolNumber) {
            case 1 -> new Protocol("ICMP");
            case 6 -> new Protocol("TCP");
            case 17 -> new Protocol("UDP");
            case 2 -> new Protocol("IGMP");
            case 47 -> new Protocol("GRE");
            case 50 -> new Protocol("ESP");
            case 51 -> new Protocol("AH");
            case 89 -> new Protocol("OSPF");
            case 112 -> new Protocol("VRRP");
            case 132 -> new Protocol("SCTP");
            case 4 -> new Protocol("IPIP");
            case 41 -> new Protocol("IPV6");
            default -> new Protocol("IP-" + ipProtocolNumber);
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Protocol that)) return false;
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