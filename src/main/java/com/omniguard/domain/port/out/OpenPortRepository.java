package com.omniguard.domain.port.out;

import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.vo.PortNumber;
import com.omniguard.domain.vo.Protocol;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpenPortRepository {
    OpenPort save(OpenPort openPort);

    Optional<OpenPort> findById(UUID id);

    List<OpenPort> findByAssetId(UUID assetId);

    Optional<OpenPort> findByAssetIdAndPortAndProtocol(UUID assetId, PortNumber portNumber, Protocol protocol);

    List<OpenPort> findByPortNumber(PortNumber portNumber);

    void deleteById(UUID id);

    void deleteByAssetId(UUID assetId);

    long countByAssetId(UUID assetId);
}