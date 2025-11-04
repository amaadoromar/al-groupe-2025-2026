package org.eSante.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eSante.interfaces.EmailService;
import org.eSante.interfaces.RealtimeNotificationService;
import org.eSante.model.EmailNotificationRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmailController.class)
public class EmailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private RealtimeNotificationService realtimeNotificationService;

    @Test
    void sendEmail_shouldReturnSuccess_whenValidRequest() throws Exception {
        EmailNotificationRequest request = new EmailNotificationRequest(
                "test@example.com",
                "Subject",
                "Message"
        );

        doNothing().when(emailService).sendSimpleMail(any());
        doNothing().when(realtimeNotificationService).sendSuccess(any(), any());

        mockMvc.perform(post("/api/notifications/email/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Mail sent successfully"));

        verify(emailService, times(1)).sendSimpleMail(any());
        verify(realtimeNotificationService, times(1)).sendSuccess(any(), any());
    }

    @Test
    void sendEmail_shouldReturnError_whenInvalidEmail() throws Exception {
        EmailNotificationRequest request = new EmailNotificationRequest(
                "invalid-email",
                "Subject",
                "Message"
        );

        mockMvc.perform(post("/api/notifications/email/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(emailService, never()).sendSimpleMail(any());
    }

    @Test
    void sendEmail_shouldReturnError_whenServiceFails() throws Exception {
        EmailNotificationRequest request = new EmailNotificationRequest(
                "test@example.com",
                "Subject",
                "Message"
        );

        doThrow(new Exception("SMTP error"))
                .when(emailService).sendSimpleMail(any());

        mockMvc.perform(post("/api/notifications/email/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void health_shouldReturnOK() throws Exception {
        mockMvc.perform(get("/api/notifications/email/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mail Notification Service is up and running"));
    }

}
