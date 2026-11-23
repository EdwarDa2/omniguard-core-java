package com.omniguard.adapter.in.rest.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleTotpRequest {

    @NotNull(message = "Enable es obligatorio (true/false)")
    private Boolean enable;
}