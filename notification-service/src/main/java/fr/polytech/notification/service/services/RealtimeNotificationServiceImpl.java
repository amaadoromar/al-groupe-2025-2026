package fr.polytech.notification.service.services;

import fr.polytech.notification.service.interfaces.RealtimeNotificationService;
import fr.polytech.notification.service.model.NotificationType;
import fr.polytech.notification.service.model.RealtimeNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeNotificationServiceImpl implements RealtimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendNotification(RealtimeNotification notification) {
        try {
            if (notification.getTimestamp() == null) {
                notification.setTimestamp(LocalDateTime.now());
            }

            messagingTemplate.convertAndSend("/topic/notifications", notification);

            log.info("Realtime notification sent: {}", notification);
        } catch (Exception e) {
            log.error("Failed to send realtime notification", e);
        }
    }

    @Override
    public void sendInfo(String title, String message) {
        sendNotification(new RealtimeNotification(NotificationType.INFO, title, message));
    }

    @Override
    public void sendSuccess(String title, String message) {
        sendNotification(new RealtimeNotification(NotificationType.SUCCESS, title, message));
    }

    @Override
    public void sendWarning(String title, String message) {
        sendNotification(new RealtimeNotification(NotificationType.WARNING, title, message));
    }

    @Override
    public void sendError(String title, String message) {
        sendNotification(new RealtimeNotification(NotificationType.ERROR, title, message));
    }
}
