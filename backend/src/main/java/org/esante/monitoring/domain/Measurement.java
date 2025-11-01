package org.esante.monitoring.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "measurements", indexes = {
        @Index(name = "idx_measurements_patient", columnList = "patient_id"),
        @Index(name = "idx_measurements_type", columnList = "type"),
        @Index(name = "idx_measurements_measured_at", columnList = "measured_at")
})
public class Measurement {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private MeasurementType type;

    @Column(nullable = false)
    private double value; // e.g., HR, SpO2, Glucose, Systolic for BP

    private Double value2; // optional, e.g., Diastolic for BP

    @Column(length = 16)
    private String unit; // bpm, %, mmHg, mg/dL, kg

    @Column(nullable = false)
    private Instant measuredAt = Instant.now();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public MeasurementType getType() { return type; }
    public void setType(MeasurementType type) { this.type = type; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public Double getValue2() { return value2; }
    public void setValue2(Double value2) { this.value2 = value2; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Instant getMeasuredAt() { return measuredAt; }
    public void setMeasuredAt(Instant measuredAt) { this.measuredAt = measuredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

