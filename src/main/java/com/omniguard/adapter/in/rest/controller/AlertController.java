package com.omniguard.adapter.in.rest.controller;

import com.omniguard.adapter.in.rest.dto.alert.*;
import com.omniguard.adapter.in.rest.dto.common.PageResponse;
import com.omniguard.adapter.in.rest.mapper.AlertRestMapper;
import com.omniguard.adapter.in.rest.sse.SseAlertService;
import com.omniguard.domain.model.Alert;
import com.omniguard.domain.model.AlertStats;
import com.omniguard.domain.port.in.AlertUseCase;
import com.omniguard.domain.vo.AlertStatus;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.ThreatType;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
@Validated
public class AlertController {

    private final AlertUseCase alertUseCase;
    private final AlertRestMapper alertRestMapper;
    private final SseAlertService sseAlertService;

    @GetMapping
    public ResponseEntity<PageResponse<AlertResponse>> findAll(
            @ModelAttribute AlertFilterRequest filter,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(), Sort.Direction.fromString(sortDir), sortBy);
        List<Alert> alerts = alertUseCase.findAll(
                new AlertUseCase.AlertFilter(
                        filter.getStatus() != null ? AlertStatus.of(filter.getStatus()) : null,
                        filter.getSeverity(),
                        filter.getThreatType() != null ? ThreatType.of(filter.getThreatType()) : null,
                        filter.getAssetId(),
                        filter.getSourceIp() != null ? IpAddress.of(filter.getSourceIp()) : null,
                        filter.getDestinationIp() != null ? IpAddress.of(filter.getDestinationIp()) : null,
                        filter.getFrom(),
                        filter.getTo()
                ),
                filter.getPage(),
                filter.getSize()
        );
        long total = alertUseCase.count(
                new AlertUseCase.AlertFilter(
                        filter.getStatus() != null ? AlertStatus.of(filter.getStatus()) : null,
                        filter.getSeverity(),
                        filter.getThreatType() != null ? ThreatType.of(filter.getThreatType()) : null,
                        filter.getAssetId(),
                        filter.getSourceIp() != null ? IpAddress.of(filter.getSourceIp()) : null,
                        filter.getDestinationIp() != null ? IpAddress.of(filter.getDestinationIp()) : null,
                        filter.getFrom(),
                        filter.getTo()
                )
        );

        List<AlertResponse> content = alertRestMapper.toResponseList(alerts);
        Page<AlertResponse> page = new org.springframework.data.domain.PageImpl<>(content, PageRequest.of(filter.getPage(), filter.getSize(), org.springframework.data.domain.Sort.by(Sort.Direction.fromString(sortDir), sortBy)), total);

        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> findById(@PathVariable UUID id) {
        Alert alert = alertUseCase.findById(id)
                .orElseThrow(() -> new com.omniguard.domain.exception.EntityNotFoundException("Alert", id));
        return ResponseEntity.ok(alertRestMapper.toResponse(alert));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<AlertResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody AlertStatusUpdateRequest request
    ) {
        Alert alert = alertUseCase.updateStatus(
                id,
                request.getStatus(),
                request.getAssignedTo(),
                request.getResolutionNotes()
        );
        return ResponseEntity.ok(alertRestMapper.toResponse(alert));
    }

    @GetMapping("/stats")
    public ResponseEntity<AlertStatsResponse> getStats() {
        AlertStats stats = alertUseCase.getStats();
        return ResponseEntity.ok(alertRestMapper.toResponse(stats));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAlerts(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no");

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();

        return sseAlertService.subscribe(userId);
    }
}