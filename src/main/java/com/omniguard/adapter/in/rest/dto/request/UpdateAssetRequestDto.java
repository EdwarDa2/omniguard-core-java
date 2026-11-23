package com.omniguard.adapter.in.rest.dto.request;

public record UpdateAssetRequestDto(
        String hostname,
        String description,
        String os,
        String status
) {}
