package org.eSante.repositories;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.eSante.domain.models.dto.VitalSignsStats;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Repository
public class InfluxDBRepository {

    @Autowired
    private InfluxDBClient influxDBClient;

    @Value("${influxdb.bucket}")
    private String bucket;

    @Value("${influxdb.org}")
    private String org;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    // --- HEART RATE ---
    public VitalSignsStats getHeartRateStats(Long patientId, Instant start, Instant stop) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: time(v: "%s"), stop: time(v: "%s"))
              |> filter(fn: (r) => r["_measurement"] == "fc")
              |> filter(fn: (r) => r["patient"] == "%d")
              |> filter(fn: (r) => r["_field"] == "value")
              |> aggregateWindow(every: 1h, fn: mean, createEmpty: false)
            """, bucket, ISO.format(start), ISO.format(stop), patientId);

        return executeStatsQuery(flux, "Heart Rate", "bpm");
    }

    // --- SPO2 ---
    public VitalSignsStats getSpO2Stats(Long patientId, Instant start, Instant stop) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: time(v: "%s"), stop: time(v: "%s"))
              |> filter(fn: (r) => r["_measurement"] == "spO2")
              |> filter(fn: (r) => r["patient"] == "%d")
              |> filter(fn: (r) => r["_field"] == "value")
            """, bucket, ISO.format(start), ISO.format(stop), patientId);

        return executeStatsQuery(flux, "SpO₂", "%");
    }

    // --- BLOOD PRESSURE ---
    public VitalSignsStats getBloodPressureStats(Long patientId, Instant start, Instant stop) {
        String fluxSystolic = String.format("""
            from(bucket: "%s")
              |> range(start: time(v: "%s"), stop: time(v: "%s"))
              |> filter(fn: (r) => r["_measurement"] == "tension")
              |> filter(fn: (r) => r["patient"] == "%d")
              |> filter(fn: (r) => r["_field"] == "systolique")
            """, bucket, ISO.format(start), ISO.format(stop), patientId);

        String fluxDiastolic = String.format("""
            from(bucket: "%s")
              |> range(start: time(v: "%s"), stop: time(v: "%s"))
              |> filter(fn: (r) => r["_measurement"] == "tension")
              |> filter(fn: (r) => r["patient"] == "%d")
              |> filter(fn: (r) => r["_field"] == "diastolique")
            """, bucket, ISO.format(start), ISO.format(stop), patientId);

        VitalSignsStats systolic = executeStatsQuery(fluxSystolic, "Systolic", "mmHg");
        VitalSignsStats diastolic = executeStatsQuery(fluxDiastolic, "Diastolic", "mmHg");

        VitalSignsStats combined = new VitalSignsStats();
        combined.setMetric("Blood Pressure");
        combined.setUnit("mmHg");

        combined.setAverageSystolic(systolic.getAverage());
        combined.setAverageDiastolic(diastolic.getAverage());
        combined.setMeasurementCount((systolic.getMeasurementCount() + diastolic.getMeasurementCount()) / 2);

        if (systolic.getAverage() != null && diastolic.getAverage() != null) {
            double map = diastolic.getAverage() + (systolic.getAverage() - diastolic.getAverage()) / 3.0;
            combined.setAverageMAP(map);
        }

        combined.setMin(Math.min(
                Optional.ofNullable(systolic.getMin()).orElse(0.0),
                Optional.ofNullable(diastolic.getMin()).orElse(0.0)
        ));
        combined.setMax(Math.max(
                Optional.ofNullable(systolic.getMax()).orElse(0.0),
                Optional.ofNullable(diastolic.getMax()).orElse(0.0)
        ));

        return combined;
    }

    // --- GLUCOSE ---
    public VitalSignsStats getGlucoseStats(Long patientId, Instant start, Instant stop) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: time(v: "%s"), stop: time(v: "%s"))
              |> filter(fn: (r) => r["_measurement"] == "glycemie")
              |> filter(fn: (r) => r["patient"] == "%d")
              |> filter(fn: (r) => r["_field"] == "value")
            """, bucket, ISO.format(start), ISO.format(stop), patientId);

        VitalSignsStats stats = executeStatsQuery(flux, "Blood Glucose", "mg/dL");

        // Time in range (TIR)
        try {
            String tirFlux = String.format("""
    from(bucket: "%s")
      |> range(start: time(v: "%s"), stop: time(v: "%s"))
      |> filter(fn: (r) => r["_measurement"] == "glycemie")
      |> filter(fn: (r) => r["patient"] == "%d")
      |> filter(fn: (r) => r["_field"] == "value")
      |> map(fn: (r) => ({
          r with inRange: if r._value >= 70.0 and r._value <= 180.0 then 1 else 0
      }))
      |> reduce(
          fn: (r, accumulator) => ({
              total: accumulator.total + 1,
              inRange: accumulator.inRange + r.inRange
          }),
          identity: {total: 0, inRange: 0}
      )
      |> map(fn: (r) => ({
          tir: if r.total > 0 then float(v: r.inRange) / float(v: r.total) * 100.0 else 0.0
      }))
""", bucket, ISO.format(start), ISO.format(stop), patientId);

            List<FluxTable> tables = influxDBClient.getQueryApi().query(tirFlux, org);
            if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
                Object tirVal = tables.get(0).getRecords().get(0).getValueByKey("tir");
                if (tirVal instanceof Number tir) {
                    stats.setTimeInRange(tir.doubleValue());
                }
            }
        } catch (Exception e) {
            System.err.println("Error calculating Time-In-Range: " + e.getMessage());
        }

        return stats;
    }

    // --- WEIGHT ---
    public VitalSignsStats getWeightStats(Long patientId, Instant start, Instant stop) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: time(v: "%s"), stop: time(v: "%s"))
              |> filter(fn: (r) => r["_measurement"] == "poids")
              |> filter(fn: (r) => r["patient"] == "%d")
              |> filter(fn: (r) => r["_field"] == "value")
            """, bucket, ISO.format(start), ISO.format(stop), patientId);

        return executeStatsQuery(flux, "Weight", "kg");
    }

    // --- CORE EXECUTION LOGIC ---
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
                    if (value instanceof Number val) {
                        values.add(val.doubleValue());
                    }
                }
            }

            if (values.isEmpty()) {
                stats.setMeasurementCount(0);
                stats.setAverage(0.0);
                stats.setMin(0.0);
                stats.setMax(0.0);
                return stats;
            }

            Collections.sort(values);
            stats.setMeasurementCount(values.size());
            stats.setMin(values.get(0));
            stats.setMax(values.get(values.size() - 1));
            stats.setAverage(values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
            stats.setMedian(getPercentile(values, 50));
            stats.setP10(getPercentile(values, 10));
            stats.setP90(getPercentile(values, 90));
            stats.setStandardDeviation(calculateStdDev(values));

        } catch (Exception e) {
            System.err.println(" Error executing query for " + metric + ": " + e.getMessage());
        }

        return stats;
    }

    private Double getPercentile(List<Double> values, int percentile) {
        if (values.isEmpty()) return 0.0;
        int index = (int) Math.ceil(percentile / 100.0 * values.size()) - 1;
        return values.get(Math.max(0, Math.min(index, values.size() - 1)));
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

    // --- COUNT ---
    public int countMeasurements(Long patientId, String measurement, Instant start, Instant stop) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: time(v: "%s"), stop: time(v: "%s"))
              |> filter(fn: (r) => r["_measurement"] == "%s")
              |> filter(fn: (r) => r["patient"] == "%d")
              |> count()
            """, bucket, ISO.format(start), ISO.format(stop), measurement, patientId);

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
