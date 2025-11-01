package org.eSante.notification.service.channel;

import org.eSante.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(EmailChannel.class);

    @Override
    public String name() { return "EMAIL"; }

    @Override
    public boolean send(Notification notification) {
        // Stub provider: integrate real SMTP/email provider later
        log.info("[EMAIL] to {}: {}", notification.getRecipientId(), notification.getTitle());
        return true;
    }
}

