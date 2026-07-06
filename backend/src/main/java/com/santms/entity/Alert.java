package com.santms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Alert entity for network events and security notifications
 */
@Entity
@Table(name = "alerts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlertSeverity severity = AlertSeverity.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AlertStatus status = AlertStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "device_ip", length = 45)
    private String deviceIp;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private String resolvedBy;

    @Column(name = "resolution_note", length = 500)
    private String resolutionNote;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "acknowledged_by")
    private String acknowledgedBy;

    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "threshold_value")
    private Double thresholdValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    public enum AlertType {
        DEVICE_OFFLINE, HIGH_CPU, HIGH_RAM, HIGH_TEMPERATURE,
        NETWORK_DOWN, BANDWIDTH_EXCEEDED, SECURITY_THREAT,
        UNAUTHORIZED_DEVICE, DUPLICATE_IP, PORT_SCAN_DETECTED,
        BRUTE_FORCE, ARP_SPOOFING, MAC_SPOOFING, ROGUE_AP,
        WEAK_PASSWORD, CONFIG_CHANGE, SYSTEM_ERROR, BACKUP_FAILED,
        DEVICE_ONLINE, MAINTENANCE_REMINDER
    }

    public enum AlertSeverity {
        CRITICAL, HIGH, MEDIUM, LOW, INFO
    }

    public enum AlertStatus {
        OPEN, ACKNOWLEDGED, RESOLVED, SUPPRESSED
    }
}
