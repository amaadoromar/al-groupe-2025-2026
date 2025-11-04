package org.eSante.repositories;

import org.eSante.domain.models.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByPatientIdOrderByReportDateDesc(Long patientId);

    List<Report> findByPatientIdAndReportType(Long patientId, String reportType);
}
