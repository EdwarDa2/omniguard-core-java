package com.omniguard.domain.port.in;

public interface SensorControlUseCase {
    String changeSensorStatus(String action);
    String getSensorStatus();
}
