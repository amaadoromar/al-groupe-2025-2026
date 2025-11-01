package org.esante.monitoring.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.esante.monitoring.domain.MeasurementType;

import java.time.Instant;

public class MeasurementRequest {
    @NotBlank
    public String patientId;

    @NotNull
    public MeasurementType type;

    @NotNull
    public Double value;

    public Double value2; // optional, e.g., diastolic for BP

    public String unit;   // bpm, %, mmHg, mg/dL, kg

    public Instant measuredAt; // optional; defaults to now if null
}

