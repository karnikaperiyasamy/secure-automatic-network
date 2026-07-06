package com.santms.controller;

import com.santms.dto.response.*;
import com.santms.service.impl.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// ===== DASHBOARD CONTROLLER =====
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.ok(dashboardService.getDashboard(orgId));
    }
}

// ===== TOPOLOGY CONTROLLER =====
@RestController
@RequestMapping("/api/topology")
@RequiredArgsConstructor
class TopologyController {
    private final TopologyService topologyService;

    @GetMapping
    public ResponseEntity<TopologyResponse> getTopology(
            @RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.ok(topologyService.getTopology(orgId));
    }

    @PostMapping("/regenerate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','NETWORK_ADMIN')")
    public ResponseEntity<TopologyResponse> regenerate(
            @RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.ok(topologyService.regenerateTopology(orgId));
    }

    @PatchMapping("/nodes/{nodeId}/position")
    public ResponseEntity<Void> updateNodePosition(@PathVariable Long nodeId,
            @RequestParam double x, @RequestParam double y) {
        topologyService.updateNodePosition(nodeId, x, y);
        return ResponseEntity.ok().build();
    }
}

// ===== ALERT CONTROLLER =====
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
class AlertController {
    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<Page<AlertResponse>> getAlerts(
            @RequestParam(defaultValue = "1") Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(alertService.getAlerts(orgId, page, size));
    }

    @GetMapping("/open")
    public ResponseEntity<List<AlertResponse>> getOpenAlerts(
            @RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.ok(alertService.getOpenAlerts(orgId));
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<AlertResponse> acknowledge(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(alertService.acknowledgeAlert(id, user.getUsername()));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<AlertResponse> resolve(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(defaultValue = "") String note) {
        return ResponseEntity.ok(alertService.resolveAlert(id, user.getUsername(), note));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCount(
            @RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.ok(Map.of(
                "open", alertService.countOpenAlerts(orgId),
                "critical", alertService.countCriticalAlerts(orgId)));
    }
}

// ===== NETWORK SCAN CONTROLLER =====
@RestController
@RequestMapping("/api/network/scans")
@RequiredArgsConstructor
class NetworkScanController {
    private final NetworkScanService scanService;

    @PostMapping("/start")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','NETWORK_ADMIN')")
    public ResponseEntity<com.santms.entity.NetworkScan> startScan(
            @RequestParam(defaultValue = "192.168.1.0/24") String network,
            @RequestParam(defaultValue = "1") Long orgId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(scanService.startScan(network, orgId, user.getUsername()));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<com.santms.entity.NetworkScan> getStatus(@PathVariable Long id) {
        return ResponseEntity.ok(scanService.getScanStatus(id));
    }

    @GetMapping
    public ResponseEntity<List<com.santms.entity.NetworkScan>> getScans(
            @RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.ok(scanService.getScans(orgId));
    }
}
