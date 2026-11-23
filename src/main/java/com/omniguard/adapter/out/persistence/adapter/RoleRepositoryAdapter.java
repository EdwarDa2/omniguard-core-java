package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.RoleJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.RolePersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataRoleRepository;
import com.omniguard.domain.model.Role;
import com.omniguard.domain.port.out.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final SpringDataRoleRepository springDataRoleRepository;
    private final RolePersistenceMapper mapper;

    @Override
    public Role save(Role role) {
        RoleJpaEntity entity = mapper.toEntity(role);
        RoleJpaEntity saved = springDataRoleRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Role> findById(UUID id) {
        return springDataRoleRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return springDataRoleRepository.findByName(RoleJpaEntity.RoleName.valueOf(name)).map(mapper::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return springDataRoleRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataRoleRepository.deleteById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return springDataRoleRepository.existsByName(RoleJpaEntity.RoleName.valueOf(name));
    }
}