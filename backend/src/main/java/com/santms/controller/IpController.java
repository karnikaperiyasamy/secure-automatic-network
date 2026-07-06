package com.santms.controller;

import com.santms.entity.IPAddressPool;
import com.santms.service.impl.IpPoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// ===== IP ADDRESS POOL CONTROLLER =====
@RestController
@RequestMapping("/api/ip")
@RequiredArgsConstructor
public class IpController {

    private final IpPoolService ipPoolService;

    @GetMapping
    public ResponseEntity<List<IPAddressPool>> list(@RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.ok(ipPoolService.getPool(orgId));
    }

    @PostMapping
    public ResponseEntity<IPAddressPool> add(@RequestParam(defaultValue = "1") Long orgId,
                                              @RequestBody Map<String, Object> body) {
        String ipAddress = (String) body.get("ipAddress");
        String subnet = (String) body.get("subnet");
        String gateway = (String) body.get("gateway");
        String description = (String) body.get("description");
        Boolean reserved = Boolean.TRUE.equals(body.get("reserved"));
        Integer vlanId = body.get("vlanId") != null ? Integer.valueOf(body.get("vlanId").toString()) : null;
        IPAddressPool.IPType type = body.get("type") != null
                ? IPAddressPool.IPType.valueOf(body.get("type").toString())
                : IPAddressPool.IPType.STATIC;

        IPAddressPool created = ipPoolService.addIp(orgId, ipAddress, subnet, gateway, type, reserved, vlanId, description);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/reserve")
    public ResponseEntity<IPAddressPool> reserve(@PathVariable Long id) {
        return ResponseEntity.ok(ipPoolService.reserveIp(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ipPoolService.deleteIp(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/conflicts")
    public ResponseEntity<Map<String, Object>> conflicts(@RequestParam(defaultValue = "1") Long orgId) {
        List<String> conflicts = ipPoolService.detectConflicts(orgId);
        return ResponseEntity.ok(Map.of("conflicts", conflicts, "count", conflicts.size()));
    }
}
