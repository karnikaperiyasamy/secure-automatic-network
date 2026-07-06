package com.santms.dto.response;

import lombok.*;
import java.util.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardResponse {
    // Device Counts
    private long totalDevices;
    private long onlineDevices;
    private long offlineDevices;
    private long warningDevices;
    private long criticalDevices;
    private long routers;
    private long switches;
    private long firewalls;
    private long servers;
    private long computers;
    private long printers;
    private long wirelessDevices;
    private long unknownDevices;

    // Alert Counts
    private long totalAlerts;
    private long criticalAlerts;
    private long highAlerts;
    private long mediumAlerts;
    private long openAlerts;

    // Performance Metrics
    private double avgCpuUsage;
    private double avgRamUsage;
    private double avgBandwidthUsage;
    private double avgStorageUsage;
    private double avgLatency;
    private double avgSecurityScore;
    private double networkAvailability;

    // Users
    private long totalUsers;
    private long activeUsers;

    // Security
    private int overallSecurityScore;
    private long unauthorizedDevices;
    private long riskDevices;

    // Recent Data
    private List<DeviceResponse> topActiveDevices;
    private List<AlertResponse> recentAlerts;
    private List<ActivityItem> recentActivities;

    // Charts data
    private Map<String, Long> devicesByType;
    private Map<String, Long> alertsByType;
    private List<TimeSeriesPoint> bandwidthTrend;
    private List<TimeSeriesPoint> alertTrend;

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class TimeSeriesPoint {
        private String label;
        private double value;
    }

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor
    public static class ActivityItem {
        private String action;
        private String description;
        private String user;
        private String time;
        private String icon;
        private String color;
    }
}
