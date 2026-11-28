package com.omniguard.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de limitación de tasa (Rate Limiting) leídas del bloque {@code bucket4j}
 * de application.yml.
 *
 * <p>Cada regla define un patrón de URL, la cantidad máxima de peticiones permitidas por
 * dirección IP y la ventana de tiempo en minutos en la que se recargan los tokens.
 */
@ConfigurationProperties(prefix = "bucket4j")
public record RateLimitProperties(
        boolean enabled,
        boolean trustForwardedHeaders,
        List<Rule> filters
) {

    public RateLimitProperties {
        filters = filters == null ? List.of() : List.copyOf(filters);
    }

    /**
     * Regla individual de limitación de tasa.
     *
     * @param urlPattern      patrón Ant de la ruta protegida (por ejemplo /api/v1/auth/login)
     * @param rateLimit       número máximo de peticiones permitidas dentro de la ventana
     * @param durationMinutes duración de la ventana de recarga en minutos
     */
    public record Rule(String urlPattern, int rateLimit, int durationMinutes) {

        public Rule {
            if (urlPattern == null || urlPattern.isBlank()) {
                throw new IllegalArgumentException("bucket4j.filters[].url-pattern es obligatorio");
            }
            if (rateLimit <= 0) {
                throw new IllegalArgumentException("bucket4j.filters[].rate-limit debe ser mayor a 0");
            }
            if (durationMinutes <= 0) {
                throw new IllegalArgumentException("bucket4j.filters[].duration-minutes debe ser mayor a 0");
            }
        }
    }
}
