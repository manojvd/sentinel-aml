package com.sentinel.aml.seed;

import com.sentinel.aml.ingestion.IngestionSummary;

public record DemoSeedSummary(
        IngestionSummary customers,
        IngestionSummary accounts,
        IngestionSummary transactions,
        long totalAlertsAfterSeed,
        long totalElapsedMillis) {
}
