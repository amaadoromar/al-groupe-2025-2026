package org.eSante.services;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.eSante.domain.models.dto.ReportData;
import org.eSante.domain.models.dto.VitalSignsStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PDFExportService {

    private static final Logger logger = LoggerFactory.getLogger(PDFExportService.class);

    @Value("${reporting.storage.path:/var/reports}")
    private String storagePath;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String generateWeeklyPDF(ReportData data) throws IOException {
        String filename = String.format("weekly_report_patient_%d_%s.pdf",
                data.getPatientId(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        Path filePath = Paths.get(storagePath, filename);
        Files.createDirectories(filePath.getParent());

        // ✅ UTILISER LE NOM COMPLET
        try (PdfWriter writer = new PdfWriter(new FileOutputStream(filePath.toFile()));
             PdfDocument pdf = new PdfDocument(writer);
             com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf)) {

            // Header
            document.add(new Paragraph("WEEKLY HEALTH REPORT")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            document.add(new Paragraph(String.format("Patient #%d", data.getPatientId()))
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            document.add(new Paragraph(String.format("Period: %s → %s",
                    data.getPeriodStart().format(FORMATTER),
                    data.getPeriodEnd().format(FORMATTER)))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30));

            // Vital Signs Section
            document.add(new Paragraph("📊 VITAL SIGNS")
                    .setFontSize(16)
                    .setBold()
                    .setMarginBottom(10));

            Table vitalTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2}));
            vitalTable.setWidth(UnitValue.createPercentValue(100));

            vitalTable.addHeaderCell(new Cell().add(new Paragraph("Metric").setBold()));
            vitalTable.addHeaderCell(new Cell().add(new Paragraph("Average").setBold()));
            vitalTable.addHeaderCell(new Cell().add(new Paragraph("Min").setBold()));
            vitalTable.addHeaderCell(new Cell().add(new Paragraph("Max").setBold()));

            // Add stats only if available
            if (data.getHeartRateStats() != null && data.getHeartRateStats().getMeasurementCount() > 0) {
                vitalTable.addCell("Heart Rate ❤️");
                vitalTable.addCell(String.format("%.0f bpm", data.getHeartRateStats().getAverage()));
                vitalTable.addCell(String.format("%.0f", data.getHeartRateStats().getMin()));
                vitalTable.addCell(String.format("%.0f", data.getHeartRateStats().getMax()));
            }

            if (data.getSpo2Stats() != null && data.getSpo2Stats().getMeasurementCount() > 0) {
                vitalTable.addCell("Oxygen Saturation 🫁");
                vitalTable.addCell(String.format("%.1f %%", data.getSpo2Stats().getAverage()));
                vitalTable.addCell(String.format("%.1f", data.getSpo2Stats().getMin()));
                vitalTable.addCell(String.format("%.1f", data.getSpo2Stats().getMax()));
            }

            if (data.getBloodPressureStats() != null && data.getBloodPressureStats().getMeasurementCount() > 0) {
                vitalTable.addCell("Blood Pressure 🩺");
                vitalTable.addCell(String.format("%.0f/%.0f mmHg",
                        data.getBloodPressureStats().getAverageSystolic(),
                        data.getBloodPressureStats().getAverageDiastolic()));
                vitalTable.addCell("-");
                vitalTable.addCell("-");
            }

            if (data.getGlucoseStats() != null && data.getGlucoseStats().getMeasurementCount() > 0) {
                vitalTable.addCell("Blood Glucose 🩸");
                vitalTable.addCell(String.format("%.0f mg/dL", data.getGlucoseStats().getAverage()));
                vitalTable.addCell(String.format("%.0f", data.getGlucoseStats().getMin()));
                vitalTable.addCell(String.format("%.0f", data.getGlucoseStats().getMax()));
            }

            document.add(vitalTable);
            document.add(new Paragraph("\n"));

            // Alerts Section
            document.add(new Paragraph("⚠️ ALERTS OF THE WEEK")
                    .setFontSize(16)
                    .setBold()
                    .setMarginTop(20)
                    .setMarginBottom(10));

            if (data.getAlertCount() == 0) {
                document.add(new Paragraph("✅ No alerts this week. Great job!")
                        .setFontSize(12)
                        .setItalic());
            } else {
                document.add(new Paragraph(String.format(
                        "You had %d alert(s) this week, including %d emergency alert(s).",
                        data.getAlertCount(),
                        data.getEmergencyCount()))
                        .setFontSize(12));
            }

            // Adherence Section
            document.add(new Paragraph("📅 ADHERENCE")
                    .setFontSize(16)
                    .setBold()
                    .setMarginTop(20)
                    .setMarginBottom(10));

            String adherenceMessage = data.getOverallAdherenceRate() >= 80
                    ? "Excellent adherence 👏"
                    : data.getOverallAdherenceRate() >= 60
                    ? "Good, keep it up!"
                    : "Please remember to take your daily measurements regularly.";

            document.add(new Paragraph(String.format(
                    "You completed %.0f%% of your scheduled measurements. %s",
                    data.getOverallAdherenceRate(), adherenceMessage))
                    .setFontSize(12));

            // Appointments Section
            if (data.getAppointments() != null && !data.getAppointments().isEmpty()) {
                document.add(new Paragraph("📆 UPCOMING APPOINTMENTS")
                        .setFontSize(16)
                        .setBold()
                        .setMarginTop(20)
                        .setMarginBottom(10));

                for (var appointment : data.getAppointments()) {
                    document.add(new Paragraph(String.format("• %s — %s",
                            appointment.getDate().format(FORMATTER),
                            appointment.getType()))
                            .setFontSize(11));
                }
            }

            // Footer
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("This report was automatically generated.")
                    .setFontSize(8)
                    .setItalic()
                    .setTextAlignment(TextAlignment.CENTER));

            logger.info("Weekly PDF generated: {}", filePath);
        }

        return filePath.toString();
    }

    public String generateMonthlyPDF(ReportData data) throws IOException {
        String filename = String.format("monthly_report_patient_%d_%s.pdf",
                data.getPatientId(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        Path filePath = Paths.get(storagePath, filename);
        Files.createDirectories(filePath.getParent());

        // ✅ UTILISER LE NOM COMPLET ICI AUSSI
        try (PdfWriter writer = new PdfWriter(new FileOutputStream(filePath.toFile()));
             PdfDocument pdf = new PdfDocument(writer);
             com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf)) {

            document.add(new Paragraph("MONTHLY CLINICAL REPORT")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            document.add(new Paragraph(String.format("Patient #%d", data.getPatientId()))
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            document.add(new Paragraph(String.format("Period: %s → %s",
                    data.getPeriodStart().format(FORMATTER),
                    data.getPeriodEnd().format(FORMATTER)))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(30));

            document.add(new Paragraph("Complete Statistics")
                    .setFontSize(16)
                    .setBold()
                    .setMarginBottom(10));

            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2}));
            statsTable.setWidth(UnitValue.createPercentValue(100));
            statsTable.addHeaderCell("Metric");
            statsTable.addHeaderCell("Average");
            statsTable.addHeaderCell("Median");
            statsTable.addHeaderCell("P10");
            statsTable.addHeaderCell("P90");

            if (data.getHeartRateStats() != null && data.getHeartRateStats().getMeasurementCount() > 0)
                addStatsRow(statsTable, "Heart Rate", data.getHeartRateStats());
            if (data.getSpo2Stats() != null && data.getSpo2Stats().getMeasurementCount() > 0)
                addStatsRow(statsTable, "SpO2", data.getSpo2Stats());
            if (data.getBloodPressureStats() != null && data.getBloodPressureStats().getMeasurementCount() > 0)
                addStatsRow(statsTable, "Blood Pressure", data.getBloodPressureStats());
            if (data.getGlucoseStats() != null && data.getGlucoseStats().getMeasurementCount() > 0)
                addStatsRow(statsTable, "Glucose", data.getGlucoseStats());

            document.add(statsTable);

            document.add(new Paragraph("\nTrends and Recommendations")
                    .setFontSize(16)
                    .setBold()
                    .setMarginTop(20));

            document.add(new Paragraph("This section may include analysis comparing previous month averages and clinical alerts.")
                    .setFontSize(12));

            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("Report automatically generated for clinician review.")
                    .setFontSize(8)
                    .setItalic()
                    .setTextAlignment(TextAlignment.CENTER));

            logger.info("Monthly PDF generated: {}", filePath);
        }

        return filePath.toString();
    }

    public String generateMonthlyCSV(ReportData data) throws IOException {
        String filename = String.format("monthly_report_patient_%d_%s.csv",
                data.getPatientId(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));

        Path filePath = Paths.get(storagePath, filename);
        Files.createDirectories(filePath.getParent());

        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write("Metric,Average,Median,Min,Max,P10,P90,StdDev,Unit\n");

            if (data.getHeartRateStats() != null && data.getHeartRateStats().getMeasurementCount() > 0)
                writer.write(formatCSVLine("Heart Rate", data.getHeartRateStats()));
            if (data.getSpo2Stats() != null && data.getSpo2Stats().getMeasurementCount() > 0)
                writer.write(formatCSVLine("SpO2", data.getSpo2Stats()));
            if (data.getBloodPressureStats() != null && data.getBloodPressureStats().getMeasurementCount() > 0)
                writer.write(formatCSVLine("Blood Pressure", data.getBloodPressureStats()));
            if (data.getGlucoseStats() != null && data.getGlucoseStats().getMeasurementCount() > 0)
                writer.write(formatCSVLine("Glucose", data.getGlucoseStats()));

            logger.info("Monthly CSV generated: {}", filePath);
        }

        return filePath.toString();
    }

    private void addStatsRow(Table table, String metric, VitalSignsStats stats) {
        table.addCell(metric);
        table.addCell(String.format("%.2f", stats.getAverage() != null ? stats.getAverage() : 0.0));
        table.addCell(String.format("%.2f", stats.getMedian() != null ? stats.getMedian() : 0.0));
        table.addCell(String.format("%.2f", stats.getP10() != null ? stats.getP10() : 0.0));
        table.addCell(String.format("%.2f", stats.getP90() != null ? stats.getP90() : 0.0));
    }

    private String formatCSVLine(String metric, VitalSignsStats stats) {
        return String.format("%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%s\n",
                metric,
                stats.getAverage() != null ? stats.getAverage() : 0.0,
                stats.getMedian() != null ? stats.getMedian() : 0.0,
                stats.getMin() != null ? stats.getMin() : 0.0,
                stats.getMax() != null ? stats.getMax() : 0.0,
                stats.getP10() != null ? stats.getP10() : 0.0,
                stats.getP90() != null ? stats.getP90() : 0.0,
                stats.getStandardDeviation() != null ? stats.getStandardDeviation() : 0.0,
                stats.getUnit() != null ? stats.getUnit() : "");
    }
}