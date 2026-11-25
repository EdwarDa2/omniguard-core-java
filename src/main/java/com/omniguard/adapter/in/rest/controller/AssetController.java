package com.omniguard.adapter.in.rest.controller;

import com.omniguard.adapter.in.rest.dto.request.CreateAssetRequestDto;
import com.omniguard.adapter.in.rest.dto.request.TriggerScanRequestDto;
import com.omniguard.adapter.in.rest.dto.request.UpdateAssetRequestDto;
import com.omniguard.adapter.in.rest.dto.response.AssetListResponseDto;
import com.omniguard.adapter.in.rest.dto.response.AssetResponseDto;
import com.omniguard.adapter.in.rest.dto.response.OpenPortResponseDto;
import com.omniguard.adapter.in.rest.dto.response.ScanResponseDto;
import com.omniguard.adapter.in.rest.dto.response.VulnerabilityResponseDto;
import com.omniguard.adapter.out.persistence.repository.SpringDataCveRecordRepository;
import com.omniguard.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.omniguard.domain.exception.EntityNotFoundException;
import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.model.Severity;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.port.in.AssetUseCase;
import com.omniguard.domain.vo.IpAddress;
import jakarta.validation.Valid;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetController {

    private final AssetUseCase assetUseCase;
    private final SpringDataServiceRepository serviceRepository;
    private final SpringDataCveRecordRepository cveRecordRepository;

    public AssetController(
            AssetUseCase assetUseCase,
            SpringDataServiceRepository serviceRepository,
            SpringDataCveRecordRepository cveRecordRepository
    ) {
        this.assetUseCase = assetUseCase;
        this.serviceRepository = serviceRepository;
        this.cveRecordRepository = cveRecordRepository;
    }

    @GetMapping
    public ResponseEntity<AssetListResponseDto> listAssets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        List<Asset> assets = assetUseCase.findAll(page, size, status);
        List<AssetResponseDto> assetDtos = assets.stream().map(this::toResponseDto).toList();

        long totalAssets = assetUseCase.count();
        long exposedPorts = serviceRepository.count();
        long vulnerableAssets = cveRecordRepository.countVulnerableAssets();

        String highestSeverity = assetDtos.stream()
                .map(AssetResponseDto::cveSeverity)
                .filter(s -> s != null && !s.equals("NORMAL"))
                .findFirst()
                .orElse("NORMAL");

        AssetListResponseDto.AssetKpisDto kpis = new AssetListResponseDto.AssetKpisDto(
                totalAssets,
                exposedPorts,
                vulnerableAssets,
                highestSeverity
        );

        int totalPages = (int) Math.ceil((double) totalAssets / Math.max(1, size));
        return ResponseEntity.ok(
                new AssetListResponseDto(kpis, assetDtos, page, size, totalAssets, totalPages)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponseDto> getAssetById(@PathVariable Long id) {
        Asset asset = assetUseCase.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Asset not found with id: " + id)
        );
        return ResponseEntity.ok(toResponseDto(asset));
    }

    @PostMapping("/{id}/scan")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_ANALYST') or hasAuthority('ROLE_INTERNAL')")
    public ResponseEntity<ScanResponseDto> triggerScan(
            @PathVariable Long id,
            @RequestBody(required = false) TriggerScanRequestDto request
    ) {
        Asset asset = assetUseCase.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Asset not found with id: " + id)
        );
        String ports = request != null && request.ports() != null ? request.ports() : "1-1024";
        UUID scanId = assetUseCase.triggerScan(id, ports);

        ScanResponseDto response = new ScanResponseDto(
                scanId.toString(),
                asset.ipAddress().getValue(),
                "QUEUED",
                "Orden de escaneo publicada al microservicio sensor C++"
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/scan-all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_ANALYST')")
    public ResponseEntity<ScanResponseDto> triggerScanAll(
            @RequestBody(required = false) TriggerScanRequestDto request
    ) {
        String ports = request != null && request.ports() != null ? request.ports() : "1-1024";
        List<UUID> scanIds = assetUseCase.triggerScanAll(ports);

        String firstScanId = !scanIds.isEmpty() ? scanIds.get(0).toString() : UUID.randomUUID().toString();
        ScanResponseDto response = new ScanResponseDto(
                firstScanId,
                "ALL (" + scanIds.size() + " activos autorizados)",
                "QUEUED",
                "Escaneo de superficie iniciado para " + scanIds.size() + " activos autorizados"
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_ANALYST') or hasAuthority('ROLE_INTERNAL')")
    public ResponseEntity<AssetResponseDto> createAsset(@Valid @RequestBody CreateAssetRequestDto request) {
        Asset created = assetUseCase.create(
                IpAddress.of(request.ip()),
                request.hostname(),
                request.description(),
                request.os()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseDto(created));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_ANALYST')")
    public ResponseEntity<AssetResponseDto> updateAsset(
            @PathVariable Long id,
            @RequestBody UpdateAssetRequestDto request
    ) {
        Asset updated = assetUseCase.update(
                id,
                request.hostname(),
                request.description(),
                request.os(),
                request.status()
        );
        return ResponseEntity.ok(toResponseDto(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        assetUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AssetResponseDto toResponseDto(Asset asset) {
        List<OpenPort> ports = asset.openPorts() != null ? asset.openPorts() : List.of();
        List<Vulnerability> vulns = asset.vulnerabilities() != null ? asset.vulnerabilities() : List.of();

        boolean hasCve = !vulns.isEmpty();

        // Determinar la severidad máxima y el puntaje CVSS
        Double maxCvss = vulns.stream()
                .map(Vulnerability::cvssScore)
                .filter(score -> score != null)
                .max(Double::compareTo)
                .orElse(null);

        Severity maxSeverity = vulns.stream()
                .map(Vulnerability::cvssSeverity)
                .filter(s -> s != null)
                .max(Comparator.comparingInt(this::severityPriority))
                .orElse(Severity.NONE);

        String cveSeverityFrontend = mapSeverityToFrontend(maxSeverity);
        String severityLabel = mapSeverityToLabel(maxSeverity, hasCve);

        List<VulnerabilityResponseDto> vulnDtos = vulns.stream().map(v -> {
            String servicePort = "Servicio detectado";
            return new VulnerabilityResponseDto(
                    v.cveId().getValue(),
                    v.cvssScore(),
                    mapSeverityToSpanish(v.cvssSeverity()),
                    servicePort,
                    v.description(),
                    "Posibles coincidencias con versión. Validar los parches instalados."
            );
        }).toList();

        List<OpenPortResponseDto> portDtos = ports.stream().map(p -> {
            // Contar si este puerto tiene vulnerabilidades
            long matchingVulns = vulns.stream()
                    .filter(v -> v.description() != null && v.description().contains(String.valueOf(p.port())))
                    .count();

            OpenPortResponseDto.VulnBadgeDto badge = null;
            if (matchingVulns > 0) {
                badge = new OpenPortResponseDto.VulnBadgeDto((int) matchingVulns, "ALTA");
            }

            return new OpenPortResponseDto(
                    p.port(),
                    p.protocol(),
                    p.appProtocol(),
                    p.productVersion() != null ? p.productVersion() : (p.banner() != null ? p.banner() : "-"),
                    "ABIERTO",
                    badge
            );
        }).toList();

        String formattedLastScanned = asset.lastSeenAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(asset.lastSeenAt())
                : "-";

        return new AssetResponseDto(
                asset.id(),
                asset.hostname() != null ? asset.hostname() : asset.ipAddress().getValue(),
                asset.ipAddress().getValue(),
                asset.description() != null ? asset.description() : "Activo de red monitorizado",
                asset.os() != null ? asset.os() : "Linux / Desconocido",
                formattedLastScanned,
                asset.status() != null ? asset.status() : "active",
                ports.size(),
                hasCve,
                cveSeverityFrontend,
                severityLabel,
                maxCvss,
                portDtos,
                vulnDtos
        );
    }

    private int severityPriority(Severity severity) {
        if (severity == null) return 0;
        return switch (severity) {
            case CRITICAL -> 5;
            case HIGH -> 4;
            case MEDIUM -> 3;
            case LOW -> 2;
            case INFO -> 1;
            case NONE -> 0;
        };
    }

    private String mapSeverityToFrontend(Severity severity) {
        if (severity == null) return "NORMAL";
        return switch (severity) {
            case CRITICAL -> "CRITICAL";
            case HIGH -> "HIGH";
            case MEDIUM -> "WARNING";
            case LOW, NONE -> "NORMAL";
            case INFO -> "INFO";
        };
    }

    private String mapSeverityToLabel(Severity severity, boolean hasCve) {
        if (!hasCve || severity == null || severity == Severity.NONE) {
            return "SIN CVES";
        }
        return switch (severity) {
            case CRITICAL -> "CRITICA";
            case HIGH -> "ALTA";
            case MEDIUM -> "MEDIA";
            case LOW -> "BAJA";
            case INFO -> "INFO";
            case NONE -> "SIN CVES";
        };
    }

    private String mapSeverityToSpanish(Severity severity) {
        if (severity == null) return "MEDIA";
        return switch (severity) {
            case CRITICAL -> "CRÍTICA";
            case HIGH -> "ALTA";
            case MEDIUM -> "MEDIA";
            case LOW -> "BAJA";
            case INFO -> "INFORMATIVA";
            case NONE -> "SIN RIESGO";
        };
    }
}
