package org.eSante.monitoring.api;

import org.eSante.monitoring.api.dto.MeasurementResponse;
import org.eSante.monitoring.service.MonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring/metrics")
@Validated
public class MonitoringMetricsController {
    private final MonitoringService monitoringService;

    public MonitoringMetricsController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping("/latest")
    public ResponseEntity<List<MeasurementResponse>> latest(@RequestParam String patientId) {
        var list = monitoringService.latestByType(patientId);
        return ResponseEntity.ok(list);
    }
}

