package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.UserJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataUserRepository;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.out.UserRepository;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public User save(User user) {
        UserJpaEntity entity = mapper.toEntity(user);
        UserJpaEntity saved = springDataUserRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return springDataUserRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return springDataUserRepository.findByUsername(username.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return springDataUserRepository.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public List<User> findAll(int page, int size) {
        return springDataUserRepository.findAll(PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<User> findByRoleId(UUID roleId, int page, int size) {
        return springDataUserRepository.findByRoleId(roleId, PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long count() {
        return springDataUserRepository.count();
    }

    @Override
    public long countByRoleId(UUID roleId) {
        return springDataUserRepository.countByRoleId(roleId);
    }

    @Override
    public boolean existsByUsername(Username username) {
        return springDataUserRepository.existsByUsername(username.value());
    }

    @Override
    public boolean existsByEmail(Email email) {
        return springDataUserRepository.existsByEmail(email.value());
    }

    @Override
    public void deleteById(UUID id) {
        springDataUserRepository.deleteById(id);
    }
}