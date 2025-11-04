package org.eSante.api;

import org.eSante.domain.models.Report;
import org.eSante.domain.models.dto.ReportData;
import org.eSante.repositories.InfluxDBRepository;
import org.eSante.repositories.ReportRepository;
import org.eSante.services.PDFExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportRepository reportRepository;

    @MockBean
    private InfluxDBRepository influxDBRepository;

    private final PDFExportService pdfExportService = new PDFExportService();

    @BeforeEach
    void clean() {
        reportRepository.deleteAll();
    }

    @Test
    void exportReport_returnsStoredPdfContent() throws Exception {
        ReportData reportData = new ReportData();
        reportData.setPatientId(1L);
        reportData.setPatientName("Alice Martin");
        reportData.setPeriodStart(LocalDateTime.of(2025, 11, 1, 10, 0));
        reportData.setPeriodEnd(LocalDateTime.of(2025, 11, 8, 10, 0));

        byte[] pdf = pdfExportService.generateWeeklyPDF(reportData);

        Report report = new Report(1L, "WEEKLY");
        report.setContent(Base64.getEncoder().encodeToString(pdf));
        report.setExportFormat("PDF_BASE64");
        report.setStatus("READY");
        report.setFilePath("/reports/test.pdf");

        Report saved = reportRepository.save(report);

        byte[] responseBytes = mockMvc.perform(get("/api/reports/{id}/export", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(responseBytes).isEqualTo(pdf);
    }
}
