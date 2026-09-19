package com.sentinel.aml.anomaly;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "sentinel.anomaly")
public class AnomalyScoringProperties {

    /** Which model backs the active {@link AnomalyScoringService} bean. */
    private AnomalyAlgorithm algorithm = AnomalyAlgorithm.ISOLATION_FOREST;

    /** Percentile (0-100) at/above which a score is flagged {@code anomalous}. */
    private double anomalyPercentileThreshold = 95.0;

    /** If true, trains the active model on synthetic data at startup so scoring works
     * immediately, ahead of the real ingestion/rule-engine pipeline being wired up. */
    private boolean bootstrapDemoData = true;

    private final IsolationForest isolationForest = new IsolationForest();
    private final KMeans kmeans = new KMeans();

    @Data
    public static class IsolationForest {
        private int trees = 150;
        private double subsampleRate = 0.8;
    }

    @Data
    public static class KMeans {
        private int clusters = 6;
    }
}
