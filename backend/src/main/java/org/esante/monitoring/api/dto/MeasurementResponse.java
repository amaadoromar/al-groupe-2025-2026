package org.esante.monitoring.api.dto;

import org.esante.monitoring.domain.MeasurementType;
import org.esante.notification.domain.NotificationSeverity;

import java.time.Instant;
import java.util.UUID;

public class MeasurementResponse {
    public UUID id;
    public String patientId;
    public MeasurementType type;
    public double value;
    public Double value2;
    public String unit;
    public Instant measuredAt;
    public Instant createdAt;

    // Event info if generated
    public UUID eventId;
    public NotificationSeverity eventSeverity;
    public String eventMessage;
}

