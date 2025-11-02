package org.esante.monitoring.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esante.monitoring.api.dto.MeasurementResponse;
import org.esante.monitoring.domain.MeasurementType;
import org.esante.monitoring.service.MonitoringService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MonitoringIngestController.class)
@org.springframework.context.annotation.Import(org.esante.common.api.GlobalExceptionHandler.class)
class MonitoringIngestControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    MonitoringService monitoringService;

    @Test
    void ingest_returnsCreated_withResponseFromService() throws Exception {
        MeasurementResponse stub = new MeasurementResponse();
        stub.id = UUID.randomUUID();
        stub.patientId = "p-1";
        stub.type = MeasurementType.HEART_RATE;
        stub.value = 72.0;
        stub.unit = "bpm";
        // Avoid JavaTime serialization nuances in slice tests
        when(monitoringService.ingest(ArgumentMatchers.any())).thenReturn(stub);

        String body = "{\n" +
                "  \"patientId\": \"p-1\",\n" +
                "  \"type\": \"HEART_RATE\",\n" +
                "  \"value\": 72.0,\n" +
                "  \"unit\": \"bpm\"\n" +
                "}";

        mockMvc.perform(post("/api/monitoring/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(stub.id.toString()))
                .andExpect(jsonPath("$.patientId").value("p-1"))
                .andExpect(jsonPath("$.type").value("HEART_RATE"))
                .andExpect(jsonPath("$.value").value(72.0));
    }

    @Test
    void ingest_validationError_returnsBadRequest() throws Exception {
        // Missing required fields -> 400 Bad Request from validation
        String body = "{ }";

        mockMvc.perform(post("/api/monitoring/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingest_malformedJson_returnsBadRequest() throws Exception {
        String malformed = "\"unit\":\"bpm\",\"patientId\":\"p-1\""; // missing braces

        mockMvc.perform(post("/api/monitoring/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(malformed))
                .andExpect(status().isBadRequest());
    }
}
