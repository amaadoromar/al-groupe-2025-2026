package org.eSante.monitoring.api;

import jakarta.validation.Valid;

import org.eSante.monitoring.api.dto.MeasurementRequest;
import org.eSante.monitoring.api.dto.MeasurementResponse;
import org.eSante.monitoring.service.MonitoringService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/monitoring/measurements")
@Validated
public class MonitoringIngestController {
    private final MonitoringService monitoringService;

    public MonitoringIngestController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @PostMapping
    public ResponseEntity<MeasurementResponse> ingest(@Valid @RequestBody MeasurementRequest request) {
        var resp = monitoringService.ingest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
}

