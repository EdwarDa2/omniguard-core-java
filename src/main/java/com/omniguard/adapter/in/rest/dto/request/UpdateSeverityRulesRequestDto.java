package com.omniguard.adapter.in.rest.dto.request;

public record UpdateSeverityRulesRequestDto(
        boolean notifyCritical,
        boolean notifyHigh,
        boolean notifyMedium,
        boolean notifyLow
) {}
