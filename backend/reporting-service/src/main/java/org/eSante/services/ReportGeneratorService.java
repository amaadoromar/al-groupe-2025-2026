package org.eSante.services;

import org.eSante.domain.models.Report;
import org.eSante.domain.models.dto.ReportData;
import org.eSante.repositories.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.io.IOException;

@Service
public class ReportGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(ReportGeneratorService.class);
    private static final DateTimeFormatter REPORT_FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private DataAggregationService dataAggregationService;

    @Autowired
    private PDFExportService pdfExportService;

    @Value("${reporting.storage.path:./reports}")
    private String storagePath;

    @Transactional
    public Report generateWeeklyReport(Long patientId) {
        logger.info("Generating weekly report for patient {}", patientId);

        Instant stop = Instant.now();
        Instant start = stop.minus(7, ChronoUnit.DAYS);

        Report report = new Report(patientId, "WEEKLY");
        report.setPeriodStart(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
        report.setPeriodEnd(LocalDateTime.ofInstant(stop, ZoneId.systemDefault()));

        try {
            // Step 1: Aggregate data
            ReportData data = dataAggregationService.aggregateWeeklyData(patientId, start, stop);

            // Step 2: Generate PDF content (in memory)
            byte[] pdfBytes = pdfExportService.generateWeeklyPDF(data);

            // Step 3: Encode to Base64
            report.setExportFormat("PDF_BASE64");
            report.setContent(Base64.getEncoder().encodeToString(pdfBytes));
            report.setFilePath(storeReportFile(pdfBytes, report, "weekly"));
            report.setStatus("READY");

            // Step 4: Add readable summary (for API responses only)
            report.setSummary(buildWeeklyTextContent(data));

            logger.info(" Weekly report successfully generated for patient {}", patientId);
        } catch (Exception e) {
            logger.error(" Error generating weekly report for patient {}", patientId, e);
            report.setStatus("ERROR");
            report.setSummary("Error: " + e.getMessage());
            report.setContent(null);
            report.setFilePath(null);
        }

        return reportRepository.save(report);
    }

    @Transactional
    public Report generateMonthlyReport(Long patientId) {
        logger.info("Generating monthly report for patient {}", patientId);

        Instant stop = Instant.now();
        Instant start = stop.minus(30, ChronoUnit.DAYS);

        Report report = new Report(patientId, "MONTHLY");
        report.setPeriodStart(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
        report.setPeriodEnd(LocalDateTime.ofInstant(stop, ZoneId.systemDefault()));

        try {
            ReportData data = dataAggregationService.aggregateMonthlyData(patientId, start, stop);
            byte[] pdfBytes = pdfExportService.generateMonthlyPDF(data);

            report.setExportFormat("PDF_BASE64");
            report.setContent(Base64.getEncoder().encodeToString(pdfBytes));
            report.setFilePath(storeReportFile(pdfBytes, report, "monthly"));
            report.setStatus("READY");
            report.setSummary(buildMonthlyTextContent(data));

            logger.info(" Monthly report generated successfully for patient {}", patientId);
        } catch (Exception e) {
            logger.error(" Error generating monthly report for patient {}", patientId, e);
            report.setStatus("ERROR");
            report.setSummary("Error: " + e.getMessage());
            report.setContent(null);
            report.setFilePath(null);
        }

        return reportRepository.save(report);
    }

    @Transactional
    public Report generateCustomReport(Long patientId, long minutes) {
        logger.info("Generating custom report for patient {} over last {} minutes", patientId, minutes);

        Instant stop = Instant.now();
        Instant start = stop.minus(minutes, ChronoUnit.MINUTES);

        Report report = new Report(patientId, "CUSTOM");
        report.setPeriodStart(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
        report.setPeriodEnd(LocalDateTime.ofInstant(stop, ZoneId.systemDefault()));

        try {
            ReportData data = dataAggregationService.aggregateRangeData(patientId, start, stop);
            byte[] pdfBytes = pdfExportService.generateCustomPDF(data);

            report.setExportFormat("PDF_BASE64");
            report.setContent(Base64.getEncoder().encodeToString(pdfBytes));
            report.setFilePath(storeReportFile(pdfBytes, report, "custom"));
            report.setStatus("READY");
            report.setSummary("Custom report for last " + minutes + " minutes generated successfully");
        } catch (Exception e) {
            logger.error(" Error generating custom report for patient {}", patientId, e);
            report.setStatus("ERROR");
            report.setSummary("Error: " + e.getMessage());
            report.setContent(null);
            report.setFilePath(null);
        }

        return reportRepository.save(report);
    }

    @Transactional
    public Report generatePostEventReport(Long patientId, Long alertId) {
        logger.info("Generating post-event report for patient {} (alert {})", patientId, alertId);

        Report report = new Report(patientId, "POST_EVENT");
        try {
            Instant alertTime = dataAggregationService.getAlertTimestamp(alertId);
            if (alertTime == null) {
                throw new IllegalArgumentException("Alert not found with ID: " + alertId);
            }

            Instant start = alertTime.minus(72, ChronoUnit.HOURS);
            Instant stop = alertTime.plus(72, ChronoUnit.HOURS);
            report.setPeriodStart(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
            report.setPeriodEnd(LocalDateTime.ofInstant(stop, ZoneId.systemDefault()));

            ReportData data = dataAggregationService.aggregatePostEventData(patientId, start, stop);
            byte[] pdfBytes = pdfExportService.generatePostEventPDF(data);

            report.setExportFormat("PDF_BASE64");
            report.setContent(Base64.getEncoder().encodeToString(pdfBytes));
            report.setFilePath(storeReportFile(pdfBytes, report, "post-event"));
            report.setStatus("READY");
            report.setSummary(buildPostEventTextContent(data, alertId));

            logger.info(" Post-event report generated successfully for alert {}", alertId);
        } catch (Exception e) {
            logger.error(" Error generating post-event report for alert {}", alertId, e);
            report.setStatus("ERROR");
            report.setSummary("Error: " + e.getMessage());
            report.setContent(null);
            report.setFilePath(null);
        }

        return reportRepository.save(report);
    }

    private String storeReportFile(byte[] pdfBytes, Report report, String type) throws IOException {
        String basePath = (storagePath == null || storagePath.isBlank()) ? "./reports" : storagePath;
        Path destinationDir = Paths.get(basePath).toAbsolutePath().normalize();
        Files.createDirectories(destinationDir);

        LocalDateTime timestamp = report.getReportDate() != null ? report.getReportDate() : LocalDateTime.now();
        String sanitizedType = type == null ? "report" : type.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        String filename = String.format(
                "report_patient-%d_%s_%s.pdf",
                report.getPatientId(),
                sanitizedType,
                REPORT_FILENAME_FORMATTER.format(timestamp)
        );

        Path targetFile = destinationDir.resolve(filename);
        Files.write(targetFile, pdfBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        return targetFile.toString();
    }

    // ============================================================
    // TEXT CONTENT BUILDERS
    // ============================================================
    private String buildWeeklyTextContent(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== WEEKLY REPORT ===\n");
        sb.append("Patient: ").append(formatPatientLabel(data)).append("\n");
        sb.append("Period: ").append(data.getPeriodStart()).append(" - ").append(data.getPeriodEnd()).append("\n\n");

        if (data.getHeartRateStats() != null)
            sb.append("Heart Rate: ").append(String.format("%.1f bpm", data.getHeartRateStats().getAverage())).append("\n");
        if (data.getSpo2Stats() != null)
            sb.append("SpO2: ").append(String.format("%.1f%%", data.getSpo2Stats().getAverage())).append("\n");
        if (data.getBloodPressureStats() != null)
            sb.append("Blood Pressure: ").append(String.format("%.0f/%.0f mmHg",
                    data.getBloodPressureStats().getAverageSystolic(),
                    data.getBloodPressureStats().getAverageDiastolic())).append("\n");
        if (data.getGlucoseStats() != null)
            sb.append("Glucose: ").append(String.format("%.1f mg/dL", data.getGlucoseStats().getAverage())).append("\n");

        sb.append("Alerts: ").append(data.getAlertCount())
                .append(" (Emergencies: ").append(data.getEmergencyCount()).append(")\n");
        sb.append("Adherence: ").append(String.format("%.1f%%", data.getOverallAdherenceRate())).append("\n");
        return sb.toString();
    }

    private String buildMonthlyTextContent(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MONTHLY CLINICAL REPORT ===\n");
        sb.append("Patient: ").append(formatPatientLabel(data)).append("\n");
        sb.append("Period: ").append(data.getPeriodStart()).append(" - ").append(data.getPeriodEnd()).append("\n\n");

        sb.append("DETAILED STATISTICS:\n-------------------\n");
        if (data.getHeartRateStats() != null)
            sb.append("• Heart Rate: Avg ").append(String.format("%.1f bpm", data.getHeartRateStats().getAverage())).append("\n");
        if (data.getSpo2Stats() != null)
            sb.append("• SpO2: Avg ").append(String.format("%.1f%%", data.getSpo2Stats().getAverage())).append("\n");
        if (data.getBloodPressureStats() != null)
            sb.append("• Blood Pressure: Avg ")
                    .append(String.format("%.0f/%.0f mmHg",
                            data.getBloodPressureStats().getAverageSystolic(),
                            data.getBloodPressureStats().getAverageDiastolic()))
                    .append("\n");
        if (data.getGlucoseStats() != null)
            sb.append("• Glucose: Avg ").append(String.format("%.1f mg/dL", data.getGlucoseStats().getAverage())).append("\n");

        sb.append("\nAlerts: ").append(data.getAlertCount())
                .append(" | Emergencies: ").append(data.getEmergencyCount()).append("\n");
        sb.append("Global Adherence: ").append(String.format("%.1f%%", data.getOverallAdherenceRate())).append("\n");
        return sb.toString();
    }

    private String buildPostEventTextContent(ReportData data, Long alertId) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== POST-EVENT REPORT ===\n");
        sb.append("Patient: ").append(formatPatientLabel(data)).append("\n");
        sb.append("Alert ID: ").append(alertId).append("\n");
        sb.append("Period: ").append(data.getPeriodStart()).append(" - ").append(data.getPeriodEnd()).append("\n\n");

        if (data.getHeartRateStats() != null)
            sb.append("Heart Rate: Avg ").append(String.format("%.1f bpm", data.getHeartRateStats().getAverage())).append("\n");
        if (data.getSpo2Stats() != null)
            sb.append("SpO2: Avg ").append(String.format("%.1f%%", data.getSpo2Stats().getAverage())).append("\n");
        if (data.getBloodPressureStats() != null)
            sb.append("Blood Pressure: ").append(String.format("%.0f/%.0f mmHg",
                    data.getBloodPressureStats().getAverageSystolic(),
                    data.getBloodPressureStats().getAverageDiastolic())).append("\n");
        if (data.getGlucoseStats() != null)
            sb.append("Glucose: Avg ").append(String.format("%.1f mg/dL", data.getGlucoseStats().getAverage())).append("\n");

        sb.append("\nAlerts during event: ").append(data.getAlertCount())
                .append(" (Emergencies: ").append(data.getEmergencyCount()).append(")\n");
        return sb.toString();
    }

    private String formatPatientLabel(ReportData data) {
        if (data.getPatientName() != null && !data.getPatientName().isBlank()) {
            return data.getPatientName();
        }
        return "Patient #" + data.getPatientId();
    }
}
