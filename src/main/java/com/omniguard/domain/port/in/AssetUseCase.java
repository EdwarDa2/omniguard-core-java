package com.omniguard.domain.port.in;

import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.vo.IpAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetUseCase {
    Asset create(IpAddress ipAddress, String hostname);

    default Asset create(IpAddress ipAddress, String hostname, String description, String os) {
        return create(ipAddress, hostname);
    }

    Optional<Asset> findById(Long id);

    Optional<Asset> findByIpAddress(IpAddress ipAddress);

    List<Asset> findAll(int page, int size, String status);

    List<Asset> findByStatus(String status, int page, int size);

    Asset update(Long id, String hostname, String status);

    default Asset update(Long id, String hostname, String description, String os, String status) {
        return update(id, hostname, status);
    }

    void delete(Long id);

    UUID triggerScan(Long assetId, String ports);

    default UUID triggerScan(Long assetId) {
        return triggerScan(assetId, "1-1024");
    }

    List<UUID> triggerScanAll(String ports);

    Asset processScanResults(IpAddress targetIp, List<OpenPort> discoveredPorts);

    List<OpenPort> getPorts(Long assetId);

    List<Vulnerability> getVulnerabilities(Long assetId);

    long count();

    long countByStatus(String status);
}
