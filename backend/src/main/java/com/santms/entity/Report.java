package com.santms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportPeriod period;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportFormat format = ReportFormat.PDF;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "generated_by", length = 100)
    private String generatedBy;

    @Column(name = "report_data", columnDefinition = "TEXT")
    private String reportData;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "is_scheduled")
    @Builder.Default
    private Boolean isScheduled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ReportType { NETWORK_HEALTH, SECURITY, BANDWIDTH, INVENTORY, AUDIT, DEVICE_STATUS }
    public enum ReportPeriod { DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }
    public enum ReportFormat { PDF, EXCEL, CSV }
}
