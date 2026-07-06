package com.santms.service.impl;

import com.santms.dto.response.DashboardResponse;
import com.santms.entity.*;
import com.santms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard statistics aggregation service
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {

    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final DeviceService deviceService;

    public DashboardResponse getDashboard(Long orgId) {
        List<Device> devices = deviceRepository.findByOrganizationId(orgId);

        // Device counts
        long total = devices.size();
        long online = devices.stream().filter(d -> d.getStatus() == Device.DeviceStatus.ONLINE).count();
        long offline = devices.stream().filter(d -> d.getStatus() == Device.DeviceStatus.OFFLINE).count();
        long warning = devices.stream().filter(d -> d.getStatus() == Device.DeviceStatus.WARNING).count();
        long critical = devices.stream().filter(d -> d.getStatus() == Device.DeviceStatus.CRITICAL).count();

        // By type
        long routers = countByType(devices, Device.DeviceType.ROUTER);
        long switches = countByType(devices, Device.DeviceType.SWITCH);
        long firewalls = countByType(devices, Device.DeviceType.FIREWALL);
        long servers = countByType(devices, Device.DeviceType.SERVER);
        long computers = countByType(devices, Device.DeviceType.COMPUTER);
        long printers = countByType(devices, Device.DeviceType.PRINTER);
        long wireless = countByType(devices, Device.DeviceType.ACCESS_POINT);

        // Averages
        double avgCpu = devices.stream().mapToDouble(Device::getCpuUsage).average().orElse(0);
        double avgRam = devices.stream().mapToDouble(Device::getRamUsage).average().orElse(0);
        double avgBw = devices.stream().mapToDouble(Device::getBandwidthUsage).average().orElse(0);
        double avgStorage = devices.stream().mapToDouble(Device::getStorageUsage).average().orElse(0);
        double avgLatency = devices.stream().mapToDouble(Device::getLatencyMs).average().orElse(0);
        double avgSecurity = devices.stream().mapToDouble(Device::getSecurityScore).average().orElse(85);
        double availability = total > 0 ? (online * 100.0 / total) : 0;

        // Alerts
        List<Alert> allAlerts = alertRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        long openAlerts = allAlerts.stream().filter(a -> a.getStatus() == Alert.AlertStatus.OPEN).count();
        long critAlerts = allAlerts.stream().filter(a -> a.getSeverity() == Alert.AlertSeverity.CRITICAL &&
                a.getStatus() == Alert.AlertStatus.OPEN).count();
        long highAlerts = allAlerts.stream().filter(a -> a.getSeverity() == Alert.AlertSeverity.HIGH &&
                a.getStatus() == Alert.AlertStatus.OPEN).count();

        // Top devices by CPU
        List<Device> topDevices = devices.stream()
                .filter(d -> d.getStatus() == Device.DeviceStatus.ONLINE)
                .sorted(Comparator.comparingDouble(Device::getCpuUsage).reversed())
                .limit(5)
                .collect(Collectors.toList());

        // Recent alerts
        List<Alert> recentAlerts = allAlerts.stream().limit(10).collect(Collectors.toList());

        // Recent audit activities
        List<AuditLog> recentLogs = auditLogRepository
                .findRecentByOrg(orgId, LocalDateTime.now().minusHours(24))
                .stream().limit(10).collect(Collectors.toList());

        List<DashboardResponse.ActivityItem> activities = recentLogs.stream()
                .map(l -> new DashboardResponse.ActivityItem(
                        l.getAction(), l.getDescription(), l.getPerformedBy(),
                        l.getCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd HH:mm")),
                        getIconForAction(l.getAction()), getColorForCategory(l.getCategory())))
                .collect(Collectors.toList());

        // Device by type map
        Map<String, Long> byType = new LinkedHashMap<>();
        for (Device.DeviceType type : Device.DeviceType.values()) {
            byType.put(type.name(), countByType(devices, type));
        }

        // Bandwidth trend (simulated last 7 days)
        List<DashboardResponse.TimeSeriesPoint> bwTrend = generateTrend(7, 30, 90, "Day");
        List<DashboardResponse.TimeSeriesPoint> alertTrend = generateTrend(7, 2, 20, "Day");

        // Users
        long totalUsers = userRepository.countActiveByOrganization(orgId);

        // Security score
        int secScore = (int) Math.round(avgSecurity);

        return DashboardResponse.builder()
                .totalDevices(total)
                .onlineDevices(online)
                .offlineDevices(offline)
                .warningDevices(warning)
                .criticalDevices(critical)
                .routers(routers).switches(switches).firewalls(firewalls)
                .servers(servers).computers(computers).printers(printers)
                .wirelessDevices(wireless)
                .totalAlerts(allAlerts.size())
                .criticalAlerts(critAlerts)
                .highAlerts(highAlerts)
                .openAlerts(openAlerts)
                .avgCpuUsage(round(avgCpu))
                .avgRamUsage(round(avgRam))
                .avgBandwidthUsage(round(avgBw))
                .avgStorageUsage(round(avgStorage))
                .avgLatency(round(avgLatency))
                .avgSecurityScore(round(avgSecurity))
                .networkAvailability(round(availability))
                .totalUsers(totalUsers)
                .overallSecurityScore(secScore)
                .unauthorizedDevices(devices.stream().filter(d -> !d.getIsAuthorized()).count())
                .topActiveDevices(topDevices.stream().map(deviceService::toResponse).collect(Collectors.toList()))
                .recentAlerts(recentAlerts.stream().map(this::toAlertResponse).collect(Collectors.toList()))
                .recentActivities(activities)
                .devicesByType(byType)
                .bandwidthTrend(bwTrend)
                .alertTrend(alertTrend)
                .build();
    }

    private long countByType(List<Device> devices, Device.DeviceType type) {
        return devices.stream().filter(d -> d.getDeviceType() == type).count();
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private List<DashboardResponse.TimeSeriesPoint> generateTrend(int days, double min, double max, String unit) {
        List<DashboardResponse.TimeSeriesPoint> list = new ArrayList<>();
        String[] labels = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        Random r = new Random();
        for (int i = 0; i < days; i++) {
            list.add(new DashboardResponse.TimeSeriesPoint(labels[i % 7], round(min + r.nextDouble() * (max - min))));
        }
        return list;
    }

    private String getIconForAction(String action) {
        return switch (action) {
            case "LOGIN" -> "fa-sign-in-alt";
            case "LOGOUT" -> "fa-sign-out-alt";
            case "CREATE" -> "fa-plus-circle";
            case "DELETE" -> "fa-trash";
            case "UPDATE" -> "fa-edit";
            default -> "fa-info-circle";
        };
    }

    private String getColorForCategory(AuditLog.LogCategory cat) {
        if (cat == null) return "#6c757d";
        return switch (cat) {
            case AUTH -> "#4ecdc4";
            case SECURITY -> "#ff6b6b";
            case DEVICE -> "#45b7d1";
            case NETWORK -> "#96ceb4";
            default -> "#6c757d";
        };
    }

    private com.santms.dto.response.AlertResponse toAlertResponse(Alert a) {
        return com.santms.dto.response.AlertResponse.builder()
                .id(a.getId()).title(a.getTitle()).message(a.getMessage())
                .type(a.getType()).severity(a.getSeverity()).status(a.getStatus())
                .deviceIp(a.getDeviceIp()).deviceName(a.getDeviceName())
                .metricValue(a.getMetricValue()).createdAt(a.getCreatedAt())
                .build();
    }
}
