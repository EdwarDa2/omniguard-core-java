package com.omniguard.adapter.in.rest.dto.response;

import java.util.List;

public record DashboardStatsResponseDto(
        KpisDto kpis,
        TrafficActivityDto trafficActivity,
        List<RecentAlertDto> recentAlerts
) {
    public record KpisDto(
            KpiItemDto openPorts,
            KpiItemDto activeThreats,
            KpiItemDto vulnerableAssets,
            KpiItemDto alerts24h
    ) {}

    public record KpiItemDto(
            String title,
            Object value,
            String level
    ) {}

    public record TrafficActivityDto(
            long currentPacketsPerSec,
            double bandwidthMbps,
            String sensorStatus,
            List<TimeSeriesPointDto> timeSeries,
            String sensorId
    ) {
        public TrafficActivityDto(long currentPacketsPerSec, double bandwidthMbps, String sensorStatus, List<TimeSeriesPointDto> timeSeries) {
            this(currentPacketsPerSec, bandwidthMbps, sensorStatus, timeSeries, "default-host");
        }
    }

    public record TimeSeriesPointDto(
            String time,
            long inbound,
            long outbound
    ) {}

    public record RecentAlertDto(
            String id,
            String time,
            String sourceIp,
            String threat,
            String severity,
            String timestamp
    ) {
        public RecentAlertDto(String id, String time, String sourceIp, String threat, String severity) {
            this(id, time, sourceIp, threat, severity, null);
        }
    }
}
