package com.omniguard.adapter.in.rest.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScanResponseDto(
        @JsonProperty("scanId")
        String scanId,

        @JsonProperty("target")
        String target,

        @JsonProperty("status")
        String status,

        @JsonProperty("message")
        String message
) {
    @JsonProperty("scan_id")
    public String getScanIdSnake() {
        return scanId;
    }
}
