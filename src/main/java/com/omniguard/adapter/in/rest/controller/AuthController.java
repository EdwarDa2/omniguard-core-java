package com.omniguard.adapter.in.rest.controller;

import com.omniguard.adapter.in.rest.dto.request.LoginRequestDto;
import com.omniguard.adapter.in.rest.dto.request.RegisterRequestDto;
import com.omniguard.adapter.in.rest.dto.response.UserResponseDto;
import com.omniguard.adapter.in.rest.security.PasetoAuthenticationFilter;
import com.omniguard.domain.exception.AuthenticationFailedException;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.in.AuthUseCase;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación (IAM)", description = "Endpoints seguros con tokens PASETO v4 en cookies HttpOnly")
public class AuthController {

    private final AuthUseCase authUseCase;

    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida credenciales, genera tokens PASETO v4 y los inyecta en cookies HttpOnly SameSite=Strict")
    public ResponseEntity<UserResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthUseCase.AuthResult result = authUseCase.login(
                Username.of(request.username()),
                request.password(),
                clientIp,
                userAgent
        );

        PasetoAuthenticationFilter.setAuthCookies(httpResponse, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok(UserResponseDto.fromDomain(result.user()));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario", description = "Crea un nuevo usuario con contraseña codificada en Argon2id")
    public ResponseEntity<UserResponseDto> register(
            @Valid @RequestBody RegisterRequestDto request
    ) {
        User user = authUseCase.register(
                Username.of(request.username()),
                Email.of(request.email()),
                request.password(),
                request.role()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseDto.fromDomain(user));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token de acceso", description = "Lee la cookie HttpOnly rt y emite una nueva cookie at")
    public ResponseEntity<UserResponseDto> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String refreshToken = extractCookie(httpRequest, "rt")
                .orElseThrow(() -> new AuthenticationFailedException("No se encontró la cookie de sesión de refresco (rt)"));

        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthUseCase.AuthResult result = authUseCase.refresh(refreshToken, clientIp, userAgent);

        PasetoAuthenticationFilter.setAuthCookies(httpResponse, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok(UserResponseDto.fromDomain(result.user()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Revoca el refresh token en base de datos y destruye las cookies HttpOnly")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        extractCookie(httpRequest, "rt").ifPresent(authUseCase::logout);
        PasetoAuthenticationFilter.clearAuthCookies(httpResponse);

        return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener usuario autenticado", description = "Devuelve el perfil del usuario autenticado mediante la cookie at")
    public ResponseEntity<UserResponseDto> getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthenticationFailedException("No se encuentra ninguna sesión autenticada");
        }

        try {
            Long userId = Long.parseLong(authentication.getName());
            User user = authUseCase.me(userId);
            return ResponseEntity.ok(UserResponseDto.fromDomain(user));
        } catch (NumberFormatException e) {
            throw new AuthenticationFailedException("Identificador de sesión inválido");
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Optional<String> extractCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
