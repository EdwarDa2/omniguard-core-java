package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.RoleJpaEntity;
import com.omniguard.adapter.out.persistence.entity.UserJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataRoleRepository;
import com.omniguard.adapter.out.persistence.repository.SpringDataUserRepository;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.out.UserRepository;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserPersistenceAdapter implements UserRepository {

    private final SpringDataUserRepository userRepository;
    private final SpringDataRoleRepository roleRepository;
    private final UserPersistenceMapper userMapper;

    public UserPersistenceAdapter(
            SpringDataUserRepository userRepository,
            SpringDataRoleRepository roleRepository,
            UserPersistenceMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public User save(User user) {
        RoleJpaEntity roleEntity = null;
        if (user.role() != null && user.role().id() != null) {
            roleEntity = roleRepository.findById(user.role().id()).orElse(null);
        } else if (user.role() != null && user.role().name() != null) {
            roleEntity = roleRepository.findByName(user.role().name()).orElse(null);
        }

        UserJpaEntity entity = userMapper.toEntity(user, roleEntity);
        UserJpaEntity saved = userRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id).map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(Username username) {
        if (username == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username.value()).map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(Email email) {
        if (email == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.value()).map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return userRepository.findAll(pageable).getContent().stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByRoleId(Long roleId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return userRepository.findByRoleId(roleId, pageable).getContent().stream()
                .map(userMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRoleId(Long roleId) {
        return userRepository.countByRoleId(roleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(Username username) {
        if (username == null) {
            return false;
        }
        return userRepository.existsByUsername(username.value());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        if (email == null) {
            return false;
        }
        return userRepository.existsByEmail(email.value());
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
