package com.santms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * NetworkScan entity for tracking network discovery operations
 */
@Entity
@Table(name = "network_scans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetworkScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scan_name", length = 100)
    private String scanName;

    @Column(name = "target_network", nullable = false, length = 50)
    private String targetNetwork;

    @Builder.Default
    @Column(name = "scan_type", length = 50)
    private String scanType = "FULL";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private ScanStatus status = ScanStatus.PENDING;

    @Builder.Default
    @Column(name = "devices_found")
    private Integer devicesFound = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "initiated_by", length = 100)
    private String initiatedBy;

    @Column(name = "scan_result", columnDefinition = "TEXT")
    private String scanResult;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Builder.Default
    @Column(name = "progress_percent")
    private Integer progressPercent = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @JsonIgnore
    private Organization organization;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ScanStatus {
        PENDING,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}