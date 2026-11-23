package com.omniguard.application.service;

import com.omniguard.domain.event.AlertCreatedEvent;
import com.omniguard.domain.model.Alert;
import com.omniguard.domain.port.in.AlertUseCase;
import com.omniguard.domain.port.out.AlertRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AlertService implements AlertUseCase {

    private final AlertRepository alertRepository;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public AlertService(AlertRepository alertRepository, ApplicationEventPublisher eventPublisher) {
        this.alertRepository = alertRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Alert create(Alert alert) {
        Alert saved = alertRepository.save(alert);
        // Publicar evento para SSE
        eventPublisher.publishEvent(new AlertCreatedEvent(this, saved));
        return saved;
    }

    @Override
    public java.util.Optional<Alert> findById(UUID id) {
        return alertRepository.findById(id);
    }

    @Override
    public List<Alert> findAll(AlertFilter filter, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // La implementación real delega en el adapter
        throw new UnsupportedOperationException("Usar AlertRepositoryAdapter directamente para queries complejas");
    }

    @Override
    public long count(AlertFilter filter) {
        throw new UnsupportedOperationException("Usar AlertRepositoryAdapter directamente");
    }

    @Override
    public Alert updateStatus(UUID id, String status, UUID assignedTo, String resolutionNotes) {
        return alertRepository.findById(id)
                .map(alert -> {
                    Alert updated = alert.updateStatus(
                            com.omniguard.domain.vo.AlertStatus.of(status),
                            assignedTo,
                            resolutionNotes
                    );
                    return alertRepository.save(updated);
                })
                .orElseThrow(() -> new com.omniguard.domain.exception.EntityNotFoundException("Alert", id));
    }

    @Override
    public AlertStats getStats() {
        // Implementación delegada al adapter
        throw new UnsupportedOperationException("Usar AlertRepositoryAdapter");
    }

    @Override
    public AlertStream getAlertStream() {
        return new AlertStream() {
            @Override
            public void subscribe(UUID userId, AlertStreamHandler handler) {
                // Implementado en SseAlertService
            }

            @Override
            public void unsubscribe(UUID userId) {
                // Implementado en SseAlertService
            }
        };
    }
}