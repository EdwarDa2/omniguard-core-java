package com.omniguard.adapter.in.rest.mapper;

import com.omniguard.adapter.in.rest.dto.asset.AssetRequest;
import com.omniguard.adapter.in.rest.dto.asset.AssetResponse;
import com.omniguard.adapter.in.rest.dto.asset.OpenPortResponse;
import com.omniguard.adapter.in.rest.dto.asset.VulnerabilityResponse;
import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.OpenPort;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.vo.Hostname;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.MacAddress;
import com.omniguard.domain.vo.PortNumber;
import com.omniguard.domain.vo.Protocol;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AssetRestMapper {
    AssetRestMapper INSTANCE = Mappers.getMapper(AssetRestMapper.class);

    @Mapping(target = "ipAddress", expression = "java(new IpAddress(request.getIpAddress()))")
    @Mapping(target = "macAddress", expression = "java(request.getMacAddress() != null ? new MacAddress(request.getMacAddress()) : null)")
    @Mapping(target = "hostname", expression = "java(request.getHostname() != null ? new Hostname(request.getHostname()) : null)")
    Asset toDomain(AssetRequest request);

    @Mapping(target = "openPortsCount", expression = "java(domain.getOpenPorts() != null ? domain.getOpenPorts().size() : 0)")
    @Mapping(target = "vulnerabilitiesCount", expression = "java(domain.getVulnerabilities() != null ? domain.getVulnerabilities().size() : 0)")
    @Mapping(target = "criticalVulnsCount", expression = "java(countCritical(domain))")
    AssetResponse toResponse(Asset domain);

    List<AssetResponse> toResponseList(List<Asset> domains);

    @Named("countCritical")
    default int countCritical(Asset asset) {
        if (asset.getVulnerabilities() == null) return 0;
        return (int) asset.getVulnerabilities().stream()
                .filter(v -> "CRITICAL".equals(v.severity()))
                .count();
    }

    OpenPortResponse toResponse(OpenPort domain);
    List<OpenPortResponse> toResponseList(List<OpenPort> domains);

    VulnerabilityResponse toResponse(Vulnerability domain);
    List<VulnerabilityResponse> toResponseList(List<Vulnerability> domains);
}