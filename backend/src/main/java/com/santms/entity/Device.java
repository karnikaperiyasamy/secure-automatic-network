package com.santms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Device entity representing network devices
 */
@Entity
@Table(name = "devices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String hostname;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "mac_address", length = 17)
    private String macAddress;

    @Column(length = 100)
    private String vendor;

    @Column(name = "operating_system", length = 100)
    private String operatingSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    @Builder.Default
    private DeviceType deviceType = DeviceType.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.UNKNOWN;

    @Column(length = 50)
    private String subnet;

    @Column(length = 45)
    private String gateway;

    @Column(name = "dns_server", length = 45)
    private String dnsServer;

    @Column(name = "open_ports", length = 1000)
    private String openPorts;

    @Column(name = "network_speed", length = 20)
    private String networkSpeed;

    @Column(name = "network_name", length = 100)
    private String networkName;

    @Column(name = "vlan_id")
    private Integer vlanId;

    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = false;

    @Column(name = "is_authorized")
    @Builder.Default
    private Boolean isAuthorized = true;

    @Column(name = "maintenance_mode")
    @Builder.Default
    private Boolean maintenanceMode = false;

    @Column(name = "cpu_usage")
    @Builder.Default
    private Double cpuUsage = 0.0;

    @Column(name = "ram_usage")
    @Builder.Default
    private Double ramUsage = 0.0;

    @Column(name = "storage_usage")
    @Builder.Default
    private Double storageUsage = 0.0;

    @Column(name = "bandwidth_usage")
    @Builder.Default
    private Double bandwidthUsage = 0.0;

    @Column(name = "temperature")
    @Builder.Default
    private Double temperature = 0.0;

    @Column(name = "latency_ms")
    @Builder.Default
    private Double latencyMs = 0.0;

    @Column(name = "packet_loss")
    @Builder.Default
    private Double packetLoss = 0.0;

    @Column(name = "uptime_seconds")
    @Builder.Default
    private Long uptimeSeconds = 0L;

    @Column(name = "availability_percent")
    @Builder.Default
    private Double availabilityPercent = 100.0;

    @Column(name = "risk_score")
    @Builder.Default
    private Integer riskScore = 0;

    @Column(name = "security_score")
    @Builder.Default
    private Integer securityScore = 100;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String tags;

    @Column(name = "qr_code")
    private String qrCode;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "first_discovered")
    private LocalDateTime firstDiscovered;

    @Column(name = "ipv6_address", length = 45)
    private String ipv6Address;

    @Column(name = "is_dhcp")
    @Builder.Default
    private Boolean isDhcp = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    public enum DeviceType {
        ROUTER, SWITCH, FIREWALL, SERVER, COMPUTER, PRINTER,
        ACCESS_POINT, IOT_DEVICE, VIRTUAL_MACHINE, UNKNOWN,
        SMARTPHONE, TABLET, CAMERA, NAS, UPS
    }

    public enum DeviceStatus {
        ONLINE, OFFLINE, WARNING, CRITICAL, MAINTENANCE, UNKNOWN
    }
}
