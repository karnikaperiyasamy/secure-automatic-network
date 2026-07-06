package com.santms.controller;

import com.santms.entity.Report;
import com.santms.service.impl.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ===== REPORT CONTROLLER =====
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<List<Report>> list(@RequestParam(defaultValue = "1") Long orgId) {
        return ResponseEntity.ok(reportService.getReports(orgId));
    }

    @PostMapping("/generate")
    public ResponseEntity<byte[]> generate(
            @RequestParam(defaultValue = "1") Long orgId,
            @RequestParam Report.ReportType type,
            @RequestParam(defaultValue = "MONTHLY") Report.ReportPeriod period,
            @RequestParam(defaultValue = "CSV") Report.ReportFormat format,
            @AuthenticationPrincipal UserDetails user) {

        String generatedBy = user != null ? user.getUsername() : "system";
        ReportService.GeneratedFile file = reportService.generate(orgId, type, period, format, generatedBy);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"");
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }
}
