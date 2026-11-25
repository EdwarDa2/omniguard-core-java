package com.omniguard.adapter.in.rest.controller;

import com.omniguard.adapter.in.rest.dto.request.ScanResultRequestDto;
import com.omniguard.adapter.in.rest.sse.AlertSseService;
import com.omniguard.application.service.ScanTargetValidator;
import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.port.in.AssetUseCase;
import com.omniguard.domain.vo.IpAddress;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internal/assets")
@Tag(name = "Escaneo Interno (M2M)", description = "Endpoints de ingesta de escaneos de red consumidos por el motor de escaneo")
public class InternalAssetController {

    private static final Logger log = LoggerFactory.getLogger(InternalAssetController.class);

    private final AssetUseCase assetUseCase;
    private final AlertSseService alertSseService;
    private final ScanTargetValidator scanTargetValidator;

    public InternalAssetController(
            AssetUseCase assetUseCase,
            AlertSseService alertSseService,
            ScanTargetValidator scanTargetValidator
    ) {
        this.assetUseCase = assetUseCase;
        this.alertSseService = alertSseService;
        this.scanTargetValidator = scanTargetValidator;
    }

    @PostMapping("/scan-results")
    @PreAuthorize("hasAuthority('ROLE_INTERNAL')")
    @Operation(summary = "Ingesta de resultados de escaneo de red", description = "Persiste los puertos y banners descubiertos y emite notificación SSE scan:completed")
    public ResponseEntity<Map<String, Object>> ingestScanResults(
            @Valid @RequestBody ScanResultRequestDto request
    ) {
        // Defensa en profundidad: validar que el objetivo reportado pertenezca a la allowlist
        scanTargetValidator.validateTarget(request.targetIp());

        // Manejo de escaneos fallidos: no alterar ni borrar datos existentes
        if ("FAILED".equalsIgnoreCase(request.status())) {
            log.warn("Escaneo reportado como FAILED para scan_id={}, target_ip={}. No se alteran los datos existentes.",
                    request.scanId(), request.targetIp());
            return ResponseEntity.ok(Map.of(
                    "status", "IGNORED_FAILED_SCAN",
                    "scan_id", request.scanId(),
                    "target_ip", request.targetIp()
            ));
        }

        log.info("Recibidos resultados de escaneo scan_id={} para target_ip={} con {} puertos",
                request.scanId(), request.targetIp(), request.openPorts() != null ? request.openPorts().size() : 0);

        List<OpenPort> domainPorts = Collections.emptyList();
        if (request.openPorts() != null) {
            domainPorts = request.openPorts().stream()
                    .map(p -> OpenPort.create(
                            null,
                            p.port(),
                            p.protocol(),
                            p.appProtocol(),
                            p.banner(),
                            p.productVersion(),
                            null
                    ))
                    .toList();
        }

        IpAddress targetIp = IpAddress.of(request.targetIp());
        Asset processedAsset = assetUseCase.processScanResults(targetIp, domainPorts);

        int portsCount = processedAsset.openPorts() != null ? processedAsset.openPorts().size() : 0;
        int vulnsCount = processedAsset.vulnerabilities() != null ? processedAsset.vulnerabilities().size() : 0;

        // Notificar al Frontend por SSE en tiempo real incluyendo los metadatos de escaneo
        alertSseService.broadcastScanCompleted(
                request.scanId(),
                processedAsset.id(),
                targetIp.getValue(),
                portsCount,
                request.scannedAt() != null ? request.scannedAt().toString() : java.time.Instant.now().toString()
        );

        return ResponseEntity.ok(Map.of(
                "status", "PROCESSED",
                "scan_id", request.scanId(),
                "asset_id", processedAsset.id(),
                "target_ip", targetIp.getValue(),
                "open_ports_count", portsCount,
                "vulnerabilities_count", vulnsCount
        ));
    }
}
