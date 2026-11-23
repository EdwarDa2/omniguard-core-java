package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.PasetoSessionJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.PasetoSessionPersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataPasetoSessionRepository;
import com.omniguard.domain.model.PasetoSession;
import com.omniguard.domain.port.out.PasetoSessionRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PasetoSessionPersistenceAdapter implements PasetoSessionRepository {

    private final SpringDataPasetoSessionRepository sessionRepository;
    private final PasetoSessionPersistenceMapper sessionMapper;

    public PasetoSessionPersistenceAdapter(
            SpringDataPasetoSessionRepository sessionRepository,
            PasetoSessionPersistenceMapper sessionMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.sessionMapper = sessionMapper;
    }

    @Override
    @Transactional
    public PasetoSession save(PasetoSession session) {
        PasetoSessionJpaEntity entity = sessionMapper.toEntity(session);
        PasetoSessionJpaEntity saved = sessionRepository.save(entity);
        return sessionMapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PasetoSession> findById(UUID id) {
        return sessionRepository.findById(id).map(sessionMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PasetoSession> findByUserId(Long userId) {
        return sessionRepository.findByUserId(userId).stream()
                .map(sessionMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        sessionRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(Long userId, String reason) {
        sessionRepository.revokeAllByUserId(userId, reason != null ? reason : "REVOKED");
    }
}
