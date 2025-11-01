package org.eSante.notification.api.dto;
import org.eSante.notification.domain.NotificationSeverity;
import org.eSante.notification.domain.NotificationStatus;

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

