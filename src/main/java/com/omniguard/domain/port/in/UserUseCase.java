package com.omniguard.domain.port.in;

import com.omniguard.domain.model.Role;
import com.omniguard.domain.model.User;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import java.util.List;
import java.util.Optional;

public interface UserUseCase {
    Optional<User> findById(Long id);

    User getMe(Long userId);

    User updateProfile(Long userId, Username username, Email email);

    void changePassword(Long userId, String currentPassword, String newPassword);

    User toggleTotp(Long userId, boolean enable);

    // Operaciones administrativas
    List<User> findAllUsers(int page, int size);

    User createUser(Username username, Email email, String password, String roleName);

    User updateUser(Long id, Username username, Email email, String roleName, boolean isActive);

    void deleteUser(Long id);

    List<Role> findAllRoles();

    Role createRole(String name, String description);

    void deleteRole(Long id);
}
