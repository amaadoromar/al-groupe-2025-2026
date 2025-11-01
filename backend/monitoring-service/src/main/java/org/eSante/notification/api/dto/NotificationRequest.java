package org.eSante.notification.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.eSante.notification.domain.NotificationChannelType;
import org.eSante.notification.domain.NotificationSeverity;

import java.util.List;


public class NotificationRequest {
    @NotBlank
    public String recipientId;

    @NotBlank
    @Size(max = 120)
    public String title;

    @NotBlank
    @Size(max = 2048)
    public String content;

    public NotificationSeverity severity = NotificationSeverity.INFO;

    @NotEmpty
    public List<NotificationChannelType> channels; // desired channels and order

    // optional idempotency key for upstream callers
    @Size(max = 64)
    public String correlationId;
}

