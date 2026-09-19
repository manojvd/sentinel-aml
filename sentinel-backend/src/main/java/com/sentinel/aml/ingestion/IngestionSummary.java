package com.sentinel.aml.ingestion;

import java.util.List;

public record IngestionSummary(int accepted, int rejected, List<String> errors, long elapsedMillis) {
}
