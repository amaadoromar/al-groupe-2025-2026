package fr.polytech.notification.service.model;

import lombok.*;

import java.time.LocalDateTime;

@ToString
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeNotification {

    private NotificationType type;
    private String title;
    private String message;
    private LocalDateTime timestamp;

    public RealtimeNotification(NotificationType notificationType, String title, String message) {
        this.type = notificationType;
        this.title = title;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

}