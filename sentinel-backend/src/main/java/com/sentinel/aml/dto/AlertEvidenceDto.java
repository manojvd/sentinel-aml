package com.sentinel.aml.dto;

import java.time.Instant;
import java.util.List;

public record AlertEvidenceDto(
        String ruleCode, int riskContribution, String explanation, List<String> evidenceTransactionIds, Instant createdAt) {
}
