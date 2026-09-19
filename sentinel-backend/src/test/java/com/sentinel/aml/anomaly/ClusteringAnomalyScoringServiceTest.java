package com.sentinel.aml.anomaly;

import com.sentinel.aml.anomaly.dto.AnomalyScoreResult;
import com.sentinel.aml.anomaly.dto.TransactionFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ClusteringAnomalyScoringServiceTest {

    private ClusteringAnomalyScoringService service;

    @BeforeEach
    void setUp() {
        service = new ClusteringAnomalyScoringService(new AnomalyScoringProperties());
    }

    @Test
    void scoreThrowsBeforeTraining() {
        assertThrows(IllegalStateException.class, () -> service.score(routineTransaction()));
    }

    @Test
    void flagsAPointFarFromEveryClusterAsAnomalous() {
        service.train(twoClusterHistory(300));

        AnomalyScoreResult routine = service.score(routineTransaction());
        AnomalyScoreResult outlier = service.score(farFromBothClusters());

        assertTrue(outlier.score() > routine.score());
        assertTrue(outlier.anomalous());
        assertEquals(AnomalyAlgorithm.KMEANS_CLUSTERING, outlier.algorithm());
    }

    private static List<TransactionFeatures> twoClusterHistory(int n) {
        Random random = new Random(11);
        List<TransactionFeatures> history = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            boolean clusterA = i % 2 == 0;
            double base = clusterA ? 1500 : 6000;
            double amount = base + random.nextGaussian() * 250;
            history.add(TransactionFeatures.builder()
                    .amount(amount)
                    .rollingAvgAmount90d(base)
                    .rollingAvgCount90d(4)
                    .txnCountLast24h(1)
                    .sumAmountLast24h(amount)
                    .pctOfDepositWithdrawnWithin48h(0.1)
                    .isHighRiskJurisdiction(0.0)
                    .isRoundNumberAmount(0.0)
                    .isJustBelowReportingThreshold(0.0)
                    .hourOfDay(clusterA ? 10 : 15)
                    .distinctCounterpartiesLast24h(1)
                    .accountAgeDays(900)
                    .build());
        }
        return history;
    }

    private static TransactionFeatures routineTransaction() {
        return TransactionFeatures.builder()
                .amount(1550)
                .rollingAvgAmount90d(1500)
                .rollingAvgCount90d(4)
                .txnCountLast24h(1)
                .sumAmountLast24h(1550)
                .pctOfDepositWithdrawnWithin48h(0.1)
                .isHighRiskJurisdiction(0.0)
                .isRoundNumberAmount(0.0)
                .isJustBelowReportingThreshold(0.0)
                .hourOfDay(10)
                .distinctCounterpartiesLast24h(1)
                .accountAgeDays(900)
                .build();
    }

    private static TransactionFeatures farFromBothClusters() {
        return TransactionFeatures.builder()
                .amount(90000)
                .rollingAvgAmount90d(1500)
                .rollingAvgCount90d(4)
                .txnCountLast24h(12)
                .sumAmountLast24h(90000)
                .pctOfDepositWithdrawnWithin48h(1.0)
                .isHighRiskJurisdiction(1.0)
                .isRoundNumberAmount(1.0)
                .isJustBelowReportingThreshold(0.0)
                .hourOfDay(3)
                .distinctCounterpartiesLast24h(9)
                .accountAgeDays(5)
                .build();
    }
}
