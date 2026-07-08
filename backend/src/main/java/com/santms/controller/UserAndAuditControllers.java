package com.santms.controller;

import com.santms.dto.request.AuthDTOs.RegisterRequest;
import com.santms.dto.response.AuthResponseDTOs.UserInfo;
//import com.santms.entity.AuditLog;
import com.santms.entity.User;
import com.santms.repository.AuditLogRepository;
import com.santms.service.impl.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ===== USER CONTROLLER =====
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
class UserController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','NETWORK_ADMIN')")
    public ResponseEntity<List<UserInfo>> getAll(@RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.ok(userService.getAllUsers(orgId));
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfo> me(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getCurrentUser(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','NETWORK_ADMIN')")
    public ResponseEntity<UserInfo> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserInfo> create(@Valid @RequestBody RegisterRequest req,
                                            @RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(req, orgId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<UserInfo> updateStatus(@PathVariable Long id,
                                                  @RequestParam String status) {
        return ResponseEntity.ok(userService.updateStatus(id, User.UserStatus.valueOf(status)));
    }
}

// ===== AUDIT LOG CONTROLLER =====
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
class AuditLogController {
    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','NETWORK_ADMIN')")
    public ResponseEntity<?> getLogs(
            @RequestParam(defaultValue = "1") Long orgId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(
                orgId, PageRequest.of(page, size)));
    }
}
