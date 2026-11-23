package com.omniguard.adapter.in.rest.controller;

import com.omniguard.adapter.in.rest.dto.notification.*;
import com.omniguard.adapter.in.rest.mapper.NotificationRestMapper;
import com.omniguard.domain.model.NotificationConfig;
import com.omniguard.domain.port.in.NotificationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Validated
public class NotificationController {

    private final NotificationUseCase notificationUseCase;
    private final NotificationRestMapper notificationRestMapper;

    @PostMapping
    public ResponseEntity<NotificationConfigResponse> create(@Valid @RequestBody NotificationConfigRequest request) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getName());

        NotificationConfig config = notificationUseCase.create(
                userId,
                request.getPlatform(),
                request.getWebhookUrl(),
                request.getChatId(),
                request.getBotToken(),
                request.getEmailAddress()
        );
        return ResponseEntity.ok(notificationRestMapper.toResponse(config));
    }

    @GetMapping
    public ResponseEntity<List<NotificationConfigResponse>> findByUser() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(auth.getName());

        List<NotificationConfig> configs = notificationUseCase.findByUserId(userId);
        return ResponseEntity.ok(notificationRestMapper.toResponseList(configs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationConfigResponse> findById(@PathVariable UUID id) {
        NotificationConfig config = notificationUseCase.findById(id)
                .orElseThrow(() -> new com.omniguard.domain.exception.EntityNotFoundException("NotificationConfig", id));
        return ResponseEntity.ok(notificationRestMapper.toResponse(config));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<NotificationConfigResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationConfigRequest request
    ) {
        NotificationConfig config = notificationUseCase.update(
                id,
                request.getWebhookUrl(),
                request.getChatId(),
                request.getBotToken(),
                request.getEmailAddress(),
                request.isActive()
        );
        return ResponseEntity.ok(notificationRestMapper.toResponse(config));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        notificationUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test")
    public ResponseEntity<NotificationTestResponse> testConfig(@Valid @RequestBody NotificationTestRequest request) {
        UUID configId = UUID.fromString(request.getConfigId());
        NotificationUseCase.TestResult result = notificationUseCase.testConfig(configId);
        return ResponseEntity.ok(new NotificationTestResponse(result.success(), result.message()));
    }
}