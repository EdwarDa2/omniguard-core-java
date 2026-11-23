package com.omniguard.adapter.in.rest.dto.alert;

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
public class AlertStatusUpdateRequest {

    @NotBlank(message = "Status es obligatorio")
    @Pattern(regexp = "^(OPEN|IN_PROGRESS|RESOLVED|FALSE_POSITIVE)$", message = "Status inválido: OPEN, IN_PROGRESS, RESOLVED, FALSE_POSITIVE")
    private String status;

    private UUID assignedTo;

    @Size(max = 2000, message = "Notas de resolución máximo 2000 caracteres")
    private String resolutionNotes;
}