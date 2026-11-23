package com.omniguard.adapter.in.rest.dto.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetRequest {

    @NotBlank(message = "IP address es obligatoria")
    @Pattern(regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$",
            message = "IP inválida (IPv4 o IPv6)")
    private String ipAddress;

    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", message = "MAC inválida (formato XX:XX:XX:XX:XX:XX)")
    private String macAddress;

    @Size(max = 255, message = "Hostname máximo 255 caracteres")
    @Pattern(regexp = "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$|^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)$",
            message = "Hostname inválido")
    private String hostname;

    @Size(max = 100, message = "OS name máximo 100 caracteres")
    private String osName;
}