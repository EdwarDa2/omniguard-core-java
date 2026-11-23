package com.omniguard.adapter.in.rest.dto.response;

import java.util.List;

public record AssetListResponseDto(
        AssetKpisDto kpis,
        List<AssetResponseDto> assets,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public record AssetKpisDto(
            long totalAssets,
            long exposedPorts,
            long vulnerableAssets,
            String highestSeverity
    ) {}
}
