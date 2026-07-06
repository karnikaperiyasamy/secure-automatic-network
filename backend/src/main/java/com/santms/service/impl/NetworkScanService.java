package com.santms.service.impl;

import com.santms.entity.*;
import com.santms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Network discovery & scanning service
 */
@Service @RequiredArgsConstructor @Slf4j
public class NetworkScanService {

    private final NetworkScanRepository scanRepository;
    private final OrganizationRepository orgRepository;
    private final NetworkScanAsyncRunner asyncRunner;

    @Transactional
    public NetworkScan startScan(String targetNetwork, Long orgId, String initiatedBy) {
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        NetworkScan scan = NetworkScan.builder()
                .targetNetwork(targetNetwork)
                .scanName("Scan-" + System.currentTimeMillis())
                .status(NetworkScan.ScanStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .initiatedBy(initiatedBy)
                .progressPercent(0)
                .organization(org)
                .build();

        scan = scanRepository.save(scan);

        // Runs on a background thread via the Spring proxy so this call
        // returns immediately instead of blocking the HTTP request.
        asyncRunner.run(scan.getId(), orgId);

        return scan;
    }

    public NetworkScan getScanStatus(Long id) {
        return scanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Scan not found: " + id));
    }

    public List<NetworkScan> getScans(Long orgId) {
        return scanRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }
}
