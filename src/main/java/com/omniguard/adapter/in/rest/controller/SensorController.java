package com.omniguard.adapter.in.rest.controller;

import com.omniguard.adapter.in.rest.dto.request.SensorStatusRequestDto;
import com.omniguard.adapter.in.rest.dto.response.SensorStatusResponseDto;
import com.omniguard.domain.port.in.SensorControlUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sensor")
@Tag(name = "Control del Sensor", description = "Endpoints de administración para activar o pausar la captura del sensor NIDS en C++")
public class SensorController {

    private final SensorControlUseCase sensorControlUseCase;

    public SensorController(SensorControlUseCase sensorControlUseCase) {
        this.sensorControlUseCase = sensorControlUseCase;
    }

    @PostMapping("/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_ANALYST')")
    @Operation(summary = "Cambiar estado operativo del sensor", description = "Envía una orden de activación o pausa al sensor de red a través de Redis")
    public ResponseEntity<SensorStatusResponseDto> changeStatus(@Valid @RequestBody SensorStatusRequestDto request) {
        String newStatus = sensorControlUseCase.changeSensorStatus(request.action());
        String message = "ACTIVE".equalsIgnoreCase(newStatus)
                ? "Sensor de red activado exitosamente"
                : "Sensor de red pausado exitosamente";

        return ResponseEntity.ok(new SensorStatusResponseDto(newStatus, message, Instant.now()));
    }

    @GetMapping("/status")
    @Operation(summary = "Consultar estado actual del sensor", description = "Retorna el estado de captura actual (ACTIVE, PAUSED o INACTIVE)")
    public ResponseEntity<SensorStatusResponseDto> getStatus() {
        String currentStatus = sensorControlUseCase.getSensorStatus();
        return ResponseEntity.ok(new SensorStatusResponseDto(currentStatus, "Estado del sensor obtenido", Instant.now()));
    }
}
