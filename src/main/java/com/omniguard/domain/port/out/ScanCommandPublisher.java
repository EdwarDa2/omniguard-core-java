package com.omniguard.domain.port.out;

import com.omniguard.domain.vo.IpAddress;
import java.util.UUID;

public interface ScanCommandPublisher {
    /**
     * Publica una orden de escaneo activo en el canal Redis correspondiente
     * para que sea consumida por el sensor en C++.
     */
    void publishScanCommand(UUID scanId, IpAddress target, String ports);
}
