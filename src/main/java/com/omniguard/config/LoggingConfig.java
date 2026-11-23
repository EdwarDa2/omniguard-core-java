package com.omniguard.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingConfig extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USER_ID_KEY = "userId";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";

    @Override
    protected void doFilterInternal(
            @jakarta.annotation.Nonnull HttpServletRequest request,
            @jakarta.annotation.Nonnull HttpServletResponse response,
            @jakarta.annotation.Nonnull FilterChain filterChain
    ) throws IOException, jakarta.servlet.ServletException {

        // Generar o extraer request ID
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        MDC.put("requestId", requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        // Correlation ID (para tracing distribuido)
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = requestId;
        }
        MDC.put("correlationId", correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        // Trace ID / Span ID (para OpenTelemetry)
        String traceId = request.getHeader("X-B3-TraceId");
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
        String spanId = request.getHeader("X-B3-SpanId");
        if (spanId != null) {
            MDC.put(SPAN_ID_KEY, spanId);
        }

        // Usuario autenticado (si está disponible en SecurityContext)
        try {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                MDC.put(USER_ID_KEY, auth.getName());
            }
        } catch (Exception ignored) {
            // No hay contexto de seguridad
        }

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            MDC.put("durationMs", String.valueOf(duration));
            MDC.put("status", String.valueOf(response.getStatus()));
            MDC.put("method", request.getMethod());
            MDC.put("uri", request.getRequestURI());

            // Log estructurado
            if (response.getStatus() >= 500) {
                log.error("HTTP {} {} - {}ms - status: {}", request.getMethod(), request.getRequestURI(), duration, response.getStatus());
            } else if (response.getStatus() >= 400) {
                log.warn("HTTP {} {} - {}ms - status: {}", request.getMethod(), request.getRequestURI(), duration, response.getStatus());
            } else {
                log.info("HTTP {} {} - {}ms - status: {}", request.getMethod(), request.getRequestURI(), duration, response.getStatus());
            }

            MDC.clear();
        }
    }
}