package com.sentinel.aml.detection;

import com.sentinel.aml.domain.Transaction;
import java.util.Optional;

/**
 * One AML typology check. Implementations are stateless Spring beans; all tunable thresholds are
 * read from {@link RuleConfigService} (backed by the {@code detection_rules} table) so behavior
 * can change without a redeploy.
 */
public interface DetectionRule {

    /** Must match a {@code rule_code} row in {@code detection_rules}. */
    String ruleCode();

    /**
     * Evaluate the rule against a newly-posted transaction, in the context of the account's/
     * customer's relevant history. Returns empty if the rule did not fire.
     */
    Optional<RuleTrigger> evaluate(Transaction transaction);
}
