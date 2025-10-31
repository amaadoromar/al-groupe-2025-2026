package org.esante.notification.service.channel;

import org.esante.notification.domain.Notification;

public interface NotificationChannel {
    String name();
    boolean send(Notification notification) throws Exception;
}

