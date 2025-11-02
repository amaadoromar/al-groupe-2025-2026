package org.esante.monitoring.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esante.monitoring.api.dto.MeasurementResponse;
import org.esante.monitoring.domain.MeasurementType;
import org.esante.monitoring.service.MonitoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MonitoringMetricsController.class)
@org.springframework.context.annotation.Import(org.esante.common.api.GlobalExceptionHandler.class)
class MonitoringMetricsControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    MonitoringService monitoringService;

    @Test
    void latest_returnsListFromService() throws Exception {
        MeasurementResponse hr = new MeasurementResponse();
        hr.id = UUID.randomUUID();
        hr.patientId = "p-1";
        hr.type = MeasurementType.HEART_RATE;
        hr.value = 70.0;
        hr.unit = "bpm";
        // Avoid JavaTime serialization nuances in slice tests

        when(monitoringService.latestByType(eq("p-1"))).thenReturn(List.of(hr));

        mockMvc.perform(get("/api/monitoring/metrics/latest").param("patientId", "p-1").accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value("p-1"))
                .andExpect(jsonPath("$[0].type").value("HEART_RATE"))
                .andExpect(jsonPath("$[0].value").value(70.0));
    }
}
