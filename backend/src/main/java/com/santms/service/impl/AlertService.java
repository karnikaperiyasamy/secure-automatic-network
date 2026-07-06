package com.santms.service.impl;

import com.santms.dto.response.AlertResponse;
import com.santms.entity.*;
import com.santms.exception.ResourceNotFoundException;
import com.santms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional
public class AlertService {

    private final AlertRepository alertRepository;

    public Page<AlertResponse> getAlerts(Long orgId, int page, int size) {
        return alertRepository.findByOrganizationId(orgId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toResponse);
    }

    public List<AlertResponse> getOpenAlerts(Long orgId) {
        return alertRepository.findByOrganizationIdAndStatus(orgId, Alert.AlertStatus.OPEN)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public AlertResponse acknowledgeAlert(Long id, String username) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        alert.setStatus(Alert.AlertStatus.ACKNOWLEDGED);
        alert.setAcknowledgedAt(LocalDateTime.now());
        alert.setAcknowledgedBy(username);
        return toResponse(alertRepository.save(alert));
    }

    public AlertResponse resolveAlert(Long id, String username, String note) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", id));
        alert.setStatus(Alert.AlertStatus.RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(username);
        alert.setResolutionNote(note);
        return toResponse(alertRepository.save(alert));
    }

    public long countOpenAlerts(Long orgId) {
        return alertRepository.countOpenAlerts(orgId);
    }

    public long countCriticalAlerts(Long orgId) {
        return alertRepository.countBySeverityAndOrg(orgId, Alert.AlertSeverity.CRITICAL);
    }

    public AlertResponse toResponse(Alert a) {
        return AlertResponse.builder()
                .id(a.getId()).title(a.getTitle()).message(a.getMessage())
                .type(a.getType()).severity(a.getSeverity()).status(a.getStatus())
                .deviceIp(a.getDeviceIp()).deviceName(a.getDeviceName())
                .resolvedAt(a.getResolvedAt()).resolvedBy(a.getResolvedBy())
                .acknowledgedAt(a.getAcknowledgedAt()).acknowledgedBy(a.getAcknowledgedBy())
                .metricValue(a.getMetricValue()).thresholdValue(a.getThresholdValue())
                .createdAt(a.getCreatedAt())
                .deviceId(a.getDevice() != null ? a.getDevice().getId() : null)
                .build();
    }
}
