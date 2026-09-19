package com.sentinel.aml.anomaly;

import com.sentinel.aml.anomaly.dto.AnomalyScoreResult;
import com.sentinel.aml.anomaly.dto.TransactionFeatures;

import java.util.List;

/**
 * ML-based anomaly scoring, meant to run alongside (not instead of) the rule engine.
 * Rules catch known typologies with an explanation; this catches statistical outliers
 * rules don't have a name for yet, and can also be used to down-weight rule hits that
 * look routine for the customer, reducing false positives. See {@link RiskScoreCombiner}
 * for how the two are meant to be blended into one alert score.
 */
public interface AnomalyScoringService {

    /**
     * (Re)fits the model on historical transaction feature vectors. Expected to be mostly
     * "normal" activity; the model learns what typical looks like and scores deviations
     * from it. Safe to call again later to retrain as more history accumulates.
     */
    void train(List<TransactionFeatures> history);

    /** Scores a single transaction against the currently trained model. */
    AnomalyScoreResult score(TransactionFeatures features);

    /** Whether {@link #train} has been called successfully at least once. */
    boolean isTrained();

    /** Number of feature vectors the current model was trained on (0 if untrained). */
    int trainingSize();

    AnomalyAlgorithm algorithm();
}
