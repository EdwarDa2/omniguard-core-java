package com.omniguard.domain.port.out;

import com.omniguard.domain.model.PasetoSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasetoSessionRepository {
    PasetoSession save(PasetoSession session);

    Optional<PasetoSession> findById(UUID id);

    List<PasetoSession> findByUserId(UUID userId);

    List<PasetoSession> findByUserIdAndValid(UUID userId);

    List<PasetoSession> findExpired(Instant before);

    List<PasetoSession> findRevokedBefore(Instant before);

    void deleteById(UUID id);

    long countByUserId(UUID userId);
}