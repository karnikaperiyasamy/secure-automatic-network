package com.santms.controller;

import com.santms.dto.request.DeviceRequest;
import com.santms.dto.response.DeviceResponse;
import com.santms.service.impl.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Device management REST API
 */
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ResponseEntity<Page<DeviceResponse>> getAllDevices(
            @RequestParam(defaultValue = "1") Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort) {
        return ResponseEntity.ok(deviceService.getAllDevices(orgId, page, size, sort));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<DeviceResponse>> search(
            @RequestParam(defaultValue = "1") Long orgId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(deviceService.searchDevices(orgId, q, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDevice(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getDevice(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','NETWORK_ADMIN')")
    public ResponseEntity<DeviceResponse> createDevice(
            @Valid @RequestBody DeviceRequest request,
            @RequestParam(defaultValue = "1") Long orgId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(deviceService.createDevice(request, orgId, userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','NETWORK_ADMIN')")
    public ResponseEntity<DeviceResponse> updateDevice(@PathVariable Long id,
                                                        @RequestBody DeviceRequest request) {
        return ResponseEntity.ok(deviceService.updateDevice(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','NETWORK_ADMIN')")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/maintenance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','NETWORK_ADMIN')")
    public ResponseEntity<DeviceResponse> toggleMaintenance(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.toggleMaintenance(id));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats(
            @RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.ok(deviceService.getDeviceStats(orgId));
    }
}
