package org.eSante.monitoring.repository;


import org.eSante.monitoring.domain.EventStatus;
import org.eSante.monitoring.domain.MonitoringEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MonitoringEventRepository extends JpaRepository<MonitoringEvent, UUID> {
    Page<MonitoringEvent> findByPatientIdOrderByCreatedAtDesc(String patientId, Pageable pageable);
    Page<MonitoringEvent> findByPatientIdAndStatusOrderByCreatedAtDesc(String patientId, EventStatus status, Pageable pageable);
}

