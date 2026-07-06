package com.santms.dto.request;

import com.santms.entity.Device;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DeviceRequest {

    @NotBlank(message = "Hostname is required")
    @Size(max = 100)
    private String hostname;

    @NotBlank(message = "IP address is required")
    @Pattern(regexp = "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$",
             message = "Invalid IP address format")
    private String ipAddress;

    private String macAddress;
    private String vendor;
    private String operatingSystem;
    private Device.DeviceType deviceType;
    private String subnet;
    private String gateway;
    private String dnsServer;
    private String networkName;
    private Integer vlanId;
    private String description;
    private String tags;
    private String ipv6Address;
    private Boolean isDhcp;
    private Boolean isApproved;
    private Boolean isAuthorized;
}
