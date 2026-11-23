package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.NotificationConfigJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.NotificationConfigPersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataNotificationConfigRepository;
import com.omniguard.domain.model.NotificationConfig;
import com.omniguard.domain.port.out.NotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationConfigRepositoryAdapter implements NotificationConfigRepository {

    private final SpringDataNotificationConfigRepository springDataNotificationConfigRepository;
    private final NotificationConfigPersistenceMapper mapper;

    @Override
    public NotificationConfig save(NotificationConfig config) {
        NotificationConfigJpaEntity entity = mapper.toEntity(config);
        NotificationConfigJpaEntity saved = springDataNotificationConfigRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<NotificationConfig> findById(UUID id) {
        return springDataNotificationConfigRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<NotificationConfig> findByUserId(UUID userId) {
        return springDataNotificationConfigRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<NotificationConfig> findByUserIdAndPlatform(UUID userId, String platform) {
        return springDataNotificationConfigRepository.findByUserIdAndPlatform(
                userId, NotificationConfigJpaEntity.NotificationPlatform.valueOf(platform)
        ).map(mapper::toDomain);
    }

    @Override
    public List<NotificationConfig> findByPlatform(String platform) {
        return springDataNotificationConfigRepository.findByPlatform(
                NotificationConfigJpaEntity.NotificationPlatform.valueOf(platform)
        ).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<NotificationConfig> findActiveByUserId(UUID userId) {
        return springDataNotificationConfigRepository.findByUserIdAndIsActiveTrue(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataNotificationConfigRepository.deleteById(id);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        springDataNotificationConfigRepository.deleteByUserId(userId);
    }
}