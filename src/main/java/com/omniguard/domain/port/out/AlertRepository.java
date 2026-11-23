package com.omniguard.domain.port.out;

import com.omniguard.domain.model.Alert;
import com.omniguard.domain.port.in.AlertUseCase.AlertFilter;
import com.omniguard.domain.port.in.AlertUseCase.AlertStats;
import java.util.List;
import java.util.Optional;

public interface AlertRepository {
    Alert save(Alert alert);

    Optional<Alert> findById(Long id);

    List<Alert> findAll(AlertFilter filter, int page, int size);

    long count(AlertFilter filter);

    AlertStats getStats();

    void deleteById(Long id);
}
