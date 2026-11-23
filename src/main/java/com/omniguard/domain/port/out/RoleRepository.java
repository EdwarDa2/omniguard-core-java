package com.omniguard.domain.port.out;

import com.omniguard.domain.model.Role;
import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Role save(Role role);

    Optional<Role> findById(Long id);

    Optional<Role> findByName(String name);

    List<Role> findAll();

    void deleteById(Long id);

    boolean existsByName(String name);
}
