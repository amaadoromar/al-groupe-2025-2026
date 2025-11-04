package fr.polytech.notification.service.interfaces;

import fr.polytech.notification.service.model.RealtimeNotification;

public interface RealtimeNotificationService {

    void sendNotification(RealtimeNotification notification);
    void sendInfo(String title, String message);
    void sendSuccess(String title, String message);
    void sendWarning(String title, String message);
    void sendError(String title, String message);

}
