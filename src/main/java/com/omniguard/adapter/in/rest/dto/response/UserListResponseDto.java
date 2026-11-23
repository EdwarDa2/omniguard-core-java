package com.omniguard.adapter.in.rest.dto.response;

import java.util.List;

public record UserListResponseDto(
        UserMetricsDto metrics,
        List<PlatformUserDto> users
) {
    public record UserMetricsDto(
            long activeCount,
            long adminCount,
            long revokedCount
    ) {}
}
