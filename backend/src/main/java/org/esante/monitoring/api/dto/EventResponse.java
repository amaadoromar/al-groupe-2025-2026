package org.esante.monitoring.api.dto;

import org.esante.monitoring.domain.EventStatus;
import org.esante.monitoring.domain.MeasurementType;
import org.esante.notification.domain.NotificationSeverity;

import java.time.Instant;
import java.util.UUID;

public class EventResponse {
    public UUID id;
    public String patientId;
    public MeasurementType type;
    public EventStatus status;
    public NotificationSeverity severity;
    public String message;
    public UUID measurementId;
    public Instant createdAt;
    public Instant resolvedAt;
}

