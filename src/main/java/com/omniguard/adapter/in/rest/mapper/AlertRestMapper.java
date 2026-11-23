package com.omniguard.adapter.in.rest.mapper;

import com.omniguard.adapter.in.rest.dto.alert.AlertResponse;
import com.omniguard.adapter.in.rest.dto.alert.AlertStatsResponse;
import com.omniguard.adapter.in.rest.dto.asset.AssetResponse;
import com.omniguard.adapter.in.rest.dto.asset.VulnerabilityResponse;
import com.omniguard.adapter.in.rest.dto.user.UserResponse;
import com.omniguard.domain.model.Alert;
import com.omniguard.domain.model.AlertStats;
import com.omniguard.domain.model.Asset;
import com.omniguard.domain.model.User;
import com.omniguard.domain.model.Vulnerability;
import com.omniguard.domain.vo.AlertStatus;
import com.omniguard.domain.vo.IpAddress;
import com.omniguard.domain.vo.Protocol;
import com.omniguard.domain.vo.ThreatType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AssetRestMapper.class, UserRestMapper.class})
public interface AlertRestMapper {
    AlertRestMapper INSTANCE = Mappers.getMapper(AlertRestMapper.class);

    @Mapping(target = "asset", source = "asset")
    @Mapping(target = "assignedTo", source = "assignedTo")
    @Mapping(target = "correlatedVulnerabilities", source = "vulnerabilities")
    @Mapping(target = "sourceIp", expression = "java(domain.getSourceIp() != null ? domain.getSourceIp().value() : null)")
    @Mapping(target = "destinationIp", expression = "java(domain.getDestinationIp() != null ? domain.getDestinationIp().value() : null)")
    @Mapping(target = "protocol", expression = "java(domain.getProtocol() != null ? domain.getProtocol().value() : null)")
    @Mapping(target = "threatType", expression = "java(domain.getThreatType() != null ? domain.getThreatType().value() : null)")
    @Mapping(target = "status", expression = "java(domain.getStatus() != null ? domain.getStatus().value() : null)")
    AlertResponse toResponse(Alert domain);

    List<AlertResponse> toResponseList(List<Alert> domains);

    @Mapping(target = "totalOpen", source = "totalOpen")
    @Mapping(target = "totalInProgress", source = "totalInProgress")
    @Mapping(target = "totalResolved", source = "totalResolved")
    @Mapping(target = "totalFalsePositive", source = "totalFalsePositive")
    @Mapping(target = "criticalCount", source = "criticalCount")
    @Mapping(target = "highCount", source = "highCount")
    @Mapping(target = "mediumCount", source = "mediumCount")
    @Mapping(target = "lowCount", source = "lowCount")
    @Mapping(target = "infoCount", source = "infoCount")
    @Mapping(target = "last24Hours", source = "last24Hours")
    AlertStatsResponse toResponse(AlertStats domain);
}