package com.omniguard.adapter.in.rest.filter;

import com.omniguard.config.RateLimitProperties;
import com.omniguard.config.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // Después del LoggingConfig
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitProperties rateLimitProperties;

    @Override
    protected void doFilterInternal(
            jakarta.annotation.Nonnull HttpServletRequest request,
            jakarta.annotation.Nonnull HttpServletResponse response,
            jakarta.annotation.Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String identifier = getClientIdentifier(request);

        // Determinar regla según endpoint
        String ruleKey = resolveRuleKey(path, method);
        RateLimitProperties.RateLimitRule rule = rateLimitProperties.getRules().getOrDefault(ruleKey, rateLimitProperties.getRules().get("api:default"));

        // Intentar consumir token
        RateLimitService.RateLimitResult result = rateLimitService.tryConsume(ruleKey, identifier, 1);

        // Headers de rate limit
        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, result.remainingTokens())));

        if (!result.allowed()) {
            response.setHeader("Retry-After", String.valueOf(result.retryAfterSeconds()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + result.retryAfterSeconds()));

            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"timestamp\":\"%s\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Try again in %d seconds.\",\"path\":\"%s\"}",
                    java.time.Instant.now(), result.retryAfterSeconds(), request.getRequestURI()
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Prioridad: usuario autenticado > IP
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return "user:" + auth.getName();
        }
        // IP con soporte para proxy
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return "ip:" + ip.split(",")[0].trim();
    }

    private String resolveRuleKey(String path, String method) {
        if (path.startsWith("/api/v1/auth/")) {
            if (path.endsWith("/login") && "POST".equals(method)) return "auth:login";
            if (path.endsWith("/register") && "POST".equals(method)) return "auth:register";
            if (path.endsWith("/refresh") && "POST".equals(method)) return "auth:refresh";
            return "auth:default";
        }
        if (path.equals("/api/v1/alerts/stream") && "GET".equals(method)) return "sse:stream";
        return "api:default";
    }
}