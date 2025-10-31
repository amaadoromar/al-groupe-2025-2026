package org.esante.notification.api.dto;

import org.esante.notification.domain.DeliveryStatus;
import org.esante.notification.domain.NotificationChannelType;

import java.time.Instant;
import java.util.UUID;

public class DeliveryDTO {
    public UUID id;
    public NotificationChannelType channel;
    public DeliveryStatus status;
    public int attempts;
    public String lastError;
    public Instant sentAt;
}

