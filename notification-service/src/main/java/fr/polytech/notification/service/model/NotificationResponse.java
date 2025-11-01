package fr.polytech.notification.service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
public class NotificationResponse {

    private final boolean success;
    private final String message;

    public static NotificationResponse success(String message) {
        return NotificationResponse.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static NotificationResponse error(String message) {
        return NotificationResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

}
