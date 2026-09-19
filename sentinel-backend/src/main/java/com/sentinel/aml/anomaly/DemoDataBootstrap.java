package com.sentinel.aml.anomaly;

import com.sentinel.aml.anomaly.dto.TransactionFeatures;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Seeds the active {@link AnomalyScoringService} with synthetic transaction history at
 * startup, so {@code POST /api/v1/anomaly/score} works out of the box for demos and
 * frontend integration before the real ingestion pipeline and rule engine exist. Once
 * that pipeline can supply real feature history, call {@code POST /api/v1/anomaly/train}
 * with it (or wire an equivalent scheduled retrain) and this bootstrap becomes a no-op
 * fallback for local/dev environments. Disable via {@code sentinel.anomaly.bootstrap-demo-data=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sentinel.anomaly", name = "bootstrap-demo-data", havingValue = "true", matchIfMissing = true)
public class DemoDataBootstrap implements ApplicationRunner {

    private static final int NORMAL_SAMPLES = 500;
    private final AnomalyScoringService anomalyScoringService;

    @Override
    public void run(ApplicationArguments args) {
        anomalyScoringService.train(generateSyntheticHistory());
        log.info("Anomaly model bootstrapped with {} synthetic samples ({}). Retrain with real "
                        + "history via POST /api/v1/anomaly/train once available.",
                NORMAL_SAMPLES, anomalyScoringService.algorithm());
    }

    /** Mostly routine retail-banking transactions, matching the amount/time distributions
     * a real customer base would show, so the model has a sensible "normal" to compare against. */
    private static List<TransactionFeatures> generateSyntheticHistory() {
        Random random = new Random(42);
        List<TransactionFeatures> history = new ArrayList<>(NORMAL_SAMPLES);

        for (int i = 0; i < NORMAL_SAMPLES; i++) {
            double avgAmount = 2000 + random.nextGaussian() * 800;
            double amount = Math.max(50, avgAmount + random.nextGaussian() * 400);
            history.add(TransactionFeatures.builder()
                    .amount(amount)
                    .rollingAvgAmount90d(Math.max(50, avgAmount))
                    .rollingAvgCount90d(3 + random.nextInt(5))
                    .txnCountLast24h(random.nextInt(4))
                    .sumAmountLast24h(amount * (1 + random.nextInt(3)))
                    .pctOfDepositWithdrawnWithin48h(Math.max(0, random.nextGaussian() * 0.15))
                    .isHighRiskJurisdiction(0.0)
                    .isRoundNumberAmount(amount % 500 < 1 ? 1.0 : 0.0)
                    .isJustBelowReportingThreshold(0.0)
                    .hourOfDay(9 + random.nextInt(9))
                    .distinctCounterpartiesLast24h(1 + random.nextInt(3))
                    .accountAgeDays(180 + random.nextInt(2000))
                    .build());
        }

        return history;
    }
}
