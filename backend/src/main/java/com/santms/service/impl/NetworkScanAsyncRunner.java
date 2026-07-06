package com.santms.service.impl;

import com.santms.entity.*;
import com.santms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Runs the (simulated) network scan in a background thread.
 *
 * NOTE: this used to be a method on NetworkScanService itself, annotated
 * with @Async. Because NetworkScanService called that method on "this"
 * (self-invocation), Spring's AOP proxy was bypassed and the method
 * actually ran synchronously on the request thread, blocking every
 * "Start Scan" call for several seconds and making the UI look frozen.
 * Moving it into its own bean makes the @Async proxy apply correctly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NetworkScanAsyncRunner {

    private final NetworkScanRepository scanRepository;
    private final DeviceRepository deviceRepository;
    private final OrganizationRepository orgRepository;

    @Async("taskExecutor")
    @Transactional
    public void run(Long scanId, Long orgId) {
        try {
            Thread.sleep(1200);

            NetworkScan scan = scanRepository.findById(scanId).orElseThrow();
            Organization org = orgRepository.findById(orgId).orElseThrow();

            scan.setProgressPercent(30);
            scanRepository.save(scan);
            Thread.sleep(1200);

            List<Device> discovered = generateSimulatedDevices(orgId, org);
            int created = 0;

            scan.setProgressPercent(70);
            scanRepository.save(scan);
            Thread.sleep(800);

            for (Device d : discovered) {
                if (!deviceRepository.existsByIpAddressAndOrganizationId(d.getIpAddress(), orgId)) {
                    deviceRepository.save(d);
                    created++;
                }
            }

            scan.setProgressPercent(100);
            scan.setStatus(NetworkScan.ScanStatus.COMPLETED);
            scan.setDevicesFound(created);
            scan.setCompletedAt(LocalDateTime.now());
            scan.setDurationSeconds(
                    java.time.Duration.between(scan.getStartedAt(), scan.getCompletedAt()).getSeconds());
            scan.setScanResult("Scan completed. " + created + " new devices discovered.");
            scanRepository.save(scan);

            log.info("Scan {} completed: {} devices discovered", scanId, created);

        } catch (Exception e) {
            log.error("Scan {} failed: {}", scanId, e.getMessage());
            scanRepository.findById(scanId).ifPresent(s -> {
                s.setStatus(NetworkScan.ScanStatus.FAILED);
                s.setErrorMessage(e.getMessage());
                scanRepository.save(s);
            });
        }
    }

    private List<Device> generateSimulatedDevices(Long orgId, Organization org) {
        List<Device> devices = new ArrayList<>();
        Random r = new Random(orgId);

        String[][] deviceTemplates = {
            {"192.168.1.1", "core-router-01", "Cisco", "IOS 15.4", "ROUTER", "ONLINE"},
            {"192.168.1.2", "fw-01", "Palo Alto", "PAN-OS 10.1", "FIREWALL", "ONLINE"},
            {"192.168.1.10", "sw-access-01", "Cisco", "IOS 12.2", "SWITCH", "ONLINE"},
            {"192.168.1.11", "sw-access-02", "HP", "ProCurve", "SWITCH", "ONLINE"},
            {"192.168.1.20", "srv-web-01", "Dell", "Ubuntu 22.04", "SERVER", "ONLINE"},
            {"192.168.1.21", "srv-db-01", "Dell", "Windows Server 2022", "SERVER", "ONLINE"},
            {"192.168.1.30", "ap-floor1", "Ubiquiti", "UniFi OS", "ACCESS_POINT", "ONLINE"},
            {"192.168.1.50", "pc-admin-01", "HP", "Windows 11", "COMPUTER", "ONLINE"},
            {"192.168.1.51", "pc-dev-01", "Lenovo", "Ubuntu 22.04", "COMPUTER", "ONLINE"},
            {"192.168.1.52", "pc-finance-01", "Dell", "Windows 10", "COMPUTER", "OFFLINE"},
            {"192.168.1.60", "printer-hq", "HP", "JetDirect", "PRINTER", "ONLINE"},
            {"192.168.1.70", "iot-hvac-01", "Honeywell", "Linux", "IOT_DEVICE", "WARNING"},
            {"192.168.1.80", "vm-dev-01", "VMware", "Ubuntu 20.04", "VIRTUAL_MACHINE", "ONLINE"},
            {"192.168.1.90", "nas-backup", "Synology", "DSM 7.0", "SERVER", "ONLINE"},
        };

        for (String[] t : deviceTemplates) {
            Device.DeviceStatus status = Device.DeviceStatus.valueOf(t[5]);
            Device d = Device.builder()
                    .ipAddress(t[0]).hostname(t[1]).vendor(t[2]).operatingSystem(t[3])
                    .deviceType(Device.DeviceType.valueOf(t[4])).status(status)
                    .macAddress(generateMac(r))
                    .subnet("192.168.1.0/24").gateway("192.168.1.1")
                    .dnsServer("8.8.8.8").networkName("Corporate-LAN")
                    .cpuUsage(10 + r.nextDouble() * 70)
                    .ramUsage(20 + r.nextDouble() * 60)
                    .storageUsage(30 + r.nextDouble() * 50)
                    .bandwidthUsage(r.nextDouble() * 80)
                    .temperature(35 + r.nextDouble() * 30)
                    .latencyMs(1 + r.nextDouble() * 20)
                    .availabilityPercent(status == Device.DeviceStatus.ONLINE ? 95 + r.nextDouble() * 5 : 60 + r.nextDouble() * 30)
                    .securityScore(70 + r.nextInt(30))
                    .riskScore(r.nextInt(40))
                    .isApproved(true).isAuthorized(true)
                    .firstDiscovered(LocalDateTime.now().minusDays(r.nextInt(30)))
                    .lastSeen(LocalDateTime.now())
                    .organization(org)
                    .build();
            devices.add(d);
        }
        return devices;
    }

    private String generateMac(Random r) {
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                r.nextInt(256), r.nextInt(256), r.nextInt(256),
                r.nextInt(256), r.nextInt(256), r.nextInt(256));
    }
}
