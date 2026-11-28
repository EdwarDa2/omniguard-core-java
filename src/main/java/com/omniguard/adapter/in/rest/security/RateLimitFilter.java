package com.omniguard.adapter.in.rest.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.omniguard.adapter.in.rest.dto.response.ErrorResponseDto;
import com.omniguard.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de limitación de tasa (Rate Limiting) basado en el algoritmo Token Bucket (Bucket4j).
 *
 * <p>Mitiga ataques de fuerza bruta contra los endpoints de autenticación y ataques de
 * denegación de servicio (DoS) sobre rutas costosas. Cada combinación (regla, IP cliente)
 * dispone de un bucket independiente. Cuando el bucket se agota, la petición se rechaza con
 * HTTP 429 Too Many Requests y la cabecera Retry-After.
 *
 * <p>Los buckets se almacenan en memoria en una caché acotada con expiración por inactividad,
 * evitando el agotamiento de memoria ante un número elevado de IPs distintas.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    static final String HEADER_LIMIT = "X-RateLimit-Limit";
    static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    static final String HEADER_RETRY_AFTER = "Retry-After";

    private static final long MAX_TRACKED_CLIENTS = 100_000L;

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Cache<String, Bucket> buckets;

    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        long maxWindowMinutes = properties.filters().stream()
                .mapToLong(RateLimitProperties.Rule::durationMinutes)
                .max()
                .orElse(1L);
        this.buckets = CacheBuilder.newBuilder()
                .maximumSize(MAX_TRACKED_CLIENTS)
                .expireAfterAccess(maxWindowMinutes * 2, TimeUnit.MINUTES)
                .build();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.enabled()
                || HttpMethod.OPTIONS.matches(request.getMethod())
                || findRule(request).isEmpty();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        RateLimitProperties.Rule rule = findRule(request).orElseThrow();
        String clientIp = resolveClientIp(request);
        Bucket bucket = resolveBucket(rule, clientIp);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        response.setHeader(HEADER_LIMIT, String.valueOf(rule.rateLimit()));

        if (probe.isConsumed()) {
            response.setHeader(HEADER_REMAINING, String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1L,
                TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1L);

        log.warn("Rate limit excedido: ip={} ruta={} limite={}/{}min",
                clientIp, request.getRequestURI(), rule.rateLimit(), rule.durationMinutes());

        writeTooManyRequests(request, response, retryAfterSeconds);
    }

    private Optional<RateLimitProperties.Rule> findRule(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        List<RateLimitProperties.Rule> rules = properties.filters();
        return rules.stream()
                .filter(rule -> pathMatcher.match(rule.urlPattern(), path))
                .findFirst();
    }

    private Bucket resolveBucket(RateLimitProperties.Rule rule, String clientIp) {
        String key = rule.urlPattern() + "|" + clientIp;
        try {
            return buckets.get(key, () -> newBucket(rule));
        } catch (ExecutionException e) {
            throw new IllegalStateException("No fue posible crear el bucket de rate limiting", e);
        }
    }

    private static Bucket newBucket(RateLimitProperties.Rule rule) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(rule.rateLimit())
                .refillIntervally(rule.rateLimit(), Duration.ofMinutes(rule.durationMinutes()))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Resuelve la IP del cliente. Detrás del proxy inverso Nginx se confía en X-Real-IP
     * (sobrescrito por Nginx con $remote_addr) o en la última entrada de X-Forwarded-For
     * (agregada por el propio proxy), ya que la primera entrada puede ser falsificada.
     */
    String resolveClientIp(HttpServletRequest request) {
        if (properties.trustForwardedHeaders()) {
            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                return realIp.trim();
            }
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String[] hops = forwardedFor.split(",");
                return hops[hops.length - 1].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HEADER_REMAINING, "0");
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponseDto body = ErrorResponseDto.of(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Demasiadas solicitudes. Intente nuevamente en " + retryAfterSeconds + " segundos.",
                request.getRequestURI()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
