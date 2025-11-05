package org.eSante.services;

import org.eSante.domain.models.dto.AlertSummary;
import org.eSante.domain.models.dto.DashboardPoint;
import org.eSante.domain.models.dto.DashboardSummary;
import org.eSante.repositories.InfluxDBRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private InfluxDBRepository influx;

    @Autowired
    private DataAggregationService dataAgg;

    public DashboardSummary buildSummary(Long patientId, long minutes) {
        Instant stop = Instant.now();
        Instant start = stop.minus(minutes, ChronoUnit.MINUTES);

        DashboardSummary s = new DashboardSummary();
        s.setPatientId(patientId);

        // Latest values
        s.setHeartRate(influx.getLatestValue(patientId, "fc", "value"));
        s.setSpo2(influx.getLatestValue(patientId, "spO2", "value"));
        s.setGlucose(influx.getLatestValue(patientId, "glycemie", "value"));
        s.setWeight(influx.getLatestValue(patientId, "poids", "value"));
        s.setBpSystolic(influx.getLatestValue(patientId, "tension", "systolique"));
        s.setBpDiastolic(influx.getLatestValue(patientId, "tension", "diastolique"));
        s.setSteps(influx.getLatestValue(patientId, "STEPS", "value"));

        // Series (for charts)
        s.setSeriesHeartRate(influx.getSeriesMean(patientId, "fc", "value", start, stop, "1m"));
        s.setSeriesSpO2(influx.getSeriesMean(patientId, "spO2", "value", start, stop, "1m"));
        s.setSeriesBloodPressureSys(influx.getSeriesMean(patientId, "tension", "systolique", start, stop, "5m"));
        s.setSeriesBloodPressureDia(influx.getSeriesMean(patientId, "tension", "diastolique", start, stop, "5m"));
        s.setSeriesGlucose(influx.getSeriesMean(patientId, "glycemie", "value", start, stop, "10m"));
        s.setSeriesWeight(influx.getSeriesMean(patientId, "poids", "value", start, stop, "1h"));

        // Recent alerts from SQL
        List<AlertSummary> alerts = dataAgg.fetchAlerts(patientId, start, stop);
        s.setRecentAlerts(alerts);
        s.setAlertCount(alerts != null ? alerts.size() : 0);
        return s;
    }
}
