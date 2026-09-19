package com.sentinel.aml.anomaly;

import java.util.Arrays;

/**
 * Turns an algorithm-native raw anomaly score into a 0-100 percentile against the
 * distribution of raw scores seen on the training set. This is what lets
 * {@link AnomalyScoreResult} mean the same thing ("more anomalous than X% of this
 * customer base's history") regardless of whether Isolation Forest or K-Means produced
 * the raw number, and regardless of retrains shifting the raw scale.
 */
final class ScoreDistribution {

    private final double[] sortedTrainingScores;

    private ScoreDistribution(double[] sortedTrainingScores) {
        this.sortedTrainingScores = sortedTrainingScores;
    }

    static ScoreDistribution fit(double[] trainingScores) {
        double[] sorted = trainingScores.clone();
        Arrays.sort(sorted);
        return new ScoreDistribution(sorted);
    }

    /** Percentile (0-100) of {@code rawScore} within the training distribution. */
    double percentileOf(double rawScore) {
        int idx = Arrays.binarySearch(sortedTrainingScores, rawScore);
        int insertionPoint = idx >= 0 ? idx : -(idx + 1);
        return 100.0 * insertionPoint / sortedTrainingScores.length;
    }
}
