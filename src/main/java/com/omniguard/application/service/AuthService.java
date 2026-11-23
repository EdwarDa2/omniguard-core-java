package com.omniguard.application.service;

import com.omniguard.adapter.out.persistence.adapter.PasetoSessionRepositoryAdapter;
import com.omniguard.adapter.out.persistence.adapter.RoleRepositoryAdapter;
import com.omniguard.adapter.out.persistence.adapter.UserRepositoryAdapter;
import com.omniguard.adapter.out.security.paseto.PasetoTokenService;
import com.omniguard.domain.exception.AuthenticationFailedException;
import com.omniguard.domain.exception.BusinessRuleViolationException;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.in.AuthUseCase;
import com.omniguard.domain.port.in.UserUseCase;
import com.omniguard.domain.port.out.PasetoSessionRepository;
import com.omniguard.domain.port.out.UserRepository;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final RoleRepositoryAdapter roleRepository;
    private final PasetoSessionRepository pasetoSessionRepository;
    private final PasetoTokenService pasetoTokenService;

    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // Argon2id: memory=65536KB, iterations=3, parallelism=4, saltLength=16, hashLength=32
        this.passwordEncoder = new Argon2PasswordEncoder(16, 32, 3, 65536, 4);
    }

    @Override
    public AuthResult login(Username username, String password, String ipAddress, String userAgent) {
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(new Email(username.value())))
                .orElseThrow(() -> new AuthenticationFailedException("Credenciales inválidas"));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("Usuario desactivado");
        }

        if (!passwordEncoder.matches(password, user.passwordHash())) {
            throw new AuthenticationFailedException("Credenciales inválidas");
        }

        // Actualizar último login
        User updatedUser = user.recordLogin();
        userRepository.save(updatedUser);

        // Emitir tokens
        String accessToken = pasetoTokenService.issueAccessToken(updatedUser);
        String refreshToken = pasetoTokenService.issueRefreshToken(updatedUser, null, ipAddress, userAgent);

        return new AuthResult(updatedUser, accessToken, refreshToken, 15 * 60); // 15 min en segundos
    }

    @Override
    public User register(Username username, Email email, String password, String roleName) {
        // Validar unicidad
        if (userRepository.existsByUsername(username)) {
            throw new BusinessRuleViolationException("USERNAME_EXISTS", "El username ya existe");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleViolationException("EMAIL_EXISTS", "El email ya está registrado");
        }

        // Buscar rol
        var role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BusinessRuleViolationException("ROLE_NOT_FOUND", "Rol no encontrado: " + roleName));

        // Hash password
        String passwordHash = passwordEncoder.encode(password);

        // Crear usuario
        User user = User.create(role, username, email, passwordHash);
        return userRepository.save(user);
    }

    @Override
    public AuthResult refresh(String refreshToken, String ipAddress, String userAgent) {
        var claims = pasetoTokenService.validateRefreshToken(refreshToken);
        UUID userId = UUID.fromString(claims.sub());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Usuario no encontrado"));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("Usuario desactivado");
        }

        // Revocar refresh token actual
        pasetoTokenService.revokeSession(UUID.fromString(claims.jti()));

        // Emitir nuevos tokens
        String newAccessToken = pasetoTokenService.issueAccessToken(user);
        String newRefreshToken = pasetoTokenService.issueRefreshToken(user, null, ipAddress, userAgent);

        return new AuthResult(user, newAccessToken, newRefreshToken, 15 * 60);
    }

    @Override
    public void logout(String refreshToken) {
        try {
            var claims = pasetoTokenService.validateRefreshToken(refreshToken);
            pasetoTokenService.revokeSession(UUID.fromString(claims.jti()));
        } catch (Exception e) {
            // Token ya inválido o expirado, no es error
        }
    }

    @Override
    public User me(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Usuario no encontrado"));
    }

    @Override
    public void revokeAllSessions(UUID userId) {
        pasetoTokenService.revokeAllUserSessions(userId);
    }

    @Override
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Usuario no encontrado"));

        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new AuthenticationFailedException("Contraseña actual incorrecta");
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);
        User updatedUser = user.changePassword(newPasswordHash);
        userRepository.save(updatedUser);

        // Revocar todas las sesiones por seguridad
        revokeAllSessions(userId);
    }

    @Override
    public User toggleTotp(UUID userId, boolean enable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Usuario no encontrado"));

        if (enable && !user.totpEnabled()) {
            // Generar secreto TOTP (base32)
            String secret = generateTotpSecret();
            User updated = user.enableTotp(secret);
            userRepository.save(updated);
            return updated;
        } else if (!enable && user.totpEnabled()) {
            User updated = user.disableTotp();
            userRepository.save(updated);
            return updated;
        }
        return user;
    }

    private String generateTotpSecret() {
        byte[] secret = new byte[20]; // 160 bits para base32
        new java.security.SecureRandom().nextBytes(secret);
        return Base32.encode(secret);
    }

    // Base32 encoding simple
    private static class Base32 {
        private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

        static String encode(byte[] data) {
            StringBuilder sb = new StringBuilder();
            int buffer = 0;
            int bitsLeft = 0;
            for (byte b : data) {
                buffer = (buffer << 8) | (b & 0xFF);
                bitsLeft += 8;
                while (bitsLeft >= 5) {
                    sb.append(ALPHABET[(buffer >> (bitsLeft - 5)) & 0x1F]);
                    bitsLeft -= 5;
                }
            }
            if (bitsLeft > 0) {
                sb.append(ALPHABET[(buffer << (5 - bitsLeft)) & 0x1F]);
            }
            return sb.toString();
        }
    }
}