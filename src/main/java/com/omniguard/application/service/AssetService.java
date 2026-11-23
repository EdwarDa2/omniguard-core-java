package com.omniguard.application.service;

import com.omniguard.adapter.out.persistence.adapter.AssetRepositoryAdapter;
import com.omniguard.adapter.out.persistence.adapter.OpenPortRepositoryAdapter;
import com.omniguard.adapter.out.persistence.adapter.VulnerabilityRepositoryAdapter;
import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.port.in.AssetUseCase;
import com.omniguard.domain.port.out.AssetRepository;
import com.omniguard.domain.port.out.OpenPortRepository;
import com.omniguard.domain.port.out.VulnerabilityRepository;
import com.omniguard.domain.vo.Hostname;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.MacAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AssetService implements AssetUseCase {

    private final AssetRepository assetRepository;
    private final OpenPortRepository openPortRepository;
    private final VulnerabilityRepository vulnerabilityRepository;

    @Override
    public Asset create(IpAddress ipAddress, String macAddress, String hostname, String osName) {
        Asset asset = Asset.create(
                ipAddress,
                macAddress != null ? MacAddress.of(macAddress) : null,
                hostname != null ? Hostname.of(hostname) : null,
                osName
        );
        return assetRepository.save(asset);
    }

    @Override
    public Optional<Asset> findById(UUID id) {
        return assetRepository.findById(id);
    }

    @Override
    public Optional<Asset> findByIpAddress(IpAddress ipAddress) {
        return assetRepository.findByIpAddress(ipAddress);
    }

    @Override
    public List<Asset> findAll(int page, int size, String status) {
        return assetRepository.findAll(page, size);
    }

    @Override
    public List<Asset> findByStatus(String status, int page, int size) {
        return assetRepository.findByStatus(status, page, size);
    }

    @Override
    public Asset update(UUID id, String hostname, String osName, String status) {
        return assetRepository.findById(id)
                .map(asset -> {
                    Asset updated = asset.update(
                            hostname != null ? Hostname.of(hostname) : null,
                            osName,
                            status
                    );
                    return assetRepository.save(updated);
                })
                .orElseThrow(() -> new com.omniguard.domain.exception.EntityNotFoundException("Asset", id));
    }

    @Override
    public void delete(UUID id) {
        assetRepository.deleteById(id);
    }

    @Override
    public void triggerScan(UUID assetId) {
        // TODO: Publicar evento en Redis para que el sensor/analyzer inicie escaneo
        // Por ahora solo actualizamos last_scanned_at
        assetRepository.findById(assetId)
                .map(Asset::recordScan)
                .ifPresent(assetRepository::save);
    }

    @Override
    public List<OpenPort> getPorts(UUID assetId) {
        return openPortRepository.findByAssetId(assetId);
    }

    @Override
    public List<Vulnerability> getVulnerabilities(UUID assetId) {
        return vulnerabilityRepository.findByAssetId(assetId);
    }

    @Override
    public long count() {
        return assetRepository.count();
    }

    @Override
    public long countByStatus(String status) {
        return assetRepository.countByStatus(status);
    }
}