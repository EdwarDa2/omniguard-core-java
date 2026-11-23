package com.omniguard.domain.port.out;

import com.omniguard.domain.model.User;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(Username username);

    Optional<User> findByEmail(Email email);

    List<User> findAll(int page, int size);

    List<User> findByRoleId(Long roleId, int page, int size);

    long count();

    long countByRoleId(Long roleId);

    boolean existsByUsername(Username username);

    boolean existsByEmail(Email email);

    void deleteById(Long id);
}
