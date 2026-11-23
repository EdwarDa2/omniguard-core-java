package com.omniguard.adapter.in.rest.dto.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenPortResponse {

    private UUID id;
    private UUID assetId;
    private int portNumber;
    private String protocol;
    private String serviceBanner;
    private Instant discoveredAt;
}