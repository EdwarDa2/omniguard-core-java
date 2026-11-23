package com.omniguard.domain.vo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public record IpAddress(String value) {
    public IpAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("IP address no puede ser nula o vacía");
        }
        String trimmed = value.trim();
        try {
            InetAddress.getByName(trimmed);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("IP address inválida: " + value);
        }
    }

    public static IpAddress of(String value) {
        return new IpAddress(value);
    }

    public static IpAddress of(InetAddress inetAddress) {
        return new IpAddress(inetAddress.getHostAddress());
    }

    public InetAddress toInetAddress() {
        try {
            return InetAddress.getByName(value);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("IP address inválida almacenada: " + value, e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IpAddress that)) return false;
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
