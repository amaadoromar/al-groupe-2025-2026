package org.eSante.services;

import org.eSante.domain.models.dto.ReportData;
import org.eSante.domain.models.dto.VitalSignsStats;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PDFExportServiceTest {

    private final PDFExportService pdfExportService = new PDFExportService();

    @Test
    void generateWeeklyPDF_includesPatientNameWithoutFooterOrId() throws Exception {
        ReportData data = new ReportData();
        data.setPatientId(1L);
        data.setPatientName("Alice Martin");
        data.setPeriodStart(LocalDateTime.of(2025, 11, 1, 10, 0));
        data.setPeriodEnd(LocalDateTime.of(2025, 11, 4, 10, 0));

        VitalSignsStats heartRate = new VitalSignsStats();
        heartRate.setAverage(75.2);
        data.setHeartRateStats(heartRate);

        VitalSignsStats spo2 = new VitalSignsStats();
        spo2.setAverage(97.4);
        data.setSpo2Stats(spo2);

        byte[] pdf = pdfExportService.generateWeeklyPDF(data);

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf))) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Alice Martin"), "Patient name should appear in PDF content");
            assertFalse(text.contains("Patient ID"), "Patient identifier should not appear in PDF content");
            assertFalse(text.contains("Generated automatically"), "Footer message must be removed");
        }
    }
}
