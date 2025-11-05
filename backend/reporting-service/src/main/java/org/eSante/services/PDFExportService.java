package org.eSante.services;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import org.eSante.domain.models.dto.ReportData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PDFExportService {

    private static final Logger logger = LoggerFactory.getLogger(PDFExportService.class);

    // ============================================================
    // WEEKLY PDF
    // ============================================================
    public byte[] generateWeeklyPDF(ReportData data) {
        return generatePDF(data, "WEEKLY HEALTH REPORT", "Weekly summary of patient metrics");
    }

    // ============================================================
    // MONTHLY PDF
    // ============================================================
    public byte[] generateMonthlyPDF(ReportData data) {
        return generatePDF(data, "MONTHLY CLINICAL REPORT", "Comprehensive 30-day clinical summary");
    }

    // ============================================================
    // POST-EVENT PDF
    // ============================================================
    public byte[] generatePostEventPDF(ReportData data) {
        return generatePDF(data, "POST-EVENT ANALYSIS REPORT", "Detailed 72-hour analysis around alert");
    }

    // ============================================================
    // CUSTOM (ON-DEMAND) PDF
    // ============================================================
    public byte[] generateCustomPDF(ReportData data) {
        return generatePDF(data, "ON-DEMAND HEALTH REPORT", "Summary for selected time window");
    }

    // ============================================================
    // CORE LOGIC
    // ============================================================
    private byte[] generatePDF(ReportData data, String title, String subtitle) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // === HEADER ===
            document.add(new Paragraph(title)
                    .setBold()
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(5));

            document.add(new Paragraph(subtitle)
                    .setItalic()
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(10));

            String patientLabel = (data.getPatientName() != null && !data.getPatientName().isBlank())
                    ? data.getPatientName()
                    : "Patient #" + data.getPatientId();

            document.add(new Paragraph(patientLabel)
                    .setBold()
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("Period: " + data.getPeriodStart() + " → " + data.getPeriodEnd())
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // === TABLE OF METRICS ===
            float[] columnWidths = {200F, 200F};
            Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

            addMetric(table, "💓 Heart Rate",
                    data.getHeartRateStats() != null
                            ? String.format("%.1f bpm", data.getHeartRateStats().getAverage())
                            : "No data");

            addMetric(table, "🫁 SpO₂",
                    data.getSpo2Stats() != null
                            ? String.format("%.1f%%", data.getSpo2Stats().getAverage())
                            : "No data");

            addMetric(table, "🩸 Blood Pressure",
                    data.getBloodPressureStats() != null
                            ? String.format("%.0f/%.0f mmHg",
                            data.getBloodPressureStats().getAverageSystolic(),
                            data.getBloodPressureStats().getAverageDiastolic())
                            : "No data");

            addMetric(table, "🍬 Glucose",
                    data.getGlucoseStats() != null
                            ? String.format("%.1f mg/dL", data.getGlucoseStats().getAverage())
                            : "No data");

            addMetric(table, "📅 Alerts",
                    data.getAlertCount() + " (Emergencies: " + data.getEmergencyCount() + ")");

            addMetric(table, "Adherence",
                    String.format("%.1f%%", data.getOverallAdherenceRate()));

            document.add(table.setMarginBottom(20));

            // === APPOINTMENTS ===
            if (data.getAppointments() != null && !data.getAppointments().isEmpty()) {
                document.add(new Paragraph("Upcoming Appointments:")
                        .setBold()
                        .setFontSize(13)
                        .setMarginBottom(5));

                for (var app : data.getAppointments()) {
                    document.add(new Paragraph("• " + app.getDate() + " — " + app.getType())
                            .setFontSize(10)
                            .setMarginLeft(15));
                }
            }

            // === FOOTER ===
            // Footer intentionally left blank per requirements

            document.close();
            logger.info("{} generated for patient {}", title, data.getPatientId());
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error generating " + title + ": " + e.getMessage(), e);
        }
    }

    private void addMetric(Table table, String label, String value) {
        Cell cell1 = new Cell().add(new Paragraph(label).setBold());
        Cell cell2 = new Cell().add(new Paragraph(value));
        cell1.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        cell1.setTextAlignment(TextAlignment.LEFT);
        cell2.setTextAlignment(TextAlignment.CENTER);
        table.addCell(cell1);
        table.addCell(cell2);
    }
}
