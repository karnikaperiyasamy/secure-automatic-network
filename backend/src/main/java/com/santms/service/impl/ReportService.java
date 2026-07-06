package com.santms.service.impl;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import com.santms.entity.*;
import com.santms.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates the report files for the Reports page. This was previously
 * entirely missing on the backend (the Report entity/repository existed
 * but nothing ever populated them) even though the CSV/Excel/PDF
 * libraries were already declared as dependencies — this wires them up.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final DeviceRepository deviceRepository;
    private final AlertRepository alertRepository;
    private final AuditLogRepository auditLogRepository;
    private final ReportRepository reportRepository;
    private final OrganizationRepository orgRepository;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public record GeneratedFile(byte[] bytes, String filename, String contentType) {}

    public List<Report> getReports(Long orgId) {
        return reportRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    @Transactional
    public GeneratedFile generate(Long orgId, Report.ReportType type, Report.ReportPeriod period,
                                   Report.ReportFormat format, String generatedBy) {
        Organization org = orgRepository.findById(orgId).orElse(null);

        String[] headers;
        List<String[]> rows;

        switch (type) {
            case SECURITY -> {
                headers = new String[]{"Hostname", "IP", "Security Score", "Risk Score", "Authorized", "Status"};
                rows = new ArrayList<>();
                for (Device d : deviceRepository.findByOrganizationId(orgId)) {
                    rows.add(new String[]{
                            d.getHostname(), d.getIpAddress(),
                            String.valueOf(d.getSecurityScore()), String.valueOf(d.getRiskScore()),
                            Boolean.TRUE.equals(d.getIsAuthorized()) ? "Yes" : "No",
                            d.getStatus() != null ? d.getStatus().name() : "UNKNOWN"
                    });
                }
            }
            case BANDWIDTH -> {
                headers = new String[]{"Hostname", "IP", "Bandwidth Usage %", "Latency (ms)", "Status"};
                rows = new ArrayList<>();
                for (Device d : deviceRepository.findByOrganizationId(orgId)) {
                    rows.add(new String[]{
                            d.getHostname(), d.getIpAddress(),
                            String.format("%.1f", d.getBandwidthUsage() != null ? d.getBandwidthUsage() : 0),
                            String.format("%.1f", d.getLatencyMs() != null ? d.getLatencyMs() : 0),
                            d.getStatus() != null ? d.getStatus().name() : "UNKNOWN"
                    });
                }
            }
            case AUDIT -> {
                headers = new String[]{"Timestamp", "Performed By", "Action", "Entity", "Description"};
                rows = new ArrayList<>();
                auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, PageRequest.of(0, 500))
                        .forEach(a -> rows.add(new String[]{
                                a.getCreatedAt() != null ? a.getCreatedAt().format(TS) : "",
                                a.getPerformedBy(), a.getAction(),
                                (a.getEntityType() != null ? a.getEntityType() : "") + " " + (a.getEntityName() != null ? a.getEntityName() : ""),
                                a.getDescription() != null ? a.getDescription() : ""
                        }));
            }
            case DEVICE_STATUS -> {
                headers = new String[]{"Hostname", "IP", "Type", "Status", "Last Seen"};
                rows = new ArrayList<>();
                for (Device d : deviceRepository.findByOrganizationId(orgId)) {
                    rows.add(new String[]{
                            d.getHostname(), d.getIpAddress(),
                            d.getDeviceType() != null ? d.getDeviceType().name() : "UNKNOWN",
                            d.getStatus() != null ? d.getStatus().name() : "UNKNOWN",
                            d.getLastSeen() != null ? d.getLastSeen().format(TS) : "Never"
                    });
                }
            }
            case NETWORK_HEALTH -> {
                headers = new String[]{"Hostname", "IP", "CPU %", "RAM %", "Availability %", "Latency (ms)", "Status"};
                rows = new ArrayList<>();
                for (Device d : deviceRepository.findByOrganizationId(orgId)) {
                    rows.add(new String[]{
                            d.getHostname(), d.getIpAddress(),
                            String.format("%.1f", d.getCpuUsage() != null ? d.getCpuUsage() : 0),
                            String.format("%.1f", d.getRamUsage() != null ? d.getRamUsage() : 0),
                            String.format("%.1f", d.getAvailabilityPercent() != null ? d.getAvailabilityPercent() : 0),
                            String.format("%.1f", d.getLatencyMs() != null ? d.getLatencyMs() : 0),
                            d.getStatus() != null ? d.getStatus().name() : "UNKNOWN"
                    });
                }
            }
            default -> { // INVENTORY
                headers = new String[]{"Hostname", "IP", "MAC", "Type", "Status", "Vendor", "OS", "CPU %", "RAM %", "Last Seen"};
                rows = new ArrayList<>();
                for (Device d : deviceRepository.findByOrganizationId(orgId)) {
                    rows.add(new String[]{
                            d.getHostname(), d.getIpAddress(), d.getMacAddress(),
                            d.getDeviceType() != null ? d.getDeviceType().name() : "UNKNOWN",
                            d.getStatus() != null ? d.getStatus().name() : "UNKNOWN",
                            d.getVendor(), d.getOperatingSystem(),
                            String.format("%.1f", d.getCpuUsage() != null ? d.getCpuUsage() : 0),
                            String.format("%.1f", d.getRamUsage() != null ? d.getRamUsage() : 0),
                            d.getLastSeen() != null ? d.getLastSeen().format(TS) : "Never"
                    });
                }
            }
        }

        String title = friendlyTitle(type) + " Report";
        byte[] bytes;
        String ext;
        String contentType;

        switch (format) {
            case EXCEL -> {
                bytes = toExcel(title, headers, rows);
                ext = "xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            }
            case PDF -> {
                bytes = toPdf(title, headers, rows);
                ext = "pdf";
                contentType = "application/pdf";
            }
            default -> {
                bytes = toCsv(headers, rows);
                ext = "csv";
                contentType = "text/csv";
            }
        }

        Report report = Report.builder()
                .title(title)
                .type(type)
                .period(period)
                .format(format)
                .fileSize((long) bytes.length)
                .generatedBy(generatedBy)
                .reportData(rows.size() + " record(s)")
                .organization(org)
                .build();
        reportRepository.save(report);

        String filename = title.toLowerCase().replaceAll("[^a-z0-9]+", "-") + "-" + System.currentTimeMillis() + "." + ext;
        return new GeneratedFile(bytes, filename, contentType);
    }

    private String friendlyTitle(Report.ReportType type) {
        return switch (type) {
            case NETWORK_HEALTH -> "Network Health";
            case SECURITY -> "Security";
            case BANDWIDTH -> "Bandwidth";
            case INVENTORY -> "Device Inventory";
            case AUDIT -> "Audit";
            case DEVICE_STATUS -> "Device Status";
        };
    }

    private byte[] toCsv(String[] headers, List<String[]> rows) {
        try {
            StringWriter sw = new StringWriter();
            CSVWriter writer = new CSVWriter(sw);
            writer.writeNext(headers);
            for (String[] row : rows) writer.writeNext(row);
            writer.close();
            return sw.toString().getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build CSV report: " + e.getMessage(), e);
        }
    }

    private byte[] toExcel(String title, String[] headers, List<String[]> rows) {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(title.length() > 30 ? title.substring(0, 30) : title);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int r = 1;
            for (String[] row : rows) {
                Row xr = sheet.createRow(r++);
                for (int c = 0; c < row.length; c++) {
                    xr.createCell(c).setCellValue(row[c] != null ? row[c] : "");
                }
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Excel report: " + e.getMessage(), e);
        }
    }

    private byte[] toPdf(String title, String[] headers, List<String[]> rows) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 24, 24, 36, 36);
            PdfWriter.getInstance(doc, bos);
            doc.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9, com.itextpdf.text.Font.ITALIC);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            doc.add(new Paragraph(title, titleFont));
            doc.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().format(TS) + " · SANTMS", metaFont));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new com.itextpdf.text.Phrase(h, headerFont));
                cell.setBackgroundColor(new com.itextpdf.text.BaseColor(230, 230, 245));
                table.addCell(cell);
            }
            for (String[] row : rows) {
                for (String value : row) {
                    table.addCell(new PdfPCell(new com.itextpdf.text.Phrase(value != null ? value : "", cellFont)));
                }
            }
            if (rows.isEmpty()) {
                PdfPCell empty = new PdfPCell(new com.itextpdf.text.Phrase("No data available", cellFont));
                empty.setColspan(headers.length);
                table.addCell(empty);
            }
            doc.add(table);
            doc.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build PDF report: " + e.getMessage(), e);
        }
    }
}
