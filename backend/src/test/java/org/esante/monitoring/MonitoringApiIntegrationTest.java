package org.esante.monitoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esante.monitoring.api.dto.MeasurementResponse;
import org.esante.monitoring.domain.EventStatus;
import org.esante.monitoring.domain.MeasurementType;
import org.esante.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MonitoringApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    NotificationRepository notificationRepository;

    private String patientId;

    @BeforeEach
    void setup() {
        patientId = "patient-42";
    }

    @Test
    void endToEnd_ingest_events_ack_resolve_metrics() throws Exception {
        // 1) Ingest an in-range heart rate: no event
        MeasurementResponse hr1 = ingest(patientId, MeasurementType.HEART_RATE, 75.0, null, "bpm", Instant.parse("2025-01-01T10:00:00Z"));
        assertThat(hr1.id).isNotNull();
        assertThat(hr1.eventId).isNull();

        // 2) Ingest a low SpO2 (ALERT) -> event + notification
        MeasurementResponse spo2 = ingest(patientId, MeasurementType.SPO2, 90.0, null, "%", Instant.parse("2025-01-01T10:01:00Z"));
        assertThat(spo2.eventId).isNotNull();
        UUID eventId = spo2.eventId;

        // events list contains the OPEN event
        String listJson = mockMvc.perform(get("/api/monitoring/events")
                        .param("patientId", patientId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var events = mapper.readTree(listJson);
        assertThat(events.isArray()).isTrue();
        assertThat(events.toString()).contains(eventId.toString());

        // notification created with correlationId = "mon-" + eventId
        assertThat(notificationRepository.findByCorrelationId("mon-" + eventId)).isPresent();

        // 3) ACK then RESOLVE
        mockMvc.perform(patch("/api/monitoring/events/" + eventId + "/ack"))
                .andExpect(status().isNoContent());

        String ackList = mockMvc.perform(get("/api/monitoring/events")
                        .param("patientId", patientId)
                        .param("status", EventStatus.ACKNOWLEDGED.name()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(ackList).contains(eventId.toString());

        mockMvc.perform(patch("/api/monitoring/events/" + eventId + "/resolve"))
                .andExpect(status().isNoContent());

        String resList = mockMvc.perform(get("/api/monitoring/events")
                        .param("patientId", patientId)
                        .param("status", EventStatus.RESOLVED.name()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(resList).contains(eventId.toString());

        // 4) Ingest CRITICAL blood pressure
        MeasurementResponse bp = ingest(patientId, MeasurementType.BLOOD_PRESSURE, 182.0, 112.0, "mmHg", Instant.parse("2025-01-01T10:02:00Z"));
        assertThat(bp.eventId).isNotNull();

        // 5) Latest metrics should contain latest per type (at least HR and SPO2 and BP)
        String latestJson = mockMvc.perform(get("/api/monitoring/metrics/latest")
                        .param("patientId", patientId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var latestArr = mapper.readTree(latestJson);
        assertThat(latestArr.isArray()).isTrue();
        String latestStr = latestArr.toString();
        assertThat(latestStr).contains("\"type\":\"HEART_RATE\"");
        assertThat(latestStr).contains("\"type\":\"SPO2\"");
        assertThat(latestStr).contains("\"type\":\"BLOOD_PRESSURE\"");
    }

    private MeasurementResponse ingest(String patientId, MeasurementType type, Double v1, Double v2, String unit, Instant measuredAt) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("patientId", patientId);
        body.put("type", type.name());
        body.put("value", v1);
        if (v2 != null) body.put("value2", v2);
        if (unit != null) body.put("unit", unit);
        if (measuredAt != null) body.put("measuredAt", measuredAt.toString());

        String resp = mockMvc.perform(post("/api/monitoring/measurements")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return mapper.readValue(resp, MeasurementResponse.class);
    }
}

