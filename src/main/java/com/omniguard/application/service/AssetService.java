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

    public AssetService(AssetRepository assetRepository, ScanCommandPublisher scanCommandPublisher) {
        this.assetRepository = assetRepository;
        this.scanCommandPublisher = scanCommandPublisher;
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
    public void triggerScan(Long assetId) {
        triggerScan(assetId, "1-1024");
    }

    @Override
    public void triggerScan(Long assetId, String ports) {
        Asset asset = assetRepository.findById(assetId).orElseThrow(
                () -> new EntityNotFoundException("Asset not found with id: " + assetId)
        );
        UUID scanId = UUID.randomUUID();
        scanCommandPublisher.publishScanCommand(scanId, asset.ipAddress(), ports);
        log.info("Triggered active SurfaceScan for asset: id={}, ip={}, scanId={}", assetId, asset.ipAddress().getValue(), scanId);
    }

    @Override
    public void triggerScanAll(String ports) {
        List<Asset> activeAssets = assetRepository.findByStatus("active", 0, 100);
        log.info("Triggering perimetric SurfaceScan for {} active assets", activeAssets.size());
        for (Asset asset : activeAssets) {
            UUID scanId = UUID.randomUUID();
            scanCommandPublisher.publishScanCommand(scanId, asset.ipAddress(), ports);
        }
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
