package com.sentinel.aml.detection;

import com.sentinel.aml.anomaly.AnomalyScoringService;
import com.sentinel.aml.anomaly.RiskScoreCombiner;
import com.sentinel.aml.anomaly.RiskScoreCombiner.CompositeRiskResult;
import com.sentinel.aml.anomaly.dto.AnomalyScoreResult;
import com.sentinel.aml.anomaly.dto.TransactionFeatures;
import com.sentinel.aml.domain.Alert;
import com.sentinel.aml.domain.Transaction;
import com.sentinel.aml.repository.TransactionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs every enabled {@link DetectionRule} against a transaction, blends the result with the ML
 * anomaly model via {@link RiskScoreCombiner} (see that class for the blending policy), and hands
 * off to {@link AlertPersistenceService} to upsert the customer's open alert with any new
 * evidence. One customer has at most one OPEN/IN_REVIEW alert at a time (dedup key = customer
 * id); repeated triggering events append evidence to it rather than spawning duplicates.
 *
 * <p>This method itself is read-only: fetching the transaction and running rules/anomaly scoring
 * needs an open Hibernate session for lazy {@code Account}/{@code Customer} associations, but the
 * actual write happens in {@link AlertPersistenceService}'s own {@code REQUIRES_NEW} transaction
 * -- see the javadoc there for why that split matters for concurrency correctness.
 */
@Service
public class DetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(DetectionEngine.class);
    private static final int MAX_RISK_SCORE = 100;

    private final List<DetectionRule> rules;
    private final RuleConfigService ruleConfigService;
    private final PerKeyLockService lockService;
    private final TransactionRepository transactionRepository;
    private final FeatureExtractionService featureExtractionService;
    private final AnomalyScoringService anomalyScoringService;
    private final AlertPersistenceService alertPersistenceService;

    public DetectionEngine(
            List<DetectionRule> rules,
            RuleConfigService ruleConfigService,
            PerKeyLockService lockService,
            TransactionRepository transactionRepository,
            FeatureExtractionService featureExtractionService,
            AnomalyScoringService anomalyScoringService,
            AlertPersistenceService alertPersistenceService) {
        this.rules = rules;
        this.ruleConfigService = ruleConfigService;
        this.lockService = lockService;
        this.transactionRepository = transactionRepository;
        this.featureExtractionService = featureExtractionService;
        this.anomalyScoringService = anomalyScoringService;
        this.alertPersistenceService = alertPersistenceService;
    }

    @Transactional(readOnly = true)
    public Optional<Alert> evaluate(UUID transactionId) {
        Transaction transaction =
                transactionRepository
                        .findById(transactionId)
                        .orElseThrow(() -> new IllegalStateException("Transaction not found: " + transactionId));

        List<RuleTrigger> triggers = new ArrayList<>();
        for (DetectionRule rule : rules) {
            if (!ruleConfigService.isEnabled(rule.ruleCode())) {
                continue;
            }
            try {
                rule.evaluate(transaction).ifPresent(triggers::add);
            } catch (Exception ex) {
                log.error("Detection rule {} failed on transaction {}", rule.ruleCode(), transaction.getId(), ex);
            }
        }

        int ruleScore =
                Math.min(MAX_RISK_SCORE, triggers.stream().mapToInt(RuleTrigger::riskContribution).sum());
        AnomalyScoreResult anomalyResult = scoreAnomaly(transaction);
        CompositeRiskResult composite = RiskScoreCombiner.combine(ruleScore, !triggers.isEmpty(), anomalyResult);

        if (composite.score() <= 0) {
            // Neither the rule engine nor the anomaly model found anything worth an analyst's time.
            return Optional.empty();
        }

        Long customerId = transaction.getAccount().getCustomer().getId();
        ReentrantLock lock = lockService.lockFor("alert-upsert:customer:" + customerId);
        lock.lock();
        try {
            // REQUIRES_NEW: commits before this call returns, so it's safe to unlock right after.
            return Optional.of(
                    alertPersistenceService.upsertAndCommit(transactionId, triggers, anomalyResult, composite));
        } finally {
            lock.unlock();
        }
    }

    private AnomalyScoreResult scoreAnomaly(Transaction transaction) {
        try {
            if (!anomalyScoringService.isTrained()) {
                return new AnomalyScoreResult(0, false, 0, anomalyScoringService.algorithm());
            }
            TransactionFeatures features = featureExtractionService.extract(transaction);
            return anomalyScoringService.score(features);
        } catch (Exception ex) {
            log.warn("Anomaly scoring failed for transaction {} -- falling back to rules only", transaction.getId(), ex);
            return new AnomalyScoreResult(0, false, 0, anomalyScoringService.algorithm());
        }
    }
}
