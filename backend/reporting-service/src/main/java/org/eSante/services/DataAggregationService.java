package org.eSante.services;
import org.eSante.domain.models.dto.*;
import org.eSante.repositories.InfluxDBRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DataAggregationService {

    @Autowired
    private InfluxDBRepository influxDBRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Aggregates data for a weekly report (7 days).
     */
    public ReportData aggregateWeeklyData(Long patientId, Instant start, Instant stop) {
        ReportData data = new ReportData();
        data.setPatientId(patientId);
        data.setPeriodStart(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
        data.setPeriodEnd(LocalDateTime.ofInstant(stop, ZoneId.systemDefault()));

        // Vital signs from InfluxDB
        data.setHeartRateStats(influxDBRepository.getHeartRateStats(patientId, start, stop));
        data.setSpo2Stats(influxDBRepository.getSpO2Stats(patientId, start, stop));
        data.setBloodPressureStats(influxDBRepository.getBloodPressureStats(patientId, start, stop));
        data.setGlucoseStats(influxDBRepository.getGlucoseStats(patientId, start, stop));

        // Alerts from PostgreSQL
        List<AlertSummary> alerts = getAlertsForPeriod(patientId, start, stop);
        data.setAlerts(alerts);
        data.setAlertCount(alerts.size());
        data.setEmergencyCount((int) alerts.stream()
                .filter(a -> "URGENCE".equalsIgnoreCase(a.getLevel()) || "EMERGENCY".equalsIgnoreCase(a.getLevel()))
                .count());

        //Adherence computation (expected vs received measures)
        Map<String, Integer> adherence = calculateAdherence(patientId, start, stop);
        data.setAdherenceBySensor(adherence);
        data.setOverallAdherenceRate(calculateGlobalAdherence(adherence));

        //  Upcoming appointments
        List<AppointmentSummary> appointments = getUpcomingAppointments(patientId);
        data.setAppointments(appointments);

        return data;
    }

    /**
     * Aggregates data for a monthly report (30 days), including additional analysis.
     */
    public ReportData aggregateMonthlyData(Long patientId, Instant start, Instant stop) {
        //  Base monthly aggregation (same logic as weekly)
        ReportData data = aggregateWeeklyData(patientId, start, stop);

        // Compute comparison with previous month
        Instant previousStart = start.minus(30, ChronoUnit.DAYS);
        Instant previousStop = start;
        ReportData previousMonthData = aggregateWeeklyData(patientId, previousStart, previousStop);

        // Example comparison: Heart rate average difference
        if (data.getHeartRateStats() != null && previousMonthData.getHeartRateStats() != null) {
            double currentAvg = Optional.ofNullable(data.getHeartRateStats().getAverage()).orElse(0.0);
            double previousAvg = Optional.ofNullable(previousMonthData.getHeartRateStats().getAverage()).orElse(0.0);
            double delta = currentAvg - previousAvg;

            System.out.printf("Patient %d | Heart Rate change vs previous month: %.2f bpm%n", patientId, delta);
        }

        detectTrendsAndAnomalies(data);
        generateClinicalRecommendations(data);

        return data;
    }

    /**
     * Aggregates data for a post-event report (±72h around an alert).
     */
    public ReportData aggregatePostEventData(Long patientId, Instant start, Instant stop) {
        ReportData data = new ReportData();
        data.setPatientId(patientId);
        data.setPeriodStart(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
        data.setPeriodEnd(LocalDateTime.ofInstant(stop, ZoneId.systemDefault()));

        // Focus only on short-term vital signs
        data.setHeartRateStats(influxDBRepository.getHeartRateStats(patientId, start, stop));
        data.setSpo2Stats(influxDBRepository.getSpO2Stats(patientId, start, stop));
        data.setBloodPressureStats(influxDBRepository.getBloodPressureStats(patientId, start, stop));
        data.setGlucoseStats(influxDBRepository.getGlucoseStats(patientId, start, stop));

        List<AlertSummary> alerts = getAlertsForPeriod(patientId, start, stop);
        data.setAlerts(alerts);
        data.setAlertCount(alerts.size());
        data.setEmergencyCount((int) alerts.stream()
                .filter(a -> "URGENCE".equalsIgnoreCase(a.getLevel()) || "EMERGENCY".equalsIgnoreCase(a.getLevel()))
                .count());

        return data;
    }

    private List<AlertSummary> getAlertsForPeriod(Long patientId, Instant start, Instant stop) {
        String sql = """
            SELECT id, type_alerte, niveau, message, date_creation, etat
            FROM alertes
            WHERE patient_id = ?
              AND date_creation BETWEEN ? AND ?
            ORDER BY date_creation DESC
            """;

        LocalDateTime startLdt = LocalDateTime.ofInstant(start, ZoneId.systemDefault());
        LocalDateTime stopLdt = LocalDateTime.ofInstant(stop, ZoneId.systemDefault());

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new AlertSummary(
                        rs.getLong("id"),
                        rs.getString("type_alerte"),
                        rs.getString("niveau"),
                        rs.getString("message"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getString("etat")
                ),
                patientId, startLdt, stopLdt
        );
    }

    private Map<String, Integer> calculateAdherence(Long patientId, Instant start, Instant stop) {
        Map<String, Integer> adherence = new HashMap<>();

        long days = java.time.Duration.between(start, stop).toDays();

        // Expected measures per protocol
        int bpExpected = (int) (days * 2);  // Blood pressure twice/day
        int glucoseExpected = (int) (days * 4);  // Glucose 4x/day
        int weightExpected = (int) days;  // Weight once/day

        // Received measures from InfluxDB
        int bpReceived = countMeasures(patientId, "tension", start, stop);
        int glucoseReceived = countMeasures(patientId, "glycemie", start, stop);
        int weightReceived = countMeasures(patientId, "poids", start, stop);

        adherence.put("tension", computePercentage(bpReceived, bpExpected));
        adherence.put("glycemie", computePercentage(glucoseReceived, glucoseExpected));
        adherence.put("poids", computePercentage(weightReceived, weightExpected));

        return adherence;
    }

    private int countMeasures(Long patientId, String measurement, Instant start, Instant stop) {
        try {
            return influxDBRepository.countMeasurements(patientId, measurement, start, stop);
        } catch (Exception e) {
            System.err.println("Error counting " + measurement + " for patient " + patientId + ": " + e.getMessage());
            return 0;
        }
    }

    private int computePercentage(int received, int expected) {
        if (expected == 0) return 0;
        return Math.min(100, (int) (100.0 * received / expected));
    }

    private double calculateGlobalAdherence(Map<String, Integer> adherence) {
        return adherence.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    private List<AppointmentSummary> getUpcomingAppointments(Long patientId) {
        String sql = """
            SELECT id, date_rdv, type_rdv, commentaire
            FROM rendez_vous
            WHERE patient_id = ?
              AND date_rdv >= NOW()
            ORDER BY date_rdv ASC
            LIMIT 5
            """;

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new AppointmentSummary(
                        rs.getLong("id"),
                        rs.getTimestamp("date_rdv").toLocalDateTime(),
                        rs.getString("type_rdv"),
                        rs.getString("commentaire")
                ),
                patientId
        );
    }

    /**
     * Utility method to retrieve an alert timestamp for post-event analysis.
     */
    public Instant getAlertTimestamp(Long alertId) {
        String sql = "SELECT date_creation FROM alertes WHERE id = ?";
        return jdbcTemplate.queryForObject(sql,
                (rs, rowNum) -> rs.getTimestamp("date_creation").toInstant(),
                alertId
        );
    }

    private void detectTrendsAndAnomalies(ReportData data) {
        if (data.getBloodPressureStats() != null) {
            Double systolic = data.getBloodPressureStats().getAverageSystolic();
            Double diastolic = data.getBloodPressureStats().getAverageDiastolic();
            if (systolic != null && diastolic != null) {
                if (systolic > 140 || diastolic > 90) {
                    System.out.println(" Elevated blood pressure detected this month.");
                }
            }
        }

        if (data.getGlucoseStats() != null) {
            Double tir = data.getGlucoseStats().getTimeInRange();
            if (tir != null && tir < 70.0) {
                System.out.println("Low Time-In-Range detected (<70%) — possible poor glycemic control.");
            }
        }
    }

    private void generateClinicalRecommendations(ReportData data) {
        List<String> recommendations = new ArrayList<>();

        if (data.getHeartRateStats() != null && data.getHeartRateStats().getAverage() != null) {
            double avgHR = data.getHeartRateStats().getAverage();
            if (avgHR > 100)
                recommendations.add("Patient shows elevated heart rate — consider further cardiovascular evaluation.");
            else if (avgHR < 50)
                recommendations.add("Patient shows low heart rate — check for bradycardia risk.");
        }

        if (data.getGlucoseStats() != null && data.getGlucoseStats().getTimeInRange() != null) {
            double tir = data.getGlucoseStats().getTimeInRange();
            if (tir < 70)
                recommendations.add("Low Time-In-Range (<70%) — recommend diet/exercise adjustment or insulin review.");
        }

        if (data.getOverallAdherenceRate() < 80)
            recommendations.add("Low sensor adherence — consider patient re-engagement or technical troubleshooting.");

        if (recommendations.isEmpty()) {
            recommendations.add("No clinical alerts this month. Continue current monitoring plan.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Clinician Recommendations ===\n");
        for (String r : recommendations) {
            sb.append("• ").append(r).append("\n");
        }

        System.out.println(sb);
    }
}