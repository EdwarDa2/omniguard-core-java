package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.RoleJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.RolePersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataRoleRepository;
import com.omniguard.domain.model.Role;
import com.omniguard.domain.port.out.RoleRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RolePersistenceAdapter implements RoleRepository {

    private final SpringDataRoleRepository roleRepository;
    private final RolePersistenceMapper roleMapper;

    public RolePersistenceAdapter(
            SpringDataRoleRepository roleRepository,
            RolePersistenceMapper roleMapper
    ) {
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
    }

    @Override
    @Transactional
    public Role save(Role role) {
        RoleJpaEntity entity = roleMapper.toEntity(role);
        RoleJpaEntity saved = roleRepository.save(entity);
        return roleMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findById(Long id) {
        return roleRepository.findById(id).map(roleMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name).map(roleMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Role> findAll() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        roleRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return roleRepository.existsByName(name);
    }
}
