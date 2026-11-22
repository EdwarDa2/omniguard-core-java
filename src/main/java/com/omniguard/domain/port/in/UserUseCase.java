package com.omniguard.domain.port.in;

import com.omniguard.domain.model.Role;
import com.omniguard.domain.model.User;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserUseCase {
    Optional<User> findById(UUID id);

    User getMe(UUID userId);

    User updateProfile(UUID userId, Username username, Email email);

    void changePassword(UUID userId, String currentPassword, String newPassword);

    User toggleTotp(UUID userId, boolean enable);

    // Admin operations
    List<User> findAllUsers(int page, int size);

    User createUser(Username username, Email email, String password, String roleName);

    User updateUser(UUID id, Username username, Email email, String roleName, boolean isActive);

    void deleteUser(UUID id);

    List<Role> findAllRoles();

    Role createRole(String name, String description);

    void deleteRole(UUID id);
}