package com.omniguard.adapter.in.rest.controller;

import com.omniguard.adapter.in.rest.dto.asset.*;
import com.omniguard.adapter.in.rest.mapper.AssetRestMapper;
import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.port.in.AssetUseCase;
import com.omniguard.domain.vo.IpAddress;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assets")
@RequiredArgsConstructor
@Validated
public class AssetController {

    private final AssetUseCase assetUseCase;
    private final AssetRestMapper assetRestMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest request) {
        Asset asset = assetUseCase.create(
                IpAddress.of(request.getIpAddress()),
                request.getMacAddress() != null ? MacAddress.of(request.getMacAddress()) : null,
                request.getHostname() != null ? Hostname.of(request.getHostname()) : null,
                request.getOsName()
        );
        return ResponseEntity.ok(assetRestMapper.toResponse(asset));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> findById(@PathVariable UUID id) {
        Asset asset = assetUseCase.findById(id)
                .orElseThrow(() -> new com.omniguard.domain.exception.EntityNotFoundException("Asset", id));
        return ResponseEntity.ok(assetRestMapper.toResponse(asset));
    }

    @GetMapping
    public ResponseEntity<com.omniguard.adapter.in.rest.dto.common.PageResponse<AssetResponse>> findAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
            @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(sortDir), sortBy);
        Page<Asset> assets = assetUseCase.findAll(status != null ? status : "", page, size);
        return ResponseEntity.ok(com.omniguard.adapter.in.rest.dto.common.PageResponse.of(assets.map(assetRestMapper::toResponse)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<AssetResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AssetRequest request
    ) {
        Asset asset = assetUseCase.update(
                id,
                request.getHostname() != null ? Hostname.of(request.getHostname()) : null,
                request.getOsName(),
                request.getStatus()
        );
        return ResponseEntity.ok(assetRestMapper.toResponse(asset));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        assetUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/scan")
    @PreAuthorize("hasRole('ADMIN') or hasRole('ANALYST')")
    public ResponseEntity<AssetScanResponse> triggerScan(@PathVariable UUID id) {
        assetUseCase.triggerScan(id);
        return ResponseEntity.ok(new AssetScanResponse("Escaneo iniciado", "STARTED"));
    }

    @GetMapping("/{id}/ports")
    public ResponseEntity<List<OpenPortResponse>> getPorts(@PathVariable UUID id) {
        List<OpenPort> ports = assetUseCase.getPorts(id);
        return ResponseEntity.ok(assetRestMapper.toResponseList(ports));
    }

    @GetMapping("/{id}/vulnerabilities")
    public ResponseEntity<List<VulnerabilityResponse>> getVulnerabilities(@PathVariable UUID id) {
        List<Vulnerability> vulns = assetUseCase.getVulnerabilities(id);
        return ResponseEntity.ok(assetRestMapper.toResponseList(vulns));
    }
}