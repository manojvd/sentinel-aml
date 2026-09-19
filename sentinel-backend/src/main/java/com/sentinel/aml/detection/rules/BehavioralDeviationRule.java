package com.sentinel.aml.detection.rules;

import com.sentinel.aml.detection.DetectionRule;
import com.sentinel.aml.detection.RuleConfigService;
import com.sentinel.aml.detection.RuleTrigger;
import com.sentinel.aml.domain.Transaction;
import com.sentinel.aml.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Business rule 5: today's customer volume/value exceeds N x their 90-day rolling daily average. */
@Component
public class BehavioralDeviationRule implements DetectionRule {

    public static final String RULE_CODE = "BEHAVIORAL_DEVIATION";

    private final RuleConfigService ruleConfigService;
    private final TransactionRepository transactionRepository;

    public BehavioralDeviationRule(
            RuleConfigService ruleConfigService, TransactionRepository transactionRepository) {
        this.ruleConfigService = ruleConfigService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public Optional<RuleTrigger> evaluate(Transaction transaction) {
        int deviationMultiplier = ruleConfigService.integer(RULE_CODE, "deviationMultiplier", 3);
        int baselineDays = ruleConfigService.integer(RULE_CODE, "baselineDays", 90);

        Instant dayStart = transaction.getTxnTimestamp().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant dayEnd = dayStart.plusSeconds(86400);
        Instant baselineStart = dayStart.minusSeconds(baselineDays * 86400L);

        Long customerId = transaction.getAccount().getCustomer().getId();
        List<Transaction> history =
                transactionRepository.findByAccount_Customer_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        customerId, baselineStart, dayEnd);

        BigDecimal today =
                history.stream()
                        .filter(t -> !t.getTxnTimestamp().isBefore(dayStart))
                        .map(Transaction::getAmountBaseCurrency)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal baselineTotal =
                history.stream()
                        .filter(t -> t.getTxnTimestamp().isBefore(dayStart))
                        .map(Transaction::getAmountBaseCurrency)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgDaily = baselineTotal.divide(BigDecimal.valueOf(baselineDays), 2, RoundingMode.HALF_UP);
        if (avgDaily.signum() <= 0) {
            // No baseline history yet -- nothing to compare against, avoid false positives on day one.
            return Optional.empty();
        }

        BigDecimal thresholdAmount = avgDaily.multiply(BigDecimal.valueOf(deviationMultiplier));
        if (today.compareTo(thresholdAmount) < 0) {
            return Optional.empty();
        }

        List<String> evidence =
                history.stream()
                        .filter(t -> !t.getTxnTimestamp().isBefore(dayStart))
                        .map(t -> t.getId().toString())
                        .toList();

        String explanation =
                "Customer's transaction value today (%s base currency) is %.1fx their %d-day rolling daily average (%s), exceeding the %dx deviation threshold."
                        .formatted(
                                today,
                                today.divide(avgDaily, 2, RoundingMode.HALF_UP).doubleValue(),
                                baselineDays,
                                avgDaily,
                                deviationMultiplier);

        return Optional.of(new RuleTrigger(RULE_CODE, ruleConfigService.weight(RULE_CODE), explanation, evidence));
    }
}
