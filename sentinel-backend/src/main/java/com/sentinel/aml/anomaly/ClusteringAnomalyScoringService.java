package com.sentinel.aml.anomaly;

import com.sentinel.aml.anomaly.dto.AnomalyScoreResult;
import com.sentinel.aml.anomaly.dto.TransactionFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import smile.clustering.KMeans;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Anomaly scoring backed by K-Means: transactions are clustered into behavioral
 * groups ("typical small retail transfer", "typical payroll credit", ...), and a new
 * transaction's anomaly score is its distance to the nearest cluster centroid. Points
 * far from every centroid don't resemble any typical behavior pattern.
 *
 * <p>Alternative to {@link IsolationForestAnomalyScoringService}, selected via
 * {@code sentinel.anomaly.algorithm=KMEANS_CLUSTERING}. Distance-based scoring is more
 * interpretable ("nearest known behavior is 4.2 std-devs away") but assumes roughly
 * convex clusters, which Isolation Forest doesn't.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sentinel.anomaly", name = "algorithm", havingValue = "KMEANS_CLUSTERING")
public class ClusteringAnomalyScoringService implements AnomalyScoringService {

    private final AnomalyScoringProperties properties;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile KMeans model;
    private volatile FeatureScaler scaler;
    private volatile ScoreDistribution distribution;
    private volatile int trainingSize;

    @Override
    public void train(List<TransactionFeatures> history) {
        if (history == null || history.isEmpty()) {
            throw new IllegalArgumentException("Cannot train an anomaly model on an empty history");
        }

        double[][] raw = history.stream().map(TransactionFeatures::toVector).toArray(double[][]::new);
        FeatureScaler newScaler = FeatureScaler.fit(raw);
        double[][] scaled = newScaler.transform(raw);

        int k = Math.min(properties.getKmeans().getClusters(), Math.max(1, scaled.length / 2));
        KMeans newModel = KMeans.fit(scaled, k);

        double[] trainingScores = new double[scaled.length];
        for (int i = 0; i < scaled.length; i++) {
            trainingScores[i] = distanceToNearestCentroid(newModel, scaled[i]);
        }
        ScoreDistribution newDistribution = ScoreDistribution.fit(trainingScores);

        lock.writeLock().lock();
        try {
            this.model = newModel;
            this.scaler = newScaler;
            this.distribution = newDistribution;
            this.trainingSize = history.size();
        } finally {
            lock.writeLock().unlock();
        }
        log.info("Trained K-Means anomaly model on {} samples ({} clusters)", history.size(), k);
    }

    @Override
    public AnomalyScoreResult score(TransactionFeatures features) {
        lock.readLock().lock();
        try {
            if (model == null) {
                throw new IllegalStateException("Anomaly model has not been trained yet");
            }
            double[] scaled = scaler.transform(features.toVector());
            double raw = distanceToNearestCentroid(model, scaled);
            double percentile = distribution.percentileOf(raw);
            boolean anomalous = percentile >= properties.getAnomalyPercentileThreshold();
            return new AnomalyScoreResult(percentile, anomalous, raw, algorithm());
        } finally {
            lock.readLock().unlock();
        }
    }

    private static double distanceToNearestCentroid(KMeans model, double[] point) {
        double best = Double.MAX_VALUE;
        for (double[] centroid : model.centroids) {
            double sumSq = 0.0;
            for (int j = 0; j < point.length; j++) {
                double d = point[j] - centroid[j];
                sumSq += d * d;
            }
            best = Math.min(best, Math.sqrt(sumSq));
        }
        return best;
    }

    @Override
    public boolean isTrained() {
        return model != null;
    }

    @Override
    public int trainingSize() {
        return trainingSize;
    }

    @Override
    public AnomalyAlgorithm algorithm() {
        return AnomalyAlgorithm.KMEANS_CLUSTERING;
    }
}
