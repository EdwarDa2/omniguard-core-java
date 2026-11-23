package com.omniguard.domain.model;

import com.omniguard.domain.vo.IpAddress;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public record Asset(
        Long id,
        IpAddress ipAddress,
        String hostname,
        String description,
        String os,
        String status,
        Instant firstSeenAt,
        Instant lastSeenAt,
        List<OpenPort> openPorts,
        List<Vulnerability> vulnerabilities
) {
    public static Asset create(IpAddress ipAddress, String hostname) {
        return create(ipAddress, hostname, null, null);
    }

    public static Asset create(IpAddress ipAddress, String hostname, String description, String os) {
        Instant now = Instant.now();
        return new Asset(
                null,
                ipAddress,
                hostname,
                description,
                os,
                "active",
                now,
                now,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    public Asset withId(Long id) {
        return new Asset(
                id,
                this.ipAddress,
                this.hostname,
                this.description,
                this.os,
                this.status,
                this.firstSeenAt,
                this.lastSeenAt,
                this.openPorts,
                this.vulnerabilities
        );
    }

    public Asset withPortsAndVulnerabilities(List<OpenPort> ports, List<Vulnerability> vulns) {
        return new Asset(
                this.id,
                this.ipAddress,
                this.hostname,
                this.description,
                this.os,
                this.status,
                this.firstSeenAt,
                this.lastSeenAt,
                ports != null ? List.copyOf(ports) : Collections.emptyList(),
                vulns != null ? List.copyOf(vulns) : Collections.emptyList()
        );
    }

    public Asset update(String hostname, String description, String os, String status) {
        return new Asset(
                this.id,
                this.ipAddress,
                hostname != null ? hostname : this.hostname,
                description != null ? description : this.description,
                os != null ? os : this.os,
                status != null ? status : this.status,
                this.firstSeenAt,
                Instant.now(),
                this.openPorts,
                this.vulnerabilities
        );
    }

    public Asset update(String hostname, String status) {
        return update(hostname, this.description, this.os, status);
    }
}
