package com.santms.service.impl;

import com.santms.dto.request.DeviceRequest;
import com.santms.dto.response.DeviceResponse;
import com.santms.entity.*;
import com.santms.exception.*;
import com.santms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Device management service - CRUD, monitoring, simulation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final OrganizationRepository orgRepository;
    private final AlertRepository alertRepository;
    private final ModelMapper modelMapper;

    public Page<DeviceResponse> getAllDevices(Long orgId, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sort));
        return deviceRepository.findAll(pageable)
                .map(this::toResponse);
    }

    public Page<DeviceResponse> searchDevices(Long orgId, String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return deviceRepository.searchDevices(orgId, query, pageable)
                .map(this::toResponse);
    }

    public DeviceResponse getDevice(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", id));
        return toResponse(device);
    }

    public DeviceResponse createDevice(DeviceRequest request, Long orgId, String createdBy) {
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", orgId));

        if (deviceRepository.existsByIpAddressAndOrganizationId(request.getIpAddress(), orgId)) {
            throw new ConflictException("Device with IP " + request.getIpAddress() + " already exists");
        }

        Device device = Device.builder()
                .hostname(request.getHostname())
                .ipAddress(request.getIpAddress())
                .macAddress(request.getMacAddress())
                .vendor(request.getVendor())
                .operatingSystem(request.getOperatingSystem())
                .deviceType(request.getDeviceType() != null ? request.getDeviceType() : Device.DeviceType.UNKNOWN)
                .status(Device.DeviceStatus.UNKNOWN)
                .subnet(request.getSubnet())
                .gateway(request.getGateway())
                .dnsServer(request.getDnsServer())
                .networkName(request.getNetworkName())
                .vlanId(request.getVlanId())
                .description(request.getDescription())
                .tags(request.getTags())
                .ipv6Address(request.getIpv6Address())
                .isDhcp(request.getIsDhcp() != null ? request.getIsDhcp() : true)
                .isApproved(false)
                .isAuthorized(true)
                .organization(org)
                .createdBy(createdBy)
                .firstDiscovered(LocalDateTime.now())
                .lastSeen(LocalDateTime.now())
                .build();

        device = deviceRepository.save(device);
        log.info("Device created: {} ({})", device.getHostname(), device.getIpAddress());
        return toResponse(device);
    }

    public DeviceResponse updateDevice(Long id, DeviceRequest request) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", id));

        if (request.getHostname() != null) device.setHostname(request.getHostname());
        if (request.getIpAddress() != null) device.setIpAddress(request.getIpAddress());
        if (request.getMacAddress() != null) device.setMacAddress(request.getMacAddress());
        if (request.getVendor() != null) device.setVendor(request.getVendor());
        if (request.getOperatingSystem() != null) device.setOperatingSystem(request.getOperatingSystem());
        if (request.getDeviceType() != null) device.setDeviceType(request.getDeviceType());
        if (request.getSubnet() != null) device.setSubnet(request.getSubnet());
        if (request.getGateway() != null) device.setGateway(request.getGateway());
        if (request.getDescription() != null) device.setDescription(request.getDescription());
        if (request.getTags() != null) device.setTags(request.getTags());
        if (request.getIsApproved() != null) device.setIsApproved(request.getIsApproved());
        if (request.getIsAuthorized() != null) device.setIsAuthorized(request.getIsAuthorized());

        return toResponse(deviceRepository.save(device));
    }

    public void deleteDevice(Long id) {
        if (!deviceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Device", id);
        }
        deviceRepository.deleteById(id);
    }

    public DeviceResponse toggleMaintenance(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device", id));
        device.setMaintenanceMode(!device.getMaintenanceMode());
        if (device.getMaintenanceMode()) {
            device.setStatus(Device.DeviceStatus.MAINTENANCE);
        }
        return toResponse(deviceRepository.save(device));
    }

    public Map<String, Long> getDeviceStats(Long orgId) {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", deviceRepository.countByOrganization(orgId));
        stats.put("online", deviceRepository.countByOrgAndStatus(orgId, Device.DeviceStatus.ONLINE));
        stats.put("offline", deviceRepository.countByOrgAndStatus(orgId, Device.DeviceStatus.OFFLINE));
        stats.put("warning", deviceRepository.countByOrgAndStatus(orgId, Device.DeviceStatus.WARNING));
        stats.put("critical", deviceRepository.countByOrgAndStatus(orgId, Device.DeviceStatus.CRITICAL));
        for (Device.DeviceType type : Device.DeviceType.values()) {
            stats.put(type.name().toLowerCase(), deviceRepository.countByOrgAndType(orgId, type));
        }
        return stats;
    }

    /**
     * Simulate live metrics update every 15 seconds
     */
    @Scheduled(fixedDelay = 15000)
    public void updateDeviceMetrics() {
        List<Device> devices = deviceRepository.findAll();
        for (Device device : devices) {
            if (device.getMaintenanceMode()) continue;

            double cpu = simulateMetric(device.getCpuUsage(), 0, 100);
            double ram = simulateMetric(device.getRamUsage(), 20, 95);
            double bw = simulateMetric(device.getBandwidthUsage(), 0, 100);
            double storage = simulateMetric(device.getStorageUsage(), 30, 90);
            double latency = simulateMetric(device.getLatencyMs(), 0.5, 150);
            double temp = simulateMetric(device.getTemperature(), 35, 80);

            // Randomly flip online/offline (95% stay online)
            if (device.getStatus() != Device.DeviceStatus.MAINTENANCE) {
                double r = Math.random();
                if (r < 0.02) device.setStatus(Device.DeviceStatus.OFFLINE);
                else if (cpu > 85 || ram > 90) device.setStatus(Device.DeviceStatus.WARNING);
                else if (cpu > 95 || temp > 75) device.setStatus(Device.DeviceStatus.CRITICAL);
                else device.setStatus(Device.DeviceStatus.ONLINE);
            }

            device.setCpuUsage(cpu);
            device.setRamUsage(ram);
            device.setBandwidthUsage(bw);
            device.setStorageUsage(storage);
            device.setLatencyMs(latency);
            device.setTemperature(temp);
            device.setLastSeen(LocalDateTime.now());

            // Generate alerts for high metrics
            if (cpu > 90 && device.getStatus() != Device.DeviceStatus.OFFLINE) {
                createMetricAlert(device, Alert.AlertType.HIGH_CPU, cpu, 90.0, "High CPU usage detected");
            }
            if (ram > 90) {
                createMetricAlert(device, Alert.AlertType.HIGH_RAM, ram, 90.0, "High RAM usage detected");
            }
            if (temp > 70) {
                createMetricAlert(device, Alert.AlertType.HIGH_TEMPERATURE, temp, 70.0, "High temperature detected");
            }
        }
        deviceRepository.saveAll(devices);
    }

    private double simulateMetric(double current, double min, double max) {
        double delta = (Math.random() - 0.5) * 10;
        double next = current + delta;
        return Math.max(min, Math.min(max, Math.round(next * 10.0) / 10.0));
    }

    private void createMetricAlert(Device device, Alert.AlertType type, double value, double threshold, String msg) {
        // Avoid duplicate open alerts
        boolean exists = alertRepository.findByDeviceIdOrderByCreatedAtDesc(device.getId())
                .stream().anyMatch(a -> a.getType() == type && a.getStatus() == Alert.AlertStatus.OPEN);
        if (exists) return;

        Alert alert = Alert.builder()
                .title(msg)
                .message(String.format("%s on %s: %.1f%% (threshold: %.0f%%)",
                        msg, device.getHostname(), value, threshold))
                .type(type)
                .severity(value > 95 ? Alert.AlertSeverity.CRITICAL : Alert.AlertSeverity.HIGH)
                .status(Alert.AlertStatus.OPEN)
                .device(device)
                .deviceIp(device.getIpAddress())
                .deviceName(device.getHostname())
                .metricValue(value)
                .thresholdValue(threshold)
                .organization(device.getOrganization())
                .build();
        alertRepository.save(alert);
    }

    public DeviceResponse toResponse(Device d) {
        return DeviceResponse.builder()
                .id(d.getId())
                .hostname(d.getHostname())
                .ipAddress(d.getIpAddress())
                .macAddress(d.getMacAddress())
                .vendor(d.getVendor())
                .operatingSystem(d.getOperatingSystem())
                .deviceType(d.getDeviceType())
                .status(d.getStatus())
                .subnet(d.getSubnet())
                .gateway(d.getGateway())
                .dnsServer(d.getDnsServer())
                .openPorts(d.getOpenPorts())
                .networkSpeed(d.getNetworkSpeed())
                .networkName(d.getNetworkName())
                .vlanId(d.getVlanId())
                .isApproved(d.getIsApproved())
                .isAuthorized(d.getIsAuthorized())
                .maintenanceMode(d.getMaintenanceMode())
                .cpuUsage(d.getCpuUsage())
                .ramUsage(d.getRamUsage())
                .storageUsage(d.getStorageUsage())
                .bandwidthUsage(d.getBandwidthUsage())
                .temperature(d.getTemperature())
                .latencyMs(d.getLatencyMs())
                .packetLoss(d.getPacketLoss())
                .uptimeSeconds(d.getUptimeSeconds())
                .availabilityPercent(d.getAvailabilityPercent())
                .riskScore(d.getRiskScore())
                .securityScore(d.getSecurityScore())
                .description(d.getDescription())
                .tags(d.getTags())
                .ipv6Address(d.getIpv6Address())
                .isDhcp(d.getIsDhcp())
                .lastSeen(d.getLastSeen())
                .firstDiscovered(d.getFirstDiscovered())
                .createdAt(d.getCreatedAt())
                .organizationName(d.getOrganization() != null ? d.getOrganization().getName() : null)
                .build();
    }
}
