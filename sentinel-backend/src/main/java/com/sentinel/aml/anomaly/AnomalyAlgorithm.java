package com.sentinel.aml.anomaly;

/** ML strategies available for anomaly scoring, selected via {@code sentinel.anomaly.algorithm}. */
public enum AnomalyAlgorithm {
    ISOLATION_FOREST,
    KMEANS_CLUSTERING
}
