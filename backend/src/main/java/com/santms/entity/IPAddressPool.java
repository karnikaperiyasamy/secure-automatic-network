package com.santms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "ip_address_pool")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IPAddressPool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false, unique = true, length = 45)
    private String ipAddress;

    @Column(name = "subnet", length = 50)
    private String subnet;

    @Column(name = "gateway", length = 45)
    private String gateway;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IPStatus status = IPStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IPType type = IPType.DYNAMIC;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "assigned_device_id")
    private Long assignedDeviceId;

    @Column(name = "mac_address", length = 17)
    private String macAddress;

    @Column(name = "hostname", length = 100)
    private String hostname;

    @Column(name = "lease_expiry")
    private LocalDateTime leaseExpiry;

    @Column(name = "is_reserved")
    @Builder.Default
    private Boolean isReserved = false;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "vlan_id")
    private Integer vlanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum IPStatus { AVAILABLE, ASSIGNED, RESERVED, CONFLICTED, EXPIRED }
    public enum IPType { STATIC, DYNAMIC, RESERVED }
}
