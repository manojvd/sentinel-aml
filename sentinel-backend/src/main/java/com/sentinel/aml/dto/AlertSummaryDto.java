package com.sentinel.aml.dto;

import java.time.Instant;
import java.util.UUID;

public record AlertSummaryDto(
        UUID id,
        String customerId,
        String customerDisplayName,
        String accountId,
        int riskScore,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
