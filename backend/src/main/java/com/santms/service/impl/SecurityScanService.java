package com.santms.service.impl;

import com.santms.entity.*;
import com.santms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Runs a security scan across every device in an organization.
 *
 * Previously "Run Security Scan" on the Security page was pure UI theatre —
 * a setTimeout() and a toast, with no backend call at all. This service
 * actually inspects each device, records a SecurityScan row, and updates the
 * device's security/risk scores so the rest of the Security page reflects
 * real (if simulated) results.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SecurityScanService {

    private final DeviceRepository deviceRepository;
    private final SecurityScanRepository securityScanRepository;
    private final OrganizationRepository orgRepository;

    public Map<String, Object> runScan(Long orgId, String initiatedBy) {
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        List<Device> devices = deviceRepository.findByOrganizationId(orgId);
        Random rnd = new Random();

        int scanned = 0, vulnerabilitiesFound = 0, weakPasswords = 0, unauthorizedFound = 0;

        for (Device d : devices) {
            List<String> vulns = new ArrayList<>();
            boolean weakPw = rnd.nextInt(100) < 10;
            boolean unauthorized = Boolean.FALSE.equals(d.getIsAuthorized());
            boolean suspiciousTraffic = (d.getBandwidthUsage() != null && d.getBandwidthUsage() > 75) && rnd.nextInt(100) < 20;
            boolean arpSpoofing = rnd.nextInt(100) < 3;

            if (weakPw) vulns.add("Weak or default credentials detected");
            if (unauthorized) vulns.add("Device is not authorized on the network");
            if (d.getOpenPorts() != null && d.getOpenPorts().split(",").length > 5) vulns.add("Excessive number of open ports");
            if (suspiciousTraffic) vulns.add("Suspicious outbound traffic pattern");
            if (arpSpoofing) vulns.add("Possible ARP spoofing activity");

            int riskDelta = vulns.size() * 8 + (weakPw ? 10 : 0) + (unauthorized ? 15 : 0);
            int newRisk = Math.min(100, Math.max(0, (d.getRiskScore() != null ? d.getRiskScore() : 0) - 5 + riskDelta));
            int newSecScore = Math.max(0, Math.min(100, 100 - newRisk));

            d.setRiskScore(newRisk);
            d.setSecurityScore(newSecScore);
            deviceRepository.save(d);

            SecurityScan scan = SecurityScan.builder()
                    .device(d)
                    .scanType("FULL_SCAN")
                    .openPorts(d.getOpenPorts())
                    .vulnerabilities(String.join("; ", vulns))
                    .riskScore(newRisk)
                    .securityScore(newSecScore)
                    .weakPasswordsDetected(weakPw)
                    .unauthorizedAccessDetected(unauthorized)
                    .suspiciousTrafficDetected(suspiciousTraffic)
                    .arpSpoofingDetected(arpSpoofing)
                    .recommendations(vulns.isEmpty() ? "No action needed" : "Review and remediate the detected issues")
                    .scanSummary(vulns.isEmpty() ? "No issues found" : vulns.size() + " issue(s) found")
                    .initiatedBy(initiatedBy)
                    .organization(org)
                    .build();
            securityScanRepository.save(scan);

            scanned++;
            vulnerabilitiesFound += vulns.size();
            if (weakPw) weakPasswords++;
            if (unauthorized) unauthorizedFound++;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("devicesScanned", scanned);
        result.put("vulnerabilitiesFound", vulnerabilitiesFound);
        result.put("weakPasswords", weakPasswords);
        result.put("unauthorizedDevices", unauthorizedFound);
        return result;
    }

    public List<SecurityScan> getRecentScans(Long orgId) {
        return securityScanRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }
}
