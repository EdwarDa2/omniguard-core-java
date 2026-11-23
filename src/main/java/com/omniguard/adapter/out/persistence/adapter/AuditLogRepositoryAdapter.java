package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.AuditLogJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.AuditLogPersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataAuditLogRepository;
import com.omniguard.domain.model.AuditLog;
import com.omniguard.domain.port.out.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final SpringDataAuditLogRepository springDataAuditLogRepository;
    private final AuditLogPersistenceMapper mapper;

    @Override
    public AuditLog save(AuditLog auditLog) {
        AuditLogJpaEntity entity = mapper.toEntity(auditLog);
        AuditLogJpaEntity saved = springDataAuditLogRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<AuditLog> findByEntity(String entityType, UUID entityId, int page, int size) {
        return springDataAuditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AuditLog> findByChangedBy(UUID changedBy, int page, int size) {
        return springDataAuditLogRepository.findByChangedBy(changedBy, PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AuditLog> findByAction(String action, int page, int size) {
        return springDataAuditLogRepository.findByAction(AuditLogJpaEntity.AuditAction.valueOf(action), PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AuditLog> findByChangedAtBetween(Instant from, Instant to, int page, int size) {
        return springDataAuditLogRepository.findByChangedAtBetween(from, to, PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<AuditLog> findByEntityAndChangedAtBetween(String entityType, UUID entityId, Instant from, Instant to, int page, int size) {
        return springDataAuditLogRepository.findByEntityAndChangedAtBetween(entityType, entityId, from, to, PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countByEntity(String entityType, UUID entityId) {
        return springDataAuditLogRepository.countByEntityTypeAndEntityId(entityType, entityId);
    }

    @Override
    public long countByChangedAtBetween(Instant from, Instant to) {
        return springDataAuditLogRepository.countByChangedAtBetween(from, to);
    }
}