package com.sentinel.aml.detection.rules;

import com.sentinel.aml.detection.DetectionRule;
import com.sentinel.aml.detection.RuleConfigService;
import com.sentinel.aml.detection.RuleTrigger;
import com.sentinel.aml.domain.Transaction;
import com.sentinel.aml.domain.TransactionDirection;
import com.sentinel.aml.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Business rule 3: a deposit where >= 80% of its value is moved back out again within 48 hours
 * (layering). Evaluated on the outbound (DEBIT) leg, looking back for the triggering deposit.
 */
@Component
public class RapidMovementRule implements DetectionRule {

    public static final String RULE_CODE = "RAPID_MOVEMENT";

    private final RuleConfigService ruleConfigService;
    private final TransactionRepository transactionRepository;

    public RapidMovementRule(RuleConfigService ruleConfigService, TransactionRepository transactionRepository) {
        this.ruleConfigService = ruleConfigService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public Optional<RuleTrigger> evaluate(Transaction transaction) {
        if (transaction.getDirection() != TransactionDirection.DEBIT) {
            return Optional.empty();
        }

        int outflowPct = ruleConfigService.integer(RULE_CODE, "outflowPct", 80);
        int windowHours = ruleConfigService.integer(RULE_CODE, "windowHours", 48);
        Instant windowStart = transaction.getTxnTimestamp().minusSeconds(windowHours * 3600L);

        List<Transaction> inWindow =
                transactionRepository.findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        transaction.getAccount().getId(), windowStart, transaction.getTxnTimestamp());

        List<Transaction> deposits =
                inWindow.stream().filter(t -> t.getDirection() == TransactionDirection.CREDIT).toList();

        for (Transaction deposit : deposits) {
            BigDecimal outflowSince =
                    inWindow.stream()
                            .filter(t -> t.getDirection() == TransactionDirection.DEBIT)
                            .filter(t -> !t.getTxnTimestamp().isBefore(deposit.getTxnTimestamp()))
                            .map(Transaction::getAmountBaseCurrency)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal thresholdAmount =
                    deposit.getAmountBaseCurrency()
                            .multiply(BigDecimal.valueOf(outflowPct))
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if (outflowSince.compareTo(thresholdAmount) >= 0) {
                List<String> evidence = new ArrayList<>();
                evidence.add(deposit.getId().toString());
                inWindow.stream()
                        .filter(t -> t.getDirection() == TransactionDirection.DEBIT)
                        .filter(t -> !t.getTxnTimestamp().isBefore(deposit.getTxnTimestamp()))
                        .forEach(t -> evidence.add(t.getId().toString()));

                String explanation =
                        "Deposit of %s (base currency) on %s was %.1f%% withdrawn/transferred out within %dh (threshold %d%%) -- consistent with rapid movement / layering."
                                .formatted(
                                        deposit.getAmountBaseCurrency(),
                                        deposit.getTxnTimestamp(),
                                        outflowSince
                                                .divide(deposit.getAmountBaseCurrency(), 4, RoundingMode.HALF_UP)
                                                .multiply(BigDecimal.valueOf(100)),
                                        windowHours,
                                        outflowPct);

                return Optional.of(
                        new RuleTrigger(RULE_CODE, ruleConfigService.weight(RULE_CODE), explanation, evidence));
            }
        }

        return Optional.empty();
    }
}
