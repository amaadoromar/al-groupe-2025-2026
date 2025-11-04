package org.eSante.services;

import org.eSante.model.NotificationType;
import org.eSante.model.RealtimeNotification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RealtimeNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RealtimeNotificationServiceImpl notificationService;

    @Test
    void sendNotification_shouldSendToCorrectTopic() {
        RealtimeNotification notification = new RealtimeNotification(
                NotificationType.INFO,
                "Test Title",
                "Test Message"
        );

        notificationService.sendNotification(notification);

        verify(messagingTemplate, times(1))
                .convertAndSend(eq("/topic/notifications"), any(RealtimeNotification.class));
    }

    @Test
    void sendNotification_shouldSetTimestamp_whenNotProvided() {
        RealtimeNotification notification = new RealtimeNotification(
                NotificationType.SUCCESS,
                "Test",
                "Message"
        );

        notification.setTimestamp(null);

        ArgumentCaptor<RealtimeNotification> captor = ArgumentCaptor
                .forClass(RealtimeNotification.class);

        notificationService.sendNotification(notification);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/notifications"),
                captor.capture()
        );

        assertThat(captor.getValue().getTimestamp()).isNotNull();
    }

    @Test
    void sendInfo_shouldSendInfoNotification() {
        String title = "Info Title";
        String message = "Info Message";

        notificationService.sendInfo(title, message);

        ArgumentCaptor<RealtimeNotification> captor = ArgumentCaptor
                .forClass(RealtimeNotification.class);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/notifications"),
                captor.capture()
        );

        RealtimeNotification sentNotification = captor.getValue();
        assertThat(sentNotification.getType()).isEqualTo(NotificationType.INFO);
        assertThat(sentNotification.getTitle()).isEqualTo(title);
        assertThat(sentNotification.getMessage()).isEqualTo(message);
    }

    @Test
    void sendSuccess_shouldSendSuccessNotification() {
        String title = "Success";
        String message = "Operation successful";

        notificationService.sendSuccess(title, message);

        ArgumentCaptor<RealtimeNotification> captor = ArgumentCaptor
                .forClass(RealtimeNotification.class);

        verify(messagingTemplate).convertAndSend(
                eq("/topic/notifications"),
                captor.capture()
        );

        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.SUCCESS);
    }

}
