package com.omniguard.domain.port.in;

import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.vo.IpAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetUseCase {
    Asset create(IpAddress ipAddress, String macAddress, String hostname, String osName);

    Optional<Asset> findById(UUID id);

    Optional<Asset> findByIpAddress(IpAddress ipAddress);

    List<Asset> findAll(int page, int size, String status);

    List<Asset> findByStatus(String status, int page, int size);

    Asset update(UUID id, String hostname, String osName, String status);

    void delete(UUID id);

    void triggerScan(UUID assetId);

    List<OpenPort> getPorts(UUID assetId);

    List<Vulnerability> getVulnerabilities(UUID assetId);

    long count();

    long countByStatus(String status);
}