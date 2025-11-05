package org.eSante.api;

import org.eSante.domain.models.dto.DashboardSummary;
import org.eSante.services.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/patient/{patientId}/summary")
    public ResponseEntity<DashboardSummary> getSummary(
            @PathVariable("patientId") Long patientId,
            @RequestParam(value = "minutes", defaultValue = "60") Long minutes) {
        if (minutes == null || minutes <= 0) minutes = 60L;
        DashboardSummary s = dashboardService.buildSummary(patientId, minutes);
        return ResponseEntity.ok(s);
    }
}

