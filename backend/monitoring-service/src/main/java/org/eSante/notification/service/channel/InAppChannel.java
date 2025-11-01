package org.eSante.notification.service.channel;

import org.eSante.notification.domain.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InAppChannel implements NotificationChannel {
    private static final Logger log = LoggerFactory.getLogger(InAppChannel.class);

    @Override
    public String name() { return "IN_APP"; }

    @Override
    public boolean send(Notification notification) {
        // In-app channel relies on SSE broadcasting; send always succeeds here
        log.info("In-app notification prepared for recipient {}: {}", notification.getRecipientId(), notification.getTitle());
        return true;
    }
}

