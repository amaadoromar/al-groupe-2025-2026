package org.eSante.services;

import org.eSante.domain.models.Report;
import org.eSante.domain.models.dto.ReportData;
import org.eSante.repositories.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Service
public class ReportGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(ReportGeneratorService.class);

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private DataAggregationService dataAggregationService;

    @Autowired
    private PDFExportService pdfExportService;

    /**
     * Generate a weekly report for a patient
     * Period: last 7 days
     * Output: PDF (simple format for patient/family)
     */
    @Transactional
    public Report generateWeeklyReport(Long patientId) {
        logger.info("Generating weekly report for patient {}", patientId);

        Instant stop = Instant.now();
        Instant start = stop.minus(7, ChronoUnit.DAYS);

        Report report = new Report(patientId, "WEEKLY");
        report.setPeriodStart(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
        report.setPeriodEnd(LocalDateTime.ofInstant(stop, ZoneId.systemDefault()));

        try {
            // Step 1: Aggregate data from InfluxDB and PostgreSQL
            ReportData data = dataAggregationService.aggregateWeeklyData(patientId, start, stop);

            // Step 2: Generate PDF
            String pdfPath = pdfExportService.generateWeeklyPDF(data);
            report.setFilePath(pdfPath);
            report.setExportFormat("PDF");

            // Step 3: Build text content for database storage
            String content = buildWeeklyTextContent(data);
            report.setContent(content);
            report.setStatus("READY");

            logger.info("Weekly report successfully generated: {}", pdfPath);

        } catch (Exception e) {
            logger.error("Error generating weekly report for patient {}", patientId, e);
            report.setStatus("ERROR");
            report.setContent("Error: " + e.getMessage());
        }

        return reportRepository.save(report);
    }

    /**
     * Generate a monthly report for a patient
     * Period: last 30 days
     * Output: PDF + CSV (detailed format for clinicians)
     */
    @Transactional
    public Report generateMonthlyReport(Long patientId) {
        logger.info("Generating monthly report for patient {}", patientId);

        Instant stop = Instant.now();
        Instant start = stop.minus(30, ChronoUnit.DAYS);

        Report report = new Report(patientId, "MONTHLY");
        report.setPeriodStart(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
        report.setPeriodEnd(LocalDateTime.ofInstant(stop, ZoneId.systemDefault()));

        try {
            // Step 1: Aggregate data
            ReportData data = dataAggregationService.aggregateMonthlyData(patientId, start, stop);

            // Step 2: Generate PDF + CSV for clinicians
            String pdfPath = pdfExportService.generateMonthlyPDF(data);
            String csvPath = pdfExportService.generateMonthlyCSV(data);

            report.setFilePath(pdfPath + ";" + csvPath);
            report.setExportFormat("PDF,CSV");
            report.setContent(buildMonthlyTextContent(data));
            report.setStatus("READY");

            logger.info("Monthly report generated: PDF={}, CSV={}", pdfPath, csvPath);

        } catch (Exception e) {
            logger.error("Error generating monthly report for patient {}", patientId, e);
            report.setStatus("ERROR");
            report.setContent("Error: " + e.getMessage());
        }

        return reportRepository.save(report);
    }

    /**
     * Generate a post-event report (±72h around an alert)
     * Period: 72 hours before and after the alert timestamp
     * Output: PDF (detailed format for medical team)
     */
    @Transactional
    public Report generatePostEventReport(Long patientId, Long alertId) {
        logger.info("Generating post-event report for patient {} and alert {}", patientId, alertId);

        Report report = new Report(patientId, "POST_EVENT");

        try {
            // Step 1: Retrieve the alert timestamp from database
            Instant alertTime = dataAggregationService.getAlertTimestamp(alertId);

            if (alertTime == null) {
                throw new IllegalArgumentException("Alert not found with ID: " + alertId);
            }

            // Define window: ±72 hours around alert
            Instant start = alertTime.minus(72, ChronoUnit.HOURS);
            Instant stop = alertTime.plus(72, ChronoUnit.HOURS);

            report.setPeriodStart(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
            report.setPeriodEnd(LocalDateTime.ofInstant(stop, ZoneId.systemDefault()));

            // Step 2: Aggregate high-resolution data
            ReportData data = dataAggregationService.aggregatePostEventData(patientId, start, stop);

            // Step 3: Generate detailed PDF
            String pdfPath = pdfExportService.generateMonthlyPDF(data); // Using monthly template for detail
            report.setFilePath(pdfPath);
            report.setExportFormat("PDF");

            report.setContent(buildPostEventTextContent(data, alertId));
            report.setStatus("READY");

            logger.info("Post-event report successfully generated: {}", pdfPath);

        } catch (Exception e) {
            logger.error("Error generating post-event report for alert {}", alertId, e);
            report.setStatus("ERROR");
            report.setContent("Error: " + e.getMessage());
        }

        return reportRepository.save(report);
    }

    // ============================================================
    // TEXT CONTENT BUILDERS (for database storage)
    // ============================================================

    /**
     * Build simple text summary for weekly report
     */
    private String buildWeeklyTextContent(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== WEEKLY REPORT ===\n");
        sb.append("Period: ").append(data.getPeriodStart()).append(" - ").append(data.getPeriodEnd()).append("\n\n");

        // Heart Rate
        if (data.getHeartRateStats() != null && data.getHeartRateStats().getMeasurementCount() > 0) {
            sb.append("Heart Rate: ").append(String.format("%.1f", data.getHeartRateStats().getAverage()))
                    .append(" bpm (min=").append(String.format("%.0f", data.getHeartRateStats().getMin()))
                    .append(", max=").append(String.format("%.0f", data.getHeartRateStats().getMax())).append(")\n");
        }

        // SpO2
        if (data.getSpo2Stats() != null && data.getSpo2Stats().getMeasurementCount() > 0) {
            sb.append("Oxygen Saturation: ").append(String.format("%.1f%%", data.getSpo2Stats().getAverage()))
                    .append(" (min=").append(String.format("%.1f", data.getSpo2Stats().getMin()))
                    .append(", max=").append(String.format("%.1f", data.getSpo2Stats().getMax())).append(")\n");
        }

        // Blood Pressure
        if (data.getBloodPressureStats() != null && data.getBloodPressureStats().getMeasurementCount() > 0) {
            sb.append("Blood Pressure: ")
                    .append(String.format("%.0f/%.0f",
                            data.getBloodPressureStats().getAverageSystolic(),
                            data.getBloodPressureStats().getAverageDiastolic()))
                    .append(" mmHg\n");
        }

        // Glucose
        if (data.getGlucoseStats() != null && data.getGlucoseStats().getMeasurementCount() > 0) {
            sb.append("Blood Glucose: ").append(String.format("%.0f", data.getGlucoseStats().getAverage()))
                    .append(" mg/dL");
            if (data.getGlucoseStats().getTimeInRange() != null) {
                sb.append(" (Time in Range: ").append(String.format("%.1f%%", data.getGlucoseStats().getTimeInRange())).append(")");
            }
            sb.append("\n");
        }

        // Alerts
        sb.append("\nAlerts: ").append(data.getAlertCount());
        if (data.getEmergencyCount() > 0) {
            sb.append(" (including ").append(data.getEmergencyCount()).append(" emergencies)");
        }
        sb.append("\n");

        // Adherence
        sb.append("Adherence: ").append(String.format("%.1f%%", data.getOverallAdherenceRate())).append("\n");

        return sb.toString();
    }

    /**
     * Build detailed text summary for monthly report
     */
    private String buildMonthlyTextContent(ReportData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MONTHLY CLINICAL REPORT ===\n");
        sb.append("Period: ").append(data.getPeriodStart()).append(" - ").append(data.getPeriodEnd()).append("\n\n");

        sb.append("DETAILED STATISTICS:\n");
        sb.append("-------------------\n");

        // Heart Rate
        if (data.getHeartRateStats() != null && data.getHeartRateStats().getMeasurementCount() > 0) {
            sb.append("• Heart Rate:\n");
            sb.append("  - Average: ").append(String.format("%.1f", data.getHeartRateStats().getAverage())).append(" bpm\n");
            sb.append("  - Median: ").append(String.format("%.1f", data.getHeartRateStats().getMedian())).append(" bpm\n");
            sb.append("  - Range: ").append(String.format("%.0f", data.getHeartRateStats().getMin()))
                    .append(" - ").append(String.format("%.0f", data.getHeartRateStats().getMax())).append(" bpm\n");
            sb.append("  - P10/P90: ").append(String.format("%.1f", data.getHeartRateStats().getP10()))
                    .append(" / ").append(String.format("%.1f", data.getHeartRateStats().getP90())).append(" bpm\n");
            sb.append("  - Measurements: ").append(data.getHeartRateStats().getMeasurementCount()).append("\n\n");
        }

        // SpO2
        if (data.getSpo2Stats() != null && data.getSpo2Stats().getMeasurementCount() > 0) {
            sb.append("• Oxygen Saturation (SpO2):\n");
            sb.append("  - Average: ").append(String.format("%.1f%%", data.getSpo2Stats().getAverage())).append("\n");
            sb.append("  - Median: ").append(String.format("%.1f%%", data.getSpo2Stats().getMedian())).append("\n");
            sb.append("  - Range: ").append(String.format("%.1f", data.getSpo2Stats().getMin()))
                    .append(" - ").append(String.format("%.1f", data.getSpo2Stats().getMax())).append("%\n");
            sb.append("  - Measurements: ").append(data.getSpo2Stats().getMeasurementCount()).append("\n\n");
        }

        // Blood Pressure
        if (data.getBloodPressureStats() != null && data.getBloodPressureStats().getMeasurementCount() > 0) {
            sb.append("• Blood Pressure:\n");
            sb.append("  - Average: ")
                    .append(String.format("%.0f/%.0f",
                            data.getBloodPressureStats().getAverageSystolic(),
                            data.getBloodPressureStats().getAverageDiastolic()))
                    .append(" mmHg\n");
            if (data.getBloodPressureStats().getAverageMAP() != null) {
                sb.append("  - MAP: ").append(String.format("%.1f", data.getBloodPressureStats().getAverageMAP())).append(" mmHg\n");
            }
            sb.append("  - Measurements: ").append(data.getBloodPressureStats().getMeasurementCount()).append("\n\n");
        }

        // Glucose
        if (data.getGlucoseStats() != null && data.getGlucoseStats().getMeasurementCount() > 0) {
            sb.append("• Blood Glucose:\n");
            sb.append("  - Average: ").append(String.format("%.1f", data.getGlucoseStats().getAverage())).append(" mg/dL\n");
            sb.append("  - Median: ").append(String.format("%.1f", data.getGlucoseStats().getMedian())).append(" mg/dL\n");
            sb.append("  - Range: ").append(String.format("%.0f", data.getGlucoseStats().getMin()))
                    .append(" - ").append(String.format("%.0f", data.getGlucoseStats().getMax())).append(" mg/dL\n");
            if (data.getGlucoseStats().getTimeInRange() != null) {
                sb.append("  - Time In Range (70-180): ").append(String.format("%.1f%%", data.getGlucoseStats().getTimeInRange())).append("\n");
            }
            sb.append("  - Measurements: ").append(data.getGlucoseStats().getMeasurementCount()).append("\n\n");
        }

        // Events Summary
        sb.append("EVENTS SUMMARY:\n");
        sb.append("---------------\n");
        sb.append("Total Alerts: ").append(data.getAlertCount()).append("\n");
        sb.append("Emergencies: ").append(data.getEmergencyCount()).append("\n");
        sb.append("Global Adherence: ").append(String.format("%.1f%%", data.getOverallAdherenceRate())).append("\n");

        // Upcoming Appointments
        if (data.getAppointments() != null && !data.getAppointments().isEmpty()) {
            sb.append("\nUPCOMING APPOINTMENTS:\n");
            sb.append("----------------------\n");
            for (var appointment : data.getAppointments()) {
                sb.append("• ").append(appointment.getDate())
                        .append(" - ").append(appointment.getType()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Build detailed text summary for post-event report
     */
    private String buildPostEventTextContent(ReportData data, Long alertId) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== POST-EVENT REPORT ===\n");
        sb.append("Alert ID: ").append(alertId).append("\n");
        sb.append("Analysis Window: ±72 hours around event\n");
        sb.append("Period: ").append(data.getPeriodStart()).append(" - ").append(data.getPeriodEnd()).append("\n\n");

        sb.append("VITAL SIGNS DURING EVENT WINDOW:\n");
        sb.append("---------------------------------\n");

        // Heart Rate
        if (data.getHeartRateStats() != null && data.getHeartRateStats().getMeasurementCount() > 0) {
            sb.append("Heart Rate:\n");
            sb.append("  Average: ").append(String.format("%.1f", data.getHeartRateStats().getAverage())).append(" bpm\n");
            sb.append("  Range: ").append(String.format("%.0f", data.getHeartRateStats().getMin()))
                    .append(" - ").append(String.format("%.0f", data.getHeartRateStats().getMax())).append(" bpm\n");
        }

        // SpO2
        if (data.getSpo2Stats() != null && data.getSpo2Stats().getMeasurementCount() > 0) {
            sb.append("SpO2:\n");
            sb.append("  Average: ").append(String.format("%.1f%%", data.getSpo2Stats().getAverage())).append("\n");
            sb.append("  Minimum: ").append(String.format("%.1f%%", data.getSpo2Stats().getMin())).append("\n");
        }

        // Blood Pressure
        if (data.getBloodPressureStats() != null && data.getBloodPressureStats().getMeasurementCount() > 0) {
            sb.append("Blood Pressure:\n");
            sb.append("  Average: ")
                    .append(String.format("%.0f/%.0f",
                            data.getBloodPressureStats().getAverageSystolic(),
                            data.getBloodPressureStats().getAverageDiastolic()))
                    .append(" mmHg\n");
        }

        // Glucose
        if (data.getGlucoseStats() != null && data.getGlucoseStats().getMeasurementCount() > 0) {
            sb.append("Blood Glucose:\n");
            sb.append("  Average: ").append(String.format("%.1f", data.getGlucoseStats().getAverage())).append(" mg/dL\n");
            sb.append("  Range: ").append(String.format("%.0f", data.getGlucoseStats().getMin()))
                    .append(" - ").append(String.format("%.0f", data.getGlucoseStats().getMax())).append(" mg/dL\n");
        }

        // Additional alerts during window
        sb.append("\nALERTS DURING EVENT WINDOW: ").append(data.getAlertCount()).append("\n");
        if (data.getEmergencyCount() > 0) {
            sb.append("Emergency Alerts: ").append(data.getEmergencyCount()).append("\n");
        }

        sb.append("\n[Detailed timeline and analysis should be reviewed in the PDF report]\n");

        return sb.toString();
    }
}