package com.omniguard.application.service;

import com.omniguard.domain.exception.BusinessRuleViolationException;
import com.omniguard.domain.exception.EntityNotFoundException;
import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.port.in.AssetUseCase;
import com.omniguard.domain.port.out.AssetRepository;
import com.omniguard.domain.port.out.ScanCommandPublisher;
import com.omniguard.domain.vo.IpAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetService implements AssetUseCase {

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);

    private final AssetRepository assetRepository;
    private final ScanCommandPublisher scanCommandPublisher;
    private final VulnerabilityCorrelationService vulnerabilityCorrelationService;
    private final ScanTargetValidator scanTargetValidator;

    public AssetService(
            AssetRepository assetRepository,
            ScanCommandPublisher scanCommandPublisher,
            VulnerabilityCorrelationService vulnerabilityCorrelationService,
            ScanTargetValidator scanTargetValidator
    ) {
        this.assetRepository = assetRepository;
        this.scanCommandPublisher = scanCommandPublisher;
        this.vulnerabilityCorrelationService = vulnerabilityCorrelationService;
        this.scanTargetValidator = scanTargetValidator;
    }

    @Override
    @Transactional
    public Asset create(IpAddress ipAddress, String hostname) {
        return create(ipAddress, hostname, null, null);
    }

    @Override
    @Transactional
    public Asset create(IpAddress ipAddress, String hostname, String description, String os) {
        if (assetRepository.findByIpAddress(ipAddress).isPresent()) {
            throw new BusinessRuleViolationException(
                    "An asset with IP address " + ipAddress.getValue() + " already exists"
            );
        }
        Asset asset = Asset.create(ipAddress, hostname, description, os);
        Asset saved = assetRepository.save(asset);
        log.info("Created network asset: id={}, ip={}, hostname={}", saved.id(), ipAddress.getValue(), hostname);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Asset> findById(Long id) {
        return assetRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Asset> findByIpAddress(IpAddress ipAddress) {
        return assetRepository.findByIpAddress(ipAddress);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findAll(int page, int size, String status) {
        return assetRepository.findAll(page, size, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findByStatus(String status, int page, int size) {
        return assetRepository.findByStatus(status, page, size);
    }

    @Override
    @Transactional
    public Asset update(Long id, String hostname, String status) {
        return update(id, hostname, null, null, status);
    }

    @Override
    @Transactional
    public Asset update(Long id, String hostname, String description, String os, String status) {
        Asset existing = assetRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Asset not found with id: " + id)
        );
        Asset updated = existing.update(hostname, description, os, status);
        Asset saved = assetRepository.save(updated);
        log.info("Updated network asset: id={}, ip={}", saved.id(), saved.ipAddress().getValue());
        return saved;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (assetRepository.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Asset not found with id: " + id);
        }
        assetRepository.deleteById(id);
        log.info("Deleted network asset: id={}", id);
    }

    @Override
    public UUID triggerScan(Long assetId) {
        return triggerScan(assetId, "1-1024");
    }

    @Override
    public UUID triggerScan(Long assetId, String ports) {
        Asset asset = assetRepository.findById(assetId).orElseThrow(
                () -> new EntityNotFoundException("Asset not found with id: " + assetId)
        );

        String normalizedPorts = ports != null && !ports.isBlank() ? ports : "1-1024";
        scanTargetValidator.validatePorts(normalizedPorts);
        scanTargetValidator.validateTarget(asset.ipAddress().getValue());

        UUID scanId = UUID.randomUUID();
        scanCommandPublisher.publishScanCommand(scanId, asset.ipAddress(), normalizedPorts);
        log.info("Triggered active SurfaceScan for asset: id={}, ip={}, scanId={}, ports={}",
                assetId, asset.ipAddress().getValue(), scanId, normalizedPorts);
        return scanId;
    }

    @Override
    public List<UUID> triggerScanAll(String ports) {
        String normalizedPorts = ports != null && !ports.isBlank() ? ports : "1-1024";
        scanTargetValidator.validatePorts(normalizedPorts);

        List<Asset> activeAssets = assetRepository.findByStatus("active", 0, 100);
        List<UUID> scanIds = new java.util.ArrayList<>();
        int skipped = 0;

        for (Asset asset : activeAssets) {
            if (scanTargetValidator.isTargetAuthorized(asset.ipAddress().getValue())) {
                UUID scanId = UUID.randomUUID();
                scanCommandPublisher.publishScanCommand(scanId, asset.ipAddress(), normalizedPorts);
                scanIds.add(scanId);
            } else {
                skipped++;
                log.warn("Omitiendo escaneo de activo id={}, ip={}: Fuera de la allowlist autorizada",
                        asset.id(), asset.ipAddress().getValue());
            }
        }

        log.info("Triggered perimetric SurfaceScan for {} authorized assets ({} skipped)",
                scanIds.size(), skipped);
        return scanIds;
    }

    @Override
    @Transactional
    public Asset processScanResults(IpAddress targetIp, List<OpenPort> discoveredPorts) {
        Asset asset = assetRepository.findByIpAddress(targetIp)
                .orElseGet(() -> assetRepository.save(
                        Asset.create(targetIp, "host-" + targetIp.getValue().replace(".", "-"),
                                "Activo detectado por escaneo perimetral", "Linux / Desconocido")
                ));

        asset = assetRepository.save(asset.withLastSeenAt(java.time.Instant.now()));

        if (discoveredPorts != null) {
            for (OpenPort p : discoveredPorts) {
                String protocol = (p.protocol() != null && !p.protocol().isBlank())
                        ? p.protocol().toUpperCase()
                        : "TCP";

                Optional<OpenPort> existingPortOpt = assetRepository.findPort(asset.id(), p.port(), protocol);
                OpenPort savedPort;
                if (existingPortOpt.isPresent()) {
                    OpenPort existing = existingPortOpt.get();
                    OpenPort updated = new OpenPort(
                            existing.id(),
                            asset.id(),
                            existing.port(),
                            existing.protocol(),
                            (p.appProtocol() != null && !p.appProtocol().isBlank() && !"Desconocido".equalsIgnoreCase(p.appProtocol()))
                                    ? p.appProtocol()
                                    : existing.appProtocol(),
                            p.banner() != null ? p.banner() : existing.banner(),
                            p.productVersion() != null ? p.productVersion() : existing.productVersion(),
                            existing.cpeName(),
                            existing.firstSeenAt(),
                            java.time.Instant.now()
                    );
                    savedPort = assetRepository.savePort(updated);
                } else {
                    OpenPort newPort = OpenPort.create(
                            asset.id(),
                            p.port(),
                            protocol,
                            p.appProtocol(),
                            p.banner(),
                            p.productVersion(),
                            null
                    );
                    savedPort = assetRepository.savePort(newPort);
                }

                if (savedPort != null && savedPort.id() != null) {
                    vulnerabilityCorrelationService.correlateService(
                            asset.id(),
                            savedPort.id(),
                            savedPort.productVersion(),
                            savedPort.appProtocol(),
                            savedPort.banner()
                    );
                }
            }
        }

        log.info("Processed {} scan results for asset id={}, ip={}",
                discoveredPorts != null ? discoveredPorts.size() : 0, asset.id(), targetIp.getValue());

        return assetRepository.findById(asset.id()).orElse(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpenPort> getPorts(Long assetId) {
        return assetRepository.findPortsByAssetId(assetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vulnerability> getVulnerabilities(Long assetId) {
        return assetRepository.findVulnerabilitiesByAssetId(assetId);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return assetRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return assetRepository.countByStatus(status);
    }
}
