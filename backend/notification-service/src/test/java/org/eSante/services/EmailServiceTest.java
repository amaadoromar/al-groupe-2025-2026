package org.eSante.services;

import org.eSante.interfaces.EmailService;
import org.eSante.model.EmailNotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private EmailNotificationRequest request;

    @BeforeEach
    void setUp() {
        request = new EmailNotificationRequest(
                "test@example.com",
                "Test Subject",
                "Test Message"
        );
    }

    @Test
    void sendSimpleEmail_shouldSendEmail_whenValidRequest() throws Exception {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendSimpleMail(request);

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendSimpleEmail_shouldThrowException_whenMailSenderFails() throws Exception {
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendSimpleMail(request))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Failed to send email: SMTP error");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

}
