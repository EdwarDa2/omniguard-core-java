package com.omniguard.adapter.in.rest.security;

import com.omniguard.adapter.out.security.paseto.PasetoTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class PasetoAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PasetoAuthenticationFilter.class);

    private static final String ACCESS_TOKEN_COOKIE = "at";
    private static final String REFRESH_TOKEN_COOKIE = "rt";

    private final PasetoTokenService pasetoTokenService;

    public PasetoAuthenticationFilter(PasetoTokenService pasetoTokenService) {
        this.pasetoTokenService = pasetoTokenService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String accessToken = extractAccessToken(request);

        if (accessToken != null) {
            try {
                PasetoTokenService.PasetoClaims claims = pasetoTokenService.validateAccessToken(accessToken);

                if ("access".equals(claims.type())) {
                    List<SimpleGrantedAuthority> authorities = parseRoles(claims.roles());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(claims.sub(), null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.debug("Access token inválido: {}", e.getMessage());
                // No autenticar, continuar como anónimo
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractAccessToken(HttpServletRequest request) {
        // 1. Intentar desde cookie HttpOnly
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> atCookie = Arrays.stream(cookies)
                    .filter(c -> ACCESS_TOKEN_COOKIE.equals(c.getName()))
                    .findFirst();
            if (atCookie.isPresent()) {
                return atCookie.get().getValue();
            }
        }

        // 2. Fallback: Header Authorization (para testing/API clients)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    private List<SimpleGrantedAuthority> parseRoles(String rolesJson) {
        if (rolesJson == null || rolesJson.isBlank() || "[]".equals(rolesJson)) {
            return List.of();
        }
        // Parse simple: ["ROLE1","ROLE2"]
        String cleaned = rolesJson.replace("[", "").replace("]", "").replace("\"", "");
        return Arrays.stream(cleaned.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    // ============================================================
    // Helpers para controladores (setear cookies)
    // ============================================================
    public static void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        boolean isProduction = "production".equalsIgnoreCase(System.getenv("SPRING_PROFILES_ACTIVE"));
        // Access token: 15 min, path /, HttpOnly, SameSite=Lax
        Cookie atCookie = new Cookie(ACCESS_TOKEN_COOKIE, accessToken);
        atCookie.setHttpOnly(true);
        atCookie.setSecure(isProduction);
        atCookie.setPath("/");
        atCookie.setMaxAge(15 * 60); // 15 minutos
        atCookie.setAttribute("SameSite", "Lax");
        response.addCookie(atCookie);

        // Refresh token: 30 días, path /api/v1/auth/refresh, HttpOnly, SameSite=Lax
        Cookie rtCookie = new Cookie(REFRESH_TOKEN_COOKIE, refreshToken);
        rtCookie.setHttpOnly(true);
        rtCookie.setSecure(isProduction);
        rtCookie.setPath("/api/v1/auth/refresh");
        rtCookie.setMaxAge(30 * 24 * 60 * 60); // 30 días
        rtCookie.setAttribute("SameSite", "Lax");
        response.addCookie(rtCookie);

        response.setHeader("Authorization", "Bearer " + accessToken);
    }

    public static void clearAuthCookies(HttpServletResponse response) {
        Cookie atCookie = new Cookie(ACCESS_TOKEN_COOKIE, "");
        atCookie.setHttpOnly(true);
        atCookie.setSecure(true);
        atCookie.setPath("/");
        atCookie.setMaxAge(0);
        atCookie.setAttribute("SameSite", "Strict");
        response.addCookie(atCookie);

        Cookie rtCookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        rtCookie.setHttpOnly(true);
        rtCookie.setSecure(true);
        rtCookie.setPath("/api/v1/auth/refresh");
        rtCookie.setMaxAge(0);
        rtCookie.setAttribute("SameSite", "Strict");
        response.addCookie(rtCookie);
    }
}