package com.sentinel.aml.anomaly;

import com.sentinel.aml.anomaly.dto.AnomalyScoreResult;

/**
 * Blends a rule-engine risk score with an ML anomaly score into one alert score.
 * This is the integration point for the rule engine (Extension: "add an ML anomaly
 * scoring model to complement rule-based detection and reduce false positives") —
 * call {@link #combine} after rules have evaluated a transaction and before an Alert
 * is persisted, rather than persisting the raw rule score directly.
 *
 * <p>Policy:
 * <ul>
 *   <li>No rule triggered, but the transaction is a strong ML outlier: surface a
 *       lower-confidence, ML-only alert for a typology the rules don't cover yet,
 *       instead of staying silent.</li>
 *   <li>A rule triggered, but the transaction looks statistically routine for this
 *       customer/account: dampen the score — this is the false-positive reduction
 *       case (e.g. a customer whose baseline already includes large transfers
 *       tripping the flat $10,000 CTR rule).</li>
 *   <li>Otherwise: a weighted blend, rules-weighted, since rules carry an explicit,
 *       auditable "why" that a raw anomaly score doesn't.</li>
 * </ul>
 */
public final class RiskScoreCombiner {

    private static final double RULE_WEIGHT = 0.7;
    private static final double ANOMALY_WEIGHT = 0.3;
    private static final double ML_ONLY_ANOMALY_THRESHOLD = 90.0;
    private static final double DAMPEN_ANOMALY_CEILING = 20.0;
    private static final double DAMPEN_FACTOR = 0.5;

    private RiskScoreCombiner() {
    }

    /**
     * @param ruleScore      0-100 combined score from triggered rules, or 0 if none triggered.
     * @param ruleTriggered  whether at least one rule fired.
     * @param anomalyResult  output of {@link AnomalyScoringService#score}.
     * @return final 0-100 alert score plus a short rationale for the audit trail.
     */
    public static CompositeRiskResult combine(int ruleScore, boolean ruleTriggered, AnomalyScoreResult anomalyResult) {
        double anomalyScore = anomalyResult.score();

        if (!ruleTriggered) {
            if (anomalyScore >= ML_ONLY_ANOMALY_THRESHOLD) {
                return new CompositeRiskResult(
                        clamp((int) Math.round(anomalyScore)),
                        String.format(
                                "No rule triggered, but transaction is more anomalous than %.0f%% of this profile's history "
                                        + "(%s) — flagged for review as a novel pattern.",
                                anomalyScore, anomalyResult.algorithm()));
            }
            return new CompositeRiskResult(0, "No rule triggered and transaction is not a statistical outlier.");
        }

        if (anomalyScore <= DAMPEN_ANOMALY_CEILING) {
            int dampened = clamp((int) Math.round(ruleScore * DAMPEN_FACTOR));
            return new CompositeRiskResult(
                    dampened,
                    String.format(
                            "Rule(s) triggered (score %d), but transaction is statistically routine for this profile "
                                    + "(anomaly percentile %.0f) — score dampened to reduce false positives.",
                            ruleScore, anomalyScore));
        }

        int blended = clamp((int) Math.round(ruleScore * RULE_WEIGHT + anomalyScore * ANOMALY_WEIGHT));
        return new CompositeRiskResult(
                blended,
                String.format(
                        "Rule(s) triggered (score %d) and corroborated by anomaly model (percentile %.0f, %s) — blended score.",
                        ruleScore, anomalyScore, anomalyResult.algorithm()));
    }

    private static int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    /** @param score 0-100 final alert score. @param rationale human-readable explanation for the audit trail. */
    public record CompositeRiskResult(int score, String rationale) {
    }
}
