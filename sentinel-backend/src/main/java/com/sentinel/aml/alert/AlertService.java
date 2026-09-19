package com.sentinel.aml.alert;

import com.sentinel.aml.common.AuditService;
import com.sentinel.aml.common.CurrentActor;
import com.sentinel.aml.common.NotFoundException;
import com.sentinel.aml.common.PiiMasker;
import com.sentinel.aml.common.ValidationFailedException;
import com.sentinel.aml.domain.Alert;
import com.sentinel.aml.domain.AlertEvidence;
import com.sentinel.aml.domain.AlertStatus;
import com.sentinel.aml.dto.AlertDetailDto;
import com.sentinel.aml.dto.AlertEvidenceDto;
import com.sentinel.aml.dto.AlertSummaryDto;
import com.sentinel.aml.repository.AlertEvidenceRepository;
import com.sentinel.aml.repository.AlertRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertEvidenceRepository alertEvidenceRepository;
    private final AuditService auditService;

    public AlertService(
            AlertRepository alertRepository, AlertEvidenceRepository alertEvidenceRepository, AuditService auditService) {
        this.alertRepository = alertRepository;
        this.alertEvidenceRepository = alertEvidenceRepository;
        this.auditService = auditService;
    }

    /** Alerts are always returned sorted by risk score first -- business rule 7. */
    public Page<AlertSummaryDto> list(AlertStatus status, Pageable pageable) {
        Page<Alert> page =
                status == null
                        ? alertRepository.findAllByOrderByRiskScoreDescCreatedAtDesc(pageable)
                        : alertRepository.findByStatusOrderByRiskScoreDescCreatedAtDesc(status, pageable);
        return page.map(this::toSummary);
    }

    public AlertDetailDto get(UUID id) {
        Alert alert = findEntity(id);
        List<AlertEvidenceDto> evidence =
                alertEvidenceRepository.findByAlertOrderByCreatedAtAsc(alert).stream().map(this::toEvidenceDto).toList();
        return toDetail(alert, evidence);
    }

    @Transactional
    public AlertDetailDto disposition(UUID id, DispositionRequest request) {
        Alert alert = findEntity(id);
        AlertStatus newStatus =
                switch (request.action()) {
                    case START_REVIEW -> AlertStatus.IN_REVIEW;
                    case CLEAR -> AlertStatus.CLEARED;
                    case ESCALATE -> AlertStatus.ESCALATED;
                };

        if (alert.getStatus() == AlertStatus.CLEARED || alert.getStatus() == AlertStatus.ESCALATED) {
            throw new ValidationFailedException(
                    "Alert already dispositioned", List.of("status: alert is already " + alert.getStatus()));
        }

        CurrentActor actor = CurrentActor.fromSecurityContext();
        alert.setStatus(newStatus);
        alert.setDispositionReason(request.reason());
        alert.setDispositionActor(actor.username());
        alert.setDispositionedAt(Instant.now());
        alert.setUpdatedAt(Instant.now());
        alert = alertRepository.save(alert);

        // Business rule 6: never delete -- the row stays, with reason and analyst identity, and
        // this transition is written to the immutable audit log.
        auditService.record(
                "ALERT",
                alert.getId().toString(),
                "DISPOSITIONED_" + newStatus,
                Map.of("reason", request.reason(), "actor", actor.username()));

        List<AlertEvidenceDto> evidence =
                alertEvidenceRepository.findByAlertOrderByCreatedAtAsc(alert).stream().map(this::toEvidenceDto).toList();
        return toDetail(alert, evidence);
    }

    private Alert findEntity(UUID id) {
        return alertRepository.findById(id).orElseThrow(() -> new NotFoundException("No such alert: " + id));
    }

    private AlertSummaryDto toSummary(Alert alert) {
        return new AlertSummaryDto(
                alert.getId(),
                alert.getCustomer().getCustomerId(),
                displayName(alert),
                alert.getAccount() == null ? null : alert.getAccount().getAccountId(),
                alert.getRiskScore(),
                alert.getStatus().name(),
                alert.getCreatedAt(),
                alert.getUpdatedAt());
    }

    private AlertDetailDto toDetail(Alert alert, List<AlertEvidenceDto> evidence) {
        return new AlertDetailDto(
                alert.getId(),
                alert.getCustomer().getCustomerId(),
                displayName(alert),
                alert.getAccount() == null ? null : alert.getAccount().getAccountId(),
                alert.getRiskScore(),
                alert.getStatus().name(),
                alert.getDispositionReason(),
                alert.getDispositionActor(),
                alert.getDispositionedAt(),
                evidence,
                alert.getCreatedAt(),
                alert.getUpdatedAt());
    }

    private AlertEvidenceDto toEvidenceDto(AlertEvidence evidence) {
        return new AlertEvidenceDto(
                evidence.getRuleCode(),
                evidence.getRiskContribution(),
                evidence.getExplanation(),
                evidence.getEvidenceTransactionIds(),
                evidence.getCreatedAt());
    }

    private String displayName(Alert alert) {
        return PiiMasker.maskName(alert.getCustomer().getFirstName())
                + " "
                + PiiMasker.maskName(alert.getCustomer().getLastName());
    }
}
