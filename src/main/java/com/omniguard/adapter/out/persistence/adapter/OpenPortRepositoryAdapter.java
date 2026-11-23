package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.OpenPortJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.OpenPortPersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataOpenPortRepository;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.port.out.OpenPortRepository;
import com.omniguard.domain.vo.PortNumber;
import com.omniguard.domain.vo.Protocol;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OpenPortRepositoryAdapter implements OpenPortRepository {

    private final SpringDataOpenPortRepository springDataOpenPortRepository;
    private final OpenPortPersistenceMapper mapper;

    @Override
    public OpenPort save(OpenPort openPort) {
        OpenPortJpaEntity entity = mapper.toEntity(openPort);
        OpenPortJpaEntity saved = springDataOpenPortRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<OpenPort> findById(UUID id) {
        return springDataOpenPortRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<OpenPort> findByAssetId(UUID assetId) {
        return springDataOpenPortRepository.findByAssetId(assetId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<OpenPort> findByAssetIdAndPortAndProtocol(UUID assetId, PortNumber portNumber, Protocol protocol) {
        return springDataOpenPortRepository.findByAssetIdAndPortNumberAndProtocol(
                assetId, portNumber.value(), OpenPortJpaEntity.PortProtocol.valueOf(protocol.value())
        ).map(mapper::toDomain);
    }

    @Override
    public List<OpenPort> findByPortNumber(PortNumber portNumber) {
        return springDataOpenPortRepository.findByPortNumber(portNumber.value()).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        springDataOpenPortRepository.deleteById(id);
    }

    @Override
    public void deleteByAssetId(UUID assetId) {
        springDataOpenPortRepository.deleteByAssetId(assetId);
    }

    @Override
    public long countByAssetId(UUID assetId) {
        return springDataOpenPortRepository.countByAssetId(assetId);
    }
}