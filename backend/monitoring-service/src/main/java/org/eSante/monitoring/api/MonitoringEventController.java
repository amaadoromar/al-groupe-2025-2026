package org.eSante.monitoring.api;

import org.eSante.monitoring.api.dto.EventResponse;

import org.eSante.monitoring.domain.EventStatus;
import org.eSante.monitoring.service.MonitoringService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitoring/events")
@Validated
public class MonitoringEventController {
    private final MonitoringService monitoringService;

    public MonitoringEventController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    @GetMapping
    public ResponseEntity<List<EventResponse>> list(@RequestParam String patientId,
                                                    @RequestParam(required = false) EventStatus status,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        var list = monitoringService.listEvents(patientId, status, page, size);
        return ResponseEntity.ok(list);
    }

    @PatchMapping("/{id}/ack")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ack(@PathVariable UUID id) {
        monitoringService.acknowledgeEvent(id);
    }

    @PatchMapping("/{id}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolve(@PathVariable UUID id) {
        monitoringService.resolveEvent(id);
    }
}

