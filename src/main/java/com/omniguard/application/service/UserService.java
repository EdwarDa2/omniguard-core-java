package com.omniguard.application.service;

import com.omniguard.adapter.out.persistence.adapter.RoleRepositoryAdapter;
import com.omniguard.adapter.out.persistence.adapter.UserRepositoryAdapter;
import com.omniguard.domain.exception.BusinessRuleViolationException;
import com.omniguard.domain.exception.EntityNotFoundException;
import com.omniguard.domain.model.Role;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.in.UserUseCase;
import com.omniguard.domain.port.out.RoleRepository;
import com.omniguard.domain.port.out.UserRepository;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder = new Argon2PasswordEncoder(16, 32, 3, 65536, 4);

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public User getMe(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
    }

    @Override
    public User updateProfile(UUID userId, Username username, Email email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (!user.username().equals(username) && userRepository.existsByUsername(username)) {
            throw new com.omniguard.domain.exception.BusinessRuleViolationException("USERNAME_EXISTS", "Username ya existe");
        }
        if (!user.email().equals(email) && userRepository.existsByEmail(email)) {
            throw new com.omniguard.domain.exception.BusinessRuleViolationException("EMAIL_EXISTS", "Email ya existe");
        }

        User updated = user.updateProfile(username, email);
        return userRepository.save(updated);
    }

    @Override
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new com.omniguard.domain.exception.AuthenticationFailedException("Contraseña actual incorrecta");
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);
        User updated = user.changePassword(newPasswordHash);
        userRepository.save(updated);
    }

    @Override
    public User toggleTotp(UUID userId, boolean enable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

        if (enable && !user.totpEnabled()) {
            String secret = generateTotpSecret();
            User updated = user.enableTotp(secret);
            return userRepository.save(updated);
        } else if (!enable && user.totpEnabled()) {
            User updated = user.disableTotp();
            return userRepository.save(updated);
        }
        return user;
    }

    // Admin operations
    @Override
    public List<User> findAllUsers(int page, int size) {
        return userRepository.findAll(page, size);
    }

    @Override
    public User createUser(Username username, Email email, String password, String roleName) {
        if (userRepository.existsByUsername(username)) {
            throw new com.omniguard.domain.exception.BusinessRuleViolationException("USERNAME_EXISTS", "Username ya existe");
        }
        if (userRepository.existsByEmail(email)) {
            throw new com.omniguard.domain.exception.BusinessRuleViolationException("EMAIL_EXISTS", "Email ya existe");
        }

        var role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new com.omniguard.domain.exception.BusinessRuleViolationException("ROLE_NOT_FOUND", "Rol no encontrado: " + roleName));

        String passwordHash = new org.springframework.security.crypto.argon2.Argon2PasswordEncoder(16, 32, 3, 65536, 4).encode(password);
        User user = User.create(role, username, email, passwordHash);
        return userRepository.save(user);
    }

    @Override
    public User updateUser(UUID id, Username username, Email email, String roleName, boolean isActive) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));

        if (!user.username().equals(username) && userRepository.existsByUsername(username)) {
            throw new com.omniguard.domain.exception.BusinessRuleViolationException("USERNAME_EXISTS", "Username ya existe");
        }
        if (!user.email().equals(email) && userRepository.existsByEmail(email)) {
            throw new com.omniguard.domain.exception.BusinessRuleViolationException("EMAIL_EXISTS", "Email ya existe");
        }

        var role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new com.omniguard.domain.exception.BusinessRuleViolationException("ROLE_NOT_FOUND", "Rol no encontrado: " + roleName));

        User updated = user.updateProfile(username, email);
        updated = updated.assignRole(role);
        updated = isActive ? updated.activate() : updated.deactivate();
        return userRepository.save(updated);
    }

    @Override
    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }

    @Override
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Role createRole(String name, String description) {
        if (roleRepository.existsByName(name)) {
            throw new com.omniguard.domain.exception.BusinessRuleViolationException("ROLE_EXISTS", "Rol ya existe: " + name);
        }
        Role role = Role.create(name, description);
        return roleRepository.save(role);
    }

    @Override
    public void deleteRole(UUID id) {
        roleRepository.deleteById(id);
    }

    private String generateTotpSecret() {
        byte[] secret = new byte[20];
        new java.security.SecureRandom().nextBytes(secret);
        return Base32.encode(secret);
    }

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