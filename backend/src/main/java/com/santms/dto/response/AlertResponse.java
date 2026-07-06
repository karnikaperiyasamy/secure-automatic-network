package com.santms.dto.response;

import com.santms.entity.Alert;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AlertResponse {
    private Long id;
    private String title;
    private String message;
    private Alert.AlertType type;
    private Alert.AlertSeverity severity;
    private Alert.AlertStatus status;
    private Long deviceId;
    private String deviceIp;
    private String deviceName;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    private Double metricValue;
    private Double thresholdValue;
    private LocalDateTime createdAt;
}
