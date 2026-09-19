package com.sentinel.aml.detection.rules;

import com.sentinel.aml.detection.DetectionRule;
import com.sentinel.aml.detection.RuleConfigService;
import com.sentinel.aml.detection.RuleTrigger;
import com.sentinel.aml.domain.Transaction;
import com.sentinel.aml.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Extra typology: repeated suspiciously round-number transactions on the same account. */
@Component
public class RoundNumberPatternRule implements DetectionRule {

    public static final String RULE_CODE = "ROUND_NUMBER";

    private final RuleConfigService ruleConfigService;
    private final TransactionRepository transactionRepository;

    public RoundNumberPatternRule(
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
        BigDecimal roundToNearest = ruleConfigService.bigDecimal(RULE_CODE, "roundToNearest", new BigDecimal("1000"));
        int windowHours = ruleConfigService.integer(RULE_CODE, "windowHours", 24);
        int minCount = ruleConfigService.integer(RULE_CODE, "minCount", 3);

        if (!isRoundAmount(transaction.getAmount(), roundToNearest)) {
            return Optional.empty();
        }

        Instant windowStart = transaction.getTxnTimestamp().minusSeconds(windowHours * 3600L);
        List<Transaction> inWindow =
                transactionRepository.findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        transaction.getAccount().getId(), windowStart, transaction.getTxnTimestamp());

        List<Transaction> matching = inWindow.stream().filter(t -> isRoundAmount(t.getAmount(), roundToNearest)).toList();

        if (matching.size() < minCount) {
            return Optional.empty();
        }

        String explanation =
                "%d round-number transactions (multiples of %s) on account %s within %dh -- an uncommon pattern for organic activity."
                        .formatted(matching.size(), roundToNearest, transaction.getAccount().getAccountId(), windowHours);
        List<String> evidence = matching.stream().map(t -> t.getId().toString()).toList();

        return Optional.of(new RuleTrigger(RULE_CODE, ruleConfigService.weight(RULE_CODE), explanation, evidence));
    }

    private boolean isRoundAmount(BigDecimal amount, BigDecimal roundToNearest) {
        if (roundToNearest.signum() <= 0) {
            return false;
        }
        return amount.remainder(roundToNearest).signum() == 0;
    }
}
