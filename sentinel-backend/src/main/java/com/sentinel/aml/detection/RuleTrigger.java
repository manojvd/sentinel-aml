package com.sentinel.aml.detection;

import java.util.List;

/** The outcome of one {@link DetectionRule} firing against a transaction. */
public record RuleTrigger(
        String ruleCode, int riskContribution, String explanation, List<String> evidenceTransactionIds) {
}
