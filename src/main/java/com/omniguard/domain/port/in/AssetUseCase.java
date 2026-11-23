package com.omniguard.domain.port.in;

import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.vo.IpAddress;
import java.util.List;
import java.util.Optional;

public interface AssetUseCase {
    Asset create(IpAddress ipAddress, String hostname);

    Optional<Asset> findById(Long id);

    Optional<Asset> findByIpAddress(IpAddress ipAddress);

    List<Asset> findAll(int page, int size, String status);

    List<Asset> findByStatus(String status, int page, int size);

    Asset update(Long id, String hostname, String status);

    void delete(Long id);

    void triggerScan(Long assetId);

    List<OpenPort> getPorts(Long assetId);

    List<Vulnerability> getVulnerabilities(Long assetId);

    long count();

    long countByStatus(String status);
}
