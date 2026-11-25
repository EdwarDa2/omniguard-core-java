package com.omniguard.application.service;

import com.omniguard.domain.exception.BusinessRuleViolationException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ScanTargetValidator {

    private static final Logger log = LoggerFactory.getLogger(ScanTargetValidator.class);

    private final List<String> authorizedTargets;
    private final int maxPorts;

    public ScanTargetValidator(
            @Value("${scan.authorized-targets:127.0.0.1,localhost,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16}") String authorizedTargetsConfig,
            @Value("${scan.max-ports:1024}") int maxPorts
    ) {
        this.authorizedTargets = Arrays.stream(authorizedTargetsConfig.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        this.maxPorts = maxPorts;
    }

    public void validatePorts(String ports) {
        if (ports == null || ports.isBlank()) {
            return;
        }

        String trimmed = ports.trim();
        int count = 0;

        if (trimmed.contains("-")) {
            String[] parts = trimmed.split("-");
            if (parts.length != 2) {
                throw new BusinessRuleViolationException("Formato de rango de puertos inválido: " + ports);
            }
            try {
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                if (start < 1 || end > 65535 || start > end) {
                    throw new BusinessRuleViolationException("Rango de puertos fuera de límites (1-65535): " + ports);
                }
                count = (end - start) + 1;
            } catch (NumberFormatException e) {
                throw new BusinessRuleViolationException("Los puertos deben ser números enteros: " + ports);
            }
        } else if (trimmed.contains(",")) {
            String[] parts = trimmed.split(",");
            count = parts.length;
            for (String p : parts) {
                try {
                    int portNum = Integer.parseInt(p.trim());
                    if (portNum < 1 || portNum > 65535) {
                        throw new BusinessRuleViolationException("Puerto fuera de límites (1-65535): " + portNum);
                    }
                } catch (NumberFormatException e) {
                    throw new BusinessRuleViolationException("Puerto no numérico inválido: " + p);
                }
            }
        } else {
            try {
                int portNum = Integer.parseInt(trimmed);
                if (portNum < 1 || portNum > 65535) {
                    throw new BusinessRuleViolationException("Puerto fuera de límites (1-65535): " + portNum);
                }
                count = 1;
            } catch (NumberFormatException e) {
                throw new BusinessRuleViolationException("Puerto no numérico inválido: " + ports);
            }
        }

        if (count > maxPorts) {
            throw new BusinessRuleViolationException(
                    "El número de puertos solicitado (" + count + ") excede el límite máximo autorizado de " + maxPorts + " puertos por orden."
            );
        }
    }

    public boolean isTargetAuthorized(String ipOrHost) {
        if (ipOrHost == null || ipOrHost.isBlank()) {
            return false;
        }

        String target = ipOrHost.trim().toLowerCase();
        for (String rule : authorizedTargets) {
            String ruleClean = rule.trim().toLowerCase();
            if (matchesRule(target, ruleClean)) {
                return true;
            }
        }
        return false;
    }

    public void validateTarget(String ipOrHost) {
        if (!isTargetAuthorized(ipOrHost)) {
            log.warn("Intento de escaneo denegado para el objetivo no autorizado: {}", ipOrHost);
            throw new BusinessRuleViolationException(
                    "El activo objetivo " + ipOrHost + " no se encuentra en la allowlist de objetivos autorizados para escaneo perimetral."
            );
        }
    }

    private boolean matchesRule(String target, String rule) {
        if (rule.equals("localhost") && target.equals("localhost")) return true;
        if (rule.equals(target)) return true;

        if (rule.contains("/")) {
            return isInSubnet(target, rule);
        }
        return false;
    }

    private boolean isInSubnet(String ipStr, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String subnetIp = parts[0];
            int prefix = Integer.parseInt(parts[1]);
            long ipVal = ipToLong(ipStr);
            long subnetVal = ipToLong(subnetIp);
            long mask = prefix == 0 ? 0 : (~0L << (32 - prefix)) & 0xFFFFFFFFL;
            return (ipVal & mask) == (subnetVal & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] octets = ip.trim().split("\\.");
        if (octets.length != 4) throw new IllegalArgumentException("Invalid IPv4 address: " + ip);
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) | (Integer.parseInt(octets[i]) & 0xFF);
        }
        return result;
    }
}
