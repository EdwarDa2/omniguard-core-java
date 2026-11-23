package com.omniguard.application.service;

import com.omniguard.domain.exception.BusinessRuleViolationException;
import com.omniguard.domain.exception.EntityNotFoundException;
import com.omniguard.domain.model.Role;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.in.UserUseCase;
import com.omniguard.domain.port.out.RoleRepository;
import com.omniguard.domain.port.out.UserRepository;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public User getMe(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + userId));
    }

    @Override
    @Transactional
    public User updateProfile(Long userId, Username username, Email email) {
        User user = getMe(userId);

        if (username != null && !username.equals(user.username())) {
            if (userRepository.existsByUsername(username)) {
                throw new BusinessRuleViolationException("El nombre de usuario ya está en uso");
            }
        }

        if (email != null && !email.equals(user.email())) {
            if (userRepository.existsByEmail(email)) {
                throw new BusinessRuleViolationException("El correo electrónico ya está en uso");
            }
        }

        User updated = user.updateProfile(
                username != null ? username : user.username(),
                email != null ? email : user.email()
        );
        return userRepository.save(updated);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getMe(userId);

        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new BusinessRuleViolationException("La contraseña actual no coincide");
        }

        User updated = user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(updated);
    }

    @Override
    @Transactional
    public User toggleTotp(Long userId, boolean enable) {
        User user = getMe(userId);
        User updated = enable ? user.enableTotp("SECRET_KEY_PLACEHOLDER") : user.disableTotp();
        return userRepository.save(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllUsers(int page, int size) {
        return userRepository.findAll(page, size);
    }

    @Override
    @Transactional
    public User createUser(Username username, Email email, String password, String roleName) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessRuleViolationException("El nombre de usuario ya existe");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleViolationException("El correo ya existe");
        }

        String targetRole = (roleName != null && !roleName.isBlank()) ? roleName.trim() : "ROLE_ANALYST";
        Role role = roleRepository.findByName(targetRole)
                .orElseGet(() -> roleRepository.save(Role.create(targetRole, "Rol asignado por administración")));

        User user = User.create(role, username, email, passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUser(Long id, Username username, Email email, String roleName, boolean isActive) {
        User user = getMe(id);

        Role role = user.role();
        if (roleName != null && !roleName.isBlank()) {
            role = roleRepository.findByName(roleName.trim())
                    .orElseGet(() -> roleRepository.save(Role.create(roleName.trim(), "Rol administrativo")));
        }

        User updated = user.assignRole(role);
        if (isActive != user.isActive()) {
            updated = isActive ? updated.activate() : updated.deactivate();
        }
        if (username != null || email != null) {
            updated = updated.updateProfile(
                    username != null ? username : user.username(),
                    email != null ? email : user.email()
            );
        }

        return userRepository.save(updated);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    @Transactional
    public Role createRole(String name, String description) {
        if (roleRepository.existsByName(name)) {
            throw new BusinessRuleViolationException("El rol ya existe: " + name);
        }
        return roleRepository.save(Role.create(name, description));
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }
}
