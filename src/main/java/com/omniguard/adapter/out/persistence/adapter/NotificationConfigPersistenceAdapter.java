package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.NotificationConfigJpaEntity;
import com.omniguard.adapter.out.persistence.repository.SpringDataNotificationConfigRepository;
import com.omniguard.domain.model.NotificationConfig;
import com.omniguard.domain.port.out.NotificationConfigRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationConfigPersistenceAdapter implements NotificationConfigRepository {

    private final SpringDataNotificationConfigRepository repository;

    public NotificationConfigPersistenceAdapter(SpringDataNotificationConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public NotificationConfig save(NotificationConfig config) {
        NotificationConfigJpaEntity entity = toJpaEntity(config);
        NotificationConfigJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<NotificationConfig> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationConfig> findByUserId(Long userId) {
        return repository.findByUserId(userId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationConfig> findActiveByPlatform(String platform) {
        return repository.findByPlatformIgnoreCaseAndIsActiveTrue(platform).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    private NotificationConfig toDomain(NotificationConfigJpaEntity entity) {
        if (entity == null) return null;
        return new NotificationConfig(
                entity.getId(),
                entity.getUserId(),
                entity.getPlatform(),
                entity.getWebhookUrl(),
                entity.getChatId(),
                entity.getBotToken(),
                entity.getEmail(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private NotificationConfigJpaEntity toJpaEntity(NotificationConfig domain) {
        if (domain == null) return null;
        return new NotificationConfigJpaEntity(
                domain.id(),
                domain.userId(),
                domain.platform(),
                domain.webhookUrl(),
                domain.chatId(),
                domain.botToken(),
                domain.email(),
                domain.isActive(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
