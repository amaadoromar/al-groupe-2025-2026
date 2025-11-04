package org.eSante.api;

import org.eSante.domain.models.Report;
import org.eSante.repositories.ReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportGeneratorService reportGeneratorService;

    @Autowired
    private ReportRepository rapportRepository;

    // GET /api/reports/patient/{id}?type=weekly
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Report>> getPatientReports(
            @PathVariable Long patientId,
            @RequestParam(required = false) String type) {

        List<Report> rapports;
        if (type != null) {
            rapports = rapportRepository.findByPatientIdAndReportType(
                    patientId,
                    type.toUpperCase()
            );
        } else {
            rapports = rapportRepository.findByPatientIdOrderByReportDateDesc(patientId);
        }

        return ResponseEntity.ok(rapports);
    }

    // POST /api/reports/generate
    @PostMapping("/generate")
    public ResponseEntity<Report> generateReport(
            @RequestParam Long patientId,
            @RequestParam String type) {

        Report rapport;
        switch (type.toUpperCase()) {
            case "WEEKLY":
                rapport = reportGeneratorService.generateWeeklyReport(patientId);
                break;
            case "MONTHLY":
                rapport = reportGeneratorService.generateMonthlyReport(patientId);
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(rapport);
    }

    // GET /api/reports/{id}/export?format=pdf
    @GetMapping("/{reportId}/export")
    public ResponseEntity<Resource> exportReport(
            @PathVariable Long reportId,
            @RequestParam(defaultValue = "pdf") String format) {

        Report rapport = rapportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Rapport non trouvé"));

        String[] paths = rapport.getFilePath().split(";");
        String filePath = format.equalsIgnoreCase("csv") && paths.length > 1 ? paths[1] : paths[0];

        Resource resource = new FileSystemResource(filePath);

        MediaType mediaType = format.equalsIgnoreCase("csv")
                ? MediaType.parseMediaType("text/csv")
                : MediaType.APPLICATION_PDF;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
