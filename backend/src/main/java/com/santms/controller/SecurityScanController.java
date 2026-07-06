package com.santms.controller;

import com.santms.entity.SecurityScan;
import com.santms.service.impl.SecurityScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// ===== SECURITY SCAN CONTROLLER =====
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityScanController {

    private final SecurityScanService securityScanService;

    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> runScan(@RequestParam(defaultValue = "1") Long orgId,
                                                         @AuthenticationPrincipal UserDetails user) {
        String initiatedBy = user != null ? user.getUsername() : "system";
        return ResponseEntity.ok(securityScanService.runScan(orgId, initiatedBy));
    }

    @GetMapping("/scans")
    public ResponseEntity<List<SecurityScan>> getScans(@RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.ok(securityScanService.getRecentScans(orgId));
    }
}
