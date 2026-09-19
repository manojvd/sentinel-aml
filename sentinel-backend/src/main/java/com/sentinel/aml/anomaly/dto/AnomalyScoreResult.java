package com.sentinel.aml.anomaly.dto;

import com.sentinel.aml.anomaly.AnomalyAlgorithm;

/**
 * Result of scoring one {@link TransactionFeatures} against a trained anomaly model.
 *
 * @param score       0-100, higher = more anomalous relative to the training population
 *                    (it is the percentile rank of {@code rawScore} within the training set's
 *                    score distribution, so it is comparable across algorithms/retrains).
 * @param anomalous   true if {@code score} is at/above the configured percentile threshold.
 * @param rawScore    the algorithm-native score (e.g. Isolation Forest's [0,1] path-length score,
 *                    or the point's Euclidean distance to its nearest K-Means centroid).
 * @param algorithm   which model produced this result.
 */
public record AnomalyScoreResult(double score, boolean anomalous, double rawScore, AnomalyAlgorithm algorithm) {
}
