package org.eSante.monitoring.repository;


import org.eSante.monitoring.domain.Measurement;
import org.eSante.monitoring.domain.MeasurementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MeasurementRepository extends JpaRepository<Measurement, UUID> {
    Page<Measurement> findByPatientIdOrderByMeasuredAtDesc(String patientId, Pageable pageable);
    Page<Measurement> findByPatientIdAndTypeOrderByMeasuredAtDesc(String patientId, MeasurementType type, Pageable pageable);
    Optional<Measurement> findTop1ByPatientIdAndTypeOrderByMeasuredAtDesc(String patientId, MeasurementType type);
}

