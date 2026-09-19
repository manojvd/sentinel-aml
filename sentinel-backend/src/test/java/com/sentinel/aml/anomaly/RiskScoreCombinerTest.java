package com.sentinel.aml.anomaly;

import com.sentinel.aml.anomaly.dto.AnomalyScoreResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskScoreCombinerTest {

    @Test
    void noRuleAndLowAnomalyScoreProducesNoAlert() {
        var result = RiskScoreCombiner.combine(
                0, false, new AnomalyScoreResult(40.0, false, 0.4, AnomalyAlgorithm.ISOLATION_FOREST));

        assertEquals(0, result.score());
    }

    @Test
    void noRuleButStrongOutlierStillSurfacesAnMlOnlyAlert() {
        var result = RiskScoreCombiner.combine(
                0, false, new AnomalyScoreResult(97.0, true, 0.9, AnomalyAlgorithm.ISOLATION_FOREST));

        assertTrue(result.score() > 0, "a strong statistical outlier should raise an alert even without a rule hit");
    }

    @Test
    void ruleTriggeredButStatisticallyRoutineIsDampenedToReduceFalsePositives() {
        var result = RiskScoreCombiner.combine(
                80, true, new AnomalyScoreResult(5.0, false, 0.1, AnomalyAlgorithm.KMEANS_CLUSTERING));

        assertTrue(result.score() < 80, "score should be dampened below the raw rule score");
        assertEquals(40, result.score());
    }

    @Test
    void ruleTriggeredAndCorroboratedByAnomalyModelBlendsTheTwoScores() {
        var result = RiskScoreCombiner.combine(
                80, true, new AnomalyScoreResult(90.0, true, 0.85, AnomalyAlgorithm.ISOLATION_FOREST));

        int expected = (int) Math.round(80 * 0.7 + 90 * 0.3);
        assertEquals(expected, result.score());
    }

    @Test
    void scoreIsAlwaysClampedToZeroToHundred() {
        var result = RiskScoreCombiner.combine(
                100, true, new AnomalyScoreResult(100.0, true, 1.0, AnomalyAlgorithm.ISOLATION_FOREST));

        assertTrue(result.score() <= 100);
    }
}
