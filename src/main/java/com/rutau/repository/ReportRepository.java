package com.rutau.repository;

import com.rutau.model.Report;
import com.rutau.model.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByStatus(ReportStatus status);

    // US19 - Reportes para el panel (los más recientes primero)
    List<Report> findAllByOrderByCreatedAtDesc();

    List<Report> findByStatusOrderByCreatedAtDesc(ReportStatus status);

    // US19 - Historial de reportes contra un usuario
    List<Report> findByReportedUserIdOrderByCreatedAtDesc(Long userId);

    long countByReportedUserIdAndStatus(Long userId, ReportStatus status);
}