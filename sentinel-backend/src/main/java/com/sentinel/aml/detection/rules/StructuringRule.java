package com.sentinel.aml.detection.rules;

import com.sentinel.aml.detection.DetectionRule;
import com.sentinel.aml.detection.RuleConfigService;
import com.sentinel.aml.detection.RuleTrigger;
import com.sentinel.aml.domain.Transaction;
import com.sentinel.aml.repository.TransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Business rule 2: 3+ transactions on the same account, in a 24h window, each just under the CTR threshold. */
@Component
public class StructuringRule implements DetectionRule {

    public static final String RULE_CODE = "STRUCTURING";

    private final RuleConfigService ruleConfigService;
    private final TransactionRepository transactionRepository;

    public StructuringRule(RuleConfigService ruleConfigService, TransactionRepository transactionRepository) {
        this.ruleConfigService = ruleConfigService;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public Optional<RuleTrigger> evaluate(Transaction transaction) {
        BigDecimal minAmount = ruleConfigService.bigDecimal(RULE_CODE, "minAmountBase", new BigDecimal("9000"));
        BigDecimal maxAmount = ruleConfigService.bigDecimal(RULE_CODE, "maxAmountBase", new BigDecimal("9999"));
        int windowHours = ruleConfigService.integer(RULE_CODE, "windowHours", 24);
        int minCount = ruleConfigService.integer(RULE_CODE, "minCount", 3);

        BigDecimal amount = transaction.getAmountBaseCurrency();
        if (amount.compareTo(minAmount) < 0 || amount.compareTo(maxAmount) > 0) {
            return Optional.empty();
        }

        var windowStart = transaction.getTxnTimestamp().minusSeconds(windowHours * 3600L);
        List<Transaction> inWindow =
                transactionRepository.findByAccount_IdAndTxnTimestampBetweenOrderByTxnTimestampAsc(
                        transaction.getAccount().getId(), windowStart, transaction.getTxnTimestamp());

        List<Transaction> matching =
                inWindow.stream()
                        .filter(
                                t ->
                                        t.getAmountBaseCurrency().compareTo(minAmount) >= 0
                                                && t.getAmountBaseCurrency().compareTo(maxAmount) <= 0)
                        .toList();

        if (matching.size() < minCount) {
            return Optional.empty();
        }

        String explanation =
                "%d transactions on account %s within %dh, each between %s and %s (base currency) -- consistent with structuring just under the reporting threshold."
                        .formatted(
                                matching.size(),
                                transaction.getAccount().getAccountId(),
                                windowHours,
                                minAmount,
                                maxAmount);
        List<String> evidence = matching.stream().map(t -> t.getId().toString()).toList();

        return Optional.of(new RuleTrigger(RULE_CODE, ruleConfigService.weight(RULE_CODE), explanation, evidence));
    }
}
