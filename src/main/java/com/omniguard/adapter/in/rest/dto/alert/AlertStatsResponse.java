package com.omniguard.adapter.in.rest.dto.alert;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatsResponse {

    private long totalOpen;
    private long totalInProgress;
    private long totalResolved;
    private long totalFalsePositive;
    private long criticalCount;
    private long highCount;
    private long mediumCount;
    private long lowCount;
    private long infoCount;
    private long last24Hours;
}