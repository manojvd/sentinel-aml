package com.sentinel.aml.detection.rules;

import com.sentinel.aml.detection.DetectionRule;
import com.sentinel.aml.detection.RuleConfigService;
import com.sentinel.aml.detection.RuleTrigger;
import com.sentinel.aml.domain.Transaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Business rule 1: any single transaction >= the CTR-style reporting threshold. */
@Component
public class CtrThresholdRule implements DetectionRule {

    public static final String RULE_CODE = "CTR_THRESHOLD";

    private final RuleConfigService ruleConfigService;

    public CtrThresholdRule(RuleConfigService ruleConfigService) {
        this.ruleConfigService = ruleConfigService;
    }

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public Optional<RuleTrigger> evaluate(Transaction transaction) {
        BigDecimal minAmount = ruleConfigService.bigDecimal(RULE_CODE, "minAmountBase", new BigDecimal("10000"));
        if (transaction.getAmountBaseCurrency().compareTo(minAmount) < 0) {
            return Optional.empty();
        }
        String explanation =
                "Single transaction of %s %s (%s base-currency) meets or exceeds the CTR-style threshold of %s."
                        .formatted(
                                transaction.getAmount(),
                                transaction.getCurrency(),
                                transaction.getAmountBaseCurrency(),
                                minAmount);
        return Optional.of(
                new RuleTrigger(
                        RULE_CODE,
                        ruleConfigService.weight(RULE_CODE),
                        explanation,
                        List.of(transaction.getId().toString())));
    }
}
