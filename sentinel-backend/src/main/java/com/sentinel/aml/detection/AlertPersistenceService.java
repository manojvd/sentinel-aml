package com.sentinel.aml.detection;

import com.sentinel.aml.anomaly.RiskScoreCombiner.CompositeRiskResult;
import com.sentinel.aml.anomaly.dto.AnomalyScoreResult;
import com.sentinel.aml.common.AuditService;
import com.sentinel.aml.domain.Alert;
import com.sentinel.aml.domain.AlertEvidence;
import com.sentinel.aml.domain.AlertStatus;
import com.sentinel.aml.domain.Transaction;
import com.sentinel.aml.repository.AlertEvidenceRepository;
import com.sentinel.aml.repository.AlertRepository;
import com.sentinel.aml.repository.TransactionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The write side of alert upsert, split out from {@link DetectionEngine} so it runs in its own
 * {@code REQUIRES_NEW} transaction. This matters for correctness: Spring commits a
 * {@code @Transactional} method's work only when the *proxied* call returns to its caller. If the
 * find-or-create-alert logic lived in the same transaction as {@link DetectionEngine#evaluate},
 * the per-customer lock in that method would be released (in its {@code finally} block, before
 * the method returns) *before* the row was actually committed -- leaving a window where a second
 * thread, holding the same lock a moment later, still can't see the first thread's insert and
 * tries to create a duplicate. Calling this bean's {@code REQUIRES_NEW} method instead forces the
 * commit to happen before control returns to the lock's critical section.
 */
@Service
public class AlertPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(AlertPersistenceService.class);
    private static final String ANOMALY_RULE_CODE = "ANOMALY_ML";

    private final TransactionRepository transactionRepository;
    private final AlertRepository alertRepository;
    private final AlertEvidenceRepository alertEvidenceRepository;
    private final AuditService auditService;

    public AlertPersistenceService(
            TransactionRepository transactionRepository,
            AlertRepository alertRepository,
            AlertEvidenceRepository alertEvidenceRepository,
            AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.alertRepository = alertRepository;
        this.alertEvidenceRepository = alertEvidenceRepository;
        this.auditService = auditService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Alert upsertAndCommit(
            UUID transactionId, List<RuleTrigger> triggers, AnomalyScoreResult anomalyResult, CompositeRiskResult composite) {
        Transaction transaction =
                transactionRepository
                        .findById(transactionId)
                        .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionId));

        String dedupKey = "customer:" + transaction.getAccount().getCustomer().getId();

        Alert alert =
                alertRepository
                        .findByDedupKeyAndStatusIn(dedupKey, List.of(AlertStatus.OPEN, AlertStatus.IN_REVIEW))
                        .orElseGet(
                                () -> {
                                    Alert created = new Alert();
                                    created.setCustomer(transaction.getAccount().getCustomer());
                                    created.setAccount(transaction.getAccount());
                                    created.setDedupKey(dedupKey);
                                    created.setStatus(AlertStatus.OPEN);
                                    return created;
                                });

        boolean isNewAlert = alert.getId() == null;
        if (isNewAlert) {
            alert = alertRepository.save(alert);
        }

        for (RuleTrigger trigger : triggers) {
            AlertEvidence evidence = new AlertEvidence();
            evidence.setAlert(alert);
            evidence.setRuleCode(trigger.ruleCode());
            evidence.setRiskContribution(trigger.riskContribution());
            evidence.setExplanation(trigger.explanation());
            evidence.setEvidenceTransactionIds(trigger.evidenceTransactionIds());
            alertEvidenceRepository.save(evidence);
        }

        // The anomaly model always contributes an evidence row when we get this far (composite
        // score > 0 already gated whether we're here at all) -- covers the "no rule triggered but
        // the model flagged a novel pattern" case, and records the dampening/corroboration
        // rationale for the audit trail either way. See RiskScoreCombiner for the policy.
        AlertEvidence anomalyEvidence = new AlertEvidence();
        anomalyEvidence.setAlert(alert);
        anomalyEvidence.setRuleCode(ANOMALY_RULE_CODE);
        anomalyEvidence.setRiskContribution((int) Math.round(anomalyResult.score()));
        anomalyEvidence.setExplanation(composite.rationale());
        anomalyEvidence.setEvidenceTransactionIds(List.of(transaction.getId().toString()));
        alertEvidenceRepository.save(anomalyEvidence);

        // Ratchet up rather than overwrite -- a later, milder event shouldn't make an alert look
        // less urgent than a stronger one already on file; the full evidence log carries history.
        alert.setRiskScore(Math.max(alert.getRiskScore(), composite.score()));
        alert.setUpdatedAt(Instant.now());
        alert = alertRepository.save(alert);

        List<String> ruleCodes = triggers.stream().map(RuleTrigger::ruleCode).sorted().toList();
        auditService.record(
                "ALERT",
                alert.getId().toString(),
                isNewAlert ? "CREATED" : "EVIDENCE_ADDED",
                Map.of(
                        "triggeredRules", ruleCodes,
                        "anomalyScore", anomalyResult.score(),
                        "anomalyAlgorithm", anomalyResult.algorithm().name(),
                        "compositeRiskScore", composite.score(),
                        "transactionId", transaction.getId().toString()));

        log.info(
                "Alert {} {} for customer {} -- rules {} -- anomaly {} ({}) -- composite score {}",
                alert.getId(),
                isNewAlert ? "created" : "updated",
                transaction.getAccount().getCustomer().getCustomerId(),
                ruleCodes,
                anomalyResult.score(),
                anomalyResult.algorithm(),
                composite.score());

        return alert;
    }
}
