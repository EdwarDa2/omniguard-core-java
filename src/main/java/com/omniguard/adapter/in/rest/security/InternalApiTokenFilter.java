package com.omniguard.adapter.in.rest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalApiTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalApiTokenFilter.class);
    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    private final String expectedToken;

    public InternalApiTokenFilter(
            @Value("${internal.api.token}") String expectedToken
    ) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String token = request.getHeader(HEADER_INTERNAL_TOKEN);

        if (token != null && !token.isBlank() && token.equals(expectedToken)) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "internal-m2m",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else if (path.startsWith("/api/v1/internal/")) {
            log.warn("Acceso denegado a ruta interna {}: Token ausente o inválido", path);
        }

        filterChain.doFilter(request, response);
    }
}
