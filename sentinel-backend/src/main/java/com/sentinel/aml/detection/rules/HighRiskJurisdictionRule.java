package com.sentinel.aml.detection.rules;

import com.sentinel.aml.detection.DetectionRule;
import com.sentinel.aml.detection.HighRiskJurisdictionService;
import com.sentinel.aml.detection.RuleConfigService;
import com.sentinel.aml.detection.RuleTrigger;
import com.sentinel.aml.domain.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Business rule 4: counterparty/jurisdiction on the high-risk list -- alerts regardless of amount. */
@Component
public class HighRiskJurisdictionRule implements DetectionRule {

    public static final String RULE_CODE = "HIGH_RISK_JURISDICTION";

    private final RuleConfigService ruleConfigService;
    private final HighRiskJurisdictionService highRiskJurisdictionService;

    public HighRiskJurisdictionRule(
            RuleConfigService ruleConfigService, HighRiskJurisdictionService highRiskJurisdictionService) {
        this.ruleConfigService = ruleConfigService;
        this.highRiskJurisdictionService = highRiskJurisdictionService;
    }

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public Optional<RuleTrigger> evaluate(Transaction transaction) {
        boolean flagged =
                highRiskJurisdictionService.isHighRisk(transaction.getJurisdiction())
                        || highRiskJurisdictionService.isHighRisk(transaction.getCounterpartyCountry());
        if (!flagged) {
            return Optional.empty();
        }

        String country =
                highRiskJurisdictionService.isHighRisk(transaction.getJurisdiction())
                        ? transaction.getJurisdiction()
                        : transaction.getCounterpartyCountry();

        String explanation =
                "Transaction involves jurisdiction/counterparty country %s, which is on the configurable high-risk list -- flagged regardless of amount."
                        .formatted(country);

        return Optional.of(
                new RuleTrigger(
                        RULE_CODE,
                        ruleConfigService.weight(RULE_CODE),
                        explanation,
                        List.of(transaction.getId().toString())));
    }
}
