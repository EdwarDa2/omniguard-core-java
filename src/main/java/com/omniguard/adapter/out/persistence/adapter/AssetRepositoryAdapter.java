package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.AssetJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.AssetPersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataAssetRepository;
import com.omniguard.domain.model.Asset;
import com.omniguard.domain.port.out.AssetRepository;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.MacAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssetRepositoryAdapter implements AssetRepository {

    private final SpringDataAssetRepository springDataAssetRepository;
    private final AssetPersistenceMapper mapper;

    @Override
    public Asset save(Asset asset) {
        AssetJpaEntity entity = mapper.toEntity(asset);
        AssetJpaEntity saved = springDataAssetRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Asset> findById(UUID id) {
        return springDataAssetRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Asset> findByIpAddress(IpAddress ipAddress) {
        return springDataAssetRepository.findByIpAddress(ipAddress.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Asset> findByMacAddress(MacAddress macAddress) {
        return springDataAssetRepository.findByMacAddress(macAddress.value()).map(mapper::toDomain);
    }

    @Override
    public List<Asset> findAll(int page, int size) {
        return springDataAssetRepository.findAll(PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Asset> findByStatus(String status, int page, int size) {
        return springDataAssetRepository.findByStatus(AssetJpaEntity.AssetStatus.valueOf(status), PageRequest.of(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long count() {
        return springDataAssetRepository.count();
    }

    @Override
    public long countByStatus(String status) {
        return springDataAssetRepository.countByStatus(AssetJpaEntity.AssetStatus.valueOf(status));
    }

    @Override
    public void deleteById(UUID id) {
        springDataAssetRepository.deleteById(id);
    }
}