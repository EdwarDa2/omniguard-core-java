package com.omniguard.adapter.out.persistence.adapter;

import com.omniguard.adapter.out.persistence.entity.NetworkAssetJpaEntity;
import com.omniguard.adapter.out.persistence.entity.ServiceJpaEntity;
import com.omniguard.adapter.out.persistence.mapper.AssetPersistenceMapper;
import com.omniguard.adapter.out.persistence.repository.SpringDataCveRecordRepository;
import com.omniguard.adapter.out.persistence.repository.SpringDataNetworkAssetRepository;
import com.omniguard.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.port.out.AssetRepository;
import com.omniguard.domain.vo.IpAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AssetPersistenceAdapter implements AssetRepository {

    private final SpringDataNetworkAssetRepository networkAssetRepository;
    private final SpringDataServiceRepository serviceRepository;
    private final SpringDataCveRecordRepository cveRecordRepository;
    private final AssetPersistenceMapper mapper;

    public AssetPersistenceAdapter(
            SpringDataNetworkAssetRepository networkAssetRepository,
            SpringDataServiceRepository serviceRepository,
            SpringDataCveRecordRepository cveRecordRepository,
            AssetPersistenceMapper mapper
    ) {
        this.networkAssetRepository = networkAssetRepository;
        this.serviceRepository = serviceRepository;
        this.cveRecordRepository = cveRecordRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Asset save(Asset asset) {
        NetworkAssetJpaEntity entity = mapper.toJpaEntity(asset);
        NetworkAssetJpaEntity saved = networkAssetRepository.save(entity);
        return mapper.toDomain(saved, asset.openPorts(), asset.vulnerabilities());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Asset> findById(Long id) {
        return networkAssetRepository.findById(id).map(this::enrichAsset);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Asset> findByIpAddress(IpAddress ipAddress) {
        return networkAssetRepository.findByIpAddress(ipAddress.getValue()).map(this::enrichAsset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findAll(int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "lastSeenAt"));
        Page<NetworkAssetJpaEntity> entityPage;
        if (status != null && !status.isBlank()) {
            entityPage = networkAssetRepository.findByStatus(status, pageable);
        } else {
            entityPage = networkAssetRepository.findAll(pageable);
        }
        return entityPage.getContent().stream().map(this::enrichAsset).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Asset> findByStatus(String status, int page, int size) {
        return findAll(page, size, status);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return networkAssetRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return networkAssetRepository.countByStatus(status);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        networkAssetRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpenPort> findPortsByAssetId(Long assetId) {
        return serviceRepository.findByAssetId(assetId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Vulnerability> findVulnerabilitiesByAssetId(Long assetId) {
        return cveRecordRepository.findVulnerabilitiesByAssetId(assetId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public OpenPort savePort(OpenPort port) {
        ServiceJpaEntity entity = mapper.toJpaEntity(port);
        ServiceJpaEntity saved = serviceRepository.save(entity);
        return mapper.toDomain(saved);
    }

    private Asset enrichAsset(NetworkAssetJpaEntity entity) {
        List<OpenPort> ports = findPortsByAssetId(entity.getId());
        List<Vulnerability> vulns = findVulnerabilitiesByAssetId(entity.getId());
        return mapper.toDomain(entity, ports, vulns);
    }
}
