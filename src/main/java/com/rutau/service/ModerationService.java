package com.rutau.service;

import com.rutau.dto.request.ModerationRequestDTO;
import com.rutau.dto.request.ReportCreateDTO;
import com.rutau.dto.response.ReportResponseDTO;
import com.rutau.dto.response.UserModerationResponseDTO;
import com.rutau.exception.BusinessRuleException;
import com.rutau.exception.ConflictException;
import com.rutau.exception.ResourceNotFoundException;
import com.rutau.model.NotificationType;
import com.rutau.model.Report;
import com.rutau.model.ReportStatus;
import com.rutau.model.Trip;
import com.rutau.model.User;
import com.rutau.repository.ReportRepository;
import com.rutau.repository.TripRepository;
import com.rutau.repository.UserRepository;
import com.rutau.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ModerationService {

    // US19 - Escenario alternativo: con esta cantidad de reportes válidos la cuenta queda "En observación"
    private static final long VALID_REPORTS_FOR_OBSERVATION = 2;

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final TripRepository tripRepository;
    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    // ==================== US19 - Un usuario reporta un viaje o a otro usuario ====================

    @Transactional
    public ReportResponseDTO create(ReportCreateDTO dto) {
        User reporter = currentUser.get();
        if (dto.reportedUserId() == null && dto.tripId() == null) {
            throw new BusinessRuleException("report.target.required");
        }

        Trip trip = null;
        if (dto.tripId() != null) {
            trip = tripRepository.findById(dto.tripId())
                    .orElseThrow(() -> new ResourceNotFoundException("trip.not.found"));
        }

        // Si solo se reporta el viaje, el usuario reportado es su conductor
        User reported = dto.reportedUserId() != null
                ? userRepository.findById(dto.reportedUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("user.not.found"))
                : trip.getDriver();

        if (reported.getId().equals(reporter.getId())) {
            throw new BusinessRuleException("report.self");
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUser(reported);
        report.setReportedTrip(trip);
        report.setReason(dto.reason().trim());
        report.setStatus(ReportStatus.PENDING);
        return toResponse(reportRepository.save(report));
    }

    // ==================== US19 - Panel del administrador ====================

    // Lista de reportes; status es opcional (PENDING, RESOLVED o DISMISSED)
    @Transactional(readOnly = true)
    public List<ReportResponseDTO> list(ReportStatus status) {
        List<Report> reports = status == null
                ? reportRepository.findAllByOrderByCreatedAtDesc()
                : reportRepository.findByStatusOrderByCreatedAtDesc(status);
        return reports.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ReportResponseDTO getById(Long id) {
        return toResponse(findReport(id));
    }

    // US19 - Escenario exitoso: el administrador toma una acción sobre el reporte
    @Transactional
    public ReportResponseDTO moderate(Long id, ModerationRequestDTO dto) {
        Report report = findReport(id);

        // US19 - Escenario de error: el reporte ya fue resuelto o descartado
        if (report.getStatus() != ReportStatus.PENDING) {
            throw new ConflictException("report.already.handled");
        }

        User reported = report.getReportedUser();
        String notes = dto.notes() == null || dto.notes().isBlank() ? "" : ": " + dto.notes().trim();

        switch (dto.action()) {
            case WARNING -> {
                report.setStatus(ReportStatus.RESOLVED);
                report.setActionTaken("Advertencia" + notes);
                notificationService.notify(reported, NotificationType.ACCOUNT_WARNING,
                        "Recibiste una advertencia del administrador por un reporte" + notes);
            }
            case SUSPENSION -> {
                report.setStatus(ReportStatus.RESOLVED);
                report.setActionTaken("Suspensión de la cuenta" + notes);
                reported.setEnabled(false);   // ya no podrá iniciar sesión
                userRepository.save(reported);
                notificationService.notify(reported, NotificationType.ACCOUNT_SUSPENDED,
                        "Tu cuenta fue suspendida por el administrador" + notes);
            }
            case DISMISS -> {
                report.setStatus(ReportStatus.DISMISSED);
                report.setActionTaken("Reporte descartado" + notes);
            }
        }
        report.setResolvedAt(LocalDateTime.now());
        return toResponse(reportRepository.save(report));
    }

    // US19 - Escenario alternativo: historial de reportes de un usuario
    @Transactional(readOnly = true)
    public UserModerationResponseDTO userHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user.not.found"));
        List<ReportResponseDTO> reports = reportRepository.findByReportedUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).toList();
        long valid = reportRepository.countByReportedUserIdAndStatus(userId, ReportStatus.RESOLVED);
        return new UserModerationResponseDTO(user.getId(), user.getFullName(), user.getEmail(),
                accountStatus(user), (long) reports.size(), valid, reports);
    }

    // ==================== Métodos de apoyo ====================

    private Report findReport(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("report.not.found"));
    }

    private String accountStatus(User user) {
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            return "Suspendida";
        }
        long valid = reportRepository.countByReportedUserIdAndStatus(user.getId(), ReportStatus.RESOLVED);
        return valid >= VALID_REPORTS_FOR_OBSERVATION ? "En observación" : "Activa";
    }

    private ReportResponseDTO toResponse(Report r) {
        User reported = r.getReportedUser();
        Trip trip = r.getReportedTrip();
        return new ReportResponseDTO(r.getId(), r.getReporter().getId(), r.getReporter().getFullName(),
                reported != null ? reported.getId() : null,
                reported != null ? reported.getFullName() : null,
                trip != null ? trip.getId() : null,
                trip != null ? trip.getOrigin() + " → " + trip.getDestination() : null,
                r.getReason(), r.getStatus(), r.getActionTaken(), r.getCreatedAt(), r.getResolvedAt(),
                reported != null ? accountStatus(reported) : null);
    }
}
