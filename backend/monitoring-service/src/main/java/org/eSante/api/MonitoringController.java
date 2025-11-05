package org.eSante.api;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/monitoring")
public class MonitoringController {

    @Autowired
    private InfluxDBClient influx;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> m = new HashMap<>();
        try {
            influx.getQueryApi().query("buckets()", System.getProperty("influxdb.org", "eSanteIdb"));
            m.put("status", "ok");
        } catch (Exception e) {
            m.put("status", "error");
            m.put("error", e.getMessage());
        }
        m.put("time", ISO.format(Instant.now()));
        return m;
    }

    @GetMapping("/patient/{patientId}/series")
    public ResponseEntity<List<Map<String, Object>>> series(
            @PathVariable("patientId") Long patientId,
            @RequestParam("measurement") String measurement,
            @RequestParam(value = "field", defaultValue = "value") String field,
            @RequestParam(value = "start", defaultValue = "-60m") String start,
            @RequestParam(value = "every", defaultValue = "1m") String every,
            @RequestParam(value = "org", defaultValue = "eSanteIdb") String org
    ) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: %s)
              |> filter(fn: (r) => r["_measurement"] == "%s")
              |> filter(fn: (r) => r["patient"] == "%d")
              |> filter(fn: (r) => r["_field"] == "%s")
              |> aggregateWindow(every: %s, fn: mean, createEmpty: false)
              |> keep(columns: ["_time","_value"]) 
            """,
                System.getProperty("influxdb.bucket", "mesure_data"), start, measurement, patientId, field, every);

        List<Map<String, Object>> out = new ArrayList<>();
        try {
            List<FluxTable> tables = influx.getQueryApi().query(flux, org);
            for (FluxTable table : tables) {
                for (FluxRecord rec : table.getRecords()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("time", rec.getTime());
                    row.put("value", rec.getValueByKey("_value"));
                    out.add(row);
                }
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok(out);
    }
}

