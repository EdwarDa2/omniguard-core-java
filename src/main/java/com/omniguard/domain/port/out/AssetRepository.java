package com.omniguard.domain.port.out;

import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.vo.IpAddress;
import java.util.List;
import java.util.Optional;

public interface AssetRepository {
    Asset save(Asset asset);

    Optional<Asset> findById(Long id);

    Optional<Asset> findByIpAddress(IpAddress ipAddress);

    List<Asset> findAll(int page, int size, String status);

    List<Asset> findByStatus(String status, int page, int size);

    long count();

    long countByStatus(String status);

    void deleteById(Long id);

    List<OpenPort> findPortsByAssetId(Long assetId);

    List<Vulnerability> findVulnerabilitiesByAssetId(Long assetId);

    Optional<OpenPort> findPort(Long assetId, int port, String protocol);

    OpenPort savePort(OpenPort port);
}
