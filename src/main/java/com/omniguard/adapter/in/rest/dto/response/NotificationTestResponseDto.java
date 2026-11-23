package com.omniguard.adapter.in.rest.dto.response;

public record NotificationTestResponseDto(
        String channel,
        boolean delivered,
        String message
) {}
