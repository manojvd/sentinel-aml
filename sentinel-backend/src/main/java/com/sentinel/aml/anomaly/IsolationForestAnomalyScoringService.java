package com.sentinel.aml.anomaly;

import com.sentinel.aml.anomaly.dto.AnomalyScoreResult;
import com.sentinel.aml.anomaly.dto.TransactionFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import smile.anomaly.IsolationForest;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Anomaly scoring backed by Smile's Isolation Forest (smile.anomaly.IsolationForest):
 * anomalies are, on average, isolated in fewer random splits than normal points, so
 * shorter average path length -> higher anomaly score. Good default because it needs
 * no distance metric/assumption about cluster shape, which suits the mixed
 * amount/flag/count features in {@link TransactionFeatures}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sentinel.anomaly", name = "algorithm", havingValue = "ISOLATION_FOREST", matchIfMissing = true)
public class IsolationForestAnomalyScoringService implements AnomalyScoringService {

    private final AnomalyScoringProperties properties;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile IsolationForest model;
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

        var props = properties.getIsolationForest();
        int maxDepth = (int) Math.ceil(Math.log(scaled.length) / Math.log(2));
        IsolationForest newModel = IsolationForest.fit(
                scaled, props.getTrees(), maxDepth, props.getSubsampleRate(), 0);

        double[] trainingScores = newModel.score(scaled);
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
        log.info("Trained Isolation Forest anomaly model on {} samples ({} trees)", history.size(), props.getTrees());
    }

    @Override
    public AnomalyScoreResult score(TransactionFeatures features) {
        lock.readLock().lock();
        try {
            if (model == null) {
                throw new IllegalStateException("Anomaly model has not been trained yet");
            }
            double[] scaled = scaler.transform(features.toVector());
            double raw = model.score(scaled);
            double percentile = distribution.percentileOf(raw);
            boolean anomalous = percentile >= properties.getAnomalyPercentileThreshold();
            return new AnomalyScoreResult(percentile, anomalous, raw, algorithm());
        } finally {
            lock.readLock().unlock();
        }
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
        return AnomalyAlgorithm.ISOLATION_FOREST;
    }
}
