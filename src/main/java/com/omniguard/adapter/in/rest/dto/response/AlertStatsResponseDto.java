package com.omniguard.adapter.in.rest.dto.response;

import com.omniguard.domain.port.in.AlertUseCase.AlertStats;

public record AlertStatsResponseDto(
        long totalOpen,
        long totalInProgress,
        long totalResolved,
        long totalFalsePositive,
        long criticalCount,
        long highCount,
        long mediumCount,
        long lowCount,
        long infoCount,
        long last24Hours
) {
    public static AlertStatsResponseDto fromStats(AlertStats stats) {
        if (stats == null) {
            return null;
        }
        return new AlertStatsResponseDto(
                stats.totalOpen(),
                stats.totalInProgress(),
                stats.totalResolved(),
                stats.totalFalsePositive(),
                stats.criticalCount(),
                stats.highCount(),
                stats.mediumCount(),
                stats.lowCount(),
                stats.infoCount(),
                stats.last24Hours()
        );
    }
}
