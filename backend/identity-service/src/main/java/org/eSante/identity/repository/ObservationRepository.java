package org.eSante.identity.repository;

import org.eSante.identity.domain.Observation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ObservationRepository extends JpaRepository<Observation, Integer> {
    @Query("select o from Observation o join fetch o.author where o.patient.id = :pid order by o.createdAt desc")
    List<Observation> findByPatientId(@Param("pid") Integer patientId);
}

