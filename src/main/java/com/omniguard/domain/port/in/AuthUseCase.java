package com.omniguard.domain.port.in;

import com.omniguard.domain.model.User;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;

public interface AuthUseCase {
    /**
     * Autentica un usuario y devuelve tokens de acceso y refresh.
     */
    AuthResult login(Username username, String password, String ipAddress, String userAgent);

    /**
     * Registra un nuevo usuario.
     */
    User register(Username username, Email email, String password, String roleName);

    /**
     * Renueva el access token usando el refresh token.
     */
    AuthResult refresh(String refreshToken, String ipAddress, String userAgent);

    /**
     * Cierra sesión y revoca el refresh token.
     */
    void logout(String refreshToken);

    /**
     * Obtiene el usuario actual desde el access token.
     */
    User me(Long userId);

    /**
     * Revoca todas las sesiones de un usuario (admin o cambio de password).
     */
    void revokeAllSessions(Long userId);

    /**
     * Cambia la contraseña del usuario actual.
     */
    void changePassword(Long userId, String currentPassword, String newPassword);

    /**
     * Habilita/deshabilita 2FA TOTP.
     */
    User toggleTotp(Long userId, boolean enable);

    record AuthResult(
            User user,
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInSeconds
    ) {}
}
