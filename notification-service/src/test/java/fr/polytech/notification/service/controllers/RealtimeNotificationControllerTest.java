package fr.polytech.notification.service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.polytech.notification.service.interfaces.RealtimeNotificationService;
import fr.polytech.notification.service.model.NotificationType;
import fr.polytech.notification.service.model.RealtimeNotification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RealtimeNotificationController.class)
public class RealtimeNotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RealtimeNotificationService notificationService;

    @Test
    void sendNotification_shouldReturnSuccess_whenValidRequest() throws Exception {
        RealtimeNotification notification = new RealtimeNotification(
                NotificationType.INFO,
                "Test Title",
                "Test Message"
        );

        doNothing().when(notificationService).sendNotification(any());
        mockMvc.perform(post("/api/notifications/realtime/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(notification)))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification sent successfully"));

        verify(notificationService, times(1)).sendNotification(any());
    }

    @Test
    void test_shouldSendInfoNotification() throws Exception {
        doNothing().when(notificationService).sendInfo(any(), any());

        mockMvc.perform(get("/api/notifications/realtime/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("Notification test sent"));

        verify(notificationService, times(1))
                .sendInfo("Test Notification", "This is a test realtime notification.");
    }

    @Test
    void health_shouldReturnOK() throws Exception {
        mockMvc.perform(get("/api/notifications/realtime/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Realtime Notification Service is up and running"));
    }

}
