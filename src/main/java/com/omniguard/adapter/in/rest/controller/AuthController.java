package com.omniguard.adapter.in.rest.controller;

import com.omniguard.adapter.in.rest.dto.auth.*;
import com.omniguard.adapter.in.rest.mapper.AuthRestMapper;
import com.omniguard.adapter.in.rest.mapper.UserRestMapper;
import com.omniguard.adapter.in.rest.security.PasetoAuthenticationFilter;
import com.omniguard.domain.exception.AuthenticationFailedException;
import com.omniguard.domain.exception.BusinessRuleViolationException;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.in.AuthUseCase;
import com.omniguard.domain.port.in.UserUseCase;
import com.omniguard.domain.vo.Username;
import com.omniguard.domain.vo.Email;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "Autenticación", description = "Endpoints de autenticación PASETO (cookies HttpOnly)")
public class AuthController {

    private final AuthUseCase authUseCase;
    private final UserUseCase userUseCase;
    private final AuthRestMapper authRestMapper;
    private final UserRestMapper userRestMapper;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Autentica usuario y establece cookies HttpOnly (access token 'at' + refresh token 'rt')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas", content = @Content(schema = @Schema(implementation = com.omniguard.adapter.in.rest.dto.common.ErrorResponse.class))),
        @ApiResponse(responseCode = "429", description = "Rate limit excedido (5 req/min/IP)")
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthUseCase.AuthResult result = authUseCase.login(
                Username.of(request.getUsernameOrEmail()),
                request.getPassword(),
                ip,
                userAgent
        );

        PasetoAuthenticationFilter.setAuthCookies(httpResponse, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok(authRestMapper.toResponse(result));
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Crea nuevo usuario y autentica automáticamente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registro exitoso", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "409", description = "Usuario/email ya existe"),
        @ApiResponse(responseCode = "429", description = "Rate limit excedido (3 req/min/IP)")
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        User user = userUseCase.createUser(
                Username.of(request.getUsername()),
                Email.of(request.getEmail()),
                request.getPassword(),
                request.getRole()
        );

        AuthUseCase.AuthResult result = authUseCase.login(
                Username.of(request.getUsername()),
                request.getPassword(),
                ip,
                userAgent
        );

        PasetoAuthenticationFilter.setAuthCookies(httpResponse, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok(authRestMapper.toResponse(result));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token", description = "Usa refresh token de cookie 'rt' para emitir nuevo access token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token renovado", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido/expirado/revocado"),
        @ApiResponse(responseCode = "429", description = "Rate limit excedido (10 req/min/user)")
    })
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String refreshToken = extractRefreshToken(httpRequest);
        if (refreshToken == null) {
            throw new AuthenticationFailedException("Refresh token no encontrado");
        }

        String ip = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthUseCase.AuthResult result = authUseCase.refresh(refreshToken, ip, userAgent);

        PasetoAuthenticationFilter.setAuthCookies(httpResponse, result.accessToken(), result.refreshToken());

        return ResponseEntity.ok(authRestMapper.toResponse(result));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión", description = "Revoca refresh token y limpia cookies")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Logout exitoso"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Void> logout(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String refreshToken = extractRefreshToken(httpRequest);
        if (refreshToken != null) {
            authUseCase.logout(refreshToken);
        }
        PasetoAuthenticationFilter.clearAuthCookies(httpResponse);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Usuario actual", description = "Obtiene info del usuario autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario autenticado", content = @Content(schema = @Schema(implementation = com.omniguard.adapter.in.rest.dto.user.UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<UserResponse> me() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationFailedException("No autenticado");
        }
        String userId = auth.getName();
        User user = userUseCase.getMe(java.util.UUID.fromString(userId));
        return ResponseEntity.ok(userRestMapper.toResponse(user));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Cambiar contraseña", description = "Cambia password del usuario autenticado (revoca todas sus sesiones)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Password cambiado"),
        @ApiResponse(responseCode = "400", description = "Password actual incorrecto"),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        userUseCase.changePassword(java.util.UUID.fromString(userId), request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/totp")
    @Operation(summary = "Habilitar/deshabilitar 2FA", description = "Activa o desactiva TOTP para el usuario actual")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "2FA configurado", content = @Content(schema = @Schema(implementation = TotpSetupResponse.class))),
        @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<TotpSetupResponse> toggleTotp(
            @Valid @RequestBody ToggleTotpRequest request
    ) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        User user = userUseCase.toggleTotp(java.util.UUID.fromString(userId), request.getEnable());

        if (request.getEnable() && user.totpSecret() != null) {
            String qrCodeUrl = String.format(
                    "otpauth://totp/OmniGuard:%s?secret=%s&issuer=OmniGuard",
                    user.email(), user.totpSecret()
            );
            return ResponseEntity.ok(new TotpSetupResponse(user.totpSecret(), qrCodeUrl, null));
        }
        return ResponseEntity.ok(new TotpSetupResponse(null, null, null));
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("rt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}