package com.omniguard.application.service;

import com.omniguard.adapter.out.security.paseto.PasetoTokenService;
import com.omniguard.domain.exception.AuthenticationFailedException;
import com.omniguard.domain.exception.BusinessRuleViolationException;
import com.omniguard.domain.exception.EntityNotFoundException;
import com.omniguard.domain.model.Role;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.in.AuthUseCase;
import com.omniguard.domain.port.out.PasetoSessionRepository;
import com.omniguard.domain.port.out.RoleRepository;
import com.omniguard.domain.port.out.UserRepository;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements AuthUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasetoSessionRepository sessionRepository;
    private final PasetoTokenService pasetoTokenService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasetoSessionRepository sessionRepository,
            PasetoTokenService pasetoTokenService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessionRepository = sessionRepository;
        this.pasetoTokenService = pasetoTokenService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResult login(Username username, String password, String ipAddress, String userAgent) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AuthenticationFailedException("Credenciales inválidas"));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("La cuenta de usuario está desactivada");
        }

        if (user.passwordHash() == null || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new AuthenticationFailedException("Credenciales inválidas");
        }

        User updatedUser = user.recordLogin();
        userRepository.save(updatedUser);

        String accessToken = pasetoTokenService.issueAccessToken(updatedUser);
        String refreshToken = pasetoTokenService.issueRefreshToken(updatedUser, userAgent, ipAddress, userAgent);

        return new AuthResult(
                updatedUser,
                accessToken,
                refreshToken,
                15 * 60 // 15 minutos en segundos
        );
    }

    @Override
    @Transactional
    public User register(Username username, Email email, String password, String roleName) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessRuleViolationException("El nombre de usuario ya está registrado");
        }

        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleViolationException("El correo electrónico ya está registrado");
        }

        String targetRole = (roleName != null && !roleName.isBlank()) ? roleName.trim() : "ROLE_ANALYST";
        Role role = roleRepository.findByName(targetRole)
                .orElseGet(() -> roleRepository.save(Role.create(targetRole, "Rol asignado automáticamente")));

        String passwordHash = passwordEncoder.encode(password);
        User newUser = User.create(role, username, email, passwordHash);

        return userRepository.save(newUser);
    }

    @Override
    @Transactional
    public AuthResult refresh(String refreshToken, String ipAddress, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationFailedException("Refresh token no proporcionado");
        }

        PasetoTokenService.PasetoClaims claims = pasetoTokenService.validateRefreshToken(refreshToken);
        Long userId = Long.parseLong(claims.sub());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationFailedException("Usuario no encontrado"));

        if (!user.isActive()) {
            throw new AuthenticationFailedException("Cuenta de usuario desactivada");
        }

        String newAccessToken = pasetoTokenService.issueAccessToken(user);

        return new AuthResult(
                user,
                newAccessToken,
                refreshToken,
                15 * 60
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        try {
            PasetoTokenService.PasetoClaims claims = pasetoTokenService.validateRefreshToken(refreshToken);
            if (claims.jti() != null) {
                pasetoTokenService.revokeSession(UUID.fromString(claims.jti()));
            }
        } catch (Exception ignored) {
            // Logout seguro: si el token ya expiró o es inválido, no interrumpir el flujo
        }
    }

    @Override
    @Transactional(readOnly = true)
    public User me(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));
    }

    @Override
    @Transactional
    public void revokeAllSessions(Long userId) {
        pasetoTokenService.revokeAllUserSessions(userId);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));

        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new AuthenticationFailedException("La contraseña actual es incorrecta");
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);
        User updated = user.changePassword(newPasswordHash);
        userRepository.save(updated);

        pasetoTokenService.revokeAllUserSessions(userId);
    }

    @Override
    @Transactional
    public User toggleTotp(Long userId, boolean enable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));

        User updated = enable ? user.enableTotp(UUID.randomUUID().toString()) : user.disableTotp();
        return userRepository.save(updated);
    }
}
