package com.omniguard.adapter.in.rest.dto.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTestRequest {

    @NotBlank(message = "Config ID es obligatorio")
    private String configId;
}