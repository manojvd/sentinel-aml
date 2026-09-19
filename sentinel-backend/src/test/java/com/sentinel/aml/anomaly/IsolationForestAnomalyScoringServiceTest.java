package com.sentinel.aml.anomaly;

import com.sentinel.aml.anomaly.dto.AnomalyScoreResult;
import com.sentinel.aml.anomaly.dto.TransactionFeatures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class IsolationForestAnomalyScoringServiceTest {

    private IsolationForestAnomalyScoringService service;

    @BeforeEach
    void setUp() {
        service = new IsolationForestAnomalyScoringService(new AnomalyScoringProperties());
    }

    @Test
    void scoreThrowsBeforeTraining() {
        assertFalse(service.isTrained());
        assertThrows(IllegalStateException.class, () -> service.score(routineTransaction()));
    }

    @Test
    void flagsAClearOutlierAsMoreAnomalousThanRoutineActivity() {
        service.train(routineHistory(400));
        assertTrue(service.isTrained());
        assertEquals(400, service.trainingSize());

        AnomalyScoreResult routine = service.score(routineTransaction());
        AnomalyScoreResult outlier = service.score(structuringLikeOutlier());

        assertTrue(outlier.score() > routine.score(),
                "outlier percentile (%s) should exceed a routine transaction's (%s)".formatted(outlier.score(), routine.score()));
        assertTrue(outlier.anomalous(), "a $48,000 high-risk-jurisdiction transfer should be flagged anomalous");
        assertEquals(AnomalyAlgorithm.ISOLATION_FOREST, outlier.algorithm());
    }

    @Test
    void retrainingReplacesThePreviousModel() {
        service.train(routineHistory(200));
        int firstSize = service.trainingSize();

        service.train(routineHistory(350));

        assertNotEquals(firstSize, service.trainingSize());
        assertEquals(350, service.trainingSize());
    }

    @Test
    void trainRejectsEmptyHistory() {
        assertThrows(IllegalArgumentException.class, () -> service.train(List.of()));
    }

    private static List<TransactionFeatures> routineHistory(int n) {
        Random random = new Random(7);
        List<TransactionFeatures> history = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            double amount = 2000 + random.nextGaussian() * 300;
            history.add(TransactionFeatures.builder()
                    .amount(amount)
                    .rollingAvgAmount90d(2000)
                    .rollingAvgCount90d(4)
                    .txnCountLast24h(1 + random.nextInt(2))
                    .sumAmountLast24h(amount)
                    .pctOfDepositWithdrawnWithin48h(0.1)
                    .isHighRiskJurisdiction(0.0)
                    .isRoundNumberAmount(0.0)
                    .isJustBelowReportingThreshold(0.0)
                    .hourOfDay(12)
                    .distinctCounterpartiesLast24h(1)
                    .accountAgeDays(900)
                    .build());
        }
        return history;
    }

    private static TransactionFeatures routineTransaction() {
        return TransactionFeatures.builder()
                .amount(2050)
                .rollingAvgAmount90d(2000)
                .rollingAvgCount90d(4)
                .txnCountLast24h(1)
                .sumAmountLast24h(2050)
                .pctOfDepositWithdrawnWithin48h(0.1)
                .isHighRiskJurisdiction(0.0)
                .isRoundNumberAmount(0.0)
                .isJustBelowReportingThreshold(0.0)
                .hourOfDay(12)
                .distinctCounterpartiesLast24h(1)
                .accountAgeDays(900)
                .build();
    }

    private static TransactionFeatures structuringLikeOutlier() {
        return TransactionFeatures.builder()
                .amount(48000)
                .rollingAvgAmount90d(2000)
                .rollingAvgCount90d(4)
                .txnCountLast24h(9)
                .sumAmountLast24h(48000)
                .pctOfDepositWithdrawnWithin48h(0.95)
                .isHighRiskJurisdiction(1.0)
                .isRoundNumberAmount(1.0)
                .isJustBelowReportingThreshold(0.0)
                .hourOfDay(3)
                .distinctCounterpartiesLast24h(7)
                .accountAgeDays(900)
                .build();
    }
}
