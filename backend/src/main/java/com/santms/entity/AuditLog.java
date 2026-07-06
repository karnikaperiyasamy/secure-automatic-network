package com.santms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * AuditLog entity for tracking all system events
 */
@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "entity_name", length = 200)
    private String entityName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LogLevel level = LogLevel.INFO;

    @Enumerated(EnumType.STRING)
    private LogCategory category;
    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @JsonIgnore
    private Organization organization;
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum LogLevel {
        INFO, WARNING, ERROR, CRITICAL
    }

    public enum LogCategory {
        AUTH, USER_MANAGEMENT, DEVICE, NETWORK, SECURITY,
        CONFIGURATION, BACKUP, REPORT, SYSTEM, TOPOLOGY
    }
}
