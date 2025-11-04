package org.eSante.scheduler;
import org.eSante.services.ReportGeneratorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReportScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReportScheduler.class);

    @Autowired
    private ReportGeneratorService reportGeneratorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Runs every Sunday at 8 PM — automatically generates weekly reports for all patients.
     * The cron expression is configured in application.properties as:
     * reporting.scheduler.weekly = 0 0 20 ? * SUN
     */
    @Scheduled(cron = "${reporting.scheduler.weekly}")
    public void generateWeeklyReportsForAllPatients() {
        logger.info("=== Starting automatic generation of weekly reports ===");

        List<Long> patientIds = jdbcTemplate.queryForList(
                "SELECT id FROM patients",
                Long.class
        );

        int successCount = 0;
        int errorCount = 0;

        for (Long patientId : patientIds) {
            try {
                reportGeneratorService.generateWeeklyReport(patientId);
                logger.info("✓ Weekly report successfully generated for patient {}", patientId);
                successCount++;
            } catch (Exception e) {
                logger.error("✗ Failed to generate weekly report for patient {}", patientId, e);
                errorCount++;
            }
        }

        logger.info("=== Weekly report generation finished ({} succeeded, {} failed, total: {}) ===",
                successCount, errorCount, patientIds.size());
    }

    /**
     * Runs on the 1st of every month at 2 AM — automatically generates monthly reports for all patients.
     * The cron expression is configured in application.properties as:
     * reporting.scheduler.monthly = 0 0 2 1 * ?
     */
    @Scheduled(cron = "${reporting.scheduler.monthly}")
    public void generateMonthlyReportsForAllPatients() {
        logger.info("=== Starting automatic generation of monthly reports ===");

        List<Long> patientIds = jdbcTemplate.queryForList(
                "SELECT id FROM patients",
                Long.class
        );

        int successCount = 0;
        int errorCount = 0;

        for (Long patientId : patientIds) {
            try {
                reportGeneratorService.generateMonthlyReport(patientId);
                logger.info("✓ Monthly report successfully generated for patient {}", patientId);
                successCount++;
            } catch (Exception e) {
                logger.error("✗ Failed to generate monthly report for patient {}", patientId, e);
                errorCount++;
            }
        }

        logger.info("=== Monthly report generation finished ({} succeeded, {} failed, total: {}) ===",
                successCount, errorCount, patientIds.size());
    }
}
