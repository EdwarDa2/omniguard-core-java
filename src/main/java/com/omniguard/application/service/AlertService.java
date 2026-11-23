package com.omniguard.application.service;

import com.omniguard.domain.exception.EntityNotFoundException;
import com.omniguard.domain.model.Alert;
import com.omniguard.domain.port.in.AlertUseCase;
import com.omniguard.domain.port.out.AlertRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertService implements AlertUseCase {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @Override
    @Transactional
    public Alert create(Alert alert) {
        return alertRepository.save(alert);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Alert> findById(Long id) {
        return alertRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> findAll(AlertFilter filter, int page, int size) {
        return alertRepository.findAll(filter, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(AlertFilter filter) {
        return alertRepository.count(filter);
    }

    @Override
    @Transactional
    public Alert updateStatus(Long id, String status, Long assignedTo, String resolutionNotes) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada con ID: " + id));

        Alert updated = alert.updateStatus(status, assignedTo, resolutionNotes);
        return alertRepository.save(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertStats getStats() {
        return alertRepository.getStats();
    }
}
