package org.esante.notification.service.channel;

import org.esante.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(SmsChannel.class);

    @Override
    public String name() { return "SMS"; }

    @Override
    public boolean send(Notification notification) {
        // Stub provider: integrate SMS gateway later (Twilio, OVH, etc.)
        log.info("[SMS] to {}: {}", notification.getRecipientId(), notification.getTitle());
        return true;
    }
}

