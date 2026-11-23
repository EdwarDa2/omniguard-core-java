package com.omniguard.domain.port.out;

import com.omniguard.domain.model.Asset;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.MacAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssetRepository {
    Asset save(Asset asset);

    Optional<Asset> findById(UUID id);

    Optional<Asset> findByIpAddress(IpAddress ipAddress);

    Optional<Asset> findByMacAddress(MacAddress macAddress);

    List<Asset> findAll(int page, int size);

    List<Asset> findByStatus(String status, int page, int size);

    long count();

    long countByStatus(String status);

    void deleteById(UUID id);
}