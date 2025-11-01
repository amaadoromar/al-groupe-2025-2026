package org.esante.monitoring.domain;

import jakarta.persistence.*;
import org.esante.notification.domain.NotificationSeverity;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monitoring_events", indexes = {
        @Index(name = "idx_events_patient", columnList = "patient_id"),
        @Index(name = "idx_events_status", columnList = "status"),
        @Index(name = "idx_events_created_at", columnList = "created_at")
})
public class MonitoringEvent {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MeasurementType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventStatus status = EventStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationSeverity severity = NotificationSeverity.INFO;

    @Column(nullable = false, length = 512)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "measurement_id")
    private Measurement measurement;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant resolvedAt;

    public UUID getId() { return id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public MeasurementType getType() { return type; }
    public void setType(MeasurementType type) { this.type = type; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public NotificationSeverity getSeverity() { return severity; }
    public void setSeverity(NotificationSeverity severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Measurement getMeasurement() { return measurement; }
    public void setMeasurement(Measurement measurement) { this.measurement = measurement; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}

