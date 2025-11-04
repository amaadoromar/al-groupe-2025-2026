package org.eSante.repositories;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.eSante.domain.models.dto.VitalSignsStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;


@Repository
public class InfluxDBRepository {

    @Autowired
    private InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.org}")
    private String org;

    // --- HEART RATE ---
    public VitalSignsStats getHeartRateStats(Long patientId, Instant start, Instant stop) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r["_measurement"] == "fc")
              |> filter(fn: (r) => r["patient"] == "%d")
              |> aggregateWindow(every: 1h, fn: mean, createEmpty: false)
            """, bucket, start, stop, patientId);

        return executeStatsQuery(flux, "Heart Rate", "bpm");
    }

    // --- SPO2 ---
    public VitalSignsStats getSpO2Stats(Long patientId, Instant start, Instant stop) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r["_measurement"] == "spO2")
              |> filter(fn: (r) => r["patient"] == "%d")
            """, bucket, start, stop, patientId);

        return executeStatsQuery(flux, "SpO2", "%");
    }

    // --- BLOOD PRESSURE ---
    public VitalSignsStats getBloodPressureStats(Long patientId, Instant start, Instant stop) {
        // Query systolic
        String fluxSystolic = String.format("""
        from(bucket: "%s")
          |> range(start: %s, stop: %s)
          |> filter(fn: (r) => r["_measurement"] == "tension")
          |> filter(fn: (r) => r["patient"] == "%d")
          |> filter(fn: (r) => r["_field"] == "systolique")
        """, bucket, start, stop, patientId);

        // Query diastolic
        String fluxDiastolic = String.format("""
        from(bucket: "%s")
          |> range(start: %s, stop: %s)
          |> filter(fn: (r) => r["_measurement"] == "tension")
          |> filter(fn: (r) => r["patient"] == "%d")
          |> filter(fn: (r) => r["_field"] == "diastolique")
        """, bucket, start, stop, patientId);

        VitalSignsStats systolicStats = executeStatsQuery(fluxSystolic, "Systolic", "mmHg");
        VitalSignsStats diastolicStats = executeStatsQuery(fluxDiastolic, "Diastolic", "mmHg");

        // Combine results
        VitalSignsStats combinedStats = new VitalSignsStats();
        combinedStats.setMetric("Blood Pressure");
        combinedStats.setUnit("mmHg");
        combinedStats.setAverageSystolic(systolicStats.getAverage());
        combinedStats.setAverageDiastolic(diastolicStats.getAverage());

        // Calculate MAP = DBP + (SBP - DBP) / 3
        if (systolicStats.getAverage() != null && diastolicStats.getAverage() != null) {
            double map = diastolicStats.getAverage() +
                    (systolicStats.getAverage() - diastolicStats.getAverage()) / 3.0;
            combinedStats.setAverageMAP(map);
        }

        // Set measurement count
        int count = 0;
        if (systolicStats.getMeasurementCount() != null) count += systolicStats.getMeasurementCount();
        if (diastolicStats.getMeasurementCount() != null) count += diastolicStats.getMeasurementCount();
        combinedStats.setMeasurementCount(count / 2); // Divide by 2 since we have both sys and dia

        // Set min/max
        if (systolicStats.getMin() != null && diastolicStats.getMin() != null) {
            combinedStats.setMin(Math.min(systolicStats.getMin(), diastolicStats.getMin()));
        }
        if (systolicStats.getMax() != null && diastolicStats.getMax() != null) {
            combinedStats.setMax(Math.max(systolicStats.getMax(), diastolicStats.getMax()));
        }

        return combinedStats;
    }

    // --- GLUCOSE ---
    public VitalSignsStats getGlucoseStats(Long patientId, Instant start, Instant stop) {
        String flux = String.format("""
        from(bucket: "%s")
          |> range(start: %s, stop: %s)
          |> filter(fn: (r) => r["_measurement"] == "glycemie")
          |> filter(fn: (r) => r["patient"] == "%d")
        """, bucket, start, stop, patientId);

        VitalSignsStats stats = executeStatsQuery(flux, "Blood Glucose", "mg/dL");

        // Calculate Time-In-Range (70-180 mg/dL)
        try {
            String tirFlux = String.format("""
            from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r["_measurement"] == "glycemie")
              |> filter(fn: (r) => r["patient"] == "%d")
              |> map(fn: (r) => ({
                  r with
                  inRange: if r._value >= 70.0 and r._value <= 180.0 then 1 else 0
                }))
              |> reduce(
                  fn: (r, accumulator) => ({
                      total: accumulator.total + 1,
                      inRange: accumulator.inRange + r.inRange
                  }),
                  identity: {total: 0, inRange: 0}
                )
              |> map(fn: (r) => ({
                  r with
                  tir: if r.total > 0 then float(v: r.inRange) / float(v: r.total) * 100.0 else 0.0
                }))
            """, bucket, start, stop, patientId);

            List<FluxTable> tirTables = influxDBClient.getQueryApi().query(tirFlux, org);

            if (!tirTables.isEmpty() && !tirTables.get(0).getRecords().isEmpty()) {
                FluxRecord record = tirTables.get(0).getRecords().get(0);
                Object tirValue = record.getValueByKey("tir");
                if (tirValue != null) {
                    stats.setTimeInRange(((Number) tirValue).doubleValue());
                }
            }
        } catch (Exception e) {
            System.err.println("Error calculating Time-In-Range: " + e.getMessage());
            stats.setTimeInRange(null);
        }

        return stats;
    }

    // --- WEIGHT ---
    public VitalSignsStats getWeightStats(Long patientId, Instant start, Instant stop) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r["_measurement"] == "poids")
              |> filter(fn: (r) => r["patient"] == "%d")
            """, bucket, start, stop, patientId);

        return executeStatsQuery(flux, "Weight", "kg");
    }

    // --- GENERIC STATS QUERY EXECUTOR ---
    private VitalSignsStats executeStatsQuery(String flux, String metric, String unit) {
        VitalSignsStats stats = new VitalSignsStats();
        stats.setMetric(metric);
        stats.setUnit(unit);

        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, org);

            List<Double> values = new ArrayList<>();
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Object value = record.getValueByKey("_value");
                    if (value != null) {
                        values.add(((Number) value).doubleValue());
                    }
                }
            }

            if (!values.isEmpty()) {
                Collections.sort(values);
                stats.setMeasurementCount(values.size());
                stats.setMin(values.get(0));
                stats.setMax(values.get(values.size() - 1));
                stats.setAverage(values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                stats.setMedian(getPercentile(values, 50));
                stats.setP10(getPercentile(values, 10));
                stats.setP90(getPercentile(values, 90));
                stats.setStandardDeviation(calculateStdDev(values));
            } else {
                // No data found, set defaults
                stats.setMeasurementCount(0);
                stats.setAverage(0.0);
                stats.setMedian(0.0);
                stats.setMin(0.0);
                stats.setMax(0.0);
                stats.setP10(0.0);
                stats.setP90(0.0);
                stats.setStandardDeviation(0.0);
            }

        } catch (Exception e) {
            System.err.println("Error executing stats query for " + metric + ": " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    private Double getPercentile(List<Double> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) return 0.0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
    }

    private Double calculateStdDev(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    // --- COUNT MEASUREMENTS (for adherence calculation) ---
    public int countMeasurements(Long patientId, String measurement, Instant start, Instant stop) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: %s, stop: %s)
              |> filter(fn: (r) => r["_measurement"] == "%s")
              |> filter(fn: (r) => r["patient"] == "%d")
              |> count()
            """, bucket, start, stop, measurement, patientId);

        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, org);

            if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
                Object count = tables.get(0).getRecords().get(0).getValueByKey("_value");
                return count != null ? ((Number) count).intValue() : 0;
            }
        } catch (Exception e) {
            System.err.println("Error counting measurements: " + e.getMessage());
        }

        return 0;
    }
}
