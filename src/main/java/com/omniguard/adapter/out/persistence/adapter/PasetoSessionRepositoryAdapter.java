package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.PasetoSessionJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.PasetoSessionPersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataPasetoSessionRepository;
import com.omniguard.domain.model.PasetoSession;
import com.omniguard.domain.port.out.PasetoSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasetoSessionRepositoryAdapter implements PasetoSessionRepository {

    private final SpringDataPasetoSessionRepository springDataPasetoSessionRepository;
    private final PasetoSessionPersistenceMapper mapper;

    @Override
    public PasetoSession save(PasetoSession session) {
        PasetoSessionJpaEntity entity = mapper.toEntity(session);
        PasetoSessionJpaEntity saved = springDataPasetoSessionRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PasetoSession> findById(UUID id) {
        return springDataPasetoSessionRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<PasetoSession> findByUserId(UUID userId) {
        return springDataPasetoSessionRepository.findByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PasetoSession> findByUserIdAndValid(UUID userId) {
        return springDataPasetoSessionRepository.findByUserIdAndRevokedAtIsNull(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PasetoSession> findExpired(Instant before) {
        return springDataPasetoSessionRepository.findByExpiresAtBefore(before).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<PasetoSession> findRevokedBefore(Instant before) {
        return springDataPasetoSessionRepository.findByRevokedAtBefore(before).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataPasetoSessionRepository.deleteById(id);
    }

    @Override
    public long countByUserId(UUID userId) {
        return springDataPasetoSessionRepository.countByUserId(userId);
    }

    public int revokeSession(UUID id, Instant now, String reason) {
        return springDataPasetoSessionRepository.revokeSession(id, now, reason);
    }

    public int revokeAllUserSessions(UUID userId, Instant now, String reason) {
        return springDataPasetoSessionRepository.revokeAllUserSessions(userId, now, reason);
    }

    public int deleteExpiredSessions(Instant before) {
        return springDataPasetoSessionRepository.deleteExpiredSessions(before);
    }

    public int deleteOldRevokedSessions(Instant before) {
        return springDataPasetoSessionRepository.deleteOldRevokedSessions(before);
    }
}