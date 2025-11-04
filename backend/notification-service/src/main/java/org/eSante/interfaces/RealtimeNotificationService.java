package org.eSante.interfaces;

import org.eSante.model.RealtimeNotification;

public interface RealtimeNotificationService {

    void sendNotification(RealtimeNotification notification);
    void sendInfo(String title, String message);
    void sendSuccess(String title, String message);
    void sendWarning(String title, String message);
    void sendError(String title, String message);

}
