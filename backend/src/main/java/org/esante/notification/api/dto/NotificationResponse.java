package org.esante.notification.api.dto;

import org.esante.notification.domain.NotificationSeverity;
import org.esante.notification.domain.NotificationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class NotificationResponse {
    public UUID id;
    public String recipientId;
    public String title;
    public String content;
    public NotificationSeverity severity;
    public NotificationStatus status;
    public Instant createdAt;
    public Instant sentAt;
    public Instant readAt;
    public String correlationId;
    public List<DeliveryDTO> deliveries;
}

