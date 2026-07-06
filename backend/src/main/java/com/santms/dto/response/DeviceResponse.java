package com.santms.dto.response;

import com.santms.entity.Device;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DeviceResponse {
    private Long id;
    private String hostname;
    private String ipAddress;
    private String macAddress;
    private String vendor;
    private String operatingSystem;
    private Device.DeviceType deviceType;
    private Device.DeviceStatus status;
    private String subnet;
    private String gateway;
    private String dnsServer;
    private String openPorts;
    private String networkSpeed;
    private String networkName;
    private Integer vlanId;
    private Boolean isApproved;
    private Boolean isAuthorized;
    private Boolean maintenanceMode;
    private Double cpuUsage;
    private Double ramUsage;
    private Double storageUsage;
    private Double bandwidthUsage;
    private Double temperature;
    private Double latencyMs;
    private Double packetLoss;
    private Long uptimeSeconds;
    private Double availabilityPercent;
    private Integer riskScore;
    private Integer securityScore;
    private String description;
    private String tags;
    private String ipv6Address;
    private Boolean isDhcp;
    private LocalDateTime lastSeen;
    private LocalDateTime firstDiscovered;
    private LocalDateTime createdAt;
    private String organizationName;
}
