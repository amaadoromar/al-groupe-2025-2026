package org.eSante.api;

import org.eSante.domain.models.Report;
import org.eSante.repositories.ReportRepository;
import org.eSante.services.ReportGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.List;

/**
 * REST controller for managing medical reports (weekly, monthly, post-event).
 * Supports generating and exporting reports in-memory as Base64-encoded PDFs.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportGeneratorService reportGeneratorService;

    @Autowired
    private ReportRepository reportRepository;

    /**
     *  Get all reports for a given patient, optionally filtered by type.
     * Example:
     *   GET /api/reports/patient/1
     *   GET /api/reports/patient/1?type=weekly
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Report>> getPatientReports(
            @PathVariable("patientId") Long patientId,
            @RequestParam(value = "type", required = false) String type) {

        List<Report> reports;
        if (type != null) {
            reports = reportRepository.findByPatientIdAndReportType(
                    patientId,
                    type.toUpperCase()
            );
        } else {
            reports = reportRepository.findByPatientIdOrderByReportDateDesc(patientId);
        }

        return ResponseEntity.ok(reports);
    }

    /**
     *  Generate a new report (weekly or monthly)
     * Example:
     *   POST /api/reports/generate?patientId=1&type=WEEKLY
     */
    @PostMapping("/generate")
    public ResponseEntity<Report> generateReport(
            @RequestParam("patientId") Long patientId,
            @RequestParam("type") String type) {

        Report report;
        switch (type.toUpperCase()) {
            case "WEEKLY":
                report = reportGeneratorService.generateWeeklyReport(patientId);
                break;
            case "MONTHLY":
                report = reportGeneratorService.generateMonthlyReport(patientId);
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(report);
    }

    /**
     *  Export a generated report as PDF (stored in Base64 format)
     * Example:
     *   GET /api/reports/1/export
     */
    @GetMapping("/{reportId}/export")
    public ResponseEntity<byte[]> exportReport(@PathVariable("reportId") Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Rapport non trouvé"));

        // Vérifie que le contenu est bien au format PDF Base64
        if (report.getContent() == null || report.getContent().isEmpty()) {
            throw new RuntimeException("Aucun contenu PDF disponible pour ce rapport.");
        }

        byte[] pdfBytes;
        try {
            pdfBytes = Base64.getDecoder().decode(report.getContent());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Erreur lors du décodage du PDF Base64 : " + e.getMessage());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=report_" + reportId + ".pdf")
                .body(pdfBytes);
    }

    /**
     *  (Optionnel) Export raw Base64 string (for frontend direct rendering)
     * Example:
     *   GET /api/reports/1/base64
     */
    @GetMapping("/{reportId}/base64")
    public ResponseEntity<String> getReportAsBase64(@PathVariable("reportId") Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Rapport non trouvé"));

        if (report.getContent() == null || report.getContent().isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(report.getContent());
    }
}
