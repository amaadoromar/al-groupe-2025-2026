package org.eSante.services;

import org.eSante.domain.models.Report;
import org.eSante.domain.models.dto.ReportData;
import org.eSante.repositories.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportGeneratorServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private DataAggregationService dataAggregationService;

    @Mock
    private PDFExportService pdfExportService;

    @InjectMocks
    private ReportGeneratorService reportGeneratorService;

    private Path tempDir;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("reports-test-");
        ReflectionTestUtils.setField(reportGeneratorService, "storagePath", tempDir.toString());

        doAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(99L);
            return report;
        }).when(reportRepository).save(any(Report.class));
    }

    @Test
    void generateWeeklyReport_storesPdfAndRemovesIdFromSummary() throws Exception {
        ReportData aggregated = new ReportData();
        aggregated.setPatientId(1L);
        aggregated.setPatientName("Alice Martin");
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        aggregated.setPeriodStart(LocalDateTime.ofInstant(now.minus(7, ChronoUnit.DAYS), ZoneId.systemDefault()));
        aggregated.setPeriodEnd(LocalDateTime.ofInstant(now, ZoneId.systemDefault()));

        when(dataAggregationService.aggregateWeeklyData(anyLong(), any(Instant.class), any(Instant.class))).thenReturn(aggregated);

        byte[] pdfBytes = "dummy-pdf-content".getBytes();
        when(pdfExportService.generateWeeklyPDF(aggregated)).thenReturn(pdfBytes);

        Report report = reportGeneratorService.generateWeeklyReport(1L);

        assertThat(report.getId()).isEqualTo(99L);
        assertThat(report.getStatus()).isEqualTo("READY");
        assertThat(report.getSummary()).contains("Alice Martin");
        assertThat(report.getSummary()).doesNotContain("ID");

        assertThat(report.getContent()).isEqualTo(Base64.getEncoder().encodeToString(pdfBytes));
        assertThat(report.getFilePath()).isNotBlank();

        Path storedPath = Path.of(report.getFilePath());
        assertThat(storedPath).exists();
        assertThat(Files.readAllBytes(storedPath)).isEqualTo(pdfBytes);
    }
}
