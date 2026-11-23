package com.omniguard.adapter.in.rest.dto.response;

public record ScanResponseDto(
        String scanId,
        String target,
        String status,
        String message
) {}
