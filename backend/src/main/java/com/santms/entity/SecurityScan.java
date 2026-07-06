package com.santms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_scans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SecurityScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "scan_type", length = 50)
    private String scanType;

    @Column(name = "open_ports", length = 1000)
    private String openPorts;

    @Column(name = "vulnerabilities", columnDefinition = "TEXT")
    private String vulnerabilities;

    @Column(name = "risk_score")
    @Builder.Default
    private Integer riskScore = 0;

    @Column(name = "security_score")
    @Builder.Default
    private Integer securityScore = 100;

    @Column(name = "weak_passwords_detected")
    @Builder.Default
    private Boolean weakPasswordsDetected = false;

    @Column(name = "unauthorized_access_detected")
    @Builder.Default
    private Boolean unauthorizedAccessDetected = false;

    @Column(name = "suspicious_traffic_detected")
    @Builder.Default
    private Boolean suspiciousTrafficDetected = false;

    @Column(name = "arp_spoofing_detected")
    @Builder.Default
    private Boolean arpSpoofingDetected = false;

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations;

    @Column(name = "scan_summary", length = 500)
    private String scanSummary;

    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
