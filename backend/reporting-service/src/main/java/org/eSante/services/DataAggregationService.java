package org.eSante.services;

import org.eSante.domain.models.dto.*;
import org.eSante.repositories.InfluxDBRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.EmptyResultDataAccessException;
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
     * Aggregates data for a weekly report (7 days)
     */
    public ReportData aggregateWeeklyData(Long patientId, Instant start, Instant stop) {
        ReportData data = new ReportData();
        data.setPatientId(patientId);
        data.setPatientName(resolvePatientName(patientId));
        data.setPeriodStart(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
        data.setPeriodEnd(LocalDateTime.ofInstant(stop, ZoneId.systemDefault()));

        data.setHeartRateStats(influxDBRepository.getHeartRateStats(patientId, start, stop));
        data.setSpo2Stats(influxDBRepository.getSpO2Stats(patientId, start, stop));
        data.setGlucoseStats(influxDBRepository.getGlucoseStats(patientId, start, stop));
        data.setBloodPressureStats(influxDBRepository.getBloodPressureStats(patientId, start, stop));
        //  Alertes depuis PostgreSQL
        List<AlertSummary> alerts = getAlertsForPeriod(patientId, start, stop);
        data.setAlerts(alerts);
        data.setAlertCount(alerts.size());
        data.setEmergencyCount((int) alerts.stream()
                .filter(a -> "URGENCE".equalsIgnoreCase(a.getLevel()) || "EMERGENCY".equalsIgnoreCase(a.getLevel()))
                .count());

        // 🕒 Adhérence
        Map<String, Integer> adherence = calculateAdherence(patientId, start, stop);
        data.setAdherenceBySensor(adherence);
        data.setOverallAdherenceRate(calculateGlobalAdherence(adherence));

        // 📅 Prochains rendez-vous
        data.setAppointments(getUpcomingAppointments(patientId));

        return data;
    }

    /**
     * Monthly report = same logic + comparison with previous period
     */
    public ReportData aggregateMonthlyData(Long patientId, Instant start, Instant stop) {
        ReportData data = aggregateWeeklyData(patientId, start, stop);

        // Comparaison avec le mois précédent
        Instant prevStart = start.minus(30, ChronoUnit.DAYS);
        Instant prevStop = start;
        ReportData prev = aggregateWeeklyData(patientId, prevStart, prevStop);

        if (data.getHeartRateStats() != null && prev.getHeartRateStats() != null) {
            double delta = data.getHeartRateStats().getAverage() - prev.getHeartRateStats().getAverage();
            System.out.printf("Δ fréquence cardiaque (patient %d): %.2f bpm%n", patientId, delta);
        }

        detectTrendsAndAnomalies(data);
        generateClinicalRecommendations(data);

        return data;
    }

    /**
     * Aggregate data for an arbitrary period [start, stop]
     */
    public ReportData aggregateRangeData(Long patientId, Instant start, Instant stop) {
        return aggregateWeeklyData(patientId, start, stop);
    }

    /**
     * Aggregates ±72h around alert
     */
    public ReportData aggregatePostEventData(Long patientId, Instant start, Instant stop) {
        ReportData data = new ReportData();
        data.setPatientId(patientId);
        data.setPatientName(resolvePatientName(patientId));
        data.setPeriodStart(LocalDateTime.ofInstant(start, ZoneId.systemDefault()));
        data.setPeriodEnd(LocalDateTime.ofInstant(stop, ZoneId.systemDefault()));

        data.setHeartRateStats(influxDBRepository.getHeartRateStats(patientId, start, stop));
        data.setSpo2Stats(influxDBRepository.getSpO2Stats(patientId, start, stop));
        data.setGlucoseStats(influxDBRepository.getGlucoseStats(patientId, start, stop));
        data.setBloodPressureStats(influxDBRepository.getBloodPressureStats(patientId, start, stop));
        List<AlertSummary> alerts = getAlertsForPeriod(patientId, start, stop);
        data.setAlerts(alerts);
        data.setAlertCount(alerts.size());
        data.setEmergencyCount((int) alerts.stream()
                .filter(a -> "URGENCE".equalsIgnoreCase(a.getLevel()) || "EMERGENCY".equalsIgnoreCase(a.getLevel()))
                .count());

        return data;
    }

    // Expose alerts fetch for other services (dashboard)
    public List<AlertSummary> fetchAlerts(Long patientId, Instant start, Instant stop) {
        return getAlertsForPeriod(patientId, start, stop);
    }

    private String resolvePatientName(Long patientId) {
        String sql = """
            SELECT u.prenom, u.nom
            FROM patients p
            JOIN utilisateurs u ON u.id = p.utilisateur_id
            WHERE p.id = ?
        """;
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{patientId}, (rs, rowNum) -> {
                String first = Optional.ofNullable(rs.getString("prenom")).map(String::trim).orElse("");
                String last = Optional.ofNullable(rs.getString("nom")).map(String::trim).orElse("");
                String fullName = (first + " " + last).trim();
                return fullName.isEmpty() ? null : fullName;
            });
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }


    private List<AlertSummary> getAlertsForPeriod(Long patientId, Instant start, Instant stop) {
        String sql = """
            SELECT id, type_alerte, niveau, message, date_creation, etat
            FROM alertes
            WHERE patient_id = ?
              AND date_creation BETWEEN ? AND ?
            ORDER BY date_creation DESC
            """;

        LocalDateTime s = LocalDateTime.ofInstant(start, ZoneId.systemDefault());
        LocalDateTime e = LocalDateTime.ofInstant(stop, ZoneId.systemDefault());

        return jdbcTemplate.query(sql,
                (rs, rowNum) -> new AlertSummary(
                        rs.getLong("id"),
                        rs.getString("type_alerte"),
                        rs.getString("niveau"),
                        rs.getString("message"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getString("etat")
                ),
                patientId, s, e);
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
                patientId);
    }

    public Instant getAlertTimestamp(Long alertId) {
        String sql = "SELECT date_creation FROM alertes WHERE id = ?";
        return jdbcTemplate.queryForObject(sql,
                (rs, rowNum) -> rs.getTimestamp("date_creation").toInstant(),
                alertId);
    }



    private Map<String, Integer> calculateAdherence(Long patientId, Instant start, Instant stop) {
        Map<String, Integer> adherence = new HashMap<>();
        long days = java.time.Duration.between(start, stop).toDays();

        int bpExpected = (int) (days * 2);
        int glucoseExpected = (int) (days * 4);
        int weightExpected = (int) days;

        int bpReceived = influxDBRepository.countMeasurements(patientId, "tension", start, stop);
        int glucoseReceived = influxDBRepository.countMeasurements(patientId, "glycemie", start, stop);
        int weightReceived = influxDBRepository.countMeasurements(patientId, "poids", start, stop);

        adherence.put("tension", computePercentage(bpReceived, bpExpected));
        adherence.put("glycemie", computePercentage(glucoseReceived, glucoseExpected));
        adherence.put("poids", computePercentage(weightReceived, weightExpected));

        return adherence;
    }

    private int computePercentage(int received, int expected) {
        if (expected <= 0) return 0;
        return Math.min(100, (int) (100.0 * received / expected));
    }

    private double calculateGlobalAdherence(Map<String, Integer> adherence) {
        return adherence.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    private void detectTrendsAndAnomalies(ReportData data) {
        if (data.getBloodPressureStats() != null &&
                data.getBloodPressureStats().getAverageSystolic() != null &&
                data.getBloodPressureStats().getAverageDiastolic() != null) {
            if (data.getBloodPressureStats().getAverageSystolic() > 140 ||
                    data.getBloodPressureStats().getAverageDiastolic() > 90) {
                System.out.println("Hypertension détectée ce mois-ci.");
            }
        }

        if (data.getGlucoseStats() != null &&
                data.getGlucoseStats().getTimeInRange() != null &&
                data.getGlucoseStats().getTimeInRange() < 70.0) {
            System.out.println("Temps dans la plage (<70%) faible — possible déséquilibre glycémique.");
        }
    }

    private void generateClinicalRecommendations(ReportData data) {
        List<String> recs = new ArrayList<>();

        if (data.getHeartRateStats() != null && data.getHeartRateStats().getAverage() != null) {
            double avg = data.getHeartRateStats().getAverage();
            if (avg > 100)
                recs.add("Fréquence cardiaque élevée — évaluer le risque cardiovasculaire.");
            else if (avg < 50)
                recs.add("Fréquence cardiaque basse — vérifier risque de bradycardie.");
        }

        if (data.getGlucoseStats() != null && data.getGlucoseStats().getTimeInRange() != null) {
            double tir = data.getGlucoseStats().getTimeInRange();
            if (tir < 70)
                recs.add("Temps dans la plage faible — ajuster régime ou traitement.");
        }

        if (data.getOverallAdherenceRate() < 80)
            recs.add("Adhérence capteurs faible — relancer le patient ou vérifier l’équipement.");

        if (recs.isEmpty())
            recs.add("Aucune alerte clinique ce mois-ci. Poursuivre le suivi.");

        System.out.println("=== Recommandations cliniques ===");
        recs.forEach(r -> System.out.println("• " + r));
    }
}
