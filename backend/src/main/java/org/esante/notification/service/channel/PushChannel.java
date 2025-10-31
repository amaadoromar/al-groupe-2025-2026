package org.esante.notification.service.channel;

import org.esante.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PushChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(PushChannel.class);

    @Override
    public String name() { return "PUSH"; }

    @Override
    public boolean send(Notification notification) {
        // Stub provider: integrate push provider later (FCM/APNs/web push)
        log.info("[PUSH] to {}: {}", notification.getRecipientId(), notification.getTitle());
        return true;
    }
}

