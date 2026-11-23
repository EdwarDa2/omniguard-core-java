package com.omniguard.domain.port.out;

import com.omniguard.domain.model.PasetoSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasetoSessionRepository {
    PasetoSession save(PasetoSession session);

    Optional<PasetoSession> findById(UUID id);

    List<PasetoSession> findByUserId(Long userId);

    void deleteById(UUID id);

    void revokeAllByUserId(Long userId, String reason);
}
