package org.eSante.notification.service.channel;


import org.eSante.notification.domain.Notification;

public interface NotificationChannel {
    String name();
    boolean send(Notification notification) throws Exception;
}

