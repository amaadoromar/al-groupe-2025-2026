package org.esante.monitoring.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esante.monitoring.api.dto.EventResponse;
import org.esante.monitoring.domain.EventStatus;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MonitoringEventController.class)
@org.springframework.context.annotation.Import(org.esante.common.api.GlobalExceptionHandler.class)
class MonitoringEventControllerWebMvcTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @MockBean
    MonitoringService monitoringService;

    @Test
    void list_returnsEventsFromService() throws Exception {
        EventResponse e = new EventResponse();
        e.id = UUID.randomUUID();
        e.patientId = "p-1";
        e.type = MeasurementType.SPO2;
        e.status = EventStatus.OPEN;
        e.message = "SpO2 low";
        // Avoid JavaTime serialization nuances in slice tests
        when(monitoringService.listEvents(eq("p-1"), isNull(), anyInt(), anyInt())).thenReturn(List.of(e));

        mockMvc.perform(get("/api/monitoring/events").param("patientId", "p-1").accept(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(e.id.toString()))
                .andExpect(jsonPath("$[0].patientId").value("p-1"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void ack_and_resolve_callService_andReturnNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/api/monitoring/events/" + id + "/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(monitoringService, times(1)).acknowledgeEvent(eq(id));

        mockMvc.perform(patch("/api/monitoring/events/" + id + "/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
        verify(monitoringService, times(1)).resolveEvent(eq(id));
    }
}
