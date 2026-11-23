package com.omniguard.adapter.in.rest.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omniguard.adapter.in.rest.dto.request.NidsAlertRequestDto;
import com.omniguard.adapter.in.rest.dto.request.UpdateAlertStatusRequestDto;
import com.omniguard.adapter.in.rest.dto.response.AlertResponseDto;
import com.omniguard.adapter.in.rest.dto.response.AlertStatsResponseDto;
import com.omniguard.adapter.in.rest.sse.AlertSseService;
import com.omniguard.domain.exception.EntityNotFoundException;
import com.omniguard.domain.model.Alert;
import com.omniguard.domain.model.Severity;
import com.omniguard.domain.port.in.AlertUseCase;
import com.omniguard.domain.port.in.AlertUseCase.AlertFilter;
import com.omniguard.domain.vo.IpAddress;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Alertas NIDS", description = "Endpoints de ingesta M2M (Python), consulta y streaming SSE en tiempo real")
public class AlertController {

    private final AlertUseCase alertUseCase;
    private final ObjectMapper objectMapper;
    private final AlertSseService alertSseService;

    public AlertController(
            AlertUseCase alertUseCase,
            ObjectMapper objectMapper,
            AlertSseService alertSseService
    ) {
        this.alertUseCase = alertUseCase;
        this.objectMapper = objectMapper;
        this.alertSseService = alertSseService;
    }

    @PostMapping("/internal/alerts")
    @Operation(summary = "Ingesta de alerta NIDS (M2M)", description = "Endpoint consumido por omniguard-analyzer-python mediante cabecera X-Internal-Token")
    public ResponseEntity<Map<String, Object>> ingestNidsAlert(
            @Valid @RequestBody NidsAlertRequestDto request
    ) {
        Severity severity;
        try {
            severity = Severity.valueOf(request.severity().toUpperCase());
        } catch (Exception e) {
            severity = Severity.MEDIUM;
        }

        String rawEventJson = "{}";
        if (request.rawEvent() != null) {
            try {
                rawEventJson = objectMapper.writeValueAsString(request.rawEvent());
            } catch (JsonProcessingException ignored) {}
        }

        Alert domainAlert = Alert.create(
                request.rule() != null ? request.rule() : request.threatType(),
                severity,
                request.title(),
                request.description(),
                IpAddress.of(request.sourceIp()),
                IpAddress.of(request.destinationIp()),
                request.destinationPort(),
                request.protocol(),
                request.appProtocol(),
                rawEventJson
        );

        Alert saved = alertUseCase.create(domainAlert);

        // Notificar en tiempo real a los navegadores conectados por SSE
        alertSseService.broadcastNewAlert(AlertResponseDto.fromDomain(saved));

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "status", "CREATED",
                "alert_id", saved.id()
        ));
    }

    @GetMapping(value = "/alerts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream SSE en tiempo real", description = "Flujo continuo Server-Sent Events para el monitor en vivo del frontend")
    public SseEmitter streamAlerts() {
        return alertSseService.registerClient();
    }

    @GetMapping("/alerts")
    @Operation(summary = "Listar alertas con filtros", description = "Endpoint para el dashboard SOC con soporte de paginación y filtros")
    public ResponseEntity<Map<String, Object>> getAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String threatType,
            @RequestParam(required = false) String sourceIp,
            @RequestParam(required = false) String destinationIp,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        IpAddress src = (sourceIp != null && !sourceIp.isBlank()) ? IpAddress.of(sourceIp) : null;
        IpAddress dst = (destinationIp != null && !destinationIp.isBlank()) ? IpAddress.of(destinationIp) : null;

        AlertFilter filter = new AlertFilter(status, severity, threatType, null, src, dst, from, to);
        List<AlertResponseDto> items = alertUseCase.findAll(filter, page, size).stream()
                .map(AlertResponseDto::fromDomain)
                .toList();

        long total = alertUseCase.count(filter);

        return ResponseEntity.ok(Map.of(
                "items", items,
                "total", total,
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/alerts/{id}")
    @Operation(summary = "Obtener detalle de alerta", description = "Devuelve el incidente específico por su identificador")
    public ResponseEntity<AlertResponseDto> getAlertById(@PathVariable Long id) {
        Alert alert = alertUseCase.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Alerta no encontrada con ID: " + id));

        return ResponseEntity.ok(AlertResponseDto.fromDomain(alert));
    }

    @PatchMapping("/alerts/{id}/status")
    @Operation(summary = "Actualizar estado de resolución de alerta", description = "Permite al analista cambiar el estado a INVESTIGATING, RESOLVED o FALSE_POSITIVE")
    public ResponseEntity<AlertResponseDto> updateAlertStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAlertStatusRequestDto request
    ) {
        Alert updated = alertUseCase.updateStatus(
                id,
                request.status(),
                request.assignedTo(),
                request.resolutionNotes()
        );

        // Notificar cambio de estado por SSE
        alertSseService.broadcastAlertUpdate(id, updated.status());

        return ResponseEntity.ok(AlertResponseDto.fromDomain(updated));
    }

    @GetMapping("/alerts/stats")
    @Operation(summary = "Estadísticas del SOC", description = "Métricas agregadas de incidentes por severidad y estado para el dashboard")
    public ResponseEntity<AlertStatsResponseDto> getStats() {
        return ResponseEntity.ok(AlertStatsResponseDto.fromStats(alertUseCase.getStats()));
    }
}
